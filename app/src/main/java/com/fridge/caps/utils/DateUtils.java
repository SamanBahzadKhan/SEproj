package com.fridge.caps.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Consistent date/time formats for Firestore storage and UI.
 */
public final class DateUtils {

    public static final String STORAGE_DATE   = "yyyy-MM-dd";
    public static final String STORAGE_TIME   = "hh:mm a";
    public static final String DISPLAY_DATE   = "MMM dd, yyyy";

    private DateUtils() {}

    public static String getTodayStorageFormat() {
        return new SimpleDateFormat(STORAGE_DATE, Locale.US).format(new Date());
    }

    public static String toDisplayDate(String storedDate) {
        if (storedDate == null || storedDate.isEmpty()) return "—";
        try {
            Date d = new SimpleDateFormat(STORAGE_DATE, Locale.US).parse(storedDate);
            if (d == null) return storedDate;
            return new SimpleDateFormat(DISPLAY_DATE, Locale.US).format(d);
        } catch (ParseException e) {
            return storedDate;
        }
    }

    /** Combine storage date + time strings into one line for lists. */
    public static String formatSessionLine(String date, String startTime) {
        String d = toDisplayDate(date);
        if (startTime == null || startTime.isEmpty()) return d;
        return d + " · " + startTime;
    }
}
