package com.fridge.caps.controllers;

import android.util.Log;

import com.fridge.caps.models.Feedback;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Persists feedback and updates the linked timeslot.
 */
public class FeedbackController {

    private static final String TAG = "Firestore";

    private final FirebaseFirestore db;
    private static final String FEEDBACK   = "feedback";
    private static final String TIMESLOTS = "timeslots";

    public interface FeedbackCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public FeedbackController() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void submitFeedback(String timeslotId, String studentId,
                               String counselorId, int rating,
                               String comment, FeedbackCallback callback) {
        String id = db.collection(FEEDBACK).document().getId();
        Feedback feedback = new Feedback(id, timeslotId, studentId, counselorId,
                rating, comment, Timestamp.now());

        db.collection(FEEDBACK)
                .document(id)
                .set(feedback)
                .addOnSuccessListener(unused ->
                        db.collection(TIMESLOTS).document(timeslotId)
                                .update("feedbackSubmitted", true)
                                .addOnSuccessListener(u2 -> callback.onSuccess())
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "ts update", e);
                                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Update failed");
                                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "feedback", e);
                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Save failed");
                });
    }
}
