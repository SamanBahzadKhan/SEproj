package com.fridge.caps.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Maps to Firestore "timeslots" — the single source of truth for bookings.
 */
public class TimeSlot {

    private String  id;
    private String  counselorId;
    private String  date;
    private String  startTime;
    private String  appointmentType;
    private String  createdAt;
    private boolean isBooked;

    private String  studentId;
    /** Denormalized for counsellor pending list (optional). */
    private String  studentName;
    /** "Morning" or "Afternoon" when set. */
    private String  period;
    private String  status;
    private String  notes;
    private boolean feedbackSubmitted;
    private String  bookedAt;

    /** Legacy fields (older documents). */
    private Timestamp legacyStartTime;
    private Timestamp legacyEndTime;
    private Boolean   legacyIsAvailable;

    public TimeSlot() {}

    public static TimeSlot fromSnapshot(DocumentSnapshot doc) {
        TimeSlot t = new TimeSlot();
        t.setId(doc.getId());
        t.setCounselorId(doc.getString("counselorId"));

        t.setDate(doc.getString("date"));
        t.setStartTime(doc.getString("startTime"));
        t.setAppointmentType(doc.getString("appointmentType"));
        t.setCreatedAt(doc.getString("createdAt"));

        Boolean booked = doc.getBoolean("isBooked");
        if (booked != null) {
            t.setBooked(booked);
        } else {
            Boolean avail = doc.getBoolean("isAvailable");
            if (avail != null) {
                t.setBooked(!avail);
            } else {
                t.setBooked(false);
            }
        }

        t.setStudentId(doc.getString("studentId"));
        t.setStudentName(doc.getString("studentName"));
        t.setPeriod(doc.getString("period"));
        t.setStatus(doc.getString("status"));
        t.setNotes(doc.getString("notes"));
        Boolean fs = doc.getBoolean("feedbackSubmitted");
        t.setFeedbackSubmitted(fs != null && fs);
        Object bookedAtRaw = doc.get("bookedAt");
        if (bookedAtRaw instanceof Long) {
            t.setBookedAt(String.valueOf((Long) bookedAtRaw));
        } else if (bookedAtRaw instanceof String) {
            t.setBookedAt((String) bookedAtRaw);
        }

        Object stRaw = doc.get("startTime");
        if (stRaw instanceof Timestamp) {
            t.setLegacyStartTime((Timestamp) stRaw);
        }
        Object enRaw = doc.get("endTime");
        if (enRaw instanceof Timestamp) {
            t.setLegacyEndTime((Timestamp) enRaw);
        }
        Boolean la = doc.getBoolean("isAvailable");
        t.setLegacyIsAvailable(la);

        if (t.getDate() == null && t.getLegacyStartTime() != null) {
            java.util.Date d = t.getLegacyStartTime().toDate();
            t.setDate(new java.text.SimpleDateFormat(
                    com.fridge.caps.utils.DateUtils.STORAGE_DATE, java.util.Locale.US).format(d));
        }
        if (t.getStartTime() == null && t.getLegacyStartTime() != null) {
            java.util.Date d = t.getLegacyStartTime().toDate();
            t.setStartTime(new java.text.SimpleDateFormat(
                    com.fridge.caps.utils.DateUtils.STORAGE_TIME, java.util.Locale.US).format(d));
        }
        return t;
    }

    /** Label for slot grid (date + time). */
    public String getSlotLabel() {
        if (date != null && startTime != null) {
            return com.fridge.caps.utils.DateUtils.formatSessionLine(date, startTime);
        }
        if (legacyStartTime != null) {
            java.util.Date d = legacyStartTime.toDate();
            return new java.text.SimpleDateFormat("MMM d · hh:mm a", java.util.Locale.US).format(d);
        }
        return "Slot";
    }

    public String getSlotId() { return id != null ? id : ""; }
    public String getId() { return id; }
    public String getCounselorId() { return counselorId; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getAppointmentType() { return appointmentType; }
    public String getCreatedAt() { return createdAt; }
    public boolean isBooked() { return isBooked; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getPeriod() { return period; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public boolean isFeedbackSubmitted() { return feedbackSubmitted; }
    public String getBookedAt() { return bookedAt; }
    public Timestamp getLegacyStartTime() { return legacyStartTime; }
    public Timestamp getLegacyEndTime() { return legacyEndTime; }
    public Boolean getLegacyIsAvailable() { return legacyIsAvailable; }

    public void setId(String id) { this.id = id; }
    public void setSlotId(String id) { this.id = id; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }
    public void setDate(String date) { this.date = date; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setAppointmentType(String appointmentType) { this.appointmentType = appointmentType; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setBooked(boolean booked) { isBooked = booked; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setPeriod(String period) { this.period = period; }
    public void setStatus(String status) { this.status = status; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setFeedbackSubmitted(boolean feedbackSubmitted) { this.feedbackSubmitted = feedbackSubmitted; }
    public void setBookedAt(String bookedAt) { this.bookedAt = bookedAt; }
    public void setLegacyStartTime(Timestamp legacyStartTime) { this.legacyStartTime = legacyStartTime; }
    public void setLegacyEndTime(Timestamp legacyEndTime) { this.legacyEndTime = legacyEndTime; }
    public void setLegacyIsAvailable(Boolean legacyIsAvailable) { this.legacyIsAvailable = legacyIsAvailable; }
}
