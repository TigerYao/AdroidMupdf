package com.fantasy.androidmupdf.utils;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import android.widget.Toast;

import com.zkteco.android.biometric.core.device.ParameterHelper;
import com.zkteco.android.biometric.core.device.TransportType;
import com.zkteco.android.biometric.core.utils.ToolUtils;
import com.zkteco.android.biometric.nidfpsensor.NIDFPFactory;
import com.zkteco.android.biometric.nidfpsensor.NIDFPSensor;
import com.zkteco.android.biometric.nidfpsensor.exception.NIDFPException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SignFingerUtils {
    private static final int VID = 6997;    //zkteco device VID  6997
    private static final int PID = 1023;    //NIDFPSensor PID 根据实际设置
    //测试模板，二代证采集的1024字节有2个模板
    private byte[] mTestFeature = new byte[512];
    //测试模板Base64字符串
    private String mstrTemplate = "QwH/EgFjSAAAAAAAAAAAAAAAACwBmmq0AP///////7QjTP5ZKRb8miux/JE4Yv5LP+L8RE8q/IJTV/7BV0H+cWIG/LtqlPxpcBT8i4FD/oCQO/45mS38d586/qGhKf5fsuv8arfv/JO3cPw2uD78b78B/qTGGf6SzBT+T9E8/GvR8/wo4j78b/Pu/K/0Ef4lCzD9oxEA/70WH/+MG+79ZB7i/a0hA/8sJSz9PCsq/VQ2Lv2+Pnj9jEI3/alFH/1QUhj9qlIA/YtXJv1pXCD9AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACpAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAJc=";

    private WorkThread mWorkThread = null;
    private boolean mbStop = true;
    private boolean mbStart = false;
    private NIDFPSensor mNIDFPSensor = null;
    private byte[] mBufImage;
    private CountDownLatch countdownLatch = new CountDownLatch(1);

    private Context mContext = null;
    private UsbManager musbManager = null;
    private final String ACTION_USB_PERMISSION = "com.zkteco.idfprdemo.USB_PERMISSION";

    private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openDevice();
                    } else {
                        Toast.makeText(mContext, "USB未授权", Toast.LENGTH_SHORT).show();
                        //mTxtReport.setText("USB未授权");
                    }
                }
            }
        }
    };

    private static SignFingerUtils mInstance;
    public  static SignFingerUtils getInstance(){
        if(mInstance == null)
            mInstance = new SignFingerUtils();
        return mInstance;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    private SignFingerUtils() {
    }

    public void OpenDeviceAndRequestDevice() {
        if (mbStart) {
//            mTxtReport.setText("设备已连接，请先断开连接");
            return;
        }
        musbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        mContext.registerReceiver(mUsbReceiver, filter);

        for (UsbDevice device : musbManager.getDeviceList().values()) {
            if (device.getVendorId() == VID && device.getProductId() == PID) {
                Intent intent = new Intent(ACTION_USB_PERMISSION);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
                musbManager.requestPermission(device, pendingIntent);
            }
        }
    }

    private void startIDFPFingerSensor() {
        // Define output log level
//        LogHelper.setLevel(Log.VERBOSE);
        // Start fingerprint sensor
        Map fingerprintParams = new HashMap();
        //set vid
        fingerprintParams.put(ParameterHelper.PARAM_KEY_VID, VID);
        //set pid
        fingerprintParams.put(ParameterHelper.PARAM_KEY_PID, PID);
        mNIDFPSensor = NIDFPFactory.createNIDFPSensor(mContext, TransportType.USBSCSI, fingerprintParams);
    }


    private class WorkThread extends Thread {

        @Override
        public void run() {
            super.run();
            mbStop = false;
            while (!mbStop && openFingDialog) {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    mNIDFPSensor.GetFPRawData(0, mBufImage);
                } catch (NIDFPException e) {
                    e.printStackTrace();
                    continue;
                }
                byte[] retQualityScore = new byte[1];
                int ret = mNIDFPSensor.getQualityScore(mBufImage, retQualityScore);
                final byte qualityScore = retQualityScore[0];
                if (1 != ret || qualityScore < 75) {
                    continue;
                }

                if(bitmapCreateListener != null){
                    Bitmap bitmap = ToolUtils.renderCroppedGreyScaleBitmap(mBufImage, mNIDFPSensor.getFpImgWidth(), mNIDFPSensor.getFpImgHeight());
                    bitmapCreateListener.bitmapCreated(bitmap);
                }
//                Activity.runOnUiThread(new Runnable() {
//                    public void run() {
//                        Bitmap bitmap = ToolUtils.renderCroppedGreyScaleBitmap(mBufImage, mNIDFPSensor.getFpImgWidth(), mNIDFPSensor.getFpImgHeight());
//                        mImgView.setImageBitmap(bitmap);
//                        mTxtStatus.setText("ImageQualityScore:" + qualityScore);
//                    }
//                });

                final float score = mNIDFPSensor.ImageMatch(0, mBufImage, mTestFeature);
//                runOnUiThread(new Runnable() {
//                    public void run() {
//                        mTxtReport.setText("score:" + score);
//                    }
//                });
            }
            countdownLatch.countDown();
            //Toast.makeText(mContext, "线程退出", Toast.LENGTH_SHORT).show();
        }
    }


    public boolean openFingDialog = false;

    public void startFinger(){
        openFingDialog = true;
        if(mWorkThread != null)
            mWorkThread.notify();
    }

    public void pauseFinger(){
        openFingDialog = false;
    }

    private void openDevice() {
        if (null == mNIDFPSensor) {
            startIDFPFingerSensor();
        }
        if (mbStart) {
            //mTxtReport.setText("设备已连接");
            Toast.makeText(mContext, "设备已连接", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            //连接设备
            //mNIDFPSensor.setEncryptFirmware(true);
            mNIDFPSensor.open(0);
            //分配读取指纹图像内存(width*height Bytes)
            mBufImage = new byte[mNIDFPSensor.getFpImgWidth() * mNIDFPSensor.getFpImgHeight()];
            //设置流模式探测（总是成功以及返回分数）
            mNIDFPSensor.setParameter(0, NIDFPSensor.PARAM_CODE_DETECT_FLAG, 0);
            //设置探测模式（按压一次手指显示采集一次成功）
            mNIDFPSensor.setParameter(0, NIDFPSensor.PARAM_CODE_DETECT_FLAG, 1);
            mWorkThread = new WorkThread();
            mWorkThread.start();// 线程启动
//            mTxtReport.setText("Open device succ");
            Toast.makeText(mContext, "设备连接成功，图像宽：" + mNIDFPSensor.getFpImgWidth() + ",图像高：" + mNIDFPSensor.getFpImgHeight(),
                    Toast.LENGTH_SHORT).show();
            mbStart = true;
        } catch (NIDFPException e) {
            e.printStackTrace();
            Toast.makeText(mContext, "Open device fail.errorcode:\"+ e.getErrorCode() + \"err message:\" + e.getMessage() + \"inner code:\" + e.getInternalErrorCode()", Toast.LENGTH_SHORT).show();
            //mTxtReport.setText("Open device fail.errorcode:"+ e.getErrorCode() + "err message:" + e.getMessage() + "inner code:" + e.getInternalErrorCode());
        }
    }

//    public void OnBnOpen(View view)
//    {
//        OpenDeviceAndRequestDevice();
//    }
//
//    public void OnBnVerify(View view)
//    {
//        /*
//        try {
//            mNIDFPSensor.GetFPRawData(0, mBufImage);
//        } catch (NIDFPException e) {
//            e.printStackTrace();
//            return;
//        }
//        mImgView.setImageBitmap(null);
//        byte[] retQualityScore = new byte[1];
//        int ret = mNIDFPSensor.getQualityScore(mBufImage, retQualityScore);
//        final byte qualityScore = retQualityScore[0];
//                Bitmap bitmap = ToolUtils.renderCroppedGreyScaleBitmap(mBufImage, mNIDFPSensor.getFpImgWidth(), mNIDFPSensor.getFpImgHeight());
//                mImgView.setImageBitmap(bitmap);
//                mTxtStatus.setText("ImageQualityScore:" + qualityScore);
//        if (1 !=  ret || qualityScore < 30)
//        {
//            return;
//        }
//        final float score = mNIDFPSensor.ImageMatch(0, mBufImage, mTestFeature);
//        mTxtReport.setText("score:" + score);
//        */
//    }


    public void CloseDevice() {
        if (mbStart) {
            mbStop = true;  //停止采集线程
            try {
                //等待线程退出，10S
                countdownLatch.await(10 * 1000, TimeUnit.SECONDS);
                mWorkThread.interrupt();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                mNIDFPSensor.close(0);  //关闭设备
            } catch (NIDFPException e) {
                e.printStackTrace();
            }
        }
        mbStart = false;
    }
    BitmapCreateListener bitmapCreateListener;

    public void setBitmapCreateListener(BitmapCreateListener bitmapCreateListener) {
        this.bitmapCreateListener = bitmapCreateListener;
    }

    public interface BitmapCreateListener{
        void bitmapCreated(Bitmap bitmap);
    }
}
