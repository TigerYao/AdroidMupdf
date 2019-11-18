package com.fantasy.androidmupdf.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class DocumentInfo extends RealmObject {
    @PrimaryKey
    public int documentId;
    public String uploadDate;
    public boolean sign;
    public String documentTitle;
    public String documentUrl;
    public String localPath;
    public String publishDate;//指纹数据
    public String signDataa;//笔迹数据
    public int userId;
}
