package com.fantasy.androidmupdf.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class DocumentInfo extends RealmObject {
    @PrimaryKey
    public int documentId;
    public boolean sign;
    public String documentTitle;
    public String documentUrl;
    public String localPath;
    public String signPath;
    public String publishDate;
    public int userId;
}
