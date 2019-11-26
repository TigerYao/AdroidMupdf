package com.fantasy.androidmupdf;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.artifex.mupdf.viewer.Logger;
import com.artifex.mupdf.viewer.MuPDFCore;
import com.artifex.mupdf.viewer.OutlineActivity;
import com.artifex.mupdf.viewer.PageAdapter;
import com.artifex.mupdf.viewer.PageView;
import com.artifex.mupdf.viewer.ReaderView;
import com.artifex.mupdf.viewer.SearchTask;
import com.artifex.mupdf.viewer.SearchTaskResult;
import com.artifex.mupdf.viewer.SignAndFingerModel;
import com.fantasy.androidmupdf.model.BaseEnty;
import com.fantasy.androidmupdf.utils.Base64BitmapUtil;
import com.fantasy.androidmupdf.utils.BitmapUtil;
import com.fantasy.androidmupdf.utils.PdfImgUtil;
import com.fantasy.androidmupdf.utils.SignFingerUtils;
import com.fantasy.androidmupdf.utils.net.HttpApiImp;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class DocumentActivity extends BaseActivity {
    /* The core rendering instance */
    enum TopBarMode {
        Main, Search, More
    }

    ;
    private final int OUTLINE_REQUEST = 0;
    private MuPDFCore core;
    private String mFileName;
    private String mFilePath;
    private ReaderView mDocView;
    private View mButtonsView;
    private boolean mButtonsVisible;
    private EditText mPasswordView;
    private TextView mFilenameView;
    private SeekBar mPageSlider;
    private int mPageSliderRes;
    private TextView mPageNumberView;
    private ImageButton mSearchButton;
    private ImageButton mOutlineButton;
    private ViewAnimator mTopBarSwitcher;
    private ImageButton mLinkButton;
    private DocumentActivity.TopBarMode mTopBarMode = DocumentActivity.TopBarMode.Main;
    private ImageButton mSearchBack;
    private ImageButton mSearchFwd;
    private ImageButton mSearchClose;
    private EditText mSearchText;
    private SearchTask mSearchTask;
    private AlertDialog.Builder mAlertBuilder;
    private boolean mLinkHighlight = false;
    private final Handler mHandler = new Handler();
    private boolean mAlertsActive = false;
    private AlertDialog mAlertDialog;
    private ArrayList<OutlineActivity.Item> mFlatOutline;
    public List<SignAndFingerModel> models;

    private MuPDFCore openFile(String path) {
        mFilePath = path;
        int lastSlashPos = path.lastIndexOf('/');
        mFileName = new String(lastSlashPos == -1 ? path : path.substring(lastSlashPos + 1));
        Logger.write(Logger.getTag(), "DocumentActivity Trying to open " + path);
        try {
            core = new MuPDFCore(path);
        } catch (Exception e) {
            Logger.write(Logger.getTag(), e, "DocumentActivity Trying to open");
            return null;
        } catch (java.lang.OutOfMemoryError e) {
            // out of memory is not an Exception, so we catch it separately.
            Logger.write(Logger.getTag(), e, "DocumentActivity out of memory");
            return null;
        }
        return core;
    }

    private MuPDFCore openBuffer(byte buffer[], String magic) {
        Logger.write(Logger.getTag(), "DocumentActivity Trying to open byte buffer");
        try {
            core = new MuPDFCore(buffer, magic);
        } catch (Exception e) {
            Logger.write(Logger.getTag(), e, "DocumentActivity openBuffer Exception");
            return null;
        }
        return core;
    }

    private int mUserId, mDocumentId;
    private String title;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mAlertBuilder = new AlertDialog.Builder(this);
        mUserId = getIntent().getIntExtra("userId", -1);
        mDocumentId = getIntent().getIntExtra("documentId", -1);
        title = getIntent().getStringExtra("title");
        if (core == null) {
            if (savedInstanceState != null && savedInstanceState.containsKey("FileName")) {
                mFileName = savedInstanceState.getString("FileName");
            }
        }
        if (core == null) {
            Intent intent = getIntent();
            byte buffer[] = null;

            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                Uri uri = intent.getData();
                Logger.write(Logger.getTag(), "DocumentActivity URI to open is : " + uri);
                if ("file".equals(uri.getScheme())) {
                    String path = uri.getPath();
                    core = openFile(path);
                } else {
                    try {
                        InputStream is = getContentResolver().openInputStream(uri);
                        int len;
                        ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
                        byte[] data = new byte[16384];
                        while ((len = is.read(data, 0, data.length)) != -1) {
                            bufferStream.write(data, 0, len);
                        }
                        bufferStream.flush();
                        buffer = bufferStream.toByteArray();
                        is.close();
                    } catch (IOException e) {
                        Logger.write(Logger.getTag(), e, "DocumentActivity onCreate IOException");
                        String reason = e.toString();
                        Resources res = getResources();
                        AlertDialog alert = mAlertBuilder.create();
                        alert.setMessage(String.format(Locale.ROOT,
                                res.getString(com.artifex.mupdf.R.string.cannot_open_document_Reason), reason));
                        alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(com.artifex.mupdf.R.string.dismiss),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                });
                        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                finish();
                            }
                        });
                        alert.setCanceledOnTouchOutside(false);
                        alert.show();
                        return;
                    }
                    core = openBuffer(buffer, intent.getType());
                }
                SearchTaskResult.set(null);
            }
            if (core != null && core.needsPassword()) {
                requestPassword(savedInstanceState);
                return;
            }
            if (core != null && core.countPages() == 0) {
                core = null;
            }
        }
        if (core == null) {
            AlertDialog alert = mAlertBuilder.create();
            alert.setMessage(getString(com.artifex.mupdf.R.string.cannot_open_document));
            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(com.artifex.mupdf.R.string.dismiss),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
            alert.setCanceledOnTouchOutside(false);
            alert.show();
            return;
        }

        createUI(savedInstanceState);
    }

    @Override
    public String getPageTitle() {
        return title;
    }

    public void requestPassword(final Bundle savedInstanceState) {
        mPasswordView = new EditText(this);
        mPasswordView.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
        mPasswordView.setTransformationMethod(new PasswordTransformationMethod());

        AlertDialog alert = mAlertBuilder.create();
        alert.setTitle(com.artifex.mupdf.R.string.enter_password);
        alert.setView(mPasswordView);
        alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(com.artifex.mupdf.R.string.okay),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (core.authenticatePassword(mPasswordView.getText().toString())) {
                            createUI(savedInstanceState);
                        } else {
                            requestPassword(savedInstanceState);
                        }
                    }
                });
        alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(com.artifex.mupdf.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }

    public void createUI(Bundle savedInstanceState) {
        if (core == null)
            return;

        // Now create the UI.
        // First create the document view
        mDocView = new ReaderView(this) {
            @Override
            protected void onMoveToChild(int i) {
                if (core == null)
                    return;

                mPageNumberView.setText(String.format(Locale.ROOT, "%d / %d", i + 1, core.countPages()));
                mPageSlider.setMax((core.countPages() - 1) * mPageSliderRes);
                mPageSlider.setProgress(i * mPageSliderRes);
                super.onMoveToChild(i);
            }

            @Override
            protected void onTapMainDocArea() {
                if (!mButtonsVisible) {
                    showButtons();
                } else {
                    if (mTopBarMode == TopBarMode.Main)
                        hideButtons();
                }
                dialogChoice();
            }

            @Override
            protected void onDocMotion() {
                hideButtons();
            }
        };

        mDocView.setAdapter(new PageAdapter(this, core));

        mSearchTask = new SearchTask(this, core) {
            @Override
            protected void onTextFound(SearchTaskResult result) {
                SearchTaskResult.set(result);
                // Ask the ReaderView to move to the resulting page
                mDocView.setDisplayedViewIndex(result.pageNumber);
                // Make the ReaderView act on the change to SearchTaskResult
                // via overridden onChildSetup method.
                mDocView.resetupChildren();
            }
        };

        // Make the buttons overlay, and store all its
        // controls in variables
        makeButtonsView();

        // Set up the page slider
        int smax = Math.max(core.countPages() - 1, 1);
        mPageSliderRes = ((10 + smax - 1) / smax) * 2;

        // Set the file-name text
        String docTitle = core.getTitle();
        if (docTitle != null)
            mFilenameView.setText(docTitle);
        else
            mFilenameView.setText(mFileName);

        // Activate the seekbar
        mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
                mDocView.pushHistory();
                mDocView.setDisplayedViewIndex((seekBar.getProgress() + mPageSliderRes / 2) / mPageSliderRes);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updatePageNumView((progress + mPageSliderRes / 2) / mPageSliderRes);
            }
        });

        // Activate the search-preparing button
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                searchModeOn();
            }
        });

        mSearchClose.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                searchModeOff();
            }
        });

        // Search invoking buttons are disabled while there is no text specified
        mSearchBack.setEnabled(false);
        mSearchFwd.setEnabled(false);
        mSearchBack.setColorFilter(Color.argb(255, 128, 128, 128));
        mSearchFwd.setColorFilter(Color.argb(255, 128, 128, 128));

        // React to interaction with the text widget
        mSearchText.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                boolean haveText = s.toString().length() > 0;
                setButtonEnabled(mSearchBack, haveText);
                setButtonEnabled(mSearchFwd, haveText);

                // Remove any previous search results
                if (SearchTaskResult.get() != null && !mSearchText.getText().toString().equals(SearchTaskResult.get().txt)) {
                    SearchTaskResult.set(null);
                    mDocView.resetupChildren();
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        //React to Done button on keyboard
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                    search(1);
                return false;
            }
        });

        mSearchText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER)
                    search(1);
                return false;
            }
        });

        // Activate search invoking buttons
        mSearchBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                search(-1);
            }
        });

        mSearchFwd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                search(1);
            }
        });

        mLinkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setLinkHighlight(!mLinkHighlight);
            }
        });

        if (core.hasOutline()) {
            mOutlineButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mFlatOutline == null)
                        mFlatOutline = core.getOutline();
                    if (mFlatOutline != null) {
                        Intent intent = new Intent(DocumentActivity.this, OutlineActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putInt("POSITION", mDocView.getDisplayedViewIndex());
                        bundle.putSerializable("OUTLINE", mFlatOutline);
                        intent.putExtras(bundle);
                        startActivityForResult(intent, OUTLINE_REQUEST);
                    }
                }
            });
        } else {
            mOutlineButton.setVisibility(View.GONE);
        }

        // Reenstate last state if it was recorded
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        mDocView.setDisplayedViewIndex(prefs.getInt("page" + mFileName, 0));

        if (savedInstanceState == null || !savedInstanceState.getBoolean("ButtonsHidden", false))
            showButtons();

        if (savedInstanceState != null && savedInstanceState.getBoolean("SearchMode", false))
            searchModeOn();

        // Stick the document view and the buttons overlay into a parent view
        RelativeLayout layout = new RelativeLayout(this);
        layout.setBackgroundColor(Color.DKGRAY);
        layout.addView(mDocView);
//        layout.addView(mButtonsView);
        setContentView(layout);
        mDocView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mDocView.refresh();
            }
        }, 500);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OUTLINE_REQUEST:
                if (resultCode >= RESULT_FIRST_USER) {
                    mDocView.pushHistory();
                    mDocView.setDisplayedViewIndex(resultCode - RESULT_FIRST_USER);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mFileName != null && mDocView != null) {
            outState.putString("FileName", mFileName);

            // Store current page in the prefs against the file name,
            // so that we can pick it up each time the file is loaded
            // Other info is needed only for screen-orientation change,
            // so it can go in the bundle
            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt("page" + mFileName, mDocView.getDisplayedViewIndex());
            edit.apply();
        }

        if (!mButtonsVisible)
            outState.putBoolean("ButtonsHidden", true);

        if (mTopBarMode == TopBarMode.Search)
            outState.putBoolean("SearchMode", true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mFingerDialog != null && mFingerDialog.isShowing())
            SignFingerUtils.getInstance().pauseFinger();
        if (mSearchTask != null)
            mSearchTask.stop();

        if (mFileName != null && mDocView != null) {
            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt("page" + mFileName, mDocView.getDisplayedViewIndex());
            edit.apply();
        }
    }

    public void onDestroy() {
        if (mDocView != null) {
            mDocView.applyToChildren(new ReaderView.ViewMapper() {
                public void applyToView(View view) {
                    ((PageView) view).releaseBitmaps();
                }
            });
        }
        if (core != null)
            core.onDestroy();
        core = null;
        super.onDestroy();
    }

    private void setButtonEnabled(ImageButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setColorFilter(enabled ? Color.argb(255, 255, 255, 255) : Color.argb(255, 128, 128, 128));
    }

    private void setLinkHighlight(boolean highlight) {
        mLinkHighlight = highlight;
        // LINK_COLOR tint
        mLinkButton.setColorFilter(highlight ? Color.argb(0xFF, 0x00, 0x66, 0xCC) : Color.argb(0xFF, 255, 255, 255));
        // Inform pages of the change.
        mDocView.setLinksEnabled(highlight);
    }

    private void showButtons() {
        if (core == null)
            return;
        if (!mButtonsVisible) {
            mButtonsVisible = true;
            // Update page number text and slider
            int index = mDocView.getDisplayedViewIndex();
            updatePageNumView(index);
            mPageSlider.setMax((core.countPages() - 1) * mPageSliderRes);
            mPageSlider.setProgress(index * mPageSliderRes);
            if (mTopBarMode == TopBarMode.Search) {
                mSearchText.requestFocus();
                showKeyboard();
            }

            Animation anim = new TranslateAnimation(0, 0, -mTopBarSwitcher.getHeight(), 0);
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mTopBarSwitcher.setVisibility(View.VISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                }
            });
            mTopBarSwitcher.startAnimation(anim);

            anim = new TranslateAnimation(0, 0, mPageSlider.getHeight(), 0);
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mPageSlider.setVisibility(View.VISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    mPageNumberView.setVisibility(View.VISIBLE);
                }
            });
            mPageSlider.startAnimation(anim);
        }
    }

    private void hideButtons() {
        if (mButtonsVisible) {
            mButtonsVisible = false;
            hideKeyboard();

            Animation anim = new TranslateAnimation(0, 0, 0, -mTopBarSwitcher.getHeight());
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    mTopBarSwitcher.setVisibility(View.INVISIBLE);
                }
            });
            mTopBarSwitcher.startAnimation(anim);

            anim = new TranslateAnimation(0, 0, 0, mPageSlider.getHeight());
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mPageNumberView.setVisibility(View.INVISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    mPageSlider.setVisibility(View.INVISIBLE);
                }
            });
            mPageSlider.startAnimation(anim);
        }
    }

    private void searchModeOn() {
        if (mTopBarMode != TopBarMode.Search) {
            mTopBarMode = TopBarMode.Search;
            //Focus on EditTextWidget
            mSearchText.requestFocus();
            showKeyboard();
            mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        }
    }

    private void searchModeOff() {
        if (mTopBarMode == TopBarMode.Search) {
            mTopBarMode = TopBarMode.Main;
            hideKeyboard();
            mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
            SearchTaskResult.set(null);
            // Make the ReaderView act on the change to mSearchTaskResult
            // via overridden onChildSetup method.
            mDocView.resetupChildren();
        }
    }

    private void updatePageNumView(int index) {
        if (core == null)
            return;
        mPageNumberView.setText(String.format(Locale.ROOT, "%d / %d", index + 1, core.countPages()));
    }

    private void makeButtonsView() {
        mButtonsView = getLayoutInflater().inflate(com.artifex.mupdf.R.layout.document_activity, null);
        mFilenameView = (TextView) mButtonsView.findViewById(com.artifex.mupdf.R.id.docNameText);
        mPageSlider = (SeekBar) mButtonsView.findViewById(com.artifex.mupdf.R.id.pageSlider);
        mPageNumberView = (TextView) mButtonsView.findViewById(com.artifex.mupdf.R.id.pageNumber);
        mSearchButton = (ImageButton) mButtonsView.findViewById(com.artifex.mupdf.R.id.searchButton);
        mOutlineButton = (ImageButton) mButtonsView.findViewById(com.artifex.mupdf.R.id.outlineButton);
        mTopBarSwitcher = (ViewAnimator) mButtonsView.findViewById(com.artifex.mupdf.R.id.switcher);
        mSearchBack = (ImageButton) mButtonsView.findViewById(com.artifex.mupdf.R.id.searchBack);
        mSearchFwd = (ImageButton) mButtonsView.findViewById(com.artifex.mupdf.R.id.searchForward);
        mSearchClose = (ImageButton) mButtonsView.findViewById(com.artifex.mupdf.R.id.searchClose);
        mSearchText = (EditText) mButtonsView.findViewById(com.artifex.mupdf.R.id.searchText);
        mLinkButton = (ImageButton) mButtonsView.findViewById(com.artifex.mupdf.R.id.linkButton);
//        mTopBarSwitcher.setVisibility(View.INVISIBLE);
//        mPageNumberView.setVisibility(View.INVISIBLE);
//        mPageSlider.setVisibility(View.INVISIBLE);
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.showSoftInput(mSearchText, 0);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
    }

    private void search(int direction) {
        hideKeyboard();
        int displayPage = mDocView.getDisplayedViewIndex();
        SearchTaskResult r = SearchTaskResult.get();
        int searchPage = r != null ? r.pageNumber : -1;
        mSearchTask.go(mSearchText.getText().toString(), direction, displayPage, searchPage);
    }

    @Override
    public boolean onSearchRequested() {
        if (mButtonsVisible && mTopBarMode == TopBarMode.Search) {
            hideButtons();
        } else {
            showButtons();
            searchModeOn();
        }
        return super.onSearchRequested();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mButtonsVisible && mTopBarMode != TopBarMode.Search) {
            hideButtons();
        } else {
            showButtons();
            searchModeOff();
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mFingerDialog != null && mFingerDialog.isShowing())
            SignFingerUtils.getInstance().startFinger();
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (models != null && !models.isEmpty()) {
//            Intent intent = new Intent();
//            intent.putExtra("path", mFilePath);
//            setResult(RESULT_OK, intent);
//            finish();
//            new AlertDialog.Builder(this, 0).setMessage("签字完成，是否确认保存？").setPositiveButton("确认", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
            showLoading();
            HttpApiImp.upLoadPdf(mUserId, mDocumentId, new Gson().toJson(models), new File(mFilePath), new HttpApiImp.NetResponse<BaseEnty<String>>() {
                @Override
                public void onError(Throwable e) {
                    hideLoading();
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "失败，稍后重试", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(BaseEnty<String> model) {
                    hideLoading();
                    try {
                        new File(mFilePath).delete();
                    }catch (Exception e){}
                    Toast.makeText(getApplicationContext(), "上传成功", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onProgress(int progress) {

                }
            });
//                }
//            }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
//                    finish();
//                }
//            }).show();
////            HttpApiImp.upLoadPdf();
        } else
//        if (!mDocView.popHistory())
            super.onBackPressed();
    }

    private AlertDialog mSelectDialog;

    /**
     * 单选
     */
    private void dialogChoice() {
        if (mSelectDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, 0);
            builder.setTitle("单选");
            builder.setSingleChoiceItems(com.yaohu.zhichuang.androidmupdf.R.array.operate_list, 0, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mSelectPositon = i;
                    switch (i) {
                        case 0:
                        case 2:
                            showPenSignDialog();
                            break;
                        case 1:
                            showFingerSignDialog();
                            break;
                    }
                    dialogInterface.dismiss();
                }
            });
//            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    dialog.dismiss();
//                }
//            });
            mSelectDialog = builder.create();
        }
        mSelectDialog.show();
    }

    private PenSignDialog mPenDialog;
    private FingerOpenDialog mFingerDialog;
    private int mSelectPositon = -1;

    /**
     * 签字
     */
    private void showPenSignDialog() {
        if (mPenDialog == null)
            mPenDialog = new PenSignDialog(this) {
                @Override
                void saveSign(Bitmap bitmap) {
                    if (mSelectPositon == 2) {
                        showFingerSignDialog();
                    }
                    createPdfImg(bitmap, mSelectPositon != 2, 0);
                }
            };
        mPenDialog.show();
    }

    /**
     * 指纹
     */
    private void showFingerSignDialog() {
        if (mFingerDialog == null)
            mFingerDialog = new FingerOpenDialog(this) {
                @Override
                void saveSign(Bitmap bitmap) {
                    createPdfImg(bitmap, true, 1);
                }
            };
        mFingerDialog.show();
    }

    private void createPdfImg(Bitmap bitmap, boolean save, int type) {
        if (models == null)
            models = new ArrayList<>();
        PageView pageView = (PageView) mDocView.getDisplayedView();
        bitmap = BitmapUtil.scaleBitmap(bitmap, 1 / pageView.getScale());
        SignAndFingerModel model = new SignAndFingerModel();
        model.type = type;
        model.data = Base64BitmapUtil.bitmapToBase64(bitmap);
        model = pageView.addSignImg(model);
        model.dataTime = System.currentTimeMillis() + "";
        models.add(model);
        if (save)
            onSaveBtm(pageView.models);
    }

    private void onSaveBtm(final List<SignAndFingerModel> pageModels) {
        showLoading();
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> e) throws Exception {
                String outString = PdfImgUtil.addText(DocumentActivity.this, pageModels, mFilePath, mFilePath.replace(".pdf", "_c.pdf"));
                try {
                    new File(mFilePath).delete();
                }catch (Exception es){}
                e.onNext(outString);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(String value) {
                if (value != null) {
                    openFile(value);
                    createUI(null);
                }
                hideLoading();

            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                hideLoading();
            }

            @Override
            public void onComplete() {
                hideLoading();
            }
        });
    }
}
//    /* The core rendering instance */
//    enum TopBarMode {
//        Main, Search, More
//    }
//
//    ;
//
//    SignatureView mSignaturePad;
//    private final int OUTLINE_REQUEST = 0;
//    private MuPDFCore core;
//    private String mFileName;
//    private ReaderView mDocView;
//    private View mButtonsView;
//    private boolean mButtonsVisible;
//    private EditText mPasswordView;
//    private TextView mFilenameView;
//    private SeekBar mPageSlider;
//    private int mPageSliderRes;
//    private TextView mPageNumberView;
//    private ImageButton mSearchButton;
//    private ImageButton mOutlineButton;
//    private ViewAnimator mTopBarSwitcher;
//    private ImageButton mLinkButton;
//    private TopBarMode mTopBarMode = TopBarMode.Main;
//    private ImageButton mSearchBack;
//    private ImageButton mSearchFwd;
//    private ImageButton mSearchClose;
//    private ImageButton mClearSign;
//    private ImageButton mSaveSign;
//    private EditText mSearchText;
//    private SearchTask mSearchTask;
//    private AlertDialog.Builder mAlertBuilder;
//    private boolean mLinkHighlight = false;
//    private final Handler mHandler = new Handler();
//    private boolean mAlertsActive = false;
//    private AlertDialog mAlertDialog;
//    private ArrayList<OutlineActivity.Item> mFlatOutline;
//    private HashMap<Integer, List<TimedPoint>> mPoints = new HashMap<>();
//    private HashMap<Integer, String> mBitmpList = new HashMap<>();
//
//    private MuPDFCore openFile(String path) {
//        int lastSlashPos = path.lastIndexOf('/');
//        mFileName = new String(lastSlashPos == -1 ? path : path.substring(lastSlashPos + 1));
//        Logger.write(Logger.getTag(), "DocumentActivity Trying to open " + path);
//        try {
//            core = new MuPDFCore(path);
//        } catch (Exception e) {
//            Logger.write(Logger.getTag(), e, "DocumentActivity Trying to open");
//            return null;
//        } catch (java.lang.OutOfMemoryError e) {
//            // out of memory is not an Exception, so we catch it separately.
//            Logger.write(Logger.getTag(), e, "DocumentActivity out of memory");
//            return null;
//        }
//        return core;
//    }
//
//    private MuPDFCore openBuffer(byte buffer[], String magic) {
//        Logger.write(Logger.getTag(), "DocumentActivity Trying to open byte buffer");
//        try {
//            core = new MuPDFCore(buffer, magic);
//        } catch (Exception e) {
//            Logger.write(Logger.getTag(), e, "DocumentActivity openBuffer Exception");
//            return null;
//        }
//        return core;
//    }
//
//    /**
//     * Called when the activity is first created.
//     */
//    @Override
//    public void onCreate(final Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//
//        mAlertBuilder = new AlertDialog.Builder(this);
//
//        if (core == null) {
//            if (savedInstanceState != null && savedInstanceState.containsKey("FileName")) {
//                mFileName = savedInstanceState.getString("FileName");
//            }
//        }
//        if (core == null) {
//            Intent intent = getIntent();
//            byte buffer[] = null;
//
//            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
//                Uri uri = intent.getData();
//                Logger.write(Logger.getTag(), "DocumentActivity URI to open is : " + uri);
//                if ("file".equals(uri.getScheme())) {
//                    String path = mFileName = uri.getPath();
//                    core = openFile(path);
//                } else {
//                    try {
//                        mFileName = FileUtils.getPath(DocumentActivity.this, uri);
//                        InputStream is = getContentResolver().openInputStream(uri);
//                        int len;
//                        ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
//                        byte[] data = new byte[16384];
//                        while ((len = is.read(data, 0, data.length)) != -1) {
//                            bufferStream.write(data, 0, len);
//                        }
//                        bufferStream.flush();
//                        buffer = bufferStream.toByteArray();
//                        is.close();
//                    } catch (IOException e) {
//                        Logger.write(Logger.getTag(), e, "DocumentActivity onCreate IOException");
//                        String reason = e.toString();
//                        Resources res = getResources();
//                        AlertDialog alert = mAlertBuilder.create();
//                        alert.setMessage(String.format(Locale.ROOT,
//                                res.getString(R.string.cannot_open_document_Reason), reason));
//                        alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
//                                new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        finish();
//                                    }
//                                });
//                        alert.setOnCancelListener(new OnCancelListener() {
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                finish();
//                            }
//                        });
//                        alert.setCanceledOnTouchOutside(false);
//                        alert.show();
//                        return;
//                    }
//                    core = openBuffer(buffer, intent.getType());
//                }
//                SearchTaskResult.set(null);
//            }
//            if (core != null && core.needsPassword()) {
//                requestPassword(savedInstanceState);
//                return;
//            }
//            if (core != null && core.countPages() == 0) {
//                core = null;
//            }
//        }
//        if (core == null) {
//            AlertDialog alert = mAlertBuilder.create();
//            alert.setMessage(getString(R.string.cannot_open_document));
//            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
//                    new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int which) {
//                            finish();
//                        }
//                    });
//            alert.setOnCancelListener(new OnCancelListener() {
//                @Override
//                public void onCancel(DialogInterface dialog) {
//                    finish();
//                }
//            });
//            alert.setCanceledOnTouchOutside(false);
//            alert.show();
//            return;
//        }
//
//        createUI(savedInstanceState);
//    }
//
//    public void requestPassword(final Bundle savedInstanceState) {
//        mPasswordView = new EditText(this);
//        mPasswordView.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
//        mPasswordView.setTransformationMethod(new PasswordTransformationMethod());
//
//        AlertDialog alert = mAlertBuilder.create();
//        alert.setTitle(R.string.enter_password);
//        alert.setView(mPasswordView);
//        alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.okay),
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        if (core.authenticatePassword(mPasswordView.getText().toString())) {
//                            createUI(savedInstanceState);
//                        } else {
//                            requestPassword(savedInstanceState);
//                        }
//                    }
//                });
//        alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        finish();
//                    }
//                });
//        alert.setOnCancelListener(new OnCancelListener() {
//            @Override
//            public void onCancel(DialogInterface dialog) {
//                finish();
//            }
//        });
//        alert.setCanceledOnTouchOutside(false);
//        alert.show();
//    }
//
//    public void createUI(Bundle savedInstanceState) {
//        if (core == null)
//            return;
//
//        // Now create the UI.
//        // First create the document view
//        mDocView = new ReaderView(this) {
//            @Override
//            protected void onMoveToChild(int i) {
//                Logger.d("MuPDF", "moveto===index==="+i);
//                if (core == null)
//                    return;
//
//                mPageNumberView.setText(String.format(Locale.ROOT, "%d / %d", i + 1, core.countPages()));
//                mPageSlider.setMax((core.countPages() - 1) * mPageSliderRes);
//                mPageSlider.setProgress(i * mPageSliderRes);
//                super.onMoveToChild(i);
//                if (mSignaturePad == null)
//                    return;
//
////                if (mPoints.get(i) != null && !mPoints.get(i).isEmpty()) {
////                    List<TimedPoint> timedPoints = new ArrayList<>(mPoints.get(i));
////                    mSignaturePad.clear();
////                    mSignaturePad.getSavedPointsCache().addAll(timedPoints);
////                    mSignaturePad.reDraw(0);
////                } else if (!mSignaturePad.getSavedPointsCache().isEmpty()) {
////                    List<TimedPoint> timedPoints = new ArrayList<>(mSignaturePad.getSavedPointsCache());
////                    mPoints.put(i - 1, timedPoints);
////                    mSignaturePad.getSavedPointsCache().clear();
////                    mSignaturePad.clear();
////                } else
////                    mSignaturePad.clear();
//            }
//
//            @Override
//            protected void onTapMainDocArea() {
//                if (!mButtonsVisible) {
//                    showButtons();
//                } else {
//                    if (mTopBarMode == TopBarMode.Main)
//                        hideButtons();
//                }
//            }
//
//            @Override
//            protected void onDocMotion() {
////				hideButtons();
//            }
//
//            @Override
//            public void onScaleEnd(ScaleGestureDetector detector) {
//                super.onScaleEnd(detector);
//            }
//        };
//
//        mDocView.setAdapter(new PageAdapter(this, core){
//            @Override
//            public View getView(int position, View convertView, ViewGroup parent) {
//                convertView = super.getView(position, convertView, parent);
//                if(mBitmpList.get(position) != null)
//                    ((PageView)convertView).setEntireBm(BitmapFactory.decodeFile(mBitmpList.get(position)));
//                return convertView;
//            }
//        });
//
//        mSearchTask = new SearchTask(this, core) {
//            @Override
//            protected void onTextFound(SearchTaskResult result) {
//                SearchTaskResult.set(result);
//                // Ask the ReaderView to move to the resulting page
//                mDocView.setDisplayedViewIndex(result.pageNumber);
//                // Make the ReaderView act on the change to SearchTaskResult
//                // via overridden onChildSetup method.
//                mDocView.resetupChildren();
//            }
//        };
//
//        // Make the buttons overlay, and store all its
//        // controls in variables
//        makeButtonsView();
//
//        // Set up the page slider
//        int smax = Math.max(core.countPages() - 1, 1);
//        mPageSliderRes = ((10 + smax - 1) / smax) * 2;
//
//        // Set the file-name text
//        String docTitle = core.getTitle();
//        if (docTitle != null)
//            mFilenameView.setText(docTitle);
//        else
//            mFilenameView.setText(mFileName);
//
//        // Activate the seekbar
//        mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            public void onStopTrackingTouch(SeekBar seekBar) {
//                mDocView.pushHistory();
//                int index = (seekBar.getProgress() + mPageSliderRes / 2) / mPageSliderRes;
//                mDocView.setDisplayedViewIndex(index);
//                Logger.d("MuPDF", "index==="+index);
//
//            }
//
//            public void onStartTrackingTouch(SeekBar seekBar) {
//            }
//
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                updatePageNumView((progress + mPageSliderRes / 2) / mPageSliderRes);
//            }
//        });
//
//        // Activate the search-preparing button
//        mSearchButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                searchModeOn();
//            }
//        });
//
//        mSearchClose.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                searchModeOff();
//            }
//        });
//
//        // Search invoking buttons are disabled while there is no text specified
//        mSearchBack.setEnabled(false);
//        mSearchFwd.setEnabled(false);
//        mSearchBack.setColorFilter(Color.argb(255, 128, 128, 128));
//        mSearchFwd.setColorFilter(Color.argb(255, 128, 128, 128));
//
//        // React to interaction with the text widget
//        mSearchText.addTextChangedListener(new TextWatcher() {
//
//            public void afterTextChanged(Editable s) {
//                boolean haveText = s.toString().length() > 0;
//                setButtonEnabled(mSearchBack, haveText);
//                setButtonEnabled(mSearchFwd, haveText);
//
//                // Remove any previous search results
//                if (SearchTaskResult.get() != null && !mSearchText.getText().toString().equals(SearchTaskResult.get().txt)) {
//                    SearchTaskResult.set(null);
//                    mDocView.resetupChildren();
//                }
//            }
//
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//            }
//
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//            }
//        });
//
//        //React to Done button on keyboard
//        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if (actionId == EditorInfo.IME_ACTION_DONE)
//                    search(1);
//                return false;
//            }
//        });
//
//        mSearchText.setOnKeyListener(new View.OnKeyListener() {
//            public boolean onKey(View v, int keyCode, KeyEvent event) {
//                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER)
//                    search(1);
//                return false;
//            }
//        });
//
//        // Activate search invoking buttons
//        mSearchBack.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                search(-1);
//            }
//        });
//
//        mSearchFwd.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                search(1);
//            }
//        });
//
//        mLinkButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                setLinkHighlight(!mLinkHighlight);
//                mSignaturePad.setEnabled(!mSignaturePad.isEnabled());
//                if (mSignaturePad.isEnabled()) {
//                    mSignaturePad.clear();
//                    mSignaturePad.setBackground((Bitmap) null);
//                    mSignaturePad.setVisibility(View.VISIBLE);
//                    int with = getResources().getDisplayMetrics().widthPixels;
//                    Bitmap bitmap = ((PageView) mDocView.getDisplayedView()).getEntireBm();
//                    if (mSignaturePad.getWidth() != with) {
//                        PointF rect = core.getPageSize(1);
//                        float ration = rect.x / with;
//                        int height = (int) (rect.y / ration);
//                        mSignaturePad.getLayoutParams().width = with;
//                        mSignaturePad.getLayoutParams().height = height;
//                        Log.d("rect...ss", rect.toString() + "......" + "......." + bitmap.getHeight() + "....." + mSignaturePad.getWidth() + "....." + mSignaturePad.getHeight());
//                    }
//                    if(!bitmap.isRecycled()) {
//                        bitmap.setHeight(mSignaturePad.getLayoutParams().height);
//                        mSignaturePad.setBackground(bitmap);
//                    }else {
//                        mSignaturePad.clear();
//                        mSignaturePad.setVisibility(View.GONE);
//                    }
//                } else {
//                    mSignaturePad.clear();
//                    mSignaturePad.setVisibility(View.GONE);
//                }
//            }
//        });
//
//        if (core.hasOutline()) {
//            mOutlineButton.setOnClickListener(new View.OnClickListener() {
//                public void onClick(View v) {
//                    if (mFlatOutline == null)
//                        mFlatOutline = core.getOutline();
//                    if (mFlatOutline != null) {
//                        Intent intent = new Intent(DocumentActivity.this, OutlineActivity.class);
//                        Bundle bundle = new Bundle();
//                        bundle.putInt("POSITION", mDocView.getDisplayedViewIndex());
//                        bundle.putSerializable("OUTLINE", mFlatOutline);
//                        intent.putExtras(bundle);
//                        startActivityForResult(intent, OUTLINE_REQUEST);
//                    }
//                }
//            });
//        } else {
//            mOutlineButton.setVisibility(View.GONE);
//        }
//
//        // Reenstate last state if it was recorded
//        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
//        mDocView.setDisplayedViewIndex(prefs.getInt("page" + mFileName, 0));
//
//        if (savedInstanceState == null || !savedInstanceState.getBoolean("ButtonsHidden", false))
//            showButtons();
//
//        if (savedInstanceState != null && savedInstanceState.getBoolean("SearchMode", false))
//            searchModeOn();
//
//        // Stick the document view and the buttons overlay into a parent view
//        setContentView(com.fantasy.androidmupdf.R.layout.document_pdf_layout);
//        RelativeLayout layout = findViewById(com.fantasy.androidmupdf.R.id.rootView);
//        layout.setBackgroundColor(Color.DKGRAY);
//        layout.addView(mDocView, 0);
//        layout.addView(mButtonsView, 1);
//        mSignaturePad = findViewById(com.fantasy.androidmupdf.R.id.signview);
////        mSignaturePad.getLayoutParams().width = mDocView.getWidth();
////		mSignaturePad.clear();
////		layout.addView(mSignaturePad, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
//
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        switch (requestCode) {
//            case OUTLINE_REQUEST:
//                if (resultCode >= RESULT_FIRST_USER) {
//                    mDocView.pushHistory();
//                    mDocView.setDisplayedViewIndex(resultCode - RESULT_FIRST_USER);
//                }
//                break;
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }
//
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//
//        if (mFileName != null && mDocView != null) {
//            outState.putString("FileName", mFileName);
//
//            // Store current page in the prefs against the file name,
//            // so that we can pick it up each time the file is loaded
//            // Other info is needed only for screen-orientation change,
//            // so it can go in the bundle
//            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
//            SharedPreferences.Editor edit = prefs.edit();
//            edit.putInt("page" + mFileName, mDocView.getDisplayedViewIndex());
//            edit.apply();
//        }
//
//        if (!mButtonsVisible)
//            outState.putBoolean("ButtonsHidden", true);
//
//        if (mTopBarMode == TopBarMode.Search)
//            outState.putBoolean("SearchMode", true);
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//
//        if (mSearchTask != null)
//            mSearchTask.stop();
//
//        if (mFileName != null && mDocView != null) {
//            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
//            SharedPreferences.Editor edit = prefs.edit();
//            edit.putInt("page" + mFileName, mDocView.getDisplayedViewIndex());
//            edit.apply();
//        }
//    }
//
//    public void onDestroy() {
//        if (mDocView != null) {
//            mDocView.applyToChildren(new ReaderView.ViewMapper() {
//                public void applyToView(View view) {
//                    ((PageView) view).releaseBitmaps();
//                }
//            });
//        }
//        if (core != null)
//            core.onDestroy();
//        core = null;
//        super.onDestroy();
//    }
//
//    private void setButtonEnabled(ImageButton button, boolean enabled) {
//        button.setEnabled(enabled);
//        button.setColorFilter(enabled ? Color.argb(255, 255, 255, 255) : Color.argb(255, 128, 128, 128));
//    }
//
//    private void setLinkHighlight(boolean highlight) {
//        mLinkHighlight = highlight;
//        // LINK_COLOR tint
//        mLinkButton.setColorFilter(highlight ? Color.argb(0xFF, 0x00, 0x66, 0xCC) : Color.argb(0xFF, 255, 255, 255));
//        // Inform pages of the change.
//        mDocView.setLinksEnabled(highlight);
//    }
//
//    private void showButtons() {
//        if (core == null)
//            return;
//        if (!mButtonsVisible) {
//            mButtonsVisible = true;
//            // Update page number text and slider
//            int index = mDocView.getDisplayedViewIndex();
//            updatePageNumView(index);
//            mPageSlider.setMax((core.countPages() - 1) * mPageSliderRes);
//            mPageSlider.setProgress(index * mPageSliderRes);
//            if (mTopBarMode == TopBarMode.Search) {
//                mSearchText.requestFocus();
//                showKeyboard();
//            }
//
//            Animation anim = new TranslateAnimation(0, 0, -mTopBarSwitcher.getHeight(), 0);
//            anim.setDuration(200);
//            anim.setAnimationListener(new Animation.AnimationListener() {
//                public void onAnimationStart(Animation animation) {
//                    mTopBarSwitcher.setVisibility(View.VISIBLE);
//                }
//
//                public void onAnimationRepeat(Animation animation) {
//                }
//
//                public void onAnimationEnd(Animation animation) {
//                }
//            });
//            mTopBarSwitcher.startAnimation(anim);
//
//            anim = new TranslateAnimation(0, 0, mPageSlider.getHeight(), 0);
//            anim.setDuration(200);
//            anim.setAnimationListener(new Animation.AnimationListener() {
//                public void onAnimationStart(Animation animation) {
//                    mPageSlider.setVisibility(View.VISIBLE);
//                }
//
//                public void onAnimationRepeat(Animation animation) {
//                }
//
//                public void onAnimationEnd(Animation animation) {
//                    mPageNumberView.setVisibility(View.VISIBLE);
//                }
//            });
//            mPageSlider.startAnimation(anim);
//        }
//    }
//
//    private void hideButtons() {
//        if (mButtonsVisible) {
//            mButtonsVisible = false;
//            hideKeyboard();
//
//            Animation anim = new TranslateAnimation(0, 0, 0, -mTopBarSwitcher.getHeight());
//            anim.setDuration(200);
//            anim.setAnimationListener(new Animation.AnimationListener() {
//                public void onAnimationStart(Animation animation) {
//                }
//
//                public void onAnimationRepeat(Animation animation) {
//                }
//
//                public void onAnimationEnd(Animation animation) {
//                    mTopBarSwitcher.setVisibility(View.INVISIBLE);
//                }
//            });
//            mTopBarSwitcher.startAnimation(anim);
//
//            anim = new TranslateAnimation(0, 0, 0, mPageSlider.getHeight());
//            anim.setDuration(200);
//            anim.setAnimationListener(new Animation.AnimationListener() {
//                public void onAnimationStart(Animation animation) {
//                    mPageNumberView.setVisibility(View.INVISIBLE);
//                }
//
//                public void onAnimationRepeat(Animation animation) {
//                }
//
//                public void onAnimationEnd(Animation animation) {
//                    mPageSlider.setVisibility(View.INVISIBLE);
//                }
//            });
//            mPageSlider.startAnimation(anim);
//        }
//    }
//
//    private void searchModeOn() {
//        if (mTopBarMode != TopBarMode.Search) {
//            mTopBarMode = TopBarMode.Search;
//            //Focus on EditTextWidget
//            mSearchText.requestFocus();
//            showKeyboard();
//            mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
//        }
//    }
//
//    private void searchModeOff() {
//        if (mTopBarMode == TopBarMode.Search) {
//            mTopBarMode = TopBarMode.Main;
//            hideKeyboard();
//            mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
//            SearchTaskResult.set(null);
//            // Make the ReaderView act on the change to mSearchTaskResult
//            // via overridden onChildSetup method.
//            mDocView.resetupChildren();
//        }
//    }
//
//    private void updatePageNumView(int index) {
//        Logger.d("MuPDF", "updatePageNumView==="+index);
//        if (core == null)
//            return;
//        mPageNumberView.setText(String.format(Locale.ROOT, "%d / %d", index + 1, core.countPages()));
//    }
//
//    private void makeButtonsView() {
//        mButtonsView = getLayoutInflater().inflate(R.layout.document_activity, null);
//        mFilenameView = (TextView) mButtonsView.findViewById(R.id.docNameText);
//        mPageSlider = (SeekBar) mButtonsView.findViewById(R.id.pageSlider);
//        mPageNumberView = (TextView) mButtonsView.findViewById(R.id.pageNumber);
//        mSearchButton = (ImageButton) mButtonsView.findViewById(R.id.searchButton);
//        mOutlineButton = (ImageButton) mButtonsView.findViewById(R.id.outlineButton);
//        mTopBarSwitcher = (ViewAnimator) mButtonsView.findViewById(R.id.switcher);
//        mSearchBack = (ImageButton) mButtonsView.findViewById(R.id.searchBack);
//        mSearchFwd = (ImageButton) mButtonsView.findViewById(R.id.searchForward);
//        mSearchClose = (ImageButton) mButtonsView.findViewById(R.id.searchClose);
//        mSearchText = (EditText) mButtonsView.findViewById(R.id.searchText);
//        mLinkButton = (ImageButton) mButtonsView.findViewById(R.id.linkButton);
//        mClearSign = mButtonsView.findViewById(R.id.clearButton);
//        mSaveSign = mButtonsView.findViewById(R.id.saveButton);
//        mClearSign.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                mSignaturePad.clear();
////                mSignaturePad.reDraw(50);
//            }
//        });
//        mSaveSign.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                String path = FileUtils.addJpgSignatureToGallery(mSignaturePad.getSignatureBitmap(), DocumentActivity.this);
//                sign(path);
////                toWeChatScan();
////                joinQQGroup("mmXe4jDiRPlDSzbVStThXrNlcdeynJwT");
//            }
//        });
//        mTopBarSwitcher.setVisibility(View.INVISIBLE);
//        mPageNumberView.setVisibility(View.INVISIBLE);
//        mPageSlider.setVisibility(View.INVISIBLE);
//    }
//
//    private void showKeyboard() {
//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        if (imm != null)
//            imm.showSoftInput(mSearchText, 0);
//    }
//
//    private void hideKeyboard() {
//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        if (imm != null)
//            imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
//    }
//
//    private void search(int direction) {
//        hideKeyboard();
//        int displayPage = mDocView.getDisplayedViewIndex();
//        SearchTaskResult r = SearchTaskResult.get();
//        int searchPage = r != null ? r.pageNumber : -1;
//        mSearchTask.go(mSearchText.getText().toString(), direction, displayPage, searchPage);
//    }
//
//    @Override
//    public boolean onSearchRequested() {
//        if (mButtonsVisible && mTopBarMode == TopBarMode.Search) {
//            hideButtons();
//        } else {
//            showButtons();
//            searchModeOn();
//        }
//        return super.onSearchRequested();
//    }
//
//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        if (mButtonsVisible && mTopBarMode != TopBarMode.Search) {
//            hideButtons();
//        } else {
//            showButtons();
//            searchModeOff();
//        }
//        return super.onPrepareOptionsMenu(menu);
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//    }
//
//    @Override
//    public void onBackPressed() {
//        if (!mDocView.popHistory())
//            super.onBackPressed();
//    }
//
//    public void manipulatePdf(String signPath) throws IOException, DocumentException {
//        int index = mDocView.getDisplayedViewIndex() + 1;
//        Bitmap bm = BitmapFactory.decodeFile(signPath);
//        ((PageView)mDocView.getDisplayedView()).setEntireBm(bm);
////        PdfReader reader = new PdfReader(mFileName);
////        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(mFileName.replace(".pdf", "0.pdf")));
////        Image image = Image.getInstance(signPath);
////        PdfImage stream = new PdfImage(image, "", null);
////        stream.put(new PdfName("ITXT_SpecialId"), new PdfName("123456789"));
////        PdfIndirectObject ref = stamper.getWriter().addToBody(stream);
////        image.setDirectReference(ref.getIndirectReference());
////        image.scaleAbsolute(reader.getPageSize(index));
////        image.setAbsolutePosition(0, 0);
////        PdfContentByte over = stamper.getOverContent(index);
////        over.addImage(image);
////        stamper.close();
////        reader.close();
//
////        PdfReader reader = new PdfReader(mFileName);
////        PdfDictionary pdfDictionary = reader.getPageN(1);
////        PdfObject po = pdfDictionary.get(new PdfName("MediaBox"));
////        //po是一个数组对象.里面包含了该页pdf的坐标轴范围.
////        PdfArray pa = (PdfArray) po;
////        Log.d("rect...ss", pa.toString());
////        PDFDocument pdfDocument = (PDFDocument) core.getDoc();
////        PDFObject pdfObject = pdfDocument.createObject();
////        com.artifex.mupdf.fitz.Image images = new com.artifex.mupdf.fitz.Image(signPath);
////        pdfObject.push();
////        pdfDocument.insertPage(2, pdfObject);
////        pdfObject.writeObject(pdfDocument.addImage(images));
////        pdfDocument.save(mFileName.replace(".pdf", "2.pdf"), "rw");
////        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(mFileName.replace(".pdf", "0.pdf")));
////        Image image = Image.getInstance(signPath);
////        image.scaleAbsolute(getResources().getDisplayMetrics().widthPixels, 300);
////        PdfImage stream = new PdfImage(image, "", null);
////        stream.put(new PdfName("ITXT_SpecialId"), new PdfName("123456789"));
////        PdfIndirectObject ref = stamper.getWriter().addToBody(stream);
////        image.setDirectReference(ref.getIndirectReference());
////        image.setAbsolutePosition(0, 0);
////        PdfContentByte over = stamper.getOverContent(mDocView.getDisplayedViewIndex() + 1);
////        over.addImage(image);
////        stamper.close();
////        reader.close();
//    }
//
//    private void sign(String signPath) {
//        try {
//            mBitmpList.put(mDocView.getDisplayedViewIndex(), signPath);
//            manipulatePdf(signPath);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
////		try {
////			KeyStore ks = KeyStore.getInstance("pkcs12");
////			File photo = new File(FileUtils.getAlbumStorageDir("SignaturePad"), "demo.p12");
////			ks.load(new FileInputStream(photo), "123456".toCharArray()); //123456为私钥密码
////			String alias = (String) ks.aliases().nextElement();
////			PrivateKey key = (PrivateKey) ks.getKey(alias, "123456".toCharArray());
////			Certificate[] chain = ks.getCertificateChain(alias);
////
//////			BouncyCastleProvider provider = new BouncyCastleProvider();
//////			Security.addProvider(provider);
////
////			PdfReader reader = new PdfReader(mFileName); //源文件
////			FileOutputStream fout = new FileOutputStream(mFileName.replace(".pdf", "0.pdf"));
////			PdfStamper stp = PdfStamper.createSignature(reader, fout, '\0');
////			PdfSignatureAppearance sap = stp.getSignatureAppearance();
//////        sap.setCrypto(key, chain, null, PdfSignatureAppearance.NOT_CERTIFIED);
////			sap.setLayer2Text("");
////			sap.setReason("");
////			sap.setLocation("");  //添加位置信息，可为空
////			sap.setContact("http://swordshadow.iteye.com/");
////			Image image = Image.getInstance(signPath); //使用png格式透明图片
////
////			sap.setSignatureGraphic(image);
//////			sap.setAcro6Layers(true);
//////			sap.setCertificate(PdfSignatureAppearance.NOT_CERTIFIED);
////			sap.setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC);
////			sap.setVisibleSignature(new Rectangle(300, 600, 400, 675), 1, "sig"); //300和600 是对应x轴和y轴坐标
////			PrivateKeySignature pks = new PrivateKeySignature(key, DigestAlgorithms.SHA256, "");
////			ExternalDigest digest = new BouncyCastleDigest();
////			MakeSignature.signDetached(sap, digest, pks, chain, null, null, null, 0, MakeSignature.CryptoStandard.CMS);
////			stp.getWriter().setCompressionLevel(5);
////			if (stp != null) {
////				stp.close();
////			}
////			if (fout != null) {
////				fout.close();
////			}
////			if (reader != null) {
////				reader.close();
////			}
////		}catch (Exception e){
////			e.printStackTrace();
////		}
////	}
//
//    private void toWeChatScan() {
//        try {
//            //利用Intent打开微信
//            Intent intent = new Intent();
//            intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI"));
//            intent.putExtra("LauncherUI.From.Scaner.Shortcut", true);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            intent.setAction("android.intent.action.VIEW");
//            startActivity(intent);
//
//        } catch (Exception e) {
//            //若无法正常跳转，在此进行错误处理
//            Toast.makeText(this, "无法跳转到微信，请检查是否安装了微信", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    public boolean joinQQGroup(String key) {
//        //https://qun.qq.com/join.html
//        Intent intent = new Intent();
//        intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D" + key));
//        // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        try {
//            startActivity(intent);
//            return true;
//        } catch (Exception e) {
//            // 未安装手Q或安装的版本不支持
//            return false;
//        }
//    }
//}
