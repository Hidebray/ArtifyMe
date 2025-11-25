package com.sevengroup.artifyme.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    public static String formatDateTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat(AppConstants.DATE_FORMAT, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}