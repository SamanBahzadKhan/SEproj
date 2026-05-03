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
import com.fridge.caps.models.JournalEntry;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class JournalStatsHelper {

    private JournalStatsHelper() {}

    public static int totalEntries(List<JournalEntry> entries) {
        return entries == null ? 0 : entries.size();
    }

    
    public static int dayStreak(List<JournalEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Set<String> days = new HashSet<>();
        for (JournalEntry e : entries) {
            days.add(fmt.format(new Date(e.getCreatedAtMillis())));
        }
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        stripTime(cal);
        if (!days.contains(fmt.format(cal.getTime()))) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        int streak = 0;
        while (days.contains(fmt.format(cal.getTime()))) {
            streak++;
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        return streak;
    }

    
    public static int entriesThisWeek(List<JournalEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        Calendar start = Calendar.getInstance(Locale.getDefault());
        stripTime(start);
        start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
        long startMs = start.getTimeInMillis();
        int n = 0;
        for (JournalEntry e : entries) {
            if (e.getCreatedAtMillis() >= startMs) {
                n++;
            }
        }
        return n;
    }

    private static void stripTime(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }
}
