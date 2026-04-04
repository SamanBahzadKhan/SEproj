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

import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CounselorDashboardActivity.java
 * Main home screen for logged-in counselors.
 */
public class CounselorDashboardActivity extends AppCompatActivity {

    private TextView     tvWelcome, tvTodayCount, tvWeekCount, tvPending;
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
        tvPending      = findViewById(R.id.tvPending);
        rvAppointments = findViewById(R.id.rvAppointments);
        progressBar    = findViewById(R.id.progressBar);
        tvEmpty        = findViewById(R.id.tvEmpty);

        rvAppointments.setLayoutManager(new LinearLayoutManager(this));

        loadWelcomeName();
        loadAppointments();

        findViewById(R.id.btnEditAvailability).setOnClickListener(v ->
                startActivity(new Intent(this, AvailabilityActivity.class)));

        findViewById(R.id.navHome).setOnClickListener(v -> { });
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

        FirebaseFirestore.getInstance().collection("counselors").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("name") != null) {
                        tvWelcome.setText("Good Morning,\nDr. " + doc.getString("name"));
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

                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        long startOfDay = cal.getTimeInMillis();
                        long endOfDay = startOfDay + 86400000L;

                        Calendar weekCal = Calendar.getInstance();
                        weekCal.add(Calendar.DAY_OF_YEAR, 7);
                        long weekEnd = weekCal.getTimeInMillis();

                        int today = 0;
                        int week = 0;
                        int pending = 0;
                        for (Appointment a : appointments) {
                            AppointmentStatus st = a.getStatus();
                            if (st == AppointmentStatus.PENDING || st == AppointmentStatus.CONFIRMED) {
                                pending++;
                            }
                            if (a.getDate() != null) {
                                long t = a.getDate().toDate().getTime();
                                if (t >= startOfDay && t < endOfDay
                                        && (st == AppointmentStatus.CONFIRMED
                                        || st == AppointmentStatus.PENDING)) {
                                    today++;
                                }
                                if (t >= System.currentTimeMillis() && t <= weekEnd) {
                                    week++;
                                }
                            }
                        }

                        tvTodayCount.setText(String.valueOf(today));
                        tvWeekCount.setText(String.valueOf(week));
                        tvPending.setText(String.valueOf(pending));

                        List<Appointment> todayList = appointments.stream()
                                .filter(a -> {
                                    if (a.getDate() == null) return false;
                                    long t = a.getDate().toDate().getTime();
                                    AppointmentStatus st = a.getStatus();
                                    return t >= startOfDay && t < endOfDay
                                            && (st == AppointmentStatus.CONFIRMED
                                            || st == AppointmentStatus.PENDING);
                                })
                                .collect(Collectors.toList());

                        tvEmpty.setVisibility(todayList.isEmpty() ? View.VISIBLE : View.GONE);

                        rvAppointments.setAdapter(new AppointmentAdapter(todayList,
                                AppointmentAdapter.MODE_COUNSELOR,
                                appt -> confirmCancel(appt),
                                null, null,
                                appt -> appointmentController.markComplete(
                                        appt.getAppointmentId(),
                                        simpleCallback("Marked complete.")),
                                appt -> appointmentController.markNoShow(
                                        appt.getAppointmentId(),
                                        simpleCallback("Marked no-show."))));
                    }

                    @Override
                    public void onFailure(String error) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private AppointmentController.AppointmentCallback simpleCallback(String msg) {
        return new AppointmentController.AppointmentCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(CounselorDashboardActivity.this, msg, Toast.LENGTH_SHORT).show();
                loadAppointments();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(CounselorDashboardActivity.this,
                        error, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void confirmCancel(Appointment appt) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel appointment")
                .setMessage("Cancel this session with "
                        + (appt.getStudentName() != null ? appt.getStudentName() : "student") + "?")
                .setPositiveButton("Cancel session", (d, w) ->
                        appointmentController.cancelAppointment(
                                appt.getAppointmentId(), appt.getTimeSlotId(),
                                simpleCallback("Session cancelled.")))
                .setNegativeButton("No", null)
                .show();
    }
}
