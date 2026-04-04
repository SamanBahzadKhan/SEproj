package com.fridge.caps.controllers;

import com.fridge.caps.models.Feedback;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * FeedbackController.java
 * Handles submission of student feedback after appointments.
 * Controller in the MVC pattern.
 */
public class FeedbackController {

    private final FirebaseFirestore db;
    private static final String FEEDBACK = "feedback";

    public interface FeedbackCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public FeedbackController() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Submits feedback for a completed appointment.
     *
     * @param appointmentId The appointment being reviewed.
     * @param studentId     The student submitting the feedback.
     * @param counselorId   The counselor being reviewed.
     * @param rating        Star rating (1-5).
     * @param comment       Optional text comment.
     * @param callback      Result callback.
     */
    public void submitFeedback(String appointmentId, String studentId,
                               String counselorId, int rating,
                               String comment, FeedbackCallback callback) {
        String id = db.collection(FEEDBACK).document().getId();
        Feedback feedback = new Feedback(id, appointmentId, studentId,
                counselorId, rating, comment, Timestamp.now());

        db.collection(FEEDBACK)
                .document(id)
                .set(feedback)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}