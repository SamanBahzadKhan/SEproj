package com.fridge.caps.models;

import com.google.firebase.Timestamp;

/**
 * Appointment.java
 * Represents a counseling appointment between student and counselor with status tracking.
 * Stores appointment details (date, time, participants, status) and maps to Firestore "appointments" collection.
 * Tracks appointment lifecycle through PENDING → CONFIRMED → COMPLETED/CANCELLED states.
 */
public class Appointment {

    private String            appointmentId;
    private String            studentId;
    private String            counselorId;
    private String            counselorName;
    private String            studentName;
    private String            timeSlotId;
    private Timestamp         date;
    private String            timeDisplay;
    private String            type;
    private String            notes;
    private AppointmentStatus status;
    private boolean           feedbackSubmitted;
    private String            meetLink;

    public Appointment() {}

    public Appointment(String appointmentId, String studentId, String counselorId,
                       String counselorName, String studentName, String timeSlotId,
                       Timestamp date, String timeDisplay, String type, String notes) {
        this.appointmentId = appointmentId;
        this.studentId     = studentId;
        this.counselorId   = counselorId;
        this.counselorName = counselorName;
        this.studentName   = studentName;
        this.timeSlotId    = timeSlotId;
        this.date          = date;
        this.timeDisplay   = timeDisplay;
        this.type          = type;
        this.notes         = notes;
        this.status        = AppointmentStatus.CONFIRMED;
    }

    /** Confirms this appointment. */
    public void confirm() { this.status = AppointmentStatus.CONFIRMED; }

    /** Cancels this appointment. */
    public void cancel()  { this.status = AppointmentStatus.CANCELLED; }

    /** Marks this appointment as completed. */
    public void complete(){ this.status = AppointmentStatus.COMPLETED; }

    public String getAppointmentId()        { return appointmentId; }
    public String getStudentId()            { return studentId; }
    public String getCounselorId()          { return counselorId; }
    public String getCounselorName()        { return counselorName; }
    public String getStudentName()          { return studentName; }
    public String getTimeSlotId()           { return timeSlotId; }
    public Timestamp getDate()              { return date; }
    public String getTimeDisplay()          { return timeDisplay; }
    public String getType()                 { return type; }
    public String getNotes()                { return notes; }
    public AppointmentStatus getStatus()    { return status; }
    public boolean isFeedbackSubmitted()   { return feedbackSubmitted; }
    public String getMeetLink()                 { return meetLink; }

    public void setAppointmentId(String id)         { this.appointmentId = id; }
    public void setStudentId(String id)             { this.studentId = id; }
    public void setCounselorId(String id)           { this.counselorId = id; }
    public void setCounselorName(String name)       { this.counselorName = name; }
    public void setStudentName(String name)         { this.studentName = name; }
    public void setTimeSlotId(String id)            { this.timeSlotId = id; }
    public void setDate(Timestamp date)             { this.date = date; }
    public void setTimeDisplay(String time)         { this.timeDisplay = time; }
    public void setType(String type)                { this.type = type; }
    public void setNotes(String notes)              { this.notes = notes; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public void setFeedbackSubmitted(boolean feedbackSubmitted) {
        this.feedbackSubmitted = feedbackSubmitted;
    }
    public void setMeetLink(String meetLink) { this.meetLink = meetLink; }
}