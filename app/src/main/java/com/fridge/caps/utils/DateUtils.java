package com.fridge.caps.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Consistent date/time formats for Firestore storage and UI.
 */
public final class DateUtils {

    public static final String STORAGE_DATE = "yyyy-MM-dd";
    public static final String STORAGE_TIME   = "hh:mm a";
    public static final String DISPLAY_DATE   = "MMM dd, yyyy";

    private DateUtils() {}

    public static String getTodayStorageFormat() {
        return new SimpleDateFormat(STORAGE_DATE, Locale.US).format(new Date());
    }

    /** Same as getTodayStorageFormat — alias for spec. */
    public static String getTodayString() {
        return getTodayStorageFormat();
    }

    /**
     * Monday of the current week (locale-dependent first day of week — uses US week starting Sunday
     * for Calendar; we normalize to Monday-start week).
     */
    public static String getThisWeekMonday() {
        Calendar cal = Calendar.getInstance(Locale.US);
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new SimpleDateFormat(STORAGE_DATE, Locale.US).format(cal.getTime());
    }

    /** Sunday of the week that contains {@link #getThisWeekMonday()}. */
    public static String getThisWeekSunday() {
        Calendar cal = Calendar.getInstance(Locale.US);
        try {
            Date mon = new SimpleDateFormat(STORAGE_DATE, Locale.US).parse(getThisWeekMonday());
            if (mon != null) {
                cal.setTime(mon);
                cal.add(Calendar.DAY_OF_MONTH, 6);
            }
        } catch (ParseException ignored) {
            cal.add(Calendar.DAY_OF_MONTH, 6);
        }
        return new SimpleDateFormat(STORAGE_DATE, Locale.US).format(cal.getTime());
    }

    public static String getDayAbbreviation(String dateString) {
        if (dateString == null || dateString.isEmpty()) return "";
        try {
            Date date = new SimpleDateFormat(STORAGE_DATE, Locale.US).parse(dateString);
            if (date == null) return "";
            return new SimpleDateFormat("EEE", Locale.US).format(date);
        } catch (ParseException e) {
            return "";
        }
    }

    public static List<String> getMorningSlots() {
        return Arrays.asList(
                "8:00 AM", "9:00 AM", "10:00 AM", "11:00 AM",
                "12:00 PM", "1:00 PM", "2:00 PM", "3:00 PM"
        );
    }

    public static List<String> getAfternoonSlots() {
        return Arrays.asList(
                "4:00 PM", "5:00 PM", "6:00 PM", "7:00 PM",
                "8:00 PM", "9:00 PM", "10:00 PM", "11:00 PM"
        );
    }

    /** Next 14 days as yyyy-MM-dd starting today. */
    public static List<String> getNextFourteenDays() {
        List<String> out = new ArrayList<>();
        Calendar base = Calendar.getInstance(Locale.US);
        SimpleDateFormat fmt = new SimpleDateFormat(STORAGE_DATE, Locale.US);
        for (int i = 0; i < 14; i++) {
            Calendar c = (Calendar) base.clone();
            c.add(Calendar.DAY_OF_YEAR, i);
            out.add(fmt.format(c.getTime()));
        }
        return out;
    }

    /** Morning block = 8 AM–4 PM in spec; use hour &lt; 16:00. */
    public static boolean isMorningSlot(String startTime) {
        if (startTime == null || startTime.isEmpty()) return true;
        try {
            Date t = new SimpleDateFormat(STORAGE_TIME, Locale.US).parse(startTime.trim());
            if (t == null) return true;
            Calendar cal = Calendar.getInstance(Locale.US);
            cal.setTime(t);
            int h = cal.get(Calendar.HOUR_OF_DAY);
            return h < 16;
        } catch (ParseException e) {
            return true;
        }
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
