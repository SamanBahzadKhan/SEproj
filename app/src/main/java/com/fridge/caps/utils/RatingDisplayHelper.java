package com.fridge.caps.utils;

import android.widget.ImageView;

import com.fridge.caps.R;

/**
 * Fills 5 {@link ImageView} stars from a 0–5 average (supports half stars).
 */
public final class RatingDisplayHelper {

    private RatingDisplayHelper() {}

    /**
     * @param stars exactly 5 views; index 0 = leftmost (rating 1).
     */
    public static void applyStarRating(ImageView[] stars, double rating) {
        if (stars == null || stars.length < 5) return;
        for (int i = 0; i < 5; i++) {
            int pos = i + 1;
            ImageView v = stars[i];
            if (v == null) continue;
            if (rating >= pos) {
                v.setImageResource(R.drawable.ic_star_filled);
                v.clearColorFilter();
            } else if (rating >= pos - 0.5) {
                v.setImageResource(R.drawable.ic_star_half);
                v.clearColorFilter();
            } else {
                v.setImageResource(R.drawable.ic_star_outline);
                v.clearColorFilter();
            }
        }
    }

    public static void applyReviewStars(ImageView[] stars, int rating1to5) {
        if (stars == null) return;
        int r = Math.max(0, Math.min(5, rating1to5));
        for (int i = 0; i < stars.length; i++) {
            if (stars[i] == null) continue;
            if (i < r) {
                stars[i].setImageResource(R.drawable.ic_star_filled);
            } else {
                stars[i].setImageResource(R.drawable.ic_star_outline);
            }
        }
    }
}