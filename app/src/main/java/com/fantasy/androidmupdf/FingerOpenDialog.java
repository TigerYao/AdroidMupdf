package com.fantasy.androidmupdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import com.fantasy.androidmupdf.utils.SignFingerUtils;
import com.fantasy.androidmupdf.view.DrawView;

public class FingerOpenDialog extends BaseDialog {

    private DrawView gameView;
    private SignatureView mSignView;
    public FingerOpenDialog(@NonNull Context context) {
        super(context,R.style.CustomBottomDialog);
    }

    @Override
    protected void initView() {
        super.initView();
        mDialogView = mInflater.inflate(R.layout.layout_finger_dialog, null);
        gameView=(DrawView)mDialogView.findViewById(R.id.drawview);

        mSignView = mDialogView.findViewById(R.id.signview);
        mDialogView.findViewById(R.id.clear_draw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                gameView.clear();
                mSignView.clear();
            }
        });
        mDialogView.findViewById(R.id.save_draw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                saveSign(mSignView.getSignatureBitmap());
                gameView.clear();
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
        SignFingerUtils.getInstance().startFinger();
        SignFingerUtils.getInstance().setBitmapCreateListener(new SignFingerUtils.BitmapCreateListener() {
            @Override
            public void bitmapCreated(final Bitmap bitmap) {
                mDialogView.post(new Runnable() {
                    @Override
                    public void run() {
                        saveSign(bitmap);
                    }
                });
            }
        });
    }

    @Override
    public void dismiss() {
        super.dismiss();
        SignFingerUtils.getInstance().pauseFinger();
    }

    void saveSign(Bitmap bitmap){}
}
