package com.fridge.caps.controllers;

import com.fridge.caps.models.Notification;
import com.fridge.caps.models.NotificationType;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationController.java
 * Handles creation and retrieval of notifications in Firestore.
 * Controller in the MVC pattern.
 */
public class NotificationController {

    private final FirebaseFirestore db;
    private static final String NOTIFICATIONS = "notifications";

    public interface NotificationListCallback {
        void onSuccess(List<Notification> notifications);
        void onFailure(String error);
    }

    public NotificationController() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Sends a confirmation notification to a student after booking.
     *
     * @param recipientId    The student's user ID.
     * @param counselorName  The counselor's name.
     * @param dateTime       The appointment date and time display string.
     */
    public void sendConfirmation(String recipientId, String counselorName, String dateTime) {
        String title   = "Appointment Confirmed";
        String message = "Your appointment with " + counselorName
                + " on " + dateTime + " has been confirmed.";
        saveNotification(recipientId, title, message, NotificationType.CONFIRMATION);
    }

    /**
     * Sends a cancellation notification to a student.
     *
     * @param recipientId   The student's user ID.
     * @param counselorName The counselor's name.
     */
    public void sendCancellation(String recipientId, String counselorName) {
        String title   = "Appointment Cancelled";
        String message = "Your appointment with " + counselorName + " has been cancelled.";
        saveNotification(recipientId, title, message, NotificationType.CANCELLATION);
    }

    /**
     * Sends a reschedule notification to a student.
     *
     * @param recipientId   The student's user ID.
     * @param counselorName The counselor's name.
     * @param newDateTime   The new appointment date and time.
     */
    public void sendReschedule(String recipientId, String counselorName, String newDateTime) {
        String title   = "Appointment Rescheduled";
        String message = "Your appointment with " + counselorName
                + " has been moved to " + newDateTime + ".";
        saveNotification(recipientId, title, message, NotificationType.RESCHEDULE);
    }

    /**
     * Fetches all notifications for the currently logged-in student.
     *
     * @param callback Result callback with list of notifications.
     */
    public void getMyNotifications(NotificationListCallback callback) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) { callback.onFailure("Not logged in."); return; }

        db.collection(NOTIFICATIONS)
                .whereEqualTo("recipientId", uid)
                .get()
                .addOnSuccessListener(query -> {
                    List<Notification> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        Notification n = doc.toObject(Notification.class);
                        n.setNotificationId(doc.getId());
                        list.add(n);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Marks a notification as read.
     *
     * @param notificationId The notification ID to mark.
     */
    public void markAsRead(String notificationId) {
        db.collection(NOTIFICATIONS)
                .document(notificationId)
                .update("isRead", true);
    }

    private void saveNotification(String recipientId, String title,
                                  String message, NotificationType type) {
        String id = db.collection(NOTIFICATIONS).document().getId();
        Notification n = new Notification(id, recipientId, title,
                message, type, Timestamp.now());
        db.collection(NOTIFICATIONS).document(id).set(n);
    }
}