package com.fridge.caps.views.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.fridge.caps.R;
import com.fridge.caps.controllers.AuthController;
import com.fridge.caps.models.Student;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * Student profile, stats from {@code timeslots}, and settings.
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS = "caps_prefs";
    private static final String KEY_NOTIF_REMINDERS = "notif_reminders_enabled";

    private TextView    tvUsername, tvStudentId, tvEmail, tvPhone,
            tvDepartment, tvYearOfStudy;
    private TextView    tvStatTotal, tvStatUpcoming, tvStatCancelled;
    private ProgressBar progressBar;
    private AuthController authController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        authController = new AuthController();

        tvUsername       = findViewById(R.id.tvUsername);
        tvStudentId      = findViewById(R.id.tvStudentId);
        tvEmail          = findViewById(R.id.tvEmail);
        tvPhone          = findViewById(R.id.tvPhone);
        tvDepartment     = findViewById(R.id.tvDepartment);
        tvYearOfStudy    = findViewById(R.id.tvYearOfStudy);
        tvStatTotal      = findViewById(R.id.tvStatTotal);
        tvStatUpcoming   = findViewById(R.id.tvStatUpcoming);
        tvStatCancelled  = findViewById(R.id.tvStatCancelled);
        progressBar      = findViewById(R.id.progressBar);

        if (tvStatTotal != null) tvStatTotal.setText("0");
        if (tvStatUpcoming != null) tvStatUpcoming.setText("0");
        if (tvStatCancelled != null) tvStatCancelled.setText("0");

        loadStudentProfile();
        loadAppointmentStats();

        findViewById(R.id.btnSignOut).setOnClickListener(v -> handleSignOut());

        findViewById(R.id.rowNotifications).setOnClickListener(v -> showNotificationPrefsDialog());
        findViewById(R.id.rowPrivacy).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Privacy")
                .setMessage("Your data is stored securely and used only for appointment management within CAPs.")
                .setPositiveButton("OK", null)
                .show());
        findViewById(R.id.rowHelp).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Help & Support")
                .setMessage("For assistance, contact: caps-support@lums.edu.pk\n\nVersion 1.0.0")
                .setPositiveButton("OK", null)
                .show());
    }

    private void showNotificationPrefsDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_NOTIF_REMINDERS, true);

        SwitchCompat sw = new SwitchCompat(this);
        sw.setText("Enable appointment reminders");
        sw.setChecked(enabled);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        sw.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle("Notification Preferences")
                .setView(sw)
                .setPositiveButton("Save", (d, w) ->
                        prefs.edit().putBoolean(KEY_NOTIF_REMINDERS, sw.isChecked()).apply())
                .setNegativeButton("Close", null)
                .show();
    }

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

    private void loadAppointmentStats() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null || tvStatTotal == null) return;

        FirebaseFirestore.getInstance().collection("timeslots")
                .whereEqualTo("studentId", uid)
                .get()
                .addOnSuccessListener(q -> {
                    int total = 0;
                    int upcoming = 0;
                    int cancelled = 0;
                    for (QueryDocumentSnapshot doc : q) {
                        total++;
                        String st = doc.getString("status");
                        if ("BOOKED".equals(st) || "PENDING".equals(st)) {
                            upcoming++;
                        }
                        if ("CANCELLED".equals(st)) {
                            cancelled++;
                        }
                    }
                    tvStatTotal.setText(String.valueOf(total));
                    tvStatUpcoming.setText(String.valueOf(upcoming));
                    tvStatCancelled.setText(String.valueOf(cancelled));
                })
                .addOnFailureListener(e -> { /* keep 0 */ });
    }

    private void handleSignOut() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply();
        authController.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
