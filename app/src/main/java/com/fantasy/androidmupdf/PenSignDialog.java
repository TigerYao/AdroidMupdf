package com.fantasy.androidmupdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.zc_penutil_v6.Zc_Penutil;
import com.example.zc_penutil_v6.Zc_Penutil_Listen;
import com.fantasy.androidmupdf.view.DrawView;

public abstract class PenSignDialog extends BaseDialog {
    private Zc_Penutil zz;
    private boolean isOpenPen = false;
    private DrawView gameView;
    //事件监听
    Zc_Penutil_Listen listen=new Zc_Penutil_Listen() {
        @Override
        public void GetTrajectory(float[] arrays) {

            switch((int)arrays[0]){
                case 1:
                    //落笔
                    gameView.pendown((float) arrays[1],(float)arrays[2],(float)arrays[3]);
                     Log.i("落笔事件", "handleMessage: "+(short)arrays[1]+ "x坐标  "+(short)arrays[2]+"y坐标   "+(short)arrays[3]+"z坐标  ");
                    break;
                case 2:
                    //抬笔
                    gameView.penup();
                    break;
                case 3:
                    //移动事件
                    gameView.penmove((float) arrays[1],(float)arrays[2],(float)arrays[3]);
                     Log.i("移动事件", "handleMessage: "+(short)arrays[1]+ "x坐标  "+(short)arrays[2]+"y坐标   "+(short)arrays[3]+"z坐标  ");
                    break;
                case 4:
                    //超出区域事件
                    gameView.penmove((float) arrays[1],(float)arrays[2],(float)arrays[3]);
                     Log.i("移动事件", "handleMessage: "+(short)arrays[1]+ "x坐标  "+(short)arrays[2]+"y坐标   "+(short)arrays[3]+"z坐标  ");
                    break;
                default:
                    break;
            }

//            Log.i("AAAA", "GetTrajectory: 收到事件了"+arrays[0]+"事件名    "+arrays[1]+"x坐标    "+arrays[2]+"y坐标    "+arrays[3]+"z坐标    ");
        }
    };

    public PenSignDialog(Context context) {
        super(context, R.style.CustomProgressDialog2);
    }

    protected void initView(){
        mDialogView = mInflater.inflate(R.layout.layout_pen_write, null);
        gameView=(DrawView)mDialogView.findViewById(R.id.drawview);
        gameView.setZOrderOnTop(true);
        mDialogView.findViewById(R.id.clear_draw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gameView.clear();
            }
        });
        mDialogView.findViewById(R.id.save_draw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSign(gameView.getBitmapCache());
                dismiss();
            }
        });

        mDialogView.findViewById(R.id.cancel_draw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                gameView.clear();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mDialogView);
        zz = new Zc_Penutil(listen);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void show() {
        super.show();
        gameView.clear();
        mDialogView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!isShowing())
                    return;
                try {
                    openPen();
                }catch (Exception e){}
            }
        }, 500);

    }

    @Override
    public void dismiss() {
        super.dismiss();
        closePen();
    }

    public void openPen(){
        if(!isOpenPen){
            boolean flag=zz.Init_pen();
            if(flag){
                int[] location=new int[2];
                gameView.getLocationOnScreen(location);
                zz.SetRect(location[0],location[1],gameView.getWidth(),gameView.getHeight());
                //开启线程读取
                if(zz.Start_pen()){
                    isOpenPen=true;
                }else{
                    Log.i("cc", "Start_pen: 返回失败");
                }
            }
        }else{
            Toast.makeText(getContext().getApplicationContext(),"已开启签字",Toast.LENGTH_SHORT).show();
        }

    }

    public void closePen(){
        if(isOpenPen){
            //关闭线程读取
            if(zz.Close_pen()){
                isOpenPen=false;
            }else{
                Log.i("cc", "Close_pen: 返回失败");
            }
        }
    }

    abstract void saveSign(Bitmap bitmap);
}
