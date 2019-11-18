package com.fantasy.androidmupdf.utils.net;

import com.fantasy.androidmupdf.model.BaseEnty;
import com.fantasy.androidmupdf.model.DocumentInfo;
import com.fantasy.androidmupdf.model.UserInfo;

import java.util.List;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface HttpService {

    @POST("ZCSign/UserLogin")
    Observable<UserInfo> login(@Body RequestBody info);

    @POST("ZCSign/GetSignPDFList")
    Observable<BaseEnty<List<DocumentInfo>>> getSignPdfList(@Body RequestBody info);

    @POST("/ZCSign/GetSignedInfo")
    Observable<BaseEnty<DocumentInfo>> getSignInfo(@Query("userId") int userId, @Query("documentId") int documentId);


    @GET("ZCSign/DownloadPDF")
    Observable<ResponseBody> downloadPdfFile(@Query("documentUrl") String documentUrl);

//    @FieldMap
    @POST
    Observable<BaseEnty> upLoadSignData(@Body RequestBody body);
}
