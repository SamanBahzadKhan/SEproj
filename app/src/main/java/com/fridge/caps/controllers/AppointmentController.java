package com.fridge.caps.controllers;

/**
 * AppointmentController.java
 * Manages appointment booking lifecycle using timeslots collection as single source of truth.
 * Handles creation, confirmation, cancellation, and completion of appointments.
 * Controller in the MVC pattern.
 */
import android.util.Log;

import com.fridge.caps.models.Appointment;
import com.fridge.caps.models.AppointmentStatus;
import com.fridge.caps.models.TimeSlot;
import com.fridge.caps.utils.DateUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Booking lifecycle using the {@code timeslots} collection only.
 */
public class AppointmentController {

    private static final String TAG = "Firestore";

    private final FirebaseFirestore db;
    private static final String TIMESLOTS = "timeslots";

    public interface AppointmentCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface AppointmentListCallback {
        void onSuccess(List<Appointment> appointments);
        void onFailure(String error);
    }

    public AppointmentController() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void bookTimeslot(String timeslotId, String studentId, String notes,
                             String appointmentType, String counselorName,
                             String studentName, AppointmentCallback callback) {
        DocumentReference slotRef = db.collection(TIMESLOTS).document(timeslotId);
        db.runTransaction(transaction -> {
                    DocumentSnapshot snap = transaction.get(slotRef);
                    if (!snap.exists()) {
                        throw new FirebaseFirestoreException("Slot not found",
                                FirebaseFirestoreException.Code.NOT_FOUND);
                    }
                    Boolean booked = snap.getBoolean("isBooked");
                    if (Boolean.TRUE.equals(booked)) {
                        throw new FirebaseFirestoreException("Slot already booked",
                                FirebaseFirestoreException.Code.ABORTED);
                    }
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("isBooked", true);
                    updates.put("studentId", studentId);
                    updates.put("studentName", studentName != null ? studentName : "");
                    updates.put("status", "PENDING");
                    updates.put("notes", notes != null ? notes : "");
                    updates.put("appointmentType", appointmentType != null ? appointmentType : "In-Person");
                    updates.put("bookedAt", new Date().toString());
                    updates.put("feedbackSubmitted", false);
                    transaction.update(slotRef, updates);
                    return null;
                }).addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "bookTimeslot", e);
                    if (e instanceof FirebaseFirestoreException
                            && ((FirebaseFirestoreException) e).getCode()
                            == FirebaseFirestoreException.Code.ABORTED) {
                        callback.onFailure("This slot is no longer available.");
                    } else {
                        callback.onFailure(e.getMessage() != null ? e.getMessage() : "Booking failed");
                    }
                });
    }

    public void cancelBooking(String timeslotId, AppointmentCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isBooked", false);
        updates.put("status", "CANCELLED");
        // Keep studentId/studentName so student profile stats and history queries still match.
        updates.put("notes", FieldValue.delete());
        updates.put("bookedAt", FieldValue.delete());
        updates.put("feedbackSubmitted", FieldValue.delete());

        db.collection(TIMESLOTS).document(timeslotId)
                .update(updates)
                .addOnSuccessListener(u -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "cancel", e);
                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Cancel failed");
                });
    }

    /**
     * Reschedule: cancel old slot then book new. Rolls back on failure.
     */
    public void rescheduleTimeslot(String oldTimeslotId, String newTimeslotId,
                                   String studentId, String studentName, String notes, String appointmentType,
                                   AppointmentCallback callback) {
        cancelBooking(oldTimeslotId, new AppointmentCallback() {
            @Override
            public void onSuccess() {
                bookTimeslot(newTimeslotId, studentId, notes, appointmentType,
                        "", studentName, new AppointmentCallback() {
                            @Override
                            public void onSuccess() {
                                callback.onSuccess();
                            }

                            @Override
                            public void onFailure(String error) {
                                restoreSlotBooking(oldTimeslotId, studentId, studentName, notes, appointmentType,
                                        new AppointmentCallback() {
                                            @Override
                                            public void onSuccess() {
                                                callback.onFailure(
                                                        "Rescheduling failed; your original appointment is kept.");
                                            }

                                            @Override
                                            public void onFailure(String e2) {
                                                callback.onFailure(
                                                        "Rescheduling failed. Please contact support.");
                                            }
                                        });
                            }
                        });
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    /** After a failed reschedule, put the student back on the original slot. */
    public void restoreSlotBooking(String timeslotId, String studentId, String studentName, String notes,
                                   String appointmentType, AppointmentCallback callback) {
        Map<String, Object> u = new HashMap<>();
        u.put("isBooked", true);
        u.put("studentId", studentId);
        u.put("studentName", studentName != null ? studentName : "");
        u.put("status", "PENDING");
        u.put("notes", notes != null ? notes : "");
        u.put("appointmentType", appointmentType != null ? appointmentType : "In-Person");
        u.put("bookedAt", new Date().toString());
        u.put("feedbackSubmitted", false);
        db.collection(TIMESLOTS).document(timeslotId)
                .update(u)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "restore", e);
                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Restore failed");
                });
    }

    /**
     * Maps raw {@link TimeSlot} list to {@link Appointment} list with counselor/student names
     * (same as internal {@link #enrichAndMap}).
     */
    public void enrichSlotsToAppointments(List<TimeSlot> slots, AppointmentListCallback callback) {
        enrichAndMap(slots, callback);
    }

    public void getStudentAppointments(AppointmentListCallback callback) {
        String uid = currentUid();
        if (uid == null) {
            callback.onFailure("Not logged in.");
            return;
        }
        db.collection(TIMESLOTS)
                .whereEqualTo("studentId", uid)
                .get()
                .addOnSuccessListener(query -> {
                    List<TimeSlot> slots = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        slots.add(TimeSlot.fromSnapshot(doc));
                    }
                    enrichAndMap(slots, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "getStudent", e);
                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Query failed");
                });
    }

    public void getCounselorAppointments(String counselorId, AppointmentListCallback callback) {
        db.collection(TIMESLOTS)
                .whereEqualTo("counselorId", counselorId)
                .whereEqualTo("isBooked", true)
                .get()
                .addOnSuccessListener(query -> {
                    List<TimeSlot> slots = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        slots.add(TimeSlot.fromSnapshot(doc));
                    }
                    enrichAndMap(slots, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "getCounselor", e);
                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Query failed");
                });
    }

    public void getAllBookedTimeslots(AppointmentListCallback callback) {
        db.collection(TIMESLOTS)
                .whereEqualTo("isBooked", true)
                .get()
                .addOnSuccessListener(query -> {
                    List<TimeSlot> slots = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        slots.add(TimeSlot.fromSnapshot(doc));
                    }
                    enrichAndMap(slots, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "getAll", e);
                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Query failed");
                });
    }

    public void markComplete(String timeslotId, AppointmentCallback callback) {
        db.collection(TIMESLOTS).document(timeslotId)
                .update("status", "COMPLETED")
                .addOnSuccessListener(u -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "markComplete", e);
                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Update failed");
                });
    }

    public void markNoShow(String timeslotId, AppointmentCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isBooked", false);
        updates.put("status", "NO_SHOW");
        // Keep studentId/studentName so student history and profile queries still match.
        updates.put("notes", FieldValue.delete());
        updates.put("bookedAt", FieldValue.delete());
        updates.put("feedbackSubmitted", FieldValue.delete());
        db.collection(TIMESLOTS).document(timeslotId)
                .update(updates)
                .addOnSuccessListener(u -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "markNoShow", e);
                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Update failed");
                });
    }

    /** Counsellor confirms a student request — slot becomes BOOKED. */
    public void confirmPendingTimeslot(String timeslotId, AppointmentCallback callback) {
        db.collection(TIMESLOTS).document(timeslotId)
                .update("status", "BOOKED")
                .addOnSuccessListener(u -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "confirmPending", e);
                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Confirm failed");
                });
    }

    /** Stable document id for counsellor + date + start time. */
    public static String slotDocumentId(String counselorId, String date, String startTime) {
        String safe = startTime == null ? "slot" : startTime.replaceAll("[^A-Za-z0-9]", "");
        return counselorId + "_" + date + "_" + safe;
    }

    /**
     * Student books a new concrete hour from availability (creates / merges timeslot doc).
     */
    public void createBookingFromAvailability(String counselorId, String date, String startTime,
                                              String period, String appointmentType,
                                              String studentId, String studentName, String notes,
                                              AppointmentCallback callback) {
        if (DateUtils.isSlotStartInPast(date, startTime)) {
            callback.onFailure("This time has already passed. Please choose a later slot.");
            return;
        }
        String docId = slotDocumentId(counselorId, date, startTime);
        DocumentReference ref = db.collection(TIMESLOTS).document(docId);
        db.runTransaction(transaction -> {
                    DocumentSnapshot snap = transaction.get(ref);
                    if (snap.exists()) {
                        if (Boolean.TRUE.equals(snap.getBoolean("isBooked"))) {
                            throw new FirebaseFirestoreException("Slot taken",
                                    FirebaseFirestoreException.Code.ABORTED);
                        }
                    }
                    Map<String, Object> data = new HashMap<>();
                    data.put("counselorId", counselorId);
                    data.put("date", date);
                    data.put("startTime", startTime);
                    data.put("period", period);
                    data.put("appointmentType", appointmentType != null ? appointmentType : "In-Person");
                    data.put("isBooked", true);
                    data.put("status", "PENDING");
                    data.put("studentId", studentId);
                    data.put("studentName", studentName != null ? studentName : "");
                    data.put("notes", notes != null ? notes : "");
                    data.put("bookedAt", System.currentTimeMillis());
                    data.put("feedbackSubmitted", false);
                    data.put("createdAt", String.valueOf(System.currentTimeMillis()));
                    if (snap.exists()) {
                        transaction.set(ref, data, SetOptions.merge());
                    } else {
                        transaction.set(ref, data);
                    }
                    return null;
                }).addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "createBooking", e);
                    if (e instanceof FirebaseFirestoreException
                            && ((FirebaseFirestoreException) e).getCode()
                            == FirebaseFirestoreException.Code.ABORTED) {
                        callback.onFailure("This slot is no longer available.");
                    } else {
                        callback.onFailure(e.getMessage() != null ? e.getMessage() : "Booking failed");
                    }
                });
    }

    public void rescheduleCreateNew(String oldTimeslotId, String counselorId, String date,
                                    String startTime, String period, String appointmentType,
                                    String studentId, String studentName, String notes,
                                    AppointmentCallback callback) {
        cancelBooking(oldTimeslotId, new AppointmentCallback() {
            @Override
            public void onSuccess() {
                createBookingFromAvailability(counselorId, date, startTime, period, appointmentType,
                        studentId, studentName, notes, callback);
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    /** Backward-compatible names used by activities. */
    public void cancelAppointment(String appointmentId, String timeSlotId,
                                  AppointmentCallback callback) {
        String id = timeSlotId != null && !timeSlotId.isEmpty() ? timeSlotId : appointmentId;
        cancelBooking(id, callback);
    }

    public void rescheduleAppointment(String oldTimeslotIdAsApptId, String oldSlotId,
                                      String newSlotId, String newTimeDisplay,
                                      String studentId, String studentName, String notes, String appointmentType,
                                      AppointmentCallback callback) {
        String oldId = oldSlotId != null && !oldSlotId.isEmpty() ? oldSlotId : oldTimeslotIdAsApptId;
        rescheduleTimeslot(oldId, newSlotId, studentId, studentName, notes, appointmentType, callback);
    }

    private String currentUid() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    private void enrichAndMap(List<TimeSlot> slots, AppointmentListCallback callback) {
        if (slots.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        Set<String> counselorIds = new HashSet<>();
        Set<String> studentIds = new HashSet<>();
        for (TimeSlot s : slots) {
            if (s.getCounselorId() != null) counselorIds.add(s.getCounselorId());
            if (s.getStudentId() != null) studentIds.add(s.getStudentId());
        }
        List<DocumentReference> refs = new ArrayList<>();
        for (String cid : counselorIds) {
            refs.add(db.collection("counselors").document(cid));
        }
        for (String sid : studentIds) {
            refs.add(db.collection("students").document(sid));
        }
        if (refs.isEmpty()) {
            List<Appointment> out = new ArrayList<>();
            for (TimeSlot s : slots) out.add(mapSlot(s, "", ""));
            callback.onSuccess(out);
            return;
        }
        List<Task<DocumentSnapshot>> fetchTasks = new ArrayList<>();
        for (DocumentReference ref : refs) {
            fetchTasks.add(ref.get());
        }
        Tasks.whenAllComplete(fetchTasks)
                .addOnSuccessListener(v -> {
                    Map<String, String> counselorNames = new HashMap<>();
                    Map<String, String> studentNames = new HashMap<>();
                    for (Task<DocumentSnapshot> t : fetchTasks) {
                        if (!t.isSuccessful() || t.getResult() == null) continue;
                        DocumentSnapshot d = t.getResult();
                        if (d.exists() && d.getString("name") != null) {
                            String path = d.getReference().getPath();
                            if (path.startsWith("counselors/")) {
                                counselorNames.put(d.getId(), d.getString("name"));
                            } else if (path.startsWith("students/")) {
                                studentNames.put(d.getId(), d.getString("name"));
                            }
                        }
                    }
                    List<Appointment> out = new ArrayList<>();
                    for (TimeSlot s : slots) {
                        String cn = counselorNames.get(s.getCounselorId());
                        String sn = studentNames.get(s.getStudentId());
                        if (s.getStudentName() != null && !s.getStudentName().isEmpty()) {
                            sn = s.getStudentName();
                        }
                        out.add(mapSlot(s, cn != null ? cn : "", sn != null ? sn : ""));
                    }
                    callback.onSuccess(out);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "enrich", e);
                    List<Appointment> out = new ArrayList<>();
                    for (TimeSlot s : slots) out.add(mapSlot(s, "", ""));
                    callback.onSuccess(out);
                });
    }

    private Appointment mapSlot(TimeSlot s, String counselorName, String studentName) {
        Appointment a = new Appointment();
        String tid = s.getSlotId();
        a.setAppointmentId(tid);
        a.setTimeSlotId(tid);
        a.setStudentId(s.getStudentId());
        a.setCounselorId(s.getCounselorId());
        a.setCounselorName(counselorName);
        a.setStudentName(studentName);
        a.setTimeDisplay(s.getStartTime() != null ? s.getStartTime() : "");
        a.setType(s.getAppointmentType());
        a.setNotes(s.getNotes());
        a.setFeedbackSubmitted(s.isFeedbackSubmitted());
        a.setMeetLink(s.getMeetLink());

        String st = s.getStatus();
        if (st == null) st = "";
        switch (st) {
            case "PENDING":
                a.setStatus(AppointmentStatus.PENDING);
                break;
            case "BOOKED":
                a.setStatus(AppointmentStatus.CONFIRMED);
                break;
            case "COMPLETED":
                a.setStatus(AppointmentStatus.COMPLETED);
                break;
            case "CANCELLED":
                a.setStatus(AppointmentStatus.CANCELLED);
                break;
            case "NO_SHOW":
                a.setStatus(AppointmentStatus.NO_SHOW);
                break;
            default:
                if (s.isBooked()) {
                    a.setStatus(AppointmentStatus.CONFIRMED);
                } else {
                    a.setStatus(AppointmentStatus.PENDING);
                }
        }

        try {
            if (s.getDate() != null && !s.getDate().isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.STORAGE_DATE, Locale.US);
                Date dt = sdf.parse(s.getDate());
                if (dt != null) {
                    a.setDate(new Timestamp(dt));
                }
            } else if (s.getLegacyStartTime() != null) {
                a.setDate(s.getLegacyStartTime());
            }
        } catch (ParseException ignored) {
        }
        return a;
    }
}