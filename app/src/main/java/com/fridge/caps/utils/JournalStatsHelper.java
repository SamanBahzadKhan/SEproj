package com.fridge.caps.utils;

import com.fridge.caps.models.JournalEntry;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Client-side stats for the My Journal dashboard cards. */
public final class JournalStatsHelper {

    private JournalStatsHelper() {}

    public static int totalEntries(List<JournalEntry> entries) {
        return entries == null ? 0 : entries.size();
    }

    /**
     * Consecutive calendar days (ending today or yesterday) that have at least one entry.
     */
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

    /** Entries whose {@code createdAt} falls on or after the start of this locale week. */
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
