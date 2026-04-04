package com.fridge.caps.controllers;

import com.fridge.caps.models.Counselor;
import com.fridge.caps.models.Student;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * AuthController.java
 * Handles student registration and login using Firebase Auth and Firestore.
 * Controller in the MVC pattern.
 */
public class AuthController {

    private final FirebaseAuth      auth;
    private final FirebaseFirestore db;

    private static final String STUDENTS_COLLECTION   = "students";
    private static final String COUNSELORS_COLLECTION = "counselors";

    public interface RegisterCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface LoginCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public AuthController() {
        this.auth = FirebaseAuth.getInstance();
        this.db   = FirebaseFirestore.getInstance();
    }

    /**
     * Creates a new student account in Firebase Auth and saves profile to Firestore.
     *
     * @param name        Student's full name.
     * @param email       Student's email address.
     * @param password    Student's password (min 6 characters).
     * @param phone       Student's phone number.
     * @param department  Student's department.
     * @param yearOfStudy Student's year of study.
     * @param callback    Result callback.
     */
    public void registerStudent(String name, String email, String password,
                                String phone, String department,
                                String yearOfStudy, RegisterCallback callback) {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            callback.onFailure("Name, email and password are required.");
            return;
        }
        if (password.length() < 6) {
            callback.onFailure("Password must be at least 6 characters.");
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    String createdAt = new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            .format(new Date());

                    Student student = new Student(uid, name, email,
                            phone, department, yearOfStudy, createdAt);

                    db.collection(STUDENTS_COLLECTION)
                            .document(uid)
                            .set(student)
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(e ->
                                    callback.onFailure("Profile save failed: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Signs in an existing student using email and password.
     *
     * @param email    Student's email address.
     * @param password Student's password.
     * @param callback Result callback.
     */
    public void loginStudent(String email, String password, LoginCallback callback) {
        if (email.isEmpty() || password.isEmpty()) {
            callback.onFailure("Email and password are required.");
            return;
        }
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Signs in and verifies the account exists in the counselors collection.
     */
    public void loginCounselor(String email, String password, LoginCallback callback) {
        if (email.isEmpty() || password.isEmpty()) {
            callback.onFailure("Email and password are required.");
            return;
        }
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser() != null
                            ? authResult.getUser().getUid() : null;
                    if (uid == null) {
                        callback.onFailure("Login failed.");
                        return;
                    }
                    db.collection(COUNSELORS_COLLECTION).document(uid).get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    callback.onSuccess();
                                } else {
                                    auth.signOut();
                                    callback.onFailure("This account is not registered as a counsellor.");
                                }
                            })
                            .addOnFailureListener(e -> {
                                auth.signOut();
                                callback.onFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Creates a counsellor account (Auth + Firestore counsellors doc). Used from admin panel.
     */
    public void registerCounselor(String name, String email, String password,
                                  String specialization, RegisterCallback callback) {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            callback.onFailure("Name, email and password are required.");
            return;
        }
        if (password.length() < 6) {
            callback.onFailure("Password must be at least 6 characters.");
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser() != null
                            ? authResult.getUser().getUid() : null;
                    if (uid == null) {
                        callback.onFailure("Account creation failed.");
                        return;
                    }
                    String createdAt = new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            .format(new Date());
                    Counselor counselor = new Counselor(uid, name, email,
                            specialization != null ? specialization : "",
                            "", "", 0f, true, createdAt);

                    db.collection(COUNSELORS_COLLECTION).document(uid)
                            .set(counselor)
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(e ->
                                    callback.onFailure("Profile save failed: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Returns true if a user session currently exists.
     */
    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    /** Signs out the current user. */
    public void logout() {
        auth.signOut();
    }
}
