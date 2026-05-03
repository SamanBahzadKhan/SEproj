package com.fridge.caps.models;



/**
 * Purpose: Defines core domain data structures and status values.
 * Depends on: Firebase timestamp types and Java/Kotlin data accessors.
 * Notes: Used as transfer objects between controllers and screens.
 */
/**
 * Purpose: Defines core domain data structures and status values.
 * Depends on: Firebase timestamp types and Java/Kotlin data accessors.
 * Notes: Used as transfer objects between controllers and screens.
 */
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.PropertyName;

import java.util.Locale;

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

    
    public static Counselor fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }
        Counselor c = new Counselor();
        c.setUserId(doc.getId());
        c.setName(doc.getString("name"));
        c.setEmail(doc.getString("email"));
        c.setCreatedAt(doc.getString("createdAt"));
        c.setSpecialization(doc.getString("specialization"));
        c.setDepartment(doc.getString("department"));
        c.setBio(doc.getString("bio"));
        c.setPhone(doc.getString("phone"));

        Double rating = doc.getDouble("rating");
        if (rating == null) {
            Long lr = doc.getLong("rating");
            if (lr != null) {
                rating = lr.doubleValue();
            }
        }
        c.setRating(rating);

        Long rc = doc.getLong("ratingCount");
        if (rc == null) {
            Object rawRc = doc.get("ratingCount");
            if (rawRc instanceof Integer) {
                rc = ((Integer) rawRc).longValue();
            } else if (rawRc instanceof Double) {
                rc = ((Double) rawRc).longValue();
            }
        }
        c.setRatingCount(rc);

        Boolean accepting = doc.getBoolean("isAcceptingClients");
        c.setAcceptingClients(accepting == null || accepting);

        c.setActive(doc.getBoolean("isActive"));
        c.setDeleted(doc.getBoolean("isDeleted"));

        String roleStr = doc.getString("role");
        if (roleStr != null && !roleStr.isEmpty()) {
            String norm = roleStr.trim().toUpperCase(Locale.US).replace('-', '_');
            try {
                c.setRole(UserRole.valueOf(norm));
            } catch (IllegalArgumentException ex) {
                c.setRole(UserRole.COUNSELOR);
            }
        } else {
            c.setRole(UserRole.COUNSELOR);
        }
        return c;
    }
}
