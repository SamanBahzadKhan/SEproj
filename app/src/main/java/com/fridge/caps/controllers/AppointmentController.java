package com.fridge.caps.controllers;

import com.fridge.caps.models.Appointment;
import com.fridge.caps.models.AppointmentStatus;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * AppointmentController.java
 * Handles booking, cancellation, rescheduling, and retrieval of appointments.
 * Communicates with Firestore "appointments" and "timeslots" collections.
 * Controller in the MVC pattern.
 */
public class AppointmentController {

    private final FirebaseFirestore db;
    private static final String APPOINTMENTS = "appointments";
    private static final String TIMESLOTS    = "timeslots";

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

    /**
     * Books a new appointment and marks the time slot as unavailable.
     *
     * @param appointment The appointment object to save.
     * @param callback    Result callback.
     */
    public void bookAppointment(Appointment appointment, AppointmentCallback callback) {
        String id = db.collection(APPOINTMENTS).document().getId();
        appointment.setAppointmentId(id);

        db.collection(APPOINTMENTS)
                .document(id)
                .set(appointment)
                .addOnSuccessListener(unused -> {
                    // Mark time slot as unavailable
                    db.collection(TIMESLOTS)
                            .document(appointment.getTimeSlotId())
                            .update("isAvailable", false)
                            .addOnSuccessListener(u -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Cancels an appointment and frees the time slot.
     *
     * @param appointmentId The ID of the appointment to cancel.
     * @param timeSlotId    The ID of the time slot to free.
     * @param callback      Result callback.
     */
    public void cancelAppointment(String appointmentId, String timeSlotId,
                                  AppointmentCallback callback) {
        db.collection(APPOINTMENTS)
                .document(appointmentId)
                .update("status", AppointmentStatus.CANCELLED)
                .addOnSuccessListener(unused -> {
                    db.collection(TIMESLOTS)
                            .document(timeSlotId)
                            .update("isAvailable", true)
                            .addOnSuccessListener(u -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Reschedules an appointment to a new time slot.
     *
     * @param appointmentId  The ID of the appointment to reschedule.
     * @param oldSlotId      The old time slot ID to free.
     * @param newSlotId      The new time slot ID to book.
     * @param newTimeDisplay The display string for the new time.
     * @param callback       Result callback.
     */
    public void rescheduleAppointment(String appointmentId, String oldSlotId,
                                      String newSlotId, String newTimeDisplay,
                                      AppointmentCallback callback) {
        db.collection(APPOINTMENTS)
                .document(appointmentId)
                .update("timeSlotId", newSlotId, "timeDisplay", newTimeDisplay)
                .addOnSuccessListener(unused -> {
                    // Free old slot
                    db.collection(TIMESLOTS).document(oldSlotId)
                            .update("isAvailable", true);
                    // Book new slot
                    db.collection(TIMESLOTS).document(newSlotId)
                            .update("isAvailable", false)
                            .addOnSuccessListener(u -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Fetches all upcoming appointments for the currently logged-in student.
     *
     * @param callback Result callback with list of appointments.
     */
    public void getStudentAppointments(AppointmentListCallback callback) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) { callback.onFailure("Not logged in."); return; }

        db.collection(APPOINTMENTS)
                .whereEqualTo("studentId", uid)
                .get()
                .addOnSuccessListener(query -> {
                    List<Appointment> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        Appointment a = doc.toObject(Appointment.class);
                        a.setAppointmentId(doc.getId());
                        list.add(a);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Fetches all upcoming appointments for a counselor.
     *
     * @param counselorId The counselor's user ID.
     * @param callback    Result callback with list of appointments.
     */
    public void getCounselorAppointments(String counselorId, AppointmentListCallback callback) {
        db.collection(APPOINTMENTS)
                .whereEqualTo("counselorId", counselorId)
                .get()
                .addOnSuccessListener(query -> {
                    List<Appointment> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        Appointment a = doc.toObject(Appointment.class);
                        a.setAppointmentId(doc.getId());
                        list.add(a);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Fetches all appointments in the system for admin view.
     *
     * @param callback Result callback with list of all appointments.
     */
    public void getAllAppointments(AppointmentListCallback callback) {
        db.collection(APPOINTMENTS)
                .get()
                .addOnSuccessListener(query -> {
                    List<Appointment> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        Appointment a = doc.toObject(Appointment.class);
                        a.setAppointmentId(doc.getId());
                        list.add(a);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Marks a student as a no-show for an appointment.
     *
     * @param appointmentId The appointment ID.
     * @param callback      Result callback.
     */
    public void markNoShow(String appointmentId, AppointmentCallback callback) {
        db.collection(APPOINTMENTS)
                .document(appointmentId)
                .update("status", AppointmentStatus.NO_SHOW)
                .addOnSuccessListener(u -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Marks an appointment as completed (session held).
     */
    public void markComplete(String appointmentId, AppointmentCallback callback) {
        db.collection(APPOINTMENTS)
                .document(appointmentId)
                .update("status", AppointmentStatus.COMPLETED)
                .addOnSuccessListener(u -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}