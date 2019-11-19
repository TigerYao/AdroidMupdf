package com.fantasy.androidmupdf.utils;

import java.text.SimpleDateFormat;

public class DateTimeUtils {
    /**
     * 把日期转换为字符串
     *
     * @param date
     * @return
     */
    public static String dateToString(java.util.Date date, String format) {
        String result = "";
        SimpleDateFormat formater = new SimpleDateFormat(format);
        try {
            result = formater.format(date);
        } catch (Exception e) {
            // log.error(e);
        }
        return result;
    }
}
