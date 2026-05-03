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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class MeetLinkTimeHelper {

    private static final TimeZone KARACHI = TimeZone.getTimeZone("Asia/Karachi");

    private MeetLinkTimeHelper() {}

    
    public static String[] buildStartEndIso(String dateYmd, String startTime12h, int durationMinutes) {
        if (dateYmd == null || dateYmd.isEmpty()) {
            return null;
        }
        String timePart = startTime12h != null && !startTime12h.isEmpty() ? startTime12h : "9:00 AM";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.US);
            in.setLenient(false);
            in.setTimeZone(KARACHI);
            Date start = in.parse(dateYmd + " " + timePart);
            if (start == null) {
                return null;
            }
            SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
            out.setTimeZone(KARACHI);
            String startIso = out.format(start);
            Date end = new Date(start.getTime() + (long) durationMinutes * 60_000);
            String endIso = out.format(end);
            return new String[] { startIso, endIso };
        } catch (ParseException e) {
            return null;
        }
    }
}
