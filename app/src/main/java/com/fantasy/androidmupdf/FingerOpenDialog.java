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
import com.fantasy.androidmupdf.view.DrawView;

public class FingerOpenDialog extends BaseDialog {

    private ImageView mSignView;
    private Bitmap mBmp;
//    private SignatureView mSignView;
    public FingerOpenDialog(@NonNull Context context) {
        super(context,R.style.CustomBottomDialog);
    }

    @Override
    protected void initView() {
        super.initView();
        mDialogView = mInflater.inflate(R.layout.layout_finger_dialog, null);
        mSignView=(ImageView) mDialogView.findViewById(R.id.signview);

//        mSignView = mDialogView.findViewById(R.id.signview);
        mDialogView.findViewById(R.id.clear_draw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                gameView.clear();
                dismiss();
//                mSignView.clear();
            }
        });
        mDialogView.findViewById(R.id.save_draw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                Bitmap bitmap = BitmapUtil.getTransparentBitmap(mBmp, 50);
                saveSign(bitmap);
//                saveSign();
//                gameView.setImageBitmap(null);
//                saveSign(gameView.getBitmapCache());

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
//                        saveSign(bitmap);
                    }
                });
            }
        });
    }

    @Override
    public void show() {
        super.show();
        SignFingerUtils.getInstance().startFinger();
        mSignView.setImageBitmap(null);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        SignFingerUtils.getInstance().pauseFinger();
    }

    void saveSign(Bitmap bitmap){}
}
