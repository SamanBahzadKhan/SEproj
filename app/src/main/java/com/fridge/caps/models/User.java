package com.fridge.caps.models;

/**
 * User.java
 * Abstract base class for all user types (Student, Counselor, Admin).
 * Stores shared fields and maps to Firestore via subclasses.
 */
public abstract class User {

    protected String   userId;
    protected String   name;
    protected String   email;
    protected UserRole role;
    protected String   createdAt;

    public User() {}

    public User(String userId, String name, String email,
                UserRole role, String createdAt) {
        this.userId    = userId;
        this.name      = name;
        this.email     = email;
        this.role      = role;
        this.createdAt = createdAt;
    }

    public abstract void getProfile();

    public String getUserId()    { return userId; }
    public String getName()      { return name; }
    public String getEmail()     { return email; }
    public UserRole getRole()    { return role; }
    public String getCreatedAt() { return createdAt; }

    public void setUserId(String userId)       { this.userId = userId; }
    public void setName(String name)           { this.name = name; }
    public void setEmail(String email)         { this.email = email; }
    public void setRole(UserRole role)         { this.role = role; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}