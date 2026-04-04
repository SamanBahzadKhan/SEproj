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
import com.fridge.caps.models.AppointmentStatus;
import com.fridge.caps.views.adapters.AppointmentAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AppointmentsActivity.java
 * Shows student's upcoming and past appointments in two tabs (US-13, US-15).
 * View in the MVC pattern.
 */
public class AppointmentsActivity extends AppCompatActivity {

    private TabLayout    tabLayout;
    private RecyclerView recyclerView;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;

    private AppointmentController appointmentController;
    private List<Appointment> allAppointments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointments);

        appointmentController = new AppointmentController();

        tabLayout    = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Appointments");
        }

        loadAppointments();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { filterList(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadAppointments() {
        progressBar.setVisibility(View.VISIBLE);
        appointmentController.getStudentAppointments(new AppointmentController.AppointmentListCallback() {
            @Override
            public void onSuccess(List<Appointment> appointments) {
                progressBar.setVisibility(View.GONE);
                allAppointments = appointments;
                filterList(tabLayout.getSelectedTabPosition());
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AppointmentsActivity.this, "Failed to load.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterList(int tabPosition) {
        List<Appointment> filtered;
        if (tabPosition == 0) {
            filtered = allAppointments.stream()
                    .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED
                            || a.getStatus() == AppointmentStatus.PENDING)
                    .collect(Collectors.toList());
        } else {
            filtered = allAppointments.stream()
                    .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED
                            || a.getStatus() == AppointmentStatus.CANCELLED
                            || a.getStatus() == AppointmentStatus.NO_SHOW)
                    .collect(Collectors.toList());
        }

        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmpty.setText(tabPosition == 0 ? "No upcoming appointments." : "No past appointments.");

        recyclerView.setAdapter(new AppointmentAdapter(filtered,
                tabPosition == 0,
                appt -> cancelAppt(appt),
                appt -> rescheduleAppt(appt),
                appt -> {
                    Intent i = new Intent(this, FeedbackActivity.class);
                    i.putExtra("appointmentId", appt.getAppointmentId());
                    i.putExtra("counselorId", appt.getCounselorId());
                    i.putExtra("counselorName", appt.getCounselorName());
                    startActivity(i);
                }));
    }

    private void cancelAppt(Appointment appt) {
        appointmentController.cancelAppointment(appt.getAppointmentId(),
                appt.getTimeSlotId(), new AppointmentController.AppointmentCallback() {
                    @Override public void onSuccess() {
                        Toast.makeText(AppointmentsActivity.this, "Cancelled.", Toast.LENGTH_SHORT).show();
                        loadAppointments();
                    }
                    @Override public void onFailure(String error) {
                        Toast.makeText(AppointmentsActivity.this, "Failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void rescheduleAppt(Appointment appt) {
        Intent i = new Intent(this, TimeSlotsActivity.class);
        i.putExtra(TimeSlotsActivity.EXTRA_COUNSELOR_ID, appt.getCounselorId());
        i.putExtra("rescheduleAppointmentId", appt.getAppointmentId());
        i.putExtra("oldSlotId", appt.getTimeSlotId());
        startActivity(i);
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}