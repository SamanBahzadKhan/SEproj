package com.fridge.caps.util;


/**
 * Purpose: Handles automated verification for application behavior.
 * Depends on: JUnit/Android test frameworks and app classes under test.
 * Notes: Provides regression coverage for key workflows.
 */
import com.fridge.caps.utils.RatingCalculator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RatingCalculatorTest {

    /**
     * Verifies testAverageRatingMultipleReviews scenario.
     */
    @Test
    public void testAverageRatingMultipleReviews() {
        int[] ratings = {5, 4, 3, 5, 4};
        assertEquals(4.2, RatingCalculator.calculateAverage(ratings), 0.01);
    }

    /**
     * Verifies testAverageRatingEmpty scenario.
     */
    @Test
    public void testAverageRatingEmpty() {
        int[] ratings = {};
        assertEquals(0.0, RatingCalculator.calculateAverage(ratings), 0.01);
    }

    /**
     * Verifies testAverageRatingAlwaysInBounds scenario.
     */
    @Test
    public void testAverageRatingAlwaysInBounds() {
        int[] ratings = {1, 2, 3, 4, 5};
        double avg = RatingCalculator.calculateAverage(ratings);
        assertTrue(avg >= 1.0 && avg <= 5.0);
    }
}
