package com.fridge.caps.controllers;

/**
 * CounselorController.java
 * Manages counselor profile retrieval and availability slot queries from Firestore.
 * Provides methods to fetch counselor lists, individual profiles, and available time slots.
 * Controller in the MVC pattern.
 */
import com.fridge.caps.models.Counselor;
import com.fridge.caps.models.TimeSlot;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
                        Boolean isActive = doc.getBoolean("isActive");
                        if (isActive != null && !isActive) {
                            continue;
                        }
                        Counselor c = Counselor.fromDocument(doc);
                        if (c != null) {
                            list.add(c);
                        }
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * All counselor documents (admin manage list), including inactive / soft-deleted profiles.
     */
    public void getAllCounselorsForAdmin(CounselorListCallback callback) {
        db.collection(COUNSELORS_COLLECTION)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Counselor> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Counselor c = Counselor.fromDocument(doc);
                        if (c != null) {
                            list.add(c);
                        }
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
                        Counselor c = Counselor.fromDocument(doc);
                        callback.onSuccess(c);
                    } else {
                        callback.onFailure("Counselor not found.");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Fetches available (unbooked) time slots for a counselor.
     */
    public void getAvailableTimeSlots(String counselorId, TimeSlotsCallback callback) {
        db.collection(TIMESLOTS_COLLECTION)
                .whereEqualTo("counselorId", counselorId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<TimeSlot> slots = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        TimeSlot slot = TimeSlot.fromSnapshot(doc);
                        if (!slot.isBooked()) {
                            slots.add(slot);
                        }
                    }
                    callback.onSuccess(slots);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Free slots for one day ({@code dateYmd} = yyyy-MM-dd). Filters client-side so
     * {@code isBooked == false} only.
     */
    public void getAvailableSlotsForDate(String counselorId, String dateYmd, TimeSlotsCallback callback) {
        db.collection(TIMESLOTS_COLLECTION)
                .whereEqualTo("counselorId", counselorId)
                .whereEqualTo("date", dateYmd)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<TimeSlot> slots = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        TimeSlot slot = TimeSlot.fromSnapshot(doc);
                        if (!slot.isBooked()) {
                            slots.add(slot);
                        }
                    }
                    Collections.sort(slots, Comparator.comparing(s ->
                            s.getStartTime() != null ? s.getStartTime() : ""));
                    callback.onSuccess(slots);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", e.getMessage() != null ? e.getMessage() : "slotsDate");
                    callback.onFailure(e.getMessage());
                });
    }
}