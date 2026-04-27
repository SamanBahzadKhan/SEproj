package com.fridge.caps.utils;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Writes documents to the {@code notifications} collection (shape matches {@code Notification#fromDocument}).
 */
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
}
