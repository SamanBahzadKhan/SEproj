package com.fridge.caps.controllers;

import android.util.Log;

import com.fridge.caps.models.Notification;
import com.fridge.caps.models.NotificationType;
import com.fridge.caps.utils.NotificationUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firestore notifications: {@code read}, {@code timestamp} (millis), {@code type} (string).
 */
public class NotificationController {

    private static final String TAG = "Firestore";

    private final FirebaseFirestore db;
    private static final String NOTIFICATIONS = "notifications";

    public interface NotificationListCallback {
        void onSuccess(List<Notification> notifications);
        void onFailure(String error);
    }

    public NotificationController() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void sendBookingRequestNotifications(String studentId, String studentName,
                                                String counselorId, String counselorName,
                                                String dateTimeLine) {
        String dr = counselorName != null && !counselorName.startsWith("Dr.")
                ? counselorName : (counselorName != null ? counselorName : "Counsellor");
        writeNotification(counselorId,
                "New Appointment Request",
                (studentName != null ? studentName : "A student") + " has requested an appointment"
                        + (dateTimeLine != null && !dateTimeLine.isEmpty() ? " (" + dateTimeLine + ")." : "."),
                "NEW_BOOKING");
        writeNotification(studentId,
                "Appointment Requested",
                "Your appointment request with Dr. " + dr
                        + (dateTimeLine != null && !dateTimeLine.isEmpty()
                        ? " on " + dateTimeLine + " is pending confirmation." : " is pending confirmation."),
                "PENDING");
    }

    public void sendConfirmation(String recipientId, String counselorName, String dateTime) {
        writeNotification(recipientId,
                "Appointment Confirmed",
                "Your appointment with " + counselorName + " on " + dateTime + " has been confirmed.",
                "CONFIRMATION");
    }

    public void sendCancellation(String recipientId, String counselorName) {
        writeNotification(recipientId,
                "Appointment Cancelled",
                "Your appointment with " + counselorName + " has been cancelled.",
                "CANCELLED");
    }

    public void sendReschedule(String recipientId, String counselorName, String newDateTime) {
        writeNotification(recipientId,
                "Appointment Rescheduled",
                "Your appointment with " + counselorName + " has been moved to " + newDateTime + ".",
                "RESCHEDULE");
    }

    public void sendSessionCompleted(String studentId, String counselorName) {
        writeNotification(studentId,
                "Session Completed",
                "Your session with Dr. " + (counselorName != null ? counselorName : "your counsellor")
                        + " is complete. You can now leave feedback.",
                "COMPLETED");
    }

    public void sendMissedSession(String studentId, String counselorName, String dateLine) {
        writeNotification(studentId,
                "Missed Session",
                "You missed your appointment with Dr. "
                        + (counselorName != null ? counselorName : "your counsellor")
                        + (dateLine != null ? " on " + dateLine + "." : "."),
                "CANCELLED");
    }

    public void sendCounselorCancelledStudent(String studentId, String counselorName,
                                              String dateLine, String timeLine) {
        writeNotification(studentId,
                "Appointment Cancelled",
                "Your appointment with Dr. " + (counselorName != null ? counselorName : "your counsellor")
                        + (dateLine != null ? " on " + dateLine : "")
                        + (timeLine != null ? " at " + timeLine : "")
                        + " was cancelled.",
                "CANCELLED");
    }

    public void sendStudentCancelledCounselor(String counselorId, String studentName,
                                              String dateLine, String timeLine) {
        writeNotification(counselorId,
                "Appointment Cancelled",
                (studentName != null ? studentName : "A student") + " has cancelled their appointment"
                        + (dateLine != null ? " on " + dateLine : "")
                        + (timeLine != null ? " at " + timeLine : "")
                        + ".",
                "CANCELLED");
    }

    public void sendDeclined(String studentId, String counselorName, String dateLine) {
        writeNotification(studentId,
                "Appointment Declined",
                "Dr. " + (counselorName != null ? counselorName : "Your counsellor")
                        + " could not accept your appointment"
                        + (dateLine != null ? " on " + dateLine : "")
                        + ". Please rebook.",
                "CANCELLED");
    }

    public void sendReminder(String studentId, String counselorName, String date, String time,
                             String message) {
        writeNotification(studentId,
                "Reminder",
                message,
                "REMINDER");
    }

    private void writeNotification(String recipientId, String title, String message, String type) {
        NotificationUtils.writeNotification(db, recipientId, title, message, type);
    }

    public void getMyNotifications(NotificationListCallback callback) {
        String uid = currentUid();
        if (uid == null) {
            callback.onFailure("Not logged in.");
            return;
        }
        db.collection(NOTIFICATIONS)
                .whereEqualTo("recipientId", uid)
                .get()
                .addOnSuccessListener(query -> {
                    List<Notification> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        list.add(Notification.fromDocument(doc));
                    }
                    list.sort((a, b) -> Long.compare(b.getTimestampMillis(), a.getTimestampMillis()));
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "getNotif");
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Real-time notifications for current user (sorted by timestamp desc in the activity).
     */
    public ListenerRegistration listenToMyNotifications(
            com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot> listener) {
        String uid = currentUid();
        if (uid == null) return null;
        return db.collection(NOTIFICATIONS)
                .whereEqualTo("recipientId", uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, e.getMessage() != null ? e.getMessage() : "listen");
                    }
                    listener.onEvent(snap, e);
                });
    }

    public void markAllReadForCurrentUser(Runnable onDone) {
        String uid = currentUid();
        if (uid == null) {
            if (onDone != null) onDone.run();
            return;
        }
        db.collection(NOTIFICATIONS)
                .whereEqualTo("recipientId", uid)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(q -> {
                    if (q.isEmpty()) {
                        if (onDone != null) onDone.run();
                        return;
                    }
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : q.getDocuments()) {
                        batch.update(doc.getReference(), "read", true);
                    }
                    batch.commit()
                            .addOnSuccessListener(u -> {
                                if (onDone != null) onDone.run();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, e.getMessage() != null ? e.getMessage() : "batch");
                                if (onDone != null) onDone.run();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "markAll");
                    if (onDone != null) onDone.run();
                });
    }

    public void markAsRead(String notificationId) {
        db.collection(NOTIFICATIONS)
                .document(notificationId)
                .update("read", true)
                .addOnFailureListener(e -> Log.e(TAG, e.getMessage() != null ? e.getMessage() : "mark"));
    }

    /** Unread count for badge (real-time). */
    public ListenerRegistration listenUnreadCount(
            com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot> listener) {
        String uid = currentUid();
        if (uid == null) return null;
        return db.collection(NOTIFICATIONS)
                .whereEqualTo("recipientId", uid)
                .whereEqualTo("read", false)
                .addSnapshotListener(listener);
    }

    private static String currentUid() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }
}
