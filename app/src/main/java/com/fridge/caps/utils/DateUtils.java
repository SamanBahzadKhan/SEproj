package com.fridge.caps.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Date / time strings for Firestore ({@code yyyy-MM-dd}, {@code h:mm a}) and UI labels.
 */
public final class DateUtils {

    private DateUtils() {}

    public static final String STORAGE_DATE = "yyyy-MM-dd";
    /** Matches slot chips and {@link com.fridge.caps.workers.ReminderWorker} parsing. */
    public static final String STORAGE_TIME = "h:mm a";
    /** Human-readable date for appointment lines. */
    public static final String DISPLAY_DATE = "MMM d, yyyy";

    public static String getTodayString() {
        return new SimpleDateFormat(STORAGE_DATE, Locale.US).format(new Date());
    }

    /**
     * Monday of the calendar week containing today ({@link Calendar} US: week Sun–Sat, anchored to Mon..Sun span).
     */
    public static String getThisWeekMonday() {
        Calendar c = startOfDayCalendar();
        int dow = c.get(Calendar.DAY_OF_WEEK);
        int delta = Calendar.MONDAY - dow;
        if (delta > 0) {
            delta -= 7;
        }
        c.add(Calendar.DAY_OF_YEAR, delta);
        return new SimpleDateFormat(STORAGE_DATE, Locale.US).format(c.getTime());
    }

    /** Sunday of the same week as {@link #getThisWeekMonday()}. */
    public static String getThisWeekSunday() {
        Calendar c = startOfDayCalendar();
        int dow = c.get(Calendar.DAY_OF_WEEK);
        int daysToSun = (Calendar.SATURDAY + 1 - dow + 7) % 7;
        c.add(Calendar.DAY_OF_YEAR, daysToSun);
        return new SimpleDateFormat(STORAGE_DATE, Locale.US).format(c.getTime());
    }

    public static List<String> getNextFourteenDays() {
        List<String> out = new ArrayList<>(14);
        SimpleDateFormat fmt = new SimpleDateFormat(STORAGE_DATE, Locale.US);
        Calendar c = startOfDayCalendar();
        for (int i = 0; i < 14; i++) {
            out.add(fmt.format(c.getTime()));
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        return out;
    }

    public static List<String> getMorningSlots() {
        return slots8(
                "8:00 AM", "8:30 AM", "9:00 AM", "9:30 AM",
                "10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM");
    }

    public static List<String> getAfternoonSlots() {
        return slots8(
                "12:00 PM", "12:30 PM", "1:00 PM", "1:30 PM",
                "2:00 PM", "2:30 PM", "3:00 PM", "3:30 PM");
    }

    private static List<String> slots8(String a, String b, String c, String d,
                                       String e, String f, String g, String h) {
        List<String> list = new ArrayList<>(8);
        Collections.addAll(list, a, b, c, d, e, f, g, h);
        return list;
    }

    public static boolean isMorningSlot(String startTime) {
        if (startTime == null || startTime.isEmpty()) {
            return false;
        }
        try {
            Date d = new SimpleDateFormat(STORAGE_TIME, Locale.US).parse(startTime.trim());
            if (d == null) {
                return false;
            }
            Calendar cal = Calendar.getInstance(Locale.US);
            cal.setTime(d);
            return cal.get(Calendar.HOUR_OF_DAY) < 12;
        } catch (ParseException e) {
            return false;
        }
    }

    public static String toDisplayDate(String ymd) {
        if (ymd == null || ymd.isEmpty()) {
            return "";
        }
        try {
            Date d = new SimpleDateFormat(STORAGE_DATE, Locale.US).parse(ymd);
            if (d == null) {
                return ymd;
            }
            return new SimpleDateFormat(DISPLAY_DATE, Locale.US).format(d);
        } catch (ParseException e) {
            return ymd;
        }
    }

    public static String getDayAbbreviation(String ymd) {
        if (ymd == null || ymd.isEmpty()) {
            return "";
        }
        try {
            Date d = new SimpleDateFormat(STORAGE_DATE, Locale.US).parse(ymd);
            if (d == null) {
                return "";
            }
            return new SimpleDateFormat("EEE", Locale.US).format(d);
        } catch (ParseException e) {
            return "";
        }
    }

    public static String formatSessionLine(String dateYmd, String startTime) {
        if (dateYmd == null || dateYmd.isEmpty()) {
            return startTime != null ? startTime : "";
        }
        String dayPart = toDisplayDate(dateYmd);
        if (startTime == null || startTime.isEmpty()) {
            return dayPart;
        }
        return dayPart + " · " + startTime;
    }

    /**
     * Whether the slot start (date + {@link #STORAGE_TIME} string) is strictly before the current instant.
     * Used to block booking times that have already passed on the selected day.
     */
    public static boolean isSlotStartInPast(String dateYmd, String startTimeHm) {
        if (dateYmd == null || dateYmd.isEmpty() || startTimeHm == null || startTimeHm.isEmpty()) {
            return false;
        }
        try {
            SimpleDateFormat df = new SimpleDateFormat(STORAGE_DATE + " " + STORAGE_TIME, Locale.US);
            df.setLenient(false);
            Date slotStart = df.parse(dateYmd + " " + startTimeHm.trim());
            if (slotStart == null) {
                return false;
            }
            return slotStart.before(new Date());
        } catch (ParseException e) {
            return false;
        }
    }

    private static Calendar startOfDayCalendar() {
        Calendar c = Calendar.getInstance(Locale.US);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }
}
