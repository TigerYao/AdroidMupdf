package com.fantasy.androidmupdf;

import android.app.Application;

import com.artifex.mupdf.viewer.SignAndFingerModel;
import com.fantasy.androidmupdf.utils.SignFingerUtils;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class SignApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .name("myrealm.realm") //文件名
                .schemaVersion(0) //版本号
                .build();
         Realm.setDefaultConfiguration(config);
        SignFingerUtils.getInstance().setContext(this);
    }

    public Realm getRealm() {
        return Realm.getDefaultInstance();
    }
}
