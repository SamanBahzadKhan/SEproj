package com.fridge.caps.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.controllers.AppointmentController;
import com.fridge.caps.models.Appointment;
import com.fridge.caps.views.adapters.AppointmentAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * CounselorDashboardActivity.java
 * Main home screen for logged-in counselors (US-11, US-12, US-13, US-14).
 * Shows today's appointments and availability management.
 * View in the MVC pattern.
 */
public class CounselorDashboardActivity extends AppCompatActivity {

    private TextView     tvWelcome, tvTodayCount, tvWeekCount;
    private RecyclerView rvAppointments;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;

    private AppointmentController appointmentController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_dashboard);

        appointmentController = new AppointmentController();

        tvWelcome      = findViewById(R.id.tvWelcome);
        tvTodayCount   = findViewById(R.id.tvTodayCount);
        tvWeekCount    = findViewById(R.id.tvWeekCount);
        rvAppointments = findViewById(R.id.rvAppointments);
        progressBar    = findViewById(R.id.progressBar);
        tvEmpty        = findViewById(R.id.tvEmpty);

        rvAppointments.setLayoutManager(new LinearLayoutManager(this));

        loadWelcomeName();
        loadAppointments();

        findViewById(R.id.btnEditAvailability).setOnClickListener(v ->
                startActivity(new Intent(this, AvailabilityActivity.class)));

        // Bottom nav
        findViewById(R.id.navDashboard).setOnClickListener(v -> { });
        findViewById(R.id.navAppointments).setOnClickListener(v ->
                startActivity(new Intent(this, AppointmentsActivity.class)));
        findViewById(R.id.navAvailability).setOnClickListener(v ->
                startActivity(new Intent(this, AvailabilityActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppointments();
    }

    private void loadWelcomeName() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("counselors").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("name") != null) {
                        tvWelcome.setText("Good Morning,\n" + doc.getString("name"));
                    }
                });
    }

    private void loadAppointments() {
        progressBar.setVisibility(View.VISIBLE);
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        appointmentController.getCounselorAppointments(uid,
                new AppointmentController.AppointmentListCallback() {
                    @Override
                    public void onSuccess(List<Appointment> appointments) {
                        progressBar.setVisibility(View.GONE);
                        tvTodayCount.setText(String.valueOf(appointments.size()));
                        tvWeekCount.setText(String.valueOf(appointments.size()));
                        tvEmpty.setVisibility(appointments.isEmpty() ? View.VISIBLE : View.GONE);

                        rvAppointments.setAdapter(new AppointmentAdapter(
                                appointments, true,
                                null, null,
                                appt -> appointmentController.markNoShow(
                                        appt.getAppointmentId(),
                                        new AppointmentController.AppointmentCallback() {
                                            @Override public void onSuccess() {
                                                Toast.makeText(CounselorDashboardActivity.this,
                                                        "Marked as no-show.", Toast.LENGTH_SHORT).show();
                                                loadAppointments();
                                            }
                                            @Override public void onFailure(String e) {}
                                        })));
                    }

                    @Override
                    public void onFailure(String error) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }
}