package com.fridge.caps.controllers;



/**
 * Purpose: Handles application business rules and data operations.
 * Depends on: Firebase Firestore/Auth models and app domain objects.
 * Notes: Coordinates validation and state changes used by app flows.
 */
/**
 * Purpose: Handles application business rules and data operations.
 * Depends on: Firebase Firestore/Auth models and app domain objects.
 * Notes: Coordinates validation and state changes used by app flows.
 */
import com.fridge.caps.models.Student;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AuthController {

    private final FirebaseAuth      auth;
    private final FirebaseFirestore db;

    private static final String STUDENTS_COLLECTION   = "students";
    private static final String COUNSELORS_COLLECTION = "counselors";
    private static final String ADMINS_COLLECTION     = "admins";
    public interface RegisterCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface LoginCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface PasswordResetCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public AuthController() {
        this.auth = FirebaseAuth.getInstance();
        this.db   = FirebaseFirestore.getInstance();
    }

    
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

    
    public void saveStudentProfile(String uid, String name, String email,
                                   String phone, String department, String yearOfStudy,
                                   RegisterCallback callback) {
        saveStudentProfile(uid, name, email, phone, null, department, yearOfStudy, callback);
    }

    public void saveStudentProfile(String uid, String name, String email,
                                   String phone, String rollNumber, String department,
                                   String yearOfStudy, RegisterCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onFailure("Invalid account.");
            return;
        }
        if (rollNumber == null || rollNumber.trim().isEmpty()) {
            callback.onFailure("Roll number is required.");
            return;
        }
        String roll = rollNumber.trim();
        db.collection(STUDENTS_COLLECTION)
                .whereEqualTo("studentId", roll)
                .get()
                .addOnSuccessListener(query -> {
                    boolean alreadyUsed = false;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot d : query) {
                        if (!uid.equals(d.getId())) {
                            alreadyUsed = true;
                            break;
                        }
                    }
                    if (alreadyUsed) {
                        callback.onFailure("Roll number already exists.");
                        return;
                    }
                    String createdAt = new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            .format(new Date());
                    Student student = new Student(uid, name, email,
                            phone, department, yearOfStudy, createdAt);
                    student.setCampusStudentId(roll);
                    db.collection(STUDENTS_COLLECTION)
                            .document(uid)
                            .set(student)
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(e ->
                                    callback.onFailure("Profile save failed: " + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        callback.onFailure("Profile save failed: " + e.getMessage()));
    }

    
    public void loginStudent(String email, String password, LoginCallback callback) {
        if (email.isEmpty() || password.isEmpty()) {
            callback.onFailure("Email and password are required.");
            return;
        }
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    
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
                                    Boolean isActive = doc.getBoolean("isActive");
                                    if (isActive != null && !isActive) {
                                        auth.signOut();
                                        callback.onFailure("This counsellor account has been disabled.");
                                    } else {
                                        callback.onSuccess();
                                    }
                                } else {
                                    db.collection(STUDENTS_COLLECTION).document(uid).get()
                                            .addOnSuccessListener(sDoc -> {
                                                auth.signOut();
                                                if (sDoc.exists()) {
                                                    callback.onFailure(
                                                            "Please use the student sign-in instead.");
                                                } else {
                                                    callback.onFailure(
                                                            "This account is not registered as a counsellor.");
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                auth.signOut();
                                                callback.onFailure(e.getMessage());
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                auth.signOut();
                                callback.onFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    
    public void loginAdmin(String email, String password, LoginCallback callback) {
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
                    db.collection(ADMINS_COLLECTION).document(uid).get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    callback.onSuccess();
                                } else {
                                    auth.signOut();
                                    callback.onFailure("This account is not registered as an admin.");
                                }
                            })
                            .addOnFailureListener(e -> {
                                auth.signOut();
                                callback.onFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    
    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    
    public void logout() {
        auth.signOut();
    }

    public void sendPasswordResetEmail(String email, PasswordResetCallback callback) {
        if (email == null || email.trim().isEmpty()) {
            callback.onFailure("Email is required.");
            return;
        }
        auth.sendPasswordResetEmail(email.trim())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        Exception e = task.getException();
                        callback.onFailure(e != null ? e.getMessage() : "Failed to send email.");
                    }
                });
    }
}