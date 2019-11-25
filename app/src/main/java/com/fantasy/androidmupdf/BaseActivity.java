package com.fantasy.androidmupdf;

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.fantasy.androidmupdf.view.LoadingView;
import com.yaohu.zhichuang.androidmupdf.R;

public class BaseActivity extends AppCompatActivity {
    // 下面是申请权限的请求码
    protected static final int PERMISSIONS_REQUEST_CODE_ON_CREATE = 0;
    protected static final int PERMISSIONS_REQUEST_CODE_SELECT_FILE = 1;

    protected static final int REQUEST_CODE_SELECT_FILE = 0;
    LoadingView mLoadingView;
    MenuItem mRefreshMenuItem;
    ActionBar actionBar;
    String mAppName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getActionBar();
        if(actionBar != null) {
            actionBar.setTitle(getPageTitle());
        }
        mAppName = getResources().getString(R.string.app_name);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            showAlertDialog("检测到您禁止 " + mAppName + " 读写手机存储，这将会导致无法正常" +
                    "读取本地文件。建议您允许读写手机存储。", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    ActivityCompat.requestPermissions(BaseActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSIONS_REQUEST_CODE_ON_CREATE);
                }
            });
        }
    }

    public String getPageTitle(){
        return "智创网签系统";
    }

    /**
     * 复写：添加菜单布局
     * */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_menu, menu);
        mRefreshMenuItem = menu.findItem(R.id.action_refresh);
        return true;
    }

    /**
     * 复写：设置菜单监听
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId){
            case R.id.action_delete:
                onBackPressed();
                break;
        }
        return true;
    }

    public void showLoading(){
        if (mLoadingView == null) {
            mLoadingView = new LoadingView(this, R.style.CustomProgressDialog2);
            mLoadingView.setCanceledOnTouchOutside(false);
        }
        mLoadingView.show(); // 显示
    }

    public void hideLoading(){
        if(mLoadingView != null && mLoadingView.isShowing())
            mLoadingView.dismiss();
    }

    /**
     * 提示对话框，带有“确定”按钮
     *
     * @param message  提示内容
     * @param listener “确定”按钮的点击监听器
     */
    protected void showAlertDialog(String message, DialogInterface.OnClickListener listener) {
        AlertDialog dialog = new AlertDialog.Builder(this).setMessage(message)
                .setPositiveButton("确定", listener).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

}
