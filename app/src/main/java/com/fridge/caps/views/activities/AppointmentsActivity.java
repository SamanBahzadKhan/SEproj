package com.fridge.caps.views.activities;
//this shit 
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
import com.fridge.caps.controllers.NotificationController;
import com.fridge.caps.models.Appointment;
import com.fridge.caps.models.AppointmentStatus;
import com.fridge.caps.views.adapters.AppointmentAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AppointmentsActivity — upcoming / past for students; counsellors see their sessions.
 */
public class AppointmentsActivity extends AppCompatActivity {

    private TabLayout    tabLayout;
    private RecyclerView recyclerView;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;

    private AppointmentController  appointmentController;
    private NotificationController notificationController;
    private List<Appointment> allAppointments = new ArrayList<>();
    private boolean isStudent = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointments);

        appointmentController  = new AppointmentController();
        notificationController = new NotificationController();

        tabLayout    = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Appointments");
        }

        tabLayout.addTab(tabLayout.newTab().setText("Upcoming"));
        tabLayout.addTab(tabLayout.newTab().setText("Past"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { filterList(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadAppointments();
    }

    private void loadAppointments() {
        progressBar.setVisibility(View.VISIBLE);
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        FirebaseFirestore.getInstance().collection("students").document(uid).get()
                .addOnSuccessListener(doc -> {
                    isStudent = doc.exists();
                    if (isStudent) {
                        appointmentController.getStudentAppointments(
                                new AppointmentController.AppointmentListCallback() {
                                    @Override
                                    public void onSuccess(List<Appointment> appointments) {
                                        progressBar.setVisibility(View.GONE);
                                        allAppointments = appointments;
                                        filterList(tabLayout.getSelectedTabPosition());
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(AppointmentsActivity.this,
                                                "Failed to load.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        appointmentController.getCounselorAppointments(uid,
                                new AppointmentController.AppointmentListCallback() {
                                    @Override
                                    public void onSuccess(List<Appointment> appointments) {
                                        progressBar.setVisibility(View.GONE);
                                        allAppointments = appointments;
                                        filterList(tabLayout.getSelectedTabPosition());
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(AppointmentsActivity.this,
                                                "Failed to load.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> progressBar.setVisibility(View.GONE));
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

        if (isStudent) {
            int mode = tabPosition == 0
                    ? AppointmentAdapter.MODE_STUDENT_UPCOMING
                    : AppointmentAdapter.MODE_STUDENT_PAST;
            recyclerView.setAdapter(new AppointmentAdapter(filtered, mode,
                    tabPosition == 0 ? this::cancelAppt : null,
                    tabPosition == 0 ? this::rescheduleAppt : null,
                    tabPosition == 1 ? appt -> {
                        Intent i = new Intent(this, FeedbackActivity.class);
                        i.putExtra(FeedbackActivity.EXTRA_TIMESLOT_ID, appt.getTimeSlotId());
                        i.putExtra(FeedbackActivity.EXTRA_COUNSELOR_ID, appt.getCounselorId());
                        i.putExtra(FeedbackActivity.EXTRA_COUNSELOR_NAME, appt.getCounselorName());
                        i.putExtra(FeedbackActivity.EXTRA_COUNSELOR_SPECIALIZATION, "");
                        String dateLine = "";
                        if (appt.getDate() != null) {
                            dateLine = new java.text.SimpleDateFormat(
                                    com.fridge.caps.utils.DateUtils.DISPLAY_DATE,
                                    java.util.Locale.US).format(appt.getDate().toDate());
                            if (appt.getTimeDisplay() != null) {
                                dateLine = dateLine + " · " + appt.getTimeDisplay();
                            }
                        }
                        i.putExtra(FeedbackActivity.EXTRA_APPOINTMENT_DATE, dateLine);
                        startActivity(i);
                    } : null,
                    null, null));
        } else {
            int mode = tabPosition == 0
                    ? AppointmentAdapter.MODE_COUNSELOR
                    : AppointmentAdapter.MODE_ADMIN;
            recyclerView.setAdapter(new AppointmentAdapter(filtered, mode,
                    tabPosition == 0 ? this::counselorCancel : null,
                    null, null,
                    tabPosition == 0 ? this::showCounselorCompleteDialog : null,
                    null));
        }
    }

    private void showCounselorCompleteDialog(Appointment appt) {
        new AlertDialog.Builder(this)
                .setTitle("Update session")
                .setItems(new String[]{
                        "Mark as Completed",
                        "Mark as No-Show",
                        "Cancel"
                }, (d, which) -> {
                    if (which == 2) return;
                    String tid = appt.getTimeSlotId();
                    if (which == 0) {
                        appointmentController.markComplete(tid, new AppointmentController.AppointmentCallback() {
                            @Override
                            public void onSuccess() {
                                notificationController.sendSessionCompleted(
                                        appt.getStudentId(), appt.getCounselorName());
                                Toast.makeText(AppointmentsActivity.this,
                                        "Marked complete.", Toast.LENGTH_SHORT).show();
                                loadAppointments();
                            }

                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(AppointmentsActivity.this,
                                        error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        appointmentController.markNoShow(tid, new AppointmentController.AppointmentCallback() {
                            @Override
                            public void onSuccess() {
                                String line = appt.getDate() != null
                                        ? new java.text.SimpleDateFormat(
                                        com.fridge.caps.utils.DateUtils.DISPLAY_DATE,
                                        java.util.Locale.US).format(appt.getDate().toDate()) : "";
                                notificationController.sendMissedSession(
                                        appt.getStudentId(), appt.getCounselorName(), line);
                                Toast.makeText(AppointmentsActivity.this,
                                        "Marked no-show.", Toast.LENGTH_SHORT).show();
                                loadAppointments();
                            }

                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(AppointmentsActivity.this,
                                        error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .show();
    }

    private AppointmentController.AppointmentCallback simpleCb(String msg) {
        return new AppointmentController.AppointmentCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AppointmentsActivity.this, msg, Toast.LENGTH_SHORT).show();
                loadAppointments();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AppointmentsActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void counselorCancel(Appointment appt) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel session")
                .setMessage("Cancel this appointment?")
                .setPositiveButton("Yes", (d, w) -> appointmentController.cancelAppointment(
                        appt.getAppointmentId(), appt.getTimeSlotId(), simpleCb("Cancelled.")))
                .setNegativeButton("No", null)
                .show();
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
        Intent i = new Intent(this, BookAppointmentActivity.class);
        i.putExtra(BookAppointmentActivity.EXTRA_COUNSELOR_ID, appt.getCounselorId());
        i.putExtra(BookAppointmentActivity.EXTRA_COUNSELOR_NAME, appt.getCounselorName());
        i.putExtra(BookAppointmentActivity.EXTRA_RESCHEDULE_APPOINTMENT_ID, appt.getAppointmentId());
        i.putExtra(BookAppointmentActivity.EXTRA_OLD_SLOT_ID, appt.getTimeSlotId());
        startActivity(i);
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
