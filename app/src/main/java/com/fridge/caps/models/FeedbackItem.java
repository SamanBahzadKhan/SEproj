package com.fridge.caps.models;

/**
 * One row of counsellor profile reviews (denormalized from {@code feedback} docs).
 */
public class FeedbackItem {

    private final String studentName;
    private final int    rating;
    private final String comment;
    private final long   timestamp;

    public FeedbackItem(String studentName, int rating, String comment, long timestamp) {
        this.studentName = studentName;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public String getStudentName() {
        return studentName;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
