package com.fantasy.androidmupdf;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;

public class BaseDialog extends Dialog {
    protected LayoutInflater mInflater;
    protected View mDialogView;
    public BaseDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        mInflater = LayoutInflater.from(context);
        initView();
    }

    protected void initView() {

    }
}
