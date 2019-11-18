package com.fantasy.androidmupdf.utils.net;

import android.util.Log;

import com.artifex.mupdf.viewer.Logger;
import com.fantasy.androidmupdf.model.BaseEnty;
import com.fantasy.androidmupdf.model.DocumentInfo;
import com.fantasy.androidmupdf.model.SignInfo;
import com.fantasy.androidmupdf.model.UserInfo;
import com.fantasy.androidmupdf.utils.LogUtils;
import com.fantasy.androidmupdf.utils.RetrofitManager;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import static com.fantasy.androidmupdf.utils.LogUtils.TAG;


public class HttpApiImp {

    public interface NetResponse<T> {
        void onError(final Throwable e);

        void onSuccess(T model);

        void onProgress(int progress);
    }

    private static Observer<BaseEnty> createResponse(final NetResponse netResponse) {
        Observer<BaseEnty> observer = new Observer<BaseEnty>() {
            @Override
            public void onSubscribe(Disposable d) {
                Logger.d("NetResponse", "onSubscribe==");
            }

            @Override
            public void onNext(BaseEnty value) {
                if (value.errorCode == 0)
                    netResponse.onSuccess(value);
                else {
                    Exception exception = new Exception(value.errorMsg);
                    netResponse.onError(exception);
                }
                Logger.d("NetResponse", "onNext==");
            }

            @Override
            public void onError(Throwable e) {
                netResponse.onError(e);
                Logger.d("NetResponse", "onError==");
            }

            @Override
            public void onComplete() {
                Logger.d("NetResponse", "onComplete==");
            }
        };
        return observer;
    }

    public static void Login(String userName, String psw, final NetResponse<UserInfo> netResponse) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userName", userName);
            jsonObject.put("userPassword", psw);
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString());
            Observable<UserInfo> infoObservable = RetrofitManager.getInstance().getService().login(requestBody);
            infoObservable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<UserInfo>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(UserInfo value) {
                            if (value.errorCode == 0)
                                netResponse.onSuccess(value);
                            else {
                                Exception exception = new Exception(value.errorMsg);
                                netResponse.onError(exception);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            netResponse.onError(e);
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void getSignPdfList(int userId, NetResponse<BaseEnty<List<DocumentInfo>>> netResponse) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", userId + "");
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString());
            Observable<BaseEnty<List<DocumentInfo>>> observable = RetrofitManager.getInstance().getService().getSignPdfList(requestBody);
            observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(createResponse(netResponse));
        } catch (Exception e) {
        }
    }

    public static void downloadPdf(final String downloadUrl, final String name, final NetResponse netResponse) {
        try {
//            JSONObject jsonObject = new JSONObject();
//            jsonObject.put("documentUrl", downloadUrl + "");
//            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString());
            Observable<ResponseBody> observable = RetrofitManager.getInstance().getService().downloadPdfFile(downloadUrl);
            observable.subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).flatMap(new Function<ResponseBody, ObservableSource<String>>() {
                @Override
                public ObservableSource<String> apply(ResponseBody responseBody) throws Exception {
                     writeFileSDcard(responseBody, new File(name));
                    return Observable.just(name);
                }
            }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<String>() {
                @Override
                public void accept(String body) throws Exception {
                    if(netResponse != null)
                        netResponse.onSuccess(body);
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) throws Exception {
                    if(netResponse != null)
                        netResponse.onError(throwable);
                }
            });
//            observable.subscribeOn(Schedulers.io()).subscribe(new Observer<ResponseBody>() {
//                @Override
//                public void onSubscribe(Disposable d) {
//
//                }
//
//                @Override
//                public void onNext(ResponseBody value) {
//                    writeFileSDcard(value, new File(name));
//                    Log.d(TAG, "onNext");
//                }
//
//                @Override
//                public void onError(Throwable e) {
//                    LogUtils.d("onError..." + e.getMessage());
//                }
//
//                @Override
//                public void onComplete() {
//                    Log.d(TAG, "onComplete");
//                }
//            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String writeFileSDcard(ResponseBody responseBody, File mFile) {
        Log.d(TAG, "writeFileSDcard");
        long currentLength = 0;
        OutputStream os = null;
        InputStream is = responseBody.byteStream();
        long totalLength = responseBody.contentLength();
        Log.d(TAG, "totalLength=" + totalLength);
        try {
            os = new FileOutputStream(mFile);
            int len;
            byte[] buff = new byte[1024];
            while ((len = is.read(buff)) != -1) {
                os.write(buff, 0, len);
                currentLength += len;
                Log.d(TAG, "当前长度: " + totalLength);
                int progress = (int) (100 * currentLength / totalLength);
                Log.d(TAG, "当前进度: " + progress);
//                downloadListener.onProgress(progress);
            }
//            downloadListener.onSuccess(mFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            Log.d(TAG, "Exception=" + e.getMessage());
//            downloadListener.onError(e);
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "Exception=" + e.getMessage());
//            downloadListener.onError(e);
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return mFile.getAbsolutePath();
    }

    public static void upLoadPdf(int userId, int documentId, String json, File file, NetResponse<BaseEnty<String>> netResponse) {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("userId", userId + "")
                .addFormDataPart("documentId", documentId + "")
                .addFormDataPart("signData", json);
        builder.addFormDataPart("file", file.getName(), RequestBody.create(MediaType.parse("image/*"), file));
        RequestBody requestBody = builder.build();
        Observable<BaseEnty<String>> observable = RetrofitManager.getInstance().getService().upLoadSignData(requestBody);
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(createResponse(netResponse));
    }

    public static void getSignedList(int userId, int documentId, NetResponse<BaseEnty<List<SignInfo>>> netResponse) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", userId + "");
            jsonObject.put("documentId", documentId + "");
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString());
            Observable<BaseEnty<List<SignInfo>>> observable = RetrofitManager.getInstance().getService().getSignInfo(requestBody);
            observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(createResponse(netResponse));
        } catch (Exception e) {
        }
    }
}
