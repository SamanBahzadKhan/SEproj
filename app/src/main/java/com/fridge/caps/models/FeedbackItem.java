package com.fridge.caps.models;

/**
 * One student review row for counsellor profile.
 */
public class FeedbackItem {

    private String studentName;
    private int    rating;
    private String comment;
    private long   timestamp;

    public FeedbackItem() {}

    public FeedbackItem(String studentName, int rating, String comment, long timestamp) {
        this.studentName = studentName;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
