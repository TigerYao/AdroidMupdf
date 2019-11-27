package com.fantasy.androidmupdf.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

import com.yaohu.zhichuang.androidmupdf.R;

public class LoadingView extends ProgressDialog {
    TextView mLoadingTip;

    public LoadingView(Context context) {
        super(context);
    }

    public LoadingView(Context context, int theme) {
        super(context, theme);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(getContext());
    }

    private void init(Context context) {
        setCancelable(true);
        setCanceledOnTouchOutside(false);
        setContentView(R.layout.loading_view);//loading的xml文件
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.CENTER;
        getWindow().setAttributes(params);
        mLoadingTip = findViewById(R.id.tv_load_dialog);
    }

    @Override
    public void show() { // 显示Dialog
        super.show();
    }

    public void setLoadingTip(String tipmsg) {
        if(mLoadingTip != null && !TextUtils.isEmpty(tipmsg))
            mLoadingTip.setText(tipmsg);
    }

    @Override
    public void dismiss() { // 关闭Dialog
        super.dismiss();
    }
}