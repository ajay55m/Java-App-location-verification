package com.app.fourscontracting.core.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateUtils {
    private DateUtils() {}

    public static String getTodayIsoDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }

    public static String getTodayDay() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd", Locale.US);
        return sdf.format(new Date());
    }

    public static String getTodayMonth() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM", Locale.US);
        return sdf.format(new Date());
    }

    public static String getTodayYear() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy", Locale.US);
        return sdf.format(new Date());
    }

    public static String formatDisplayTime(String rawTime) {
        if (rawTime == null || rawTime.trim().isEmpty() || "--".equals(rawTime) || "--:--".equals(rawTime)) {
            return "--:--";
        }
        if (rawTime.toLowerCase(Locale.US).contains("am") || rawTime.toLowerCase(Locale.US).contains("pm")) {
            return rawTime;
        }
        try {
            SimpleDateFormat inFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
            Date date = inFormat.parse(rawTime);
            if (date != null) {
                SimpleDateFormat outFormat = new SimpleDateFormat("hh:mm a", Locale.US);
                return outFormat.format(date);
            }
        } catch (Exception ignored) {}
        return rawTime;
    }
}
