package com.fridge.caps.models;

/**
 * Admin user; document lives in {@code admins}.
 */
public class Admin extends User {

    public Admin() {
        super();
    }

    public Admin(String userId, String name, String email, String createdAt) {
        super(userId, name, email, UserRole.ADMIN, createdAt);
    }

    @Override
    public void getProfile() {
        // Hook for shared {@link User} API.
    }
}
