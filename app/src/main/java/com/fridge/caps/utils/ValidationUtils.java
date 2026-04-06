package com.fridge.caps.utils;

import java.util.regex.Pattern;

/**
 * Utility class that provides common validation methods
 * for user input such as email, password, and ratings.
 *
 * This class cannot be instantiated.
 */
public final class ValidationUtils {

    /**
     * Regular expression pattern used to validate email format.
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Private constructor to prevent instantiation.
     */
    private ValidationUtils() {}

    /**
     * Checks if the given email is valid.
     *
     * @param email the email string to validate
     * @return true if email is non-null and matches the pattern, false otherwise
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Checks if the password is valid.
     * A valid password must be at least 6 characters long.
     *
     * @param password the password string
     * @return true if password is non-null and length >= 6
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    /**
     * Checks if two passwords match.
     *
     * @param a first password
     * @param b second password
     * @return true if both are non-null and equal
     */
    public static boolean passwordsMatch(String a, String b) {
        return a != null && a.equals(b);
    }

    /**
     * Checks if a string is not empty.
     *
     * @param s the string to check
     * @return true if string is non-null and not just whitespace
     */
    public static boolean isNotEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /**
     * Validates a rating value.
     *
     * @param r the rating integer
     * @return true if rating is between 1 and 5 (inclusive)
     */
    public static boolean isValidRating(int r) {
        return r >= 1 && r <= 5;
    }
}