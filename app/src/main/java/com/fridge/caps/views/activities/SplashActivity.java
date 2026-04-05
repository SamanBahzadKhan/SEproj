package com.fridge.caps.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Launcher: routes by auth + Firestore role (student / counsellor / admin).
 */
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String uid = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("students").document(uid).get()
                .addOnSuccessListener(studentDoc -> {
                    if (studentDoc.exists()) {
                        go(new Intent(this, StudentDashboardActivity.class));
                        return;
                    }
                    db.collection("counselors").document(uid).get()
                            .addOnSuccessListener(counselorDoc -> {
                                if (counselorDoc.exists()) {
                                    go(new Intent(this, CounselorDashboardActivity.class));
                                    return;
                                }
                                db.collection("admins").document(uid).get()
                                        .addOnSuccessListener(adminDoc -> {
                                            if (adminDoc.exists()) {
                                                go(new Intent(this, AdminDashboardActivity.class));
                                            } else {
                                                FirebaseAuth.getInstance().signOut();
                                                go(new Intent(this, LoginActivity.class));
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, e.getMessage() != null ? e.getMessage() : "admin");
                                            FirebaseAuth.getInstance().signOut();
                                            go(new Intent(this, LoginActivity.class));
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, e.getMessage() != null ? e.getMessage() : "counselor");
                                go(new Intent(this, LoginActivity.class));
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : "student");
                    go(new Intent(this, LoginActivity.class));
                });
    }

    private void go(Intent intent) {
        startActivity(intent);
        finish();
    }
}
