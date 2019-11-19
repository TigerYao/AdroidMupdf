package com.fantasy.androidmupdf;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.fantasy.androidmupdf.view.LoadingView;
import com.yaohu.zhichuang.androidmupdf.R;

public class BaseActivity extends AppCompatActivity {
    LoadingView mLoadingView;
    MenuItem mRefreshMenuItem;
    ActionBar actionBar;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getActionBar();
        if(actionBar != null) actionBar.setTitle("智创网签系统");
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

}
