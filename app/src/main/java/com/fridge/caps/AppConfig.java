package com.fridge.caps;

/**
 * Toggle app-wide behaviour while iterating on features.
 */
public final class AppConfig {

    private AppConfig() {}

    /**
     * When {@code false}, registration skips verification email + dialog and login does not require
     * {@link com.google.firebase.auth.FirebaseUser#isEmailVerified()}. Use while testing (e.g. Google Meet).
     * Set to {@code true} before release.
     */
    public static final boolean REQUIRE_EMAIL_VERIFICATION = true;

    /**
     * Region hosting callable Cloud Functions (must match Firebase Console → Functions → region).
     */
    public static final String FIREBASE_FUNCTIONS_REGION = "us-central1";

    /**
     * Supabase Edge Function path (under project URL). Change if your deployed function slug differs.
     */
    public static final String SUPABASE_CREATE_MEET_PATH = "functions/v1/create-meet";
}
