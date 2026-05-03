package com.fridge.caps.util;


/**
 * Purpose: Handles automated verification for application behavior.
 * Depends on: JUnit/Android test frameworks and app classes under test.
 * Notes: Provides regression coverage for key workflows.
 */
import com.fridge.caps.utils.ValidationUtils;

import org.junit.Test;

import static org.junit.Assert.*;

public class ValidationUtilsTest {

    /**
     * Verifies testValidEmail scenario.
     */
    @Test
    public void testValidEmail() {
        assertTrue(ValidationUtils.isValidEmail("ahmad@lums.edu.pk"));
        assertFalse(ValidationUtils.isValidEmail("notanemail"));
    }

    /**
     * Verifies testValidPassword scenario.
     */
    @Test
    public void testValidPassword() {
        assertTrue(ValidationUtils.isValidPassword("password123"));
        assertFalse(ValidationUtils.isValidPassword("abc"));
    }

    /**
     * Verifies testPasswordsMatch scenario.
     */
    @Test
    public void testPasswordsMatch() {
        assertTrue(ValidationUtils.passwordsMatch("mypassword", "mypassword"));
        assertFalse(ValidationUtils.passwordsMatch("a", "b"));
    }

    /**
     * Verifies testFieldNotEmpty scenario.
     */
    @Test
    public void testFieldNotEmpty() {
        assertTrue(ValidationUtils.isNotEmpty("Ahmad Raza"));
        assertFalse(ValidationUtils.isNotEmpty("   "));
    }

    /**
     * Verifies testRatingRange scenario.
     */
    @Test
    public void testRatingRange() {
        assertTrue(ValidationUtils.isValidRating(5));
        assertFalse(ValidationUtils.isValidRating(0));
    }
}
