package com.fantasy.androidmupdf.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.Log;

import java.io.ByteArrayOutputStream;

public class BitmapUtil {
    /**
     * 按比例缩放图片
     *
     * @param origin 原图
     * @param ratio  比例
     * @return 新的bitmap
     */
    public static Bitmap scaleBitmap(Bitmap origin, float ratio) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.preScale(ratio, ratio);
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }

    /**
     * 去除多余白框
     *
     * @param originBitmap
     * @return
     */
    public static Bitmap deleteNoUseWhiteSpace(Bitmap originBitmap, int color) {
        int[] imgThePixels = new int[originBitmap.getWidth() * originBitmap.getHeight()];
        originBitmap.getPixels(
                imgThePixels,
                0,
                originBitmap.getWidth(),
                0,
                0,
                originBitmap.getWidth(),
                originBitmap.getHeight());
        // 灰度化 bitmap
        Bitmap bitmap = originBitmap;
        int top = 0; // 上边框白色高度
        int left = 0; // 左边框白色高度
        int right = 0; // 右边框白色高度
        int bottom = 0; // 底边框白色高度
        for (int h = 0; h < bitmap.getHeight(); h++) {
            boolean holdBlackPix = false;
            for (int w = 0; w < bitmap.getWidth(); w++) {
                if (bitmap.getPixel(w, h) != color) { // -1 是白色
                    holdBlackPix = true; // 如果不是-1 则是其他颜色
                    break;
                }
            }
            if (holdBlackPix) {
                break;
            }
            top++;
        }
        for (int w = 0; w < bitmap.getWidth(); w++) {
            boolean holdBlackPix = false;
            for (int h = 0; h < bitmap.getHeight(); h++) {
                if (bitmap.getPixel(w, h) != color) {
                    holdBlackPix = true;
                    break;
                }
            }
            if (holdBlackPix) {
                break;
            }
            left++;
        }
        for (int w = bitmap.getWidth() - 1; w >= 0; w--) {
            boolean holdBlackPix = false;
            for (int h = 0; h < bitmap.getHeight(); h++) {
                if (bitmap.getPixel(w, h) != color) {
                    holdBlackPix = true;
                    break;
                }
            }
            if (holdBlackPix) {
                break;
            }
            right++;
        }
        for (int h = bitmap.getHeight() - 1; h >= 0; h--) {
            boolean holdBlackPix = false;
            for (int w = 0; w < bitmap.getWidth(); w++) {
                if (bitmap.getPixel(w, h) != color) {
                    holdBlackPix = true;
                    break;
                }
            }
            if (holdBlackPix) {
                break;
            }
            bottom++;
        }
        // 获取内容区域的宽高
        int cropHeight = bitmap.getHeight() - bottom - top;
        int cropWidth = bitmap.getWidth() - left - right;
        // 获取内容区域的像素点
        int[] newPix = new int[cropWidth * cropHeight];
        int i = 0;
        for (int h = top; h < top + cropHeight; h++) {
            for (int w = left; w < left + cropWidth; w++) {
                newPix[i++] = bitmap.getPixel(w, h);
            }
        }
        // 创建切割后的 bitmap， 针对彩色图，把 newPix 替换为 originBitmap 的 pixs
        return Bitmap.createBitmap(newPix, cropWidth, cropHeight, Bitmap.Config.ARGB_8888);
    }

    public static byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    public static Bitmap getTransparentBitmap(Bitmap sourceImg, int number){
        int[] argb = new int[sourceImg.getWidth() * sourceImg.getHeight()];

        sourceImg.getPixels(argb, 0, sourceImg.getWidth(), 0, 0, sourceImg

                .getWidth(), sourceImg.getHeight());// 获得图片的ARGB值

        number = number * 255 / 100;

        for (int i = 0; i < argb.length; i++) {

            argb[i] = (number << 24) | (argb[i] & 0x00FFFFFF);

        }

        sourceImg = Bitmap.createBitmap(argb, sourceImg.getWidth(), sourceImg

                .getHeight(), Bitmap.Config.ARGB_8888);

        return sourceImg;
    }
}
