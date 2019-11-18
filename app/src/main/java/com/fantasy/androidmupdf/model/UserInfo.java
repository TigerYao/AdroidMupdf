package com.fantasy.androidmupdf.model;

import android.text.TextUtils;

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

@RealmClass
public class UserInfo implements RealmModel {
    public int errorCode;
    public String errorMsg;
    @PrimaryKey
    public int userId;
    public String userName;
    public String psw;

    public UserInfo() {
    }

    public UserInfo(String userName, String psw) {
        this.userName = userName;
        this.psw = psw;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof UserInfo){
            UserInfo info = (UserInfo) obj;
            return info.userId == userId || (TextUtils.equals(userName, info.userName) && TextUtils.equals(psw, info.psw));
        }
        return super.equals(obj);
    }
}
