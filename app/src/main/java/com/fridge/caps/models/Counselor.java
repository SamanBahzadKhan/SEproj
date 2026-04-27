package com.fridge.caps.models;

import com.google.firebase.firestore.PropertyName;

/**
 * Counsellor profile document in {@code counselors}. Deserializes with {@code toObject(Counselor.class)}.
 */
public class Counselor extends User {

    private String specialization;
    private String department;
    private String bio;
    private String phone;
    private Double rating;
    private Long   ratingCount;

    private boolean acceptingClients = true;
    private Boolean active;
    private Boolean deleted;

    public Counselor() {
        super();
    }

    /**
     * Admin-created counsellor account (see {@link com.fridge.caps.controllers.AuthController#registerCounselor}).
     */
    public Counselor(String userId, String name, String email, String specialization,
                     String bio, String phone, float rating, boolean acceptingClients, String createdAt) {
        super(userId, name, email, UserRole.COUNSELOR, createdAt);
        this.specialization = specialization != null ? specialization : "";
        this.department = "";
        this.bio = bio != null ? bio : "";
        this.phone = phone != null ? phone : "";
        this.rating = (double) rating;
        this.ratingCount = 0L;
        this.acceptingClients = acceptingClients;
    }

    @Override
    public void getProfile() {
        // Hook for shared {@link User} API; profile UI uses Firestore snapshots directly.
    }

    public String getSpecialization() {
        return specialization != null ? specialization : "";
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getDepartment() {
        return department != null ? department : "";
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getBio() {
        return bio != null ? bio : "";
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPhone() {
        return phone != null ? phone : "";
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public double getRating() {
        return rating != null ? rating : 0.0;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public int getRatingCount() {
        return ratingCount != null ? ratingCount.intValue() : 0;
    }

    public void setRatingCount(Long ratingCount) {
        this.ratingCount = ratingCount;
    }

    @PropertyName("isAcceptingClients")
    public boolean isAcceptingClients() {
        return acceptingClients;
    }

    @PropertyName("isAcceptingClients")
    public void setAcceptingClients(boolean acceptingClients) {
        this.acceptingClients = acceptingClients;
    }

    @PropertyName("isActive")
    public Boolean getActive() {
        return active;
    }

    @PropertyName("isActive")
    public void setActive(Boolean active) {
        this.active = active;
    }

    @PropertyName("isDeleted")
    public Boolean getDeleted() {
        return deleted;
    }

    @PropertyName("isDeleted")
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
