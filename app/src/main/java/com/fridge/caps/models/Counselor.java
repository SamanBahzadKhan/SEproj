package com.fridge.caps.models;

/**
 * Counselor.java
 * Represents a counselor user. Extends User with counselor-specific fields.
 * Maps to the Firestore "counselors" collection.
 */
public class Counselor extends User {

    private String  specialization;
    private String  department;
    private String  phone;
    private String  bio;
    private String  profilePhoto;
    private float   rating;
    /** Number of feedback submissions used for {@link #rating} mean. */
    private Long    ratingCount;
    /** Null from Firestore means “accepting” (default). */
    private Boolean isAcceptingClients;

    public Counselor() {}

    public Counselor(String userId, String name, String email,
                     String specialization, String bio, String profilePhoto,
                     float rating, boolean isAcceptingClients, String createdAt) {
        super(userId, name, email, UserRole.COUNSELOR, createdAt);
        this.specialization     = specialization;
        this.bio                = bio;
        this.profilePhoto       = profilePhoto;
        this.rating             = rating;
        this.isAcceptingClients = isAcceptingClients;
    }

    @Override
    public void getProfile() {}

    public String getSpecialization()   { return specialization; }
    public String getBio()              { return bio; }
    public String getProfilePhoto()     { return profilePhoto; }
    public float getRating()            { return rating; }
    public int getRatingCount() {
        return ratingCount == null ? 0 : ratingCount.intValue();
    }
    public boolean isAcceptingClients() {
        return isAcceptingClients == null || Boolean.TRUE.equals(isAcceptingClients);
    }

    public void setSpecialization(String specialization)      { this.specialization = specialization; }
    public void setDepartment(String department)              { this.department = department; }
    public void setPhone(String phone)                        { this.phone = phone; }
    public void setBio(String bio)                            { this.bio = bio; }
    public void setProfilePhoto(String profilePhoto)          { this.profilePhoto = profilePhoto; }
    public void setRating(float rating)                       { this.rating = rating; }
    public void setRatingCount(long ratingCount) {
        this.ratingCount = ratingCount;
    }
    public void setAcceptingClients(boolean acceptingClients) {
        this.isAcceptingClients = acceptingClients;
    }
}
