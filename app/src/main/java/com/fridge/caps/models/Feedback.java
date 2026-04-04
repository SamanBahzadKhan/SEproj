package com.fridge.caps.models;

import com.google.firebase.Timestamp;

/**
 * Feedback.java
 * Represents feedback submitted by a student after an appointment.
 * Maps to the Firestore "feedback" collection.
 */
public class Feedback {

    private String    feedbackId;
    private String    appointmentId;
    private String    studentId;
    private String    counselorId;
    private int       rating;
    private String    comment;
    private Timestamp submittedAt;

    public Feedback() {}

    public Feedback(String feedbackId, String appointmentId,
                    String studentId, String counselorId,
                    int rating, String comment, Timestamp submittedAt) {
        this.feedbackId    = feedbackId;
        this.appointmentId = appointmentId;
        this.studentId     = studentId;
        this.counselorId   = counselorId;
        this.rating        = rating;
        this.comment       = comment;
        this.submittedAt   = submittedAt;
    }

    public String getFeedbackId()    { return feedbackId; }
    public String getAppointmentId() { return appointmentId; }
    public String getStudentId()     { return studentId; }
    public String getCounselorId()   { return counselorId; }
    public int getRating()           { return rating; }
    public String getComment()       { return comment; }
    public Timestamp getSubmittedAt(){ return submittedAt; }

    public void setFeedbackId(String id)        { this.feedbackId = id; }
    public void setAppointmentId(String id)     { this.appointmentId = id; }
    public void setStudentId(String id)         { this.studentId = id; }
    public void setCounselorId(String id)       { this.counselorId = id; }
    public void setRating(int rating)           { this.rating = rating; }
    public void setComment(String comment)      { this.comment = comment; }
    public void setSubmittedAt(Timestamp t)     { this.submittedAt = t; }
}