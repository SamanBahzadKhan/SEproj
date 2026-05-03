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
import android.widget.ImageView;

import com.fridge.caps.R;

public final class RatingDisplayHelper {

    private RatingDisplayHelper() {}

    
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