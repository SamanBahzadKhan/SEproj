package com.fridge.caps.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.R;
import com.fridge.caps.controllers.AuthController;
import com.fridge.caps.models.Student;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * ProfileActivity.java
 * Displays the logged-in student's profile information and settings.
 * Fetches student data from Firestore and handles sign out.
 * View in the MVC pattern.
 */
public class ProfileActivity extends AppCompatActivity {

    private TextView    tvUsername, tvStudentId, tvEmail, tvPhone,
            tvDepartment, tvYearOfStudy;
    private ProgressBar progressBar;
    private AuthController authController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        authController = new AuthController();

        tvUsername    = findViewById(R.id.tvUsername);
        tvStudentId   = findViewById(R.id.tvStudentId);
        tvEmail       = findViewById(R.id.tvEmail);
        tvPhone       = findViewById(R.id.tvPhone);
        tvDepartment  = findViewById(R.id.tvDepartment);
        tvYearOfStudy = findViewById(R.id.tvYearOfStudy);
        progressBar   = findViewById(R.id.progressBar);

        loadStudentProfile();

        findViewById(R.id.btnSignOut).setOnClickListener(v -> handleSignOut());

        // Settings rows — placeholder toasts for now
        findViewById(R.id.rowNotifications).setOnClickListener(v ->
                Toast.makeText(this, "Notification Preferences coming soon.", Toast.LENGTH_SHORT).show());
        findViewById(R.id.rowPrivacy).setOnClickListener(v ->
                Toast.makeText(this, "Privacy Settings coming soon.", Toast.LENGTH_SHORT).show());
        findViewById(R.id.rowHelp).setOnClickListener(v ->
                Toast.makeText(this, "Help & Support coming soon.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Fetches the current student's profile from Firestore and populates the UI.
     */
    private void loadStudentProfile() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection("students")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    progressBar.setVisibility(View.GONE);
                    if (doc.exists()) {
                        Student student = doc.toObject(Student.class);
                        if (student != null) {
                            tvUsername.setText(student.getName());
                            tvStudentId.setText(student.getUserId().substring(0, 8).toUpperCase());
                            tvEmail.setText(student.getEmail());
                            tvPhone.setText(student.getPhone() != null ? student.getPhone() : "Not set");
                            tvDepartment.setText(student.getDepartment() != null ? student.getDepartment() : "Not set");
                            tvYearOfStudy.setText(student.getYearOfStudy() != null ? student.getYearOfStudy() : "Not set");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Signs out the current user and navigates back to LoginActivity.
     */
    private void handleSignOut() {
        authController.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}