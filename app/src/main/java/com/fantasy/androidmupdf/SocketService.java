package com.fantasy.androidmupdf;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.fantasy.androidmupdf.utils.LogUtils;

public class SocketService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SocketUtils.getInstance().startServerSocket();
        LogUtils.d("SocketService 。。。start");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SocketUtils.getInstance().startServerSocket();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SocketUtils.getInstance().stopServer();
    }
}
