package com.fridge.caps.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

public class PendingCounselorSignup {
    private String signupId;
    private String userId;
    private String name;
    private String email;
    private String specialization;
    private String department;
    private String phone;
    private String bio;
    private Boolean isAcceptingClients;
    private String status;
    private Timestamp timestamp;

    public static PendingCounselorSignup fromDocument(DocumentSnapshot doc) {
        PendingCounselorSignup p = new PendingCounselorSignup();
        p.signupId = doc.getId();
        p.userId = doc.getString("userId");
        p.name = doc.getString("name");
        p.email = doc.getString("email");
        p.specialization = doc.getString("specialization");
        p.department = doc.getString("department");
        p.phone = doc.getString("phone");
        p.bio = doc.getString("bio");
        p.isAcceptingClients = doc.getBoolean("isAcceptingClients");
        p.status = doc.getString("status");
        p.timestamp = doc.getTimestamp("timestamp");
        return p;
    }

    public String getSignupId() { return signupId; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getSpecialization() { return specialization; }
    public String getDepartment() { return department; }
    public String getPhone() { return phone; }
    public String getBio() { return bio; }
    public Boolean getAcceptingClients() { return isAcceptingClients; }
    public String getStatus() { return status; }
}
