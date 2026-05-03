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
public class Admin extends User {

    public Admin() {
        super();
    }

    public Admin(String userId, String name, String email, String createdAt) {
        super(userId, name, email, UserRole.ADMIN, createdAt);
    }

    @Override
    public void getProfile() {
    }
}
