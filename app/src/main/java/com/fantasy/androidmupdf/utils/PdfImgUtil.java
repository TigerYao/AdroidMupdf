package com.fantasy.androidmupdf.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.util.Log;

import com.artifex.mupdf.viewer.SignAndFingerModel;
import com.fantasy.androidmupdf.FileUtils;
import com.lowagie.text.BadElementException;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

public class PdfImgUtil {


    /**
     * 将图片加入PDF并保存
     */
    public static String addText(Context ctx, List<SignAndFingerModel> models, String inPath, String outPath) {
        try {
            PdfReader reader = new PdfReader(inPath, "PDF".getBytes());///打开要写入的PDF
            FileOutputStream outputStream = new FileOutputStream(outPath);//设置涂鸦后的PDF
            PdfStamper stamp = new PdfStamper(reader, outputStream);
            for (SignAndFingerModel model : models) {
                Bitmap bitmap = Base64BitmapUtil.base64ToBitmap(model.data);
                if(model.type == 0)
                   bitmap = BitmapUtil.deleteNoUseWhiteSpace(bitmap, Color.TRANSPARENT);
                int pageNum = model.page + 1;
                PointF pointF = model.rect;
                PdfContentByte over = stamp.getOverContent(pageNum);//////用于设置在第几页打印签名
                byte[] bytes = Bitmap2Bytes(bitmap);
//                FileUtils.addJpgSignatureToGallery(BitmapUtil.renderCroppedGreyScaleBitmap(bytes, bitmap.getWidth(), bitmap.getHeight()), ctx);
                Image img = Image.getInstance(bytes);//将要放到PDF的图片传过来，要设置为byte[]类型
                com.lowagie.text.Rectangle rectangle = reader.getPageSize(pageNum);
                img.setAlignment(1);
//                float dpScale = DisplayUtil.getDensity(ctx)/3;
                float scale = (model.type == 0 ? 35f : 60f) / bitmap.getHeight();
                img.scaleToFit(bitmap.getWidth() * scale , bitmap.getHeight() * scale);
                img.setAbsolutePosition(pointF.x, rectangle.getHeight() - pointF.y - img.getPlainHeight() * 0.5f);
                over.addImage(img);
            }
            stamp.close();
            return outPath;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BadElementException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将BitMap转换为Bytes
     *
     * @param bm
     * @return
     */
    public static byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }
}
