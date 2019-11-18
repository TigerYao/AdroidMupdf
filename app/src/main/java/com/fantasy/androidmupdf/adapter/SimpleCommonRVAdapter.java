package com.fantasy.androidmupdf.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public abstract class SimpleCommonRVAdapter<T> extends RecyclerView.Adapter {
    public Context mContext;
    protected List<T> mData = new ArrayList<>();
    protected int layoutId;

    public SimpleCommonRVAdapter(Context context, List<T> data, int layoutId) {
        this.mContext = context;
        if (data != null) {
            this.mData.addAll(data);
        }
        this.layoutId = layoutId;
    }

    public void setData(List<T> data) {
        this.mData = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SimpleViewHolder(LayoutInflater.from(mContext).inflate(layoutId, null), getItemCount());
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        convert((SimpleViewHolder) holder, getItem(position), position);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    private T getItem(int position) {
        return mData.get(position);
    }

    public abstract void convert(SimpleViewHolder holder, final T item, final int position);

    public static class SimpleViewHolder extends RecyclerView.ViewHolder {
        private SparseArray<View> views;
        private View convertView;
        public int layoutId;
        public int count;
        public Context context;

        public <T extends View> T getView(int viewId) {
            View view = views.get(viewId);
            if (view == null) {
                view = convertView.findViewById(viewId);
                views.put(viewId, view);
            }
            return (T) view;
        }

        public SimpleViewHolder(View itemView, int count) {
            super(itemView);
            this.context = context;
            this.views = new SparseArray<View>();
            convertView = itemView;
            this.layoutId = layoutId;
            convertView.setTag(this);
            this.count = count;
        }


        public View getConvertView() {
            return convertView;
        }

        public SimpleViewHolder getPaint(int viewId, int flags) {
            TextView view = getView(viewId);
            view.getPaint().setFlags(flags);
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setText(int viewId, String text) {
            TextView view = getView(viewId);
            view.setText(text);
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setText(int viewId, SpannableStringBuilder text) {
            TextView view = getView(viewId);
            view.setText(text);
            return this;
        }

        public String getText(int viewId) {
            EditText view = getView(viewId);
            return view.getText().toString();

        }

        public SimpleCommonRVAdapter.SimpleViewHolder addEditChangeListener(int viewId, TextWatcher textWatcher) {
            EditText view = getView(viewId);
            if (view != null) {
                view.addTextChangedListener(textWatcher);
            }
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setEditText(int viewId, String text) {
            EditText view = getView(viewId);
            view.setText(text);
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setEnable(int viewId, boolean isEnable) {
            ImageView view = getView(viewId);
            view.setEnabled(isEnable);
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setSsbText(int viewId, SpannableStringBuilder text) {
            TextView view = getView(viewId);
            view.setText(text, TextView.BufferType.SPANNABLE);
            view.setMovementMethod(LinkMovementMethod.getInstance());
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setCheckBoxText(int viewId, String text) {
            CheckBox view = getView(viewId);
            view.setText(text);
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setColorText(int viewId, int color, String text) {
            TextView view = getView(viewId);
            view.setText(text);
            view.setTextColor(ContextCompat.getColor(context, color));
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setTextFaceType(int viewId, String type) {
            TextView view = getView(viewId);
            Typeface TextType = Typeface.createFromAsset(context.getAssets(), type);
            view.setTypeface(TextType);
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setBoldFaceType(int viewId, int type) {
            TextView view = getView(viewId);
            view.setTypeface(Typeface.defaultFromStyle(type));
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setTextFromHtml(int viewId, String text) {
            TextView view = getView(viewId);
            view.setText(Html.fromHtml(text));
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setTextColor(int viewId, int color) {
            TextView view = getView(viewId);
            view.setTextColor(color);
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setTextColorRes(int viewId, int color) {
            TextView view = getView(viewId);
            view.setTextColor(ContextCompat.getColor(context, color));
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setEditTextColorRes(int viewId, int color) {
            EditText view = getView(viewId);
            view.setTextColor(ContextCompat.getColor(context, color));
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setCheckBoxTextColor(int viewId, int color) {
            CheckBox view = getView(viewId);
            view.setTextColor(color);
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setViewVisibility(int viewId, int visibility) {
            View view = getView(viewId);
            view.setVisibility(visibility);
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setSeekBarProgress(int viewId, int progress) {
            SeekBar view = getView(viewId);
            view.setProgress(progress);
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setViewBackgroundColor(int viewId, int res) {
            View view = getView(viewId);
            view.setBackgroundColor(ContextCompat.getColor(context, res));
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setViewBackgroundRes(int viewId, int res) {
            View view = getView(viewId);
            view.setBackgroundResource(res);
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setViewImageResource(int viewId, int res) {
            ImageView view = getView(viewId);
            view.setImageResource(res);
            return this;
        }

        /**
         * 为ImageView设置图片
         *
         * @param viewId
         * @param drawableId
         * @return
         */
        public SimpleCommonRVAdapter.SimpleViewHolder setImageResource(int viewId, int drawableId) {
            ImageView view = getView(viewId);
            view.setImageResource(drawableId);
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder setViewOnClickListener(int viewId, View.OnClickListener l) {
            View view = getView(viewId);
            if (view != null) {
                view.setOnClickListener(l);
            }
            return this;
        }

        /**
         * 为ImageView设置图片
         *
         * @param viewId
         * @param bm     drawableId
         * @return
         */
        public SimpleCommonRVAdapter.SimpleViewHolder setImageBitmap(int viewId, Bitmap bm) {
            ImageView view = getView(viewId);
            view.setImageBitmap(bm);
            return this;
        }

//        public SimpleCommonRVAdapter.SimpleViewHolder setImage(int viewId, String imageUrl) {
//            ImageView view = getView(viewId);
//            if (view != null) {
//                GlideApp.with(view.getContext()).load(imageUrl).into(view);
//            }
//
//            return this;
//        }

        public SimpleCommonRVAdapter.SimpleViewHolder addItemView(int viewId, View child) {
            LinearLayout view = getView(viewId);
            view.addView(child);
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder addItemView(int viewId, ViewGroup child) {
            LinearLayout view = getView(viewId);
            view.addView(child);
            return this;
        }

        public SimpleCommonRVAdapter.SimpleViewHolder removeAllViews(int viewId) {
            LinearLayout view = getView(viewId);
            view.removeAllViews();
            return this;
        }

//        public SimpleCommonRVAdapter.SimpleViewHolder setAutoDivider(int viewId, int position) {
//            return setAutoDivider(viewId, position, 0);
//        }

//        public SimpleCommonRVAdapter.SimpleViewHolder setAutoDivider(int viewId, int position, int topMargin) {
//            View view = getView(viewId);
//            if (view != null) {
//                if (topMargin != 0) {
//                    if (view.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
//                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
//                        params.topMargin = topMargin;
//                        view.setLayoutParams(params);
//                    }
//                }
//                view.setVisibility(View.VISIBLE);
//                if (position + 1 == count) {
//                    view.setBackgroundResource(R.color.transparent);
//
//                } else {
//                    view.setBackgroundResource(R.color.divider);
//                }
//            }
//            return this;
//        }

//        //最后一行需要divider
//        public SimpleCommonRVAdapter.SimpleViewHolder setAutoDividerInSelect(int viewId, int position, int topMargin) {
//            View view = getView(viewId);
//            if (view != null) {
//                if (topMargin != 0) {
//                    if (view.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
//                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
//                        params.topMargin = topMargin;
//                        view.setLayoutParams(params);
//                    }
//                }
//                view.setVisibility(View.VISIBLE);
//                if (position + 1 == count) {
//                    view.setBackgroundResource(R.color.divider);
//                } else {
//                    view.setBackgroundResource(R.color.divider);
//                }
//            }
//            return this;
//        }

    }
}
