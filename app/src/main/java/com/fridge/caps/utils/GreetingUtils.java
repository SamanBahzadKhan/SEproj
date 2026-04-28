package com.fridge.caps.utils;

import java.util.Calendar;
import java.util.Locale;

/**
 * Time-of-day greeting for dashboards (device local time).
 */
public final class GreetingUtils {

    private GreetingUtils() {}

    /** "Good morning" | "Good afternoon" | "Good evening" */
    public static String timeOfDayGreeting(Calendar cal) {
        int h = cal.get(Calendar.HOUR_OF_DAY);
        if (h >= 5 && h < 12) {
            return "Good morning";
        }
        if (h >= 12 && h < 17) {
            return "Good afternoon";
        }
        return "Good evening";
    }

    public static String greetingWithName(String displayName) {
        String g = timeOfDayGreeting(Calendar.getInstance(Locale.getDefault()));
        String n = displayName != null ? displayName.trim() : "";
        if (n.isEmpty()) {
            return g + ",";
        }
        return g + ",\n" + n;
    }
}
