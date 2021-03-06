package com.fantasy.androidmupdf;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.fantasy.androidmupdf.adapter.SimpleCommonRVAdapter;
import com.fantasy.androidmupdf.model.BaseEnty;
import com.fantasy.androidmupdf.model.DocumentInfo;
import com.fantasy.androidmupdf.model.SignInfo;
import com.fantasy.androidmupdf.utils.SignFingerUtils;
import com.fantasy.androidmupdf.utils.net.HttpApiImp;
import com.yaohu.zhichuang.androidmupdf.R;

import java.io.File;
import java.util.List;

import io.realm.Realm;

public class DocumentListActivty extends BaseActivity {

    RecyclerView mListView;
    BaseEnty<List<DocumentInfo>> documentListBaseEnty;
    int userId;
    SimpleCommonRVAdapter mAdapter;
    Realm mRealm;
    private final int REQUEST_SIGN_CODE = 1002;
    private DocumentInfo mClickItem;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_list_activty);
        userId = getIntent().getIntExtra("userId", -1);
        mListView = findViewById(R.id.content_list);
        mListView.setLayoutManager(new LinearLayoutManager(getBaseContext()));
        mAdapter = new SimpleCommonRVAdapter<DocumentInfo>(getBaseContext(), null, R.layout.document_list_item) {
            @Override
            public void convert(SimpleViewHolder holder, final DocumentInfo item, int position) {
                holder.setText(R.id.document_title, item.documentTitle);
                holder.setText(R.id.document_time, item.publishDate);
                holder.setText(R.id.document_state, item.sign ? "已签" : "未签");
                holder.getView(R.id.document_state).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onItemClick(item);
                    }
                });
            }
        };
        mListView.setAdapter(mAdapter);
        mRealm = Realm.getDefaultInstance();
        loadData();
    }

    @Override
    public String getPageTitle() {
        return "签批文档列表";
    }

    public void onItemClick(final DocumentInfo item){
        mClickItem = item;
        if(item.sign){
            if(!TextUtils.isEmpty(item.signPath)) {
                File file = new File(item.signPath);
                if (file.exists() && file.length() > 0) {
                    startDcoment(item.signPath, item.documentId);
                    return;
                }
            }
            showLoading();
            HttpApiImp.getSignedList(userId, item.documentId, new HttpApiImp.NetResponse<BaseEnty<SignInfo>>() {
                @Override
                public void onError(Throwable e) {
                    Toast.makeText(getApplicationContext(), "文件下载失败，稍后重试", Toast.LENGTH_LONG).show();
                    hideLoading();
                }

                @Override
                public void onSuccess(final BaseEnty<SignInfo> model) {
                    mRealm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            realm.copyToRealmOrUpdate(model.data);
                        }
                    });
                    hideLoading();
                    downPdf(item, model.data);
                }

                @Override
                public void onProgress(int progress) {

                }
            });
        }else
            downPdf(item, null);
    }
    private void downPdf(final DocumentInfo item, SignInfo signInfo) {
        String localPath = item.sign || signInfo == null ? item.signPath : item.localPath;
        String urlPath = item.sign && signInfo != null ? signInfo.signaturePDFUrl : item.documentUrl;
        if(TextUtils.isEmpty(urlPath)){
            Toast.makeText(getApplicationContext(), "文件不存在", Toast.LENGTH_LONG).show();
            return;
        }
        final int documentId = item.documentId;
        if (TextUtils.isEmpty(localPath)) {
            String name = urlPath;
            //通过Url得到保存到本地的文件名
            int index = name.lastIndexOf('/');//一定是找最后一个'/'出现的位置
            if (index != -1) {
                name = name.substring(index);
                File sdcardDir = Environment.getExternalStorageDirectory();
                String path = sdcardDir.getPath() + "/zhiyuweilai/pdfdownload";
                name = path + name;
                isCanOpen(name, urlPath, documentId, item);
            }
        } else {
//            startDcoment(localPath, documentId);
            isCanOpen(localPath, urlPath, documentId, item);
        }
    }

    private void isCanOpen(String name, String urlPath, final int documentId, final DocumentInfo item){
        File file = new File(name);
        if (file.exists() && file.length() > 0) {
            startDcoment(name, documentId);
        } else {
            showLoading();
            FileUtils.createFile(name);
            HttpApiImp.downloadPdf(urlPath, name, new HttpApiImp.NetResponse<String>() {
                @Override
                public void onError(Throwable e) {
                    hideLoading();
                }

                @Override
                public void onSuccess(String model) {
                    mRealm.beginTransaction();
                    if (item.sign)
                        item.signPath = model;
                    else
                        item.localPath = model;
                    mRealm.copyToRealmOrUpdate(item);
                    mRealm.commitTransaction();
                    mAdapter.notifyDataSetChanged();
                    startDcoment(model, documentId);
                    hideLoading();
                }

                @Override
                public void onProgress(int progress) {

                }
            });
        }
    }

    private void startDcoment(String path, int doucmentid) {
        Intent intent = new Intent(DocumentListActivty.this, DocumentActivity.class);
        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT); // API>=21，launch as a new document
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET); // launch as a new document
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra("userId", userId);
        intent.putExtra("documentId", doucmentid);
        intent.putExtra("title", mClickItem.documentTitle);
        intent.setData(Uri.fromFile(new File(path)));
        //intent.setData(data.getData()); // 会报错
        //intent.setData(Uri.parse(path)); // 会报错
        startActivityForResult(intent, REQUEST_SIGN_CODE);
//        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_SIGN_CODE){
            if(resultCode == RESULT_OK) {
                String path = data.getStringExtra("path");
                mRealm.beginTransaction();
                mClickItem.sign = true;
                mClickItem.signPath = path;
                mRealm.copyToRealmOrUpdate(mClickItem);
                mRealm.commitTransaction();
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean isok = super.onCreateOptionsMenu(menu);
        mRefreshMenuItem.setVisible(true);
        return isok;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh)
            loadFromNet();
        return super.onOptionsItemSelected(item);
    }

    private void loadData() {
        List<DocumentInfo> documentInfos = ((SignApplication) getApplication()).getRealm()
                .where(DocumentInfo.class)
                .equalTo("userId", userId)
                .findAll();
        if (documentInfos == null || documentInfos.size() == 0) {
            loadFromNet();
        } else {
            if (documentListBaseEnty == null)
                documentListBaseEnty = new BaseEnty<>();
            documentListBaseEnty.data = documentInfos;
            mAdapter.setData(documentListBaseEnty.data);
        }
    }

    private void loadFromNet() {
        showLoading();
        HttpApiImp.getSignPdfList(userId, new HttpApiImp.NetResponse<BaseEnty<List<DocumentInfo>>>() {
            @Override
            public void onError(Throwable e) {
                hideLoading();
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess(BaseEnty<List<DocumentInfo>> model) {
                documentListBaseEnty = model;
                mAdapter.setData(documentListBaseEnty.data);
                hideLoading();
                mRealm.executeTransaction(
                        new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                for (DocumentInfo documentInfo : documentListBaseEnty.data) {
                                    documentInfo.userId = userId;
                                    realm.copyToRealmOrUpdate(documentInfo);
                                }
                            }
                        }
                );
            }

            @Override
            public void onProgress(int progress) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        mRealm.close();
        SignFingerUtils.getInstance().CloseDevice();
        super.onDestroy();
    }
}
