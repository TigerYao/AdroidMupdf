package com.fantasy.androidmupdf.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.Display;
import android.view.View;
import android.view.Window;

import java.lang.reflect.Field;

/**
 * Created by saiyuan on 2016/10/26.
 */
public class DisplayUtil {
    private static float density = 0;
    private static float scaledDensity = 0;
    private static int screenWidth = 0;
    private static int screenHeight = 0;
    private static int navigationBarHeight;

    public static int getScreenWidth(Context ctx) {
        if (screenWidth <= 0) {
            screenWidth = ctx.getResources().getDisplayMetrics().widthPixels;
        }
        return screenWidth;
    }

    public static int getScreenHeight(Context ctx) {
        if (screenHeight <= 0) {
            screenHeight = ctx.getResources().getDisplayMetrics().heightPixels;
        }
        return screenHeight;
    }

    /**
     * return system bar height
     *
     * @param context
     * @return
     */
    public static int getStatuBarHeight(Context context) {
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj;
            obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int width = Integer.parseInt(field.get(obj).toString());
            int height = context.getResources().getDimensionPixelSize(width);
            return height;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static float getDensity(Context ctx) {
        if (density <= 0) {
            density = ctx.getResources().getDisplayMetrics().density;
        }
        return density;
    }

    public static float getScaledDensity(Context ctx) {
        if (scaledDensity <= 0) {
            scaledDensity = ctx.getResources().getDisplayMetrics().scaledDensity;
        }
        return scaledDensity;
    }

    /*
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static float dp2px(float dpValue, Context ctx) {
        return (dpValue * getDensity(ctx) + 0.5f);
    }

    public static int px2dp(float pxValue, Context ctx) {
        return (int) (pxValue / getDensity(ctx) + 0.5f);
    }

    public static int sp2px(float pxValue, Context ctx) {
        return (int) (pxValue * getScaledDensity(ctx) + 0.5f);
    }

    public static int px2sp(float pxValue,Context ctx) {
        return (int) (pxValue / getScaledDensity(ctx) + 0.5f);
    }

    /**
     * 获取NavigationBar高度
     *
     * @param activity
     * @return
     */
    public static int getNavigationBarHeight(Context activity) {
        if (navigationBarHeight <= 0) {
            Resources resources = activity.getResources();
            int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
            navigationBarHeight = resources.getDimensionPixelSize(resourceId);
        }
        return navigationBarHeight;
    }
    /**
     *  判断是否有虚拟条
     *  true 有虚拟条
     *  false 没有虚拟条或者不显示
     *
     */

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean checkNavigationBarShow(@NonNull Context context, @NonNull Window window) {
        boolean show;
        Display display = window.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getRealSize(point);

        View decorView = window.getDecorView();
        Configuration conf = context.getResources().getConfiguration();
        if (Configuration.ORIENTATION_LANDSCAPE == conf.orientation) {
            View contentView = decorView.findViewById(android.R.id.content);
            show = (point.x != contentView.getWidth());
        } else {
            Rect rect = new Rect();
            decorView.getWindowVisibleDisplayFrame(rect);
            show = (rect.bottom != point.y);
        }
        return show;
    }


    /**
     * 判断当前设备是手机还是平板，代码来自 Google I/O App for Android
     * @param context
     * @return 平板返回 True，手机返回 False
     */
  /*  public static boolean isPad(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }*/
}
