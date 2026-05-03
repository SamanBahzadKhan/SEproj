package com.fridge.caps.controllers;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * Persists student session feedback and marks the timeslot as submitted.
 */
public class FeedbackController {

    private final FirebaseFirestore db;

    public FeedbackController() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface FeedbackCallback {
        void onSuccess();

        void onFailure(String error);
    }

    public void submitFeedback(String timeslotId, String studentId, String studentName,
                               String counselorId, int rating, String comment,
                               FeedbackCallback callback) {
        if (timeslotId == null || timeslotId.isEmpty()) {
            if (callback != null) {
                callback.onFailure("Missing timeslot.");
            }
            return;
        }
        if (studentId == null || studentId.isEmpty()) {
            if (callback != null) {
                callback.onFailure("Not logged in.");
            }
            return;
        }
        if (counselorId == null || counselorId.isEmpty()) {
            if (callback != null) {
                callback.onFailure("Missing counsellor.");
            }
            return;
        }
        if (rating < 1 || rating > 5) {
            if (callback != null) {
                callback.onFailure("Invalid rating.");
            }
            return;
        }

        Map<String, Object> doc = new HashMap<>();
        doc.put("timeslotId", timeslotId);
        doc.put("studentId", studentId);
        doc.put("studentName", studentName != null ? studentName : "Student");
        doc.put("counselorId", counselorId);
        doc.put("rating", (long) rating);
        doc.put("comment", comment != null ? comment : "");
        doc.put("timestamp", System.currentTimeMillis());

        db.collection("feedback").add(doc)
                .addOnSuccessListener(ref ->
                        db.collection("timeslots").document(timeslotId)
                                .update("feedbackSubmitted", true)
                                .addOnSuccessListener(u ->
                                        updateCounselorRatingFromFeedback(counselorId, rating, callback))
                                .addOnFailureListener(e -> {
                                    if (callback != null) {
                                        callback.onFailure(e.getMessage() != null
                                                ? e.getMessage() : "Could not update session.");
                                    }
                                }))
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e.getMessage() != null
                                ? e.getMessage() : "Could not save feedback.");
                    }
                });
    }

    /**
     * Updates {@code counselors/{id}} aggregate {@code rating} and {@code ratingCount} after feedback is saved.
     */
    private void updateCounselorRatingFromFeedback(String counselorId, int newRating,
                                                 FeedbackCallback callback) {
        if (counselorId == null || counselorId.isEmpty()) {
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }
        db.runTransaction((Transaction transaction) -> {
            DocumentReference ref = db.collection("counselors").document(counselorId);
            DocumentSnapshot snap = transaction.get(ref);
            long currentCount = snap.getLong("ratingCount") != null ? snap.getLong("ratingCount") : 0L;
            double currentAvg = snap.getDouble("rating") != null ? snap.getDouble("rating") : 0.0;
            long newCount = currentCount + 1;
            double newAvg = ((currentAvg * currentCount) + newRating) / newCount;
            Map<String, Object> updates = new HashMap<>();
            updates.put("ratingCount", newCount);
            updates.put("rating", newAvg);
            transaction.update(ref, updates);
            return null;
        }).addOnSuccessListener(v -> {
            if (callback != null) {
                callback.onSuccess();
            }
        }).addOnFailureListener(e -> {
            if (callback != null) {
                callback.onFailure(e.getMessage() != null
                        ? e.getMessage()
                        : "Could not update counsellor rating.");
            }
        });
    }
}
