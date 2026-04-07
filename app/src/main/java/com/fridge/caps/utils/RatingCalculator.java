package com.fridge.caps.utils;

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
