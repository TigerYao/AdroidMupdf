package com.fantasy.androidmupdf.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class SignInfo extends RealmObject {
    public int documentId;
    public int userId;
    @PrimaryKey
    public int signatureId;
//    public String signatureData;
    public String signaturePDFUrl;
}
