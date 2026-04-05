package com.fridge.caps.utils;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Central Firestore notification writes.
 */
public final class NotificationUtils {

    private static final String TAG = "Notification";

    private NotificationUtils() {}

    public static void writeNotification(FirebaseFirestore db, String recipientId,
                                         String title, String message, String type) {
        Map<String, Object> notif = new HashMap<>();
        notif.put("recipientId", recipientId);
        notif.put("title", title);
        notif.put("message", message);
        notif.put("type", type);
        notif.put("timestamp", System.currentTimeMillis());
        notif.put("read", false);
        db.collection("notifications").add(notif)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed: " + (e.getMessage() != null ? e.getMessage() : "")));
    }
}
