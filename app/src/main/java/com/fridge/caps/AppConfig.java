package com.fridge.caps;



/**
 * Purpose: Handles application-wide constants and runtime configuration values.
 * Depends on: Build/runtime environment values and shared app modules.
 * Notes: Centralizes static config used across features.
 */
/**
 * Purpose: Handles core application configuration and runtime constants.
 * Depends on: Android app configuration and environment values.
 * Notes: Centralizes non-UI constants shared across modules.
 */
public final class AppConfig {

    private AppConfig() {}

    
    public static final boolean REQUIRE_EMAIL_VERIFICATION = true;

    
    public static final String FIREBASE_FUNCTIONS_REGION = "us-central1";

    
    public static final String FIREBASE_PROJECT_ID = "fridge-156bb";

    
    public static final String FIREBASE_AUTH_DOMAIN = "fridge-156bb.firebaseapp.com";

    
    public static final String FIREBASE_STORAGE_BUCKET = "fridge-156bb.firebasestorage.app";

    
    public static final String FIREBASE_MESSAGING_SENDER_ID = "744198650286";

    
    public static final String SUPABASE_CREATE_MEET_PATH = "functions/v1/create-meet";

    
    public static final String SUPABASE_RECOMMEND_COUNSELLOR_PATH = "functions/v1/recommend-counsellor";
}
