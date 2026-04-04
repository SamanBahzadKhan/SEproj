package com.fridge.caps.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.controllers.AppointmentController;
import com.fridge.caps.models.Appointment;
import com.fridge.caps.models.AppointmentStatus;
import com.fridge.caps.views.adapters.AppointmentAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * StudentDashboardActivity.java
 * Main home screen for logged-in students.
 * Shows upcoming appointments, quick actions, and past appointment history.
 * View in the MVC pattern.
 */
public class StudentDashboardActivity extends AppCompatActivity {

    private TextView    tvWelcome;
    private RecyclerView rvUpcoming, rvPast;
    private ProgressBar progressBar;
    private TextView    tvNoUpcoming, tvNoPast;

    private AppointmentController appointmentController;
    private List<Appointment> allAppointments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        appointmentController = new AppointmentController();

        tvWelcome    = findViewById(R.id.tvWelcome);
        rvUpcoming   = findViewById(R.id.rvUpcoming);
        rvPast       = findViewById(R.id.rvPast);
        progressBar  = findViewById(R.id.progressBar);
        tvNoUpcoming = findViewById(R.id.tvNoUpcoming);
        tvNoPast     = findViewById(R.id.tvNoPast);

        rvUpcoming.setLayoutManager(new LinearLayoutManager(this));
        rvPast.setLayoutManager(new LinearLayoutManager(this));

        loadWelcomeName();
        loadAppointments();

        // Quick action buttons
        findViewById(R.id.btnBookAppointment).setOnClickListener(v ->
                startActivity(new Intent(this, CounselorListActivity.class)));
        findViewById(R.id.btnViewProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.btnNotifications).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
        findViewById(R.id.btnHistory).setOnClickListener(v ->
                startActivity(new Intent(this, AppointmentsActivity.class)));

        // Bottom nav
        findViewById(R.id.navHome).setOnClickListener(v -> { /* already here */ });
        findViewById(R.id.navCounsel).setOnClickListener(v ->
                startActivity(new Intent(this, CounselorListActivity.class)));
        findViewById(R.id.navAppts).setOnClickListener(v ->
                startActivity(new Intent(this, AppointmentsActivity.class)));
        findViewById(R.id.navAlerts).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
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

        FirebaseFirestore.getInstance().collection("students").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("name") != null) {
                        tvWelcome.setText("Welcome back,\n" + doc.getString("name"));
                    }
                });
    }

    private void loadAppointments() {
        progressBar.setVisibility(View.VISIBLE);
        appointmentController.getStudentAppointments(new AppointmentController.AppointmentListCallback() {
            @Override
            public void onSuccess(List<Appointment> appointments) {
                progressBar.setVisibility(View.GONE);
                allAppointments = appointments;

                List<Appointment> upcoming = appointments.stream()
                        .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED
                                || a.getStatus() == AppointmentStatus.PENDING)
                        .collect(Collectors.toList());

                List<Appointment> past = appointments.stream()
                        .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED
                                || a.getStatus() == AppointmentStatus.CANCELLED
                                || a.getStatus() == AppointmentStatus.NO_SHOW)
                        .collect(Collectors.toList());

                tvNoUpcoming.setVisibility(upcoming.isEmpty() ? View.VISIBLE : View.GONE);
                tvNoPast.setVisibility(past.isEmpty() ? View.VISIBLE : View.GONE);

                rvUpcoming.setAdapter(new AppointmentAdapter(upcoming,
                        AppointmentAdapter.MODE_STUDENT_UPCOMING,
                        appt -> cancelAppointment(appt),
                        appt -> rescheduleAppointment(appt),
                        null, null, null));

                rvPast.setAdapter(new AppointmentAdapter(past,
                        AppointmentAdapter.MODE_STUDENT_PAST,
                        null, null,
                        appt -> {
                            Intent i = new Intent(StudentDashboardActivity.this,
                                    FeedbackActivity.class);
                            i.putExtra("appointmentId", appt.getAppointmentId());
                            i.putExtra("counselorId", appt.getCounselorId());
                            i.putExtra("counselorName", appt.getCounselorName());
                            startActivity(i);
                        },
                        null, null));
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(StudentDashboardActivity.this, "Failed to load appointments.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelAppointment(Appointment appt) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel your appointment with "
                        + appt.getCounselorName() + "?")
                .setPositiveButton("Yes, Cancel", (d, w) -> {
                    appointmentController.cancelAppointment(
                            appt.getAppointmentId(), appt.getTimeSlotId(),
                            new AppointmentController.AppointmentCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(StudentDashboardActivity.this,
                                            "Appointment cancelled.", Toast.LENGTH_SHORT).show();
                                    loadAppointments();
                                }
                                @Override
                                public void onFailure(String error) {
                                    Toast.makeText(StudentDashboardActivity.this,
                                            "Failed: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void rescheduleAppointment(Appointment appt) {
        Intent i = new Intent(this, TimeSlotsActivity.class);
        i.putExtra(TimeSlotsActivity.EXTRA_COUNSELOR_ID, appt.getCounselorId());
        i.putExtra(TimeSlotsActivity.EXTRA_COUNSELOR_NAME, appt.getCounselorName());
        i.putExtra(TimeSlotsActivity.EXTRA_RESCHEDULE_APPOINTMENT_ID, appt.getAppointmentId());
        i.putExtra(TimeSlotsActivity.EXTRA_OLD_SLOT_ID, appt.getTimeSlotId());
        startActivity(i);
    }
}