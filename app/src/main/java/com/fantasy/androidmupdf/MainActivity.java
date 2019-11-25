package com.fantasy.androidmupdf;

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

import com.artifex.mupdf.viewer.Logger;
import com.fantasy.androidmupdf.model.UserInfo;
import com.fantasy.androidmupdf.utils.DisplayUtil;
import com.fantasy.androidmupdf.utils.net.HttpApiImp;
import com.yaohu.zhichuang.androidmupdf.R;

import java.io.File;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * 使用 MuPDF 1.12.0 预览文件<br>
 * 支持的文件格式有：PDF、EPub、PNG、JPG、BMP、TIFF、GIF、SVG、CBZ、CBR、XPS
 * <pre>
 *     author  : Fantasy
 *     version : 1.0, 2018-04-01
 *     since   : 1.0, 2018-04-01
 * </pre>
 */
public class MainActivity extends BaseActivity {
//    private String mAppName;
//    Realm mRealm;
    EditText mUserNameEt;
    EditText mPswEt;
    List<UserInfo> userInfos;
    ViewGroup mRootView;
    ScrollView mScrollView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);
        mUserNameEt = findViewById(R.id.username_et);
        mPswEt = findViewById(R.id.psw_et);
        mRootView = findViewById(R.id.rootView);
        mScrollView = findViewById(R.id.scrollview);
        findViewById(R.id.btn_main).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 调用系统自带的文件选择器
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
            }
        });
         userInfos = Realm.getDefaultInstance().where(UserInfo.class).findAll();
         mScrollView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
             @Override
             public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                 if (oldBottom>bottom) {
                     mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
//                     mScrollView.scrollBy(0, (int)DisplayUtil.dp2px(-160, MainActivity.this));
                 } else if(bottom>oldBottom) {
//                     mScrollView.fullScroll(ScrollView.FOCUS_UP);
                 }
             }
         });
//        if(userInfos != null && userInfos.size() > 0){
//            Intent intent = new Intent(MainActivity.this, DocumentListActivty.class);
//            intent.putExtra("userId", userInfos.get(0).userId);
//            MainActivity.this.startActivity(intent);
//            finish();
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SELECT_FILE:
                if (resultCode == RESULT_OK) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_DENIED) {
                        showAlertDialog("打开文件失败，请允许 " + mAppName + " 读写手机储存。",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        ActivityCompat.requestPermissions(MainActivity.this,
                                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                PERMISSIONS_REQUEST_CODE_SELECT_FILE);
                                    }
                                });
                    } else {
                        // TODO 使用 MuPDF 打开文件
                        String path = getPath(MainActivity.this, data.getData());
                        if (path == null) {
                            return;
                        }
                        Intent intent = new Intent(MainActivity.this, DocumentActivity.class);
                        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT); // API>=21，launch as a new document
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET); // launch as a new document
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setData(Uri.fromFile(new File(path)));
                        //intent.setData(data.getData()); // 会报错
                        //intent.setData(Uri.parse(path)); // 会报错
                        startActivity(intent);
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE_ON_CREATE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showLongToast("您已允许 " + mAppName + " 读写手机存储");
                } else {
                    showLongToast("您已拒绝 " + mAppName + " 读写手机存储");
                }
                break;
            case PERMISSIONS_REQUEST_CODE_SELECT_FILE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showLongToast("您已允许 " + mAppName + " 读写手机存储，请重新选择文件");
                } else {
                    showLongToast("您已拒绝 " + mAppName + " 读写手机存储，文件打开失败");
                }
                break;
            default:
                break;
        }
    }

    /**
     * 获取文件选择器选中的文件绝对路径
     *
     * @param context 上下文
     * @param uri     文件URI
     * @return 文件绝对路径
     */
    private String getPath(Context context, Uri uri) {
        Log.e("PDF", uri.toString());

        return FileUtils.getPath(context, uri);
    }

    private void showLongToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }


    public void onClick(View view) {
        if (mUserNameEt.getText() == null || TextUtils.isEmpty(mUserNameEt.getText().toString()))
            return;
        String userNm = mUserNameEt.getText().toString();
        if (mPswEt.getText() == null || TextUtils.isEmpty(mPswEt.getText().toString()))
            return;
        showLoading();
        String psw = mPswEt.getText().toString();
        if(userInfos != null && userInfos.size() > 0) {
            UserInfo userInfo = new UserInfo(userNm, psw);
            int index = userInfos.indexOf(userInfo);
            if (index > -1) {
                userInfo = userInfos.get(index);
                Intent intent = new Intent(MainActivity.this, DocumentListActivty.class);
                intent.putExtra("userId", userInfo.userId);
                MainActivity.this.startActivity(intent);
                finish();
                return;
            }
        }
        HttpApiImp.NetResponse netResponse = new HttpApiImp.NetResponse<UserInfo>(){
            @Override
            public void onSuccess(final UserInfo model) {
                hideLoading();
                ((SignApplication)getApplication()).getRealm().executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        realm.copyToRealmOrUpdate(model);
                        Intent intent = new Intent(MainActivity.this, DocumentListActivty.class);
                        intent.putExtra("userId", model.userId);
                        MainActivity.this.startActivity(intent);
                        finish();
                    }
                });
                Logger.d("NetResponse", "onSuccess==" + model);
                Toast.makeText(getApplicationContext(), model.errorMsg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable e) {
                hideLoading();
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                Logger.d("NetResponse", "onError..."+e.getMessage());
            }

            @Override
            public void onProgress(int progress) {

            }
        };
        HttpApiImp.Login(userNm, psw, netResponse);
//        mRealm.where(UserInfo.class).equalTo("userName", mUserNameEt.getText().toString()).findAllAsync().addChangeListener(new RealmChangeListener<RealmResults<UserInfo>>() {
//            @Override
//            public void onChange(RealmResults<UserInfo> element) {
//                if (element != null && element.isLoaded()) {
//                    if (element.size() > 0) {
//                        UserInfo info = element.first();
//                        if (!TextUtils.equals(mPswEt.getText().toString(), info.psw))
//                            Toast.makeText(getBaseContext(), "密码错误", Toast.LENGTH_SHORT).show();
//                        else {
//                            startActivity(new Intent(getBaseContext(), DocumentListActivty.class));
//                            finish();
//                        }
//                    } else
//                        saveInfo();
//                }
//
//            }
//        });
    }

//    private void saveInfo() {
//        mRealm.executeTransactionAsync(new Realm.Transaction() {
//            @Override
//            public void execute(Realm realm) {
//                UserInfo userInfo = realm.createObject(UserInfo.class);
//                userInfo.userName = (mUserNameEt.getText().toString());
//                userInfo.psw = (mPswEt.getText().toString());
//            }
//        }, new Realm.Transaction.OnSuccess() {
//            @Override
//            public void onSuccess() {
//                Log.d("MainActivity", "onSuccess");
//                startActivity(new Intent(getBaseContext(), DocumentListActivty.class));
//                finish();
//            }
//        }, new Realm.Transaction.OnError() {
//            @Override
//            public void onError(Throwable error) {
//                Log.d("MainActivity", "onError");
//            }
//        });
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mRealm.removeAllChangeListeners();
//        mRealm.close();
    }
}
