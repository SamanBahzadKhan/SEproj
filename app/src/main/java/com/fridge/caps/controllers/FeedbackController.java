package com.fridge.caps.controllers;

import android.util.Log;

import com.fridge.caps.utils.NotificationUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Persists feedback, updates counsellor rating, notifies counsellor, marks timeslot.
 */
public class FeedbackController {

    private static final String TAG = "Feedback";

    private final FirebaseFirestore db;
    private static final String FEEDBACK   = "feedback";
    private static final String TIMESLOTS  = "timeslots";
    private static final String COUNSELORS = "counselors";

    public interface FeedbackCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public FeedbackController() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void submitFeedback(String timeslotId, String studentId, String studentName,
                               String counselorId, int rating, String comment,
                               FeedbackCallback callback) {
        if (counselorId == null || counselorId.isEmpty()) {
            callback.onFailure("Missing counsellor.");
            return;
        }
        String name = studentName != null ? studentName : "Student";

        Map<String, Object> feedbackData = new HashMap<>();
        feedbackData.put("timeslotId", timeslotId);
        feedbackData.put("studentId", studentId);
        feedbackData.put("studentName", name);
        feedbackData.put("counselorId", counselorId);
        feedbackData.put("rating", (long) rating);
        feedbackData.put("comment", comment != null ? comment : "");
        feedbackData.put("timestamp", System.currentTimeMillis());

        db.collection(FEEDBACK).add(feedbackData)
                .addOnSuccessListener(docRef ->
                        db.collection(TIMESLOTS).document(timeslotId)
                                .update("feedbackSubmitted", true)
                                .addOnSuccessListener(u2 -> {
                                    updateCounselorAverageRating(counselorId);
                                    String msg = name + " left you a " + rating
                                            + "-star rating: \"" + truncate(comment, 60) + "\"";
                                    NotificationUtils.writeNotification(db, counselorId,
                                            "New Feedback Received", msg, "FEEDBACK");
                                    callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "ts", e);
                                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Update failed");
                                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "feedback", e);
                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Save failed");
                });
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() <= max) return t;
        return t.substring(0, max) + "...";
    }

    private void updateCounselorAverageRating(String counselorId) {
        db.collection(FEEDBACK)
                .whereEqualTo("counselorId", counselorId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return;
                    double total = 0;
                    int count = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Long r = doc.getLong("rating");
                        if (r != null) {
                            total += r;
                            count++;
                        }
                    }
                    if (count == 0) return;
                    double average = total / count;
                    double rounded = Math.round(average * 10.0) / 10.0;
                    Map<String, Object> u = new HashMap<>();
                    u.put("rating", rounded);
                    u.put("ratingCount", count);
                    db.collection(COUNSELORS).document(counselorId)
                            .update(u)
                            .addOnFailureListener(e ->
                                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "rating"));
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, e.getMessage() != null ? e.getMessage() : "feedback query"));
    }
}
