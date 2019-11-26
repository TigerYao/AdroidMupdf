package com.fantasy.androidmupdf.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.fantasy.androidmupdf.utils.SignFingerUtils;

import java.util.ArrayList;

/**
 * 执行画方法 继承surfaceview
 */
public class DrawView extends SurfaceView implements SurfaceHolder.Callback {
    private final static String TAG = "DrawView";
    private final static float penwidth = 1.1f;
    private float upx, upy, upz;
    private Paint paint = new Paint();
    private Path path = new Path();
    private int count, packCount;
    private Canvas canvas;
    private SurfaceHolder mSurfaceHolder;
    private Bitmap bitmapCache;
    private Thread CanvasDraw;

    //笔轨迹、笔压力
    private ArrayList<Path> pen_paths = new ArrayList<>();
    private ArrayList<Float> pen_widths = new ArrayList<>();
    //线程控制
    private boolean drawing = false;

    public DrawView(Context context) {
        super(context);
        //初始化
        initSurfaceView();

    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSurfaceView();

    }

    public DrawView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initSurfaceView();
    }

    void initSurfaceView() {
        //初始化画笔
        paint.setColor(Color.BLACK);
        //画笔设置
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStrokeWidth(penwidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);

        mSurfaceHolder = getHolder();
        mSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
//        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);
        openFingDialog = true;
    }

    //移动事件
    public void penmove(float x, float y, float z) {

        float zlx = (x + upx) / 2;
        float zly = (y + upy) / 2;
        float valupz = ((z / 100) / 2) * penwidth;

        //变化过大直接退出
        if (valupz < 0.2) {
            return;
        }
        /*float zlx=(x+upx)/2;
        float zly=(y+upy)/2;

        path.quadTo(upx,upy,zlx,zly)*/
        ;

        float valx = Math.abs(x - upx);
        float valy = Math.abs(y - upy);
        if (valx >= 3 || valy >= 3) {
            path.quadTo(upx, upy, zlx, zly);
        } else {
            path.lineTo(x, y);
        }


        if(upz!=valupz){

            /*if(valupz>upz){
                upz=upz/0.95f;
            }else{
                upz=upz*0.95f;
            }*/
        upz = valupz;
        pen_widths.add(valupz);
        pen_paths.add(path);
        path = new Path();
        path.moveTo(zlx, zly);
//        paint.setStrokeWidth(upz);
        }
        upx = x;
        upy = y;
    }

    //按下事件
    public void pendown(float x, float y, float z) {

        path.moveTo(x, y);
        upx = x;
        upy = y;
        upz = ((z / 100) / 2) * penwidth;
    }

    //抬起事件
    public void penup() {
        //初始化画笔宽度
        //pen_widths.add(upz);
        //pen_widths.clear();
        //pen_paths.clear();
        // pen_paths.add(path);
    }

    //清理画布
    public void clear() {
        //路径重置
        path.reset();
        pen_widths.clear();
        pen_paths.clear();
        packCount = 0;
        bitmapCache = null;
    }

    private boolean openFingDialog;
    public void openThread(){
        if(CanvasDraw != null && !openFingDialog) {
            openFingDialog = true;
            synchronized (CanvasDraw) {
                CanvasDraw.notifyAll();
            }
        }
    }

    public void closeThread(){
        if(CanvasDraw != null && openFingDialog)
//        try {
            openFingDialog = false;
//            synchronized (CanvasDraw) {
//                CanvasDraw.wait();
//            }
//        }catch (Exception e){}
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                pendown(event.getX(), event.getY(), 500);
//                break;
//            case MotionEvent.ACTION_MOVE:
//                penmove(event.getX(), event.getY(), 500);
//                break;
//            case MotionEvent.ACTION_UP:
//                penup();
//                break;
//        }
//        return true;
//    }

    //surfaceview 创建时
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //开启线程
        drawing = true;
        CanvasDraw = new Thread() {
            @Override
            public void run() {
                while (drawing) {
                    synchronized (CanvasDraw) {
                        try {
                            if (!openFingDialog) {
                                CanvasDraw.wait();
                                return;
                            }
                        } catch (Exception e) {
//
                        }
                        long start_time = System.currentTimeMillis();
                        count = pen_paths.size();
                        draw_Penpath();
                        long end_time = System.currentTimeMillis();

                        long value_time = end_time - start_time;
                        if (value_time < 30) {
                            try {
                                Thread.sleep(30 - (value_time));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        };
        CanvasDraw.start();
    }

    //surfaceview 发生改变时
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    //surfaceview 销毁时
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //关闭线程
        drawing = false;
    }


    private synchronized void draw_Penpath() {
        try {
            //获得canvas对象
            canvas = mSurfaceHolder.lockCanvas();
            //Log.i(TAG, "draw_Penpath: "+rect.toString());
            //绘制背景
            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);

            if (bitmapCache == null) {
                bitmapCache = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
//                Canvas canvas2 = new Canvas(bitmapCache);
//                canvas2.drawColor(Color.RED);
//                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                canvas.drawBitmap(bitmapCache, 0, 0, paint);
            } else {
                Canvas canvas1 = new Canvas(bitmapCache);
                //绘制路径  多重路径
                if (count == 0) {
                } else {
                    long start_time = System.currentTimeMillis();
                    while (packCount < count) {
                        paint.setStrokeWidth(pen_widths.get(packCount));
                        canvas1.drawPath(pen_paths.get(packCount), paint);
                        packCount++;
                    }
                    long end_time = System.currentTimeMillis();
                    //Log.i(TAG, "draw_Penpath:耗时"+(end_time-start_time));
                    //pen_widths.clear();
                    //pen_paths.clear();
                    if (!path.isEmpty()) {
                        paint.setStrokeWidth(upz);
                        canvas1.drawPath(path, paint);
                    }
                    paint.setStrokeWidth(0);
                }

                canvas.drawBitmap(bitmapCache, 0, 0, paint);
            }


        } catch (Exception e) {
            Log.i(TAG, e.toString());
        } finally {
            if (canvas != null) {
                //释放canvas对象并提交画布
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    public boolean isDraw(){
        return pen_paths!= null && pen_paths.size() > 0 && packCount > 0;
    }

    public Bitmap getBitmapCache() {
        return bitmapCache;
    }
}
