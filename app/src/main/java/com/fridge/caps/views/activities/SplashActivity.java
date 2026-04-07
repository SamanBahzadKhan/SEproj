package com.fridge.caps.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/*
 * DEVELOPER ACTION REQUIRED:
 * The following Firebase Auth accounts exist but have NO corresponding Firestore
 * document. They will be caught by the orphan check in this SplashActivity and
 * signed out automatically. However, they remain in Firebase Auth.
 *
 * To clean up: Go to Firebase Console → Authentication → Users
 * Delete any accounts whose email you do not recognize as registered students,
 * counsellors, or admins. Known orphaned accounts include:
 *   - samanb@gmail.com
 *   - saman@gmail.com
 *
 * These will no longer be able to reach any dashboard screen after this fix,
 * but deleting them from Firebase Auth Console keeps the auth table clean.
 */

/**
 * Launcher: routes by auth + Firestore role (student / counsellor / admin).
 */
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            navigateTo(LoginActivity.class);
            return;
        }

        String uid = firebaseUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("students").document(uid).get()
                .addOnSuccessListener(studentDoc -> {
                    if (studentDoc.exists()) {
                        navigateTo(StudentDashboardActivity.class);
                        return;
                    }
                    db.collection("counselors").document(uid).get()
                            .addOnSuccessListener(counselorDoc -> {
                                if (counselorDoc.exists()) {
                                    navigateTo(CounselorDashboardActivity.class);
                                    return;
                                }
                                db.collection("admins").document(uid).get()
                                        .addOnSuccessListener(adminDoc -> {
                                            if (adminDoc.exists()) {
                                                navigateTo(AdminDashboardActivity.class);
                                                return;
                                            }
                                            Log.w(TAG, "Orphaned auth account: " + uid
                                                    + " email: " + firebaseUser.getEmail()
                                                    + " — no Firestore document found in any collection");
                                            FirebaseAuth.getInstance().signOut();
                                            Toast.makeText(this,
                                                    "Account not found. Please sign in again.",
                                                    Toast.LENGTH_LONG).show();
                                            navigateTo(LoginActivity.class);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Admin check failed: " + (e.getMessage() != null ? e.getMessage() : ""));
                                            FirebaseAuth.getInstance().signOut();
                                            navigateTo(LoginActivity.class);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Counselor check failed: " + (e.getMessage() != null ? e.getMessage() : ""));
                                FirebaseAuth.getInstance().signOut();
                                navigateTo(LoginActivity.class);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Student check failed: " + (e.getMessage() != null ? e.getMessage() : ""));
                    FirebaseAuth.getInstance().signOut();
                    Toast.makeText(this, "Connection error. Please sign in again.",
                            Toast.LENGTH_SHORT).show();
                    navigateTo(LoginActivity.class);
                });
    }

    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
