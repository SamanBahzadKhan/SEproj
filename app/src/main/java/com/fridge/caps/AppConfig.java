package com.fridge.caps;

/**
 * Toggle app-wide behaviour while iterating on features.
 *
 * <p>Firebase Auth / Firestore / Storage are initialized from {@code google-services.json}
 * (Android client credentials). Identifiers below mirror Firebase Console → Project settings and are
 * safe to reference when building URLs or documentation.</p>
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

    /** Firebase project ID (same as {@code project_id} in google-services.json). */
    public static final String FIREBASE_PROJECT_ID = "fridge-156bb";

    /** Firebase Auth hosting domain for web-style URLs (password reset links, etc.). */
    public static final String FIREBASE_AUTH_DOMAIN = "fridge-156bb.firebaseapp.com";

    /** Default Cloud Storage bucket for this project. */
    public static final String FIREBASE_STORAGE_BUCKET = "fridge-156bb.firebasestorage.app";

    /** GCM / FCM sender id (= numeric project id / {@code project_number}). */
    public static final String FIREBASE_MESSAGING_SENDER_ID = "744198650286";

    /**
     * Supabase Edge Function path segments appended to {@link com.fridge.caps.R.string#supabase_url}.
     */
    public static final String SUPABASE_CREATE_MEET_PATH = "functions/v1/create-meet";

    /** AI assistant → Hugging Face pipeline (Supabase Edge Function). */
    public static final String SUPABASE_RECOMMEND_COUNSELLOR_PATH = "functions/v1/recommend-counsellor";
}
