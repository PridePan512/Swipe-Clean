package com.example.swipeclean.utils;

import java.util.Locale;

public class StringUtils {

    public static String getHumanFriendlyByteCount(long bytes, int decimalPlaces) {
        long unit = 1024;
        if (bytes == 0) {
            return "0 KB";
        }
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.getDefault(), "%." + decimalPlaces + "f %sB",
                bytes / Math.pow(unit, exp), pre);
    }
}
