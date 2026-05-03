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
public final class RatingCalculator {

    private RatingCalculator() {}

    public static double calculateAverage(int[] ratings) {
        if (ratings == null || ratings.length == 0) return 0.0;
        double sum = 0;
        for (int r : ratings) sum += r;
        double avg = sum / ratings.length;
        return Math.round(avg * 10.0) / 10.0;
    }
}