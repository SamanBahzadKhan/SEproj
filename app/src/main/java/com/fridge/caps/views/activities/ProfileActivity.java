package com.fridge.caps.views.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.bumptech.glide.Glide;
import com.fridge.caps.R;
import com.fridge.caps.controllers.AuthController;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Student profile, stats from {@code timeslots}, and settings.
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private static final String PREFS = "caps_prefs";
    private static final String KEY_NOTIF_REMINDERS = "notif_reminders_enabled";

    private TextView    tvUsername, tvStudentId, tvEmail, tvPhone,
            tvDepartment, tvYearOfStudy;
    private TextView    tvStatTotal, tvStatUpcoming, tvStatCancelled;
    private ProgressBar progressBar;
    private CircleImageView ivAvatar;
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
        ivAvatar         = findViewById(R.id.ivAvatar);

        if (tvStatTotal != null) tvStatTotal.setText("0");
        if (tvStatUpcoming != null) tvStatUpcoming.setText("0");
        if (tvStatCancelled != null) tvStatCancelled.setText("0");

        loadStudentProfile();

        View edit = findViewById(R.id.btnEditProfile);
        if (edit != null) {
            edit.setOnClickListener(v ->
                    startActivity(new Intent(this, EditProfileActivity.class)));
        }

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

    @Override
    protected void onResume() {
        super.onResume();
        loadAppointmentStats();
    }

    private void loadStudentProfile() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection("students")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    progressBar.setVisibility(View.GONE);
                    if (!doc.exists()) {
                        Log.w(TAG, "No student document for uid: " + uid);
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        return;
                    }

                    String name = doc.getString("name");
                    String studentId = doc.getString("studentId");
                    String email = doc.getString("email");
                    String phone = doc.getString("phone");
                    String department = doc.getString("department");
                    String yearOfStudy = doc.getString("yearOfStudy");
                    String profilePicUrl = doc.getString("profilePictureUrl");

                    tvUsername.setText(name != null && !name.isEmpty() ? name : "Not set");

                    if (studentId != null && !studentId.isEmpty()) {
                        tvStudentId.setText(studentId);
                    } else if (uid.length() >= 8) {
                        tvStudentId.setText(uid.substring(0, 8).toUpperCase(java.util.Locale.US));
                    } else {
                        tvStudentId.setText("—");
                    }

                    tvEmail.setText(email != null && !email.isEmpty() ? email : "Not set");
                    tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "Not set");
                    tvDepartment.setText(department != null && !department.isEmpty() ? department : "Not set");
                    tvYearOfStudy.setText(yearOfStudy != null && !yearOfStudy.isEmpty() ? yearOfStudy : "Not set");

                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        Glide.with(this).load(profilePicUrl).circleCrop().into(ivAvatar);
                    } else {
                        ivAvatar.setImageResource(R.drawable.circle_blue_bg);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Failed to load profile: " + (e.getMessage() != null ? e.getMessage() : ""));
                    Toast.makeText(this, "Failed to load profile. Check connection.",
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadAppointmentStats() {
        String currentStudentId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentStudentId == null || tvStatTotal == null) return;

        FirebaseFirestore.getInstance().collection("timeslots")
                .whereEqualTo("studentId", currentStudentId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int total = 0;
                    int upcoming = 0;
                    int cancelled = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc
                            : querySnapshot.getDocuments()) {
                        String status = doc.getString("status");
                        if (status == null) continue;
                        switch (status) {
                            case "PENDING":
                            case "BOOKED":
                                upcoming++;
                                total++;
                                break;
                            case "COMPLETED":
                            case "NO_SHOW":
                                total++;
                                break;
                            case "CANCELLED":
                                cancelled++;
                                total++;
                                break;
                            default:
                                break;
                        }
                    }
                    tvStatTotal.setText(String.valueOf(total));
                    tvStatUpcoming.setText(String.valueOf(upcoming));
                    tvStatCancelled.setText(String.valueOf(cancelled));
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Query failed: " + (e.getMessage() != null ? e.getMessage() : ""), e));
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
