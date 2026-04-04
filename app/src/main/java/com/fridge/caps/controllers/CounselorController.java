package com.fridge.caps.controllers;

import com.fridge.caps.models.Counselor;
import com.fridge.caps.models.TimeSlot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * CounselorController.java
 * Fetches counselor profiles and available time slots from Firestore.
 * Controller in the MVC pattern.
 */
public class CounselorController {

    private final FirebaseFirestore db;

    private static final String COUNSELORS_COLLECTION = "counselors";
    private static final String TIMESLOTS_COLLECTION  = "timeslots";

    public interface CounselorCallback {
        void onSuccess(Counselor counselor);
        void onFailure(String errorMessage);
    }

    public interface CounselorListCallback {
        void onSuccess(List<Counselor> counselors);
        void onFailure(String errorMessage);
    }

    public interface TimeSlotsCallback {
        void onSuccess(List<TimeSlot> slots);
        void onFailure(String errorMessage);
    }

    public CounselorController() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Fetches all counselors from Firestore.
     *
     * @param callback Result callback with list of counselors.
     */
    public void getAllCounselors(CounselorListCallback callback) {
        db.collection(COUNSELORS_COLLECTION)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Counselor> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Counselor c = doc.toObject(Counselor.class);
                        c.setUserId(doc.getId());
                        list.add(c);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Fetches a single counselor profile by ID.
     *
     * @param counselorId Firestore document ID of the counselor.
     * @param callback    Result callback with counselor object.
     */
    public void getCounselorProfile(String counselorId, CounselorCallback callback) {
        db.collection(COUNSELORS_COLLECTION)
                .document(counselorId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Counselor c = doc.toObject(Counselor.class);
                        if (c != null) c.setUserId(doc.getId());
                        callback.onSuccess(c);
                    } else {
                        callback.onFailure("Counselor not found.");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Fetches available time slots for a given counselor.
     * Only returns slots where isAvailable is true.
     *
     * @param counselorId ID of the counselor.
     * @param callback    Result callback with list of available slots.
     */
    public void getAvailableTimeSlots(String counselorId, TimeSlotsCallback callback) {
        db.collection(TIMESLOTS_COLLECTION)
                .whereEqualTo("counselorId", counselorId)
                .whereEqualTo("isAvailable", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<TimeSlot> slots = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        TimeSlot slot = doc.toObject(TimeSlot.class);
                        slot.setSlotId(doc.getId());
                        slots.add(slot);
                    }
                    callback.onSuccess(slots);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
