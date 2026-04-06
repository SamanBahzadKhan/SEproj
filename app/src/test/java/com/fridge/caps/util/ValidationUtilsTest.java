package com.fridge.caps.util;

import com.fridge.caps.utils.ValidationUtils;

import org.junit.Test;

import static org.junit.Assert.*;

public class ValidationUtilsTest {

    @Test
    public void testValidEmail() {
        assertTrue(ValidationUtils.isValidEmail("ahmad@lums.edu.pk"));
        assertFalse(ValidationUtils.isValidEmail("notanemail"));
    }

    @Test
    public void testValidPassword() {
        assertTrue(ValidationUtils.isValidPassword("password123"));
        assertFalse(ValidationUtils.isValidPassword("abc"));
    }

    @Test
    public void testPasswordsMatch() {
        assertTrue(ValidationUtils.passwordsMatch("mypassword", "mypassword"));
        assertFalse(ValidationUtils.passwordsMatch("a", "b"));
    }

    @Test
    public void testFieldNotEmpty() {
        assertTrue(ValidationUtils.isNotEmpty("Ahmad Raza"));
        assertFalse(ValidationUtils.isNotEmpty("   "));
    }

    @Test
    public void testRatingRange() {
        assertTrue(ValidationUtils.isValidRating(5));
        assertFalse(ValidationUtils.isValidRating(0));
    }
}
