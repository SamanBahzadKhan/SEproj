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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public final class NotificationUtils {

    private NotificationUtils() {}

    public static void writeNotification(FirebaseFirestore db, String recipientId,
                                         String title, String message, String type) {
        if (db == null || recipientId == null || recipientId.isEmpty()) {
            return;
        }
        Map<String, Object> notif = new HashMap<>();
        notif.put("recipientId", recipientId);
        notif.put("title", title != null ? title : "");
        notif.put("message", message != null ? message : "");
        notif.put("type", type != null ? type : "CONFIRMATION");
        notif.put("timestamp", System.currentTimeMillis());
        notif.put("read", false);
        notif.put("isRead", false);
        db.collection("notifications").add(notif);
    }

    
    public static void writeMeetLinkNotification(FirebaseFirestore db, String recipientId,
                                               String title, String message, String meetLink,
                                               String appointmentId, String type) {
        if (db == null || recipientId == null || recipientId.isEmpty()) {
            return;
        }
        Map<String, Object> notif = new HashMap<>();
        notif.put("recipientId", recipientId);
        notif.put("title", title != null ? title : "");
        notif.put("message", message != null ? message : "");
        notif.put("type", type != null ? type : "meet_link");
        notif.put("meetLink", meetLink != null ? meetLink : "");
        notif.put("appointmentId", appointmentId != null ? appointmentId : "");
        notif.put("timestamp", System.currentTimeMillis());
        notif.put("read", false);
        notif.put("isRead", false);
        db.collection("notifications").add(notif);
    }
}
