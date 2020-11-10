package com.fantasy.androidmupdf;

import android.text.TextUtils;
import android.util.Log;

import com.fantasy.androidmupdf.utils.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketUtils {
    private final static String TAG = "SocketUtils";
    private boolean mRunning = false;
    private final static int PORT = 33333;
    private ServerSocket mServerSocket = null;
    private static SocketUtils instance;
    ProcessClientRequestThread processClientRequestThread;
    public static SocketUtils getInstance(){
        if(instance == null)
            instance = new SocketUtils();
        return instance;
    }

    public void startServerSocket(){
        if(mRunning)
            return;
        new ServerThread().start();
    }

    public void sendMsg(String msg){
        if(mRunning && processClientRequestThread != null)
            processClientRequestThread.mSendMsg = msg;
    }

    private class ServerThread extends Thread {
        @Override
        public void run() {
            if (!mRunning) {
                try {
                    mServerSocket = new ServerSocket(PORT);
                    mServerSocket.setReuseAddress(true);
                    mRunning = true;
                    LogUtils.d();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                while (mRunning) {
                    if (mServerSocket == null) {
                        break;
                    }
                    Socket socket;
                    try {
                        if(mServerSocket.isBound() && !mServerSocket.isClosed()) {
                            socket = mServerSocket.accept();
                            processClientRequestThread = new ProcessClientRequestThread(socket);
                            processClientRequestThread.start();
                            LogUtils.d();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class ProcessClientRequestThread extends Thread {
        private Socket mSocket = null;
        InputStream in;
        OutputStream out;
        private String mSendMsg = null;
        ProcessClientRequestThread(Socket socket) {
            mSocket = socket;
        }

        @Override
        public void run() {
            while (mRunning) {
                if (!mSocket.isConnected()) {
                    break;
                }
                try {
                    in = mSocket.getInputStream();
                    out = mSocket.getOutputStream();

                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                InputStreamReader reader = new InputStreamReader(in);
                try {
                    char[] buf = new char[10240];
                    int cnt = reader.read(buf);
                    if (cnt > 0) {
                        String msg = new String(buf, 0, cnt);
                        Log.d(TAG, "Receive: " + msg);
                        if(TextUtils.equals(msg, "heart")){
                            out.write("reheart".getBytes());
                            out.flush();
                        }

                        if(mSendMsg != null) {
                            out.write(mSendMsg.getBytes());
                            out.flush();
                            mSendMsg = null;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public void stopServer() {
        if (mRunning) {
            mRunning = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mServerSocket.close();
                        mServerSocket = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}
