package com.fantasy.androidmupdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;

import com.fantasy.androidmupdf.utils.BitmapUtil;
import com.fantasy.androidmupdf.utils.SignFingerUtils;
import com.fantasy.androidmupdf.utils.SoundUtils;
import com.fantasy.androidmupdf.view.DrawView;
import com.yaohu.zhichuang.androidmupdf.R;

public class FingerOpenDialog extends BaseDialog {

    private ImageView mSignView;
    private Bitmap mBmp;
    private SoundUtils soundUtils;
//    private SignatureView mSignView;
    public FingerOpenDialog(@NonNull Context context) {
        super(context,R.style.CustomBottomDialog);
    }

    @Override
    protected void initView() {
        super.initView();
        mDialogView = mInflater.inflate(R.layout.layout_finger_dialog, null);
        mSignView=(ImageView) mDialogView.findViewById(R.id.signview);
        mDialogView.findViewById(R.id.clear_draw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBmp = null;
                mSignView.setImageBitmap(null);
            }
        });
        mDialogView.findViewById(R.id.save_draw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                if(mBmp != null) {
                    Bitmap bitmap = BitmapUtil.getTransparentBitmap(mBmp, 50);
                    saveSign(bitmap);
                }
            }
        });

        mDialogView.findViewById(R.id.cancel_draw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mDialogView);
        SignFingerUtils.getInstance().OpenDeviceAndRequestDevice();
        SignFingerUtils.getInstance().setBitmapCreateListener(new SignFingerUtils.BitmapCreateListener() {
            @Override
            public void bitmapCreated(final Bitmap bitmap) {
                if(bitmap ==null)
                    return;
                mDialogView.post(new Runnable() {
                    @Override
                    public void run() {
                        if(bitmap != null) {
                            mBmp = bitmap;
                            mSignView.setImageBitmap(bitmap);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void show() {
        super.show();
        playTip();
        SignFingerUtils.getInstance().startFinger();
        mSignView.setImageBitmap(null);
    }

    @Override
    public void dismiss() {
        if(soundUtils != null)
            soundUtils.release();
        super.dismiss();
        SignFingerUtils.getInstance().pauseFinger();
    }

    void saveSign(Bitmap bitmap){}

    private void playTip(){
        if(soundUtils == null)
            soundUtils = new SoundUtils();
        soundUtils.playSound(getContext(), R.raw.finger_hint);
    }
}
