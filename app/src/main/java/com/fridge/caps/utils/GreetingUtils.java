package com.fridge.caps.utils;



/**
 * Purpose: Handles shared helper logic used across application features.
 * Depends on: Standard libraries and app domain value types.
 * Notes: Provides reusable utility behavior to reduce duplicated logic.
 */
/**
 * Purpose: Handles shared helper logic used across non-UI features.
 * Depends on: Java standard libraries and app domain value types.
 * Notes: Provides reusable pure helpers to reduce duplicated logic.
 */
import java.util.Calendar;
import java.util.Locale;

public final class GreetingUtils {

    private GreetingUtils() {}

    
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

    
    public static String greetingLineComma() {
        return timeOfDayGreeting(Calendar.getInstance(Locale.getDefault())) + ",";
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
