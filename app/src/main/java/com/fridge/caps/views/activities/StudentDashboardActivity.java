package com.fridge.caps.views.activities;

/**
 * StudentDashboardActivity.java
 * Main home screen for logged-in students showing upcoming appointments and quick navigation options.
 * Displays upcoming and past appointment lists with action buttons for booking, profile, and notifications.
 * View in the MVC pattern.
 */
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
import com.fridge.caps.utils.GreetingUtils;
import com.fridge.caps.views.BottomNavUi;
import com.fridge.caps.views.adapters.AppointmentAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

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

    private AppointmentController  appointmentController;
    private NotificationController notificationController;
    private ListenerRegistration   unreadListener;
    private View                     bellBadge;
    private boolean                  isTestMode;

    private List<Appointment> allAppointments = new ArrayList<>();

    /** Which bottom tab is visually selected (updated when launching destinations from the bar). */
    private int selectedBottomNavId = R.id.navHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        appointmentController  = new AppointmentController();
        notificationController = new NotificationController();

        tvWelcome    = findViewById(R.id.tvWelcome);
        rvUpcoming   = findViewById(R.id.rvUpcoming);
        rvPast       = findViewById(R.id.rvPast);
        progressBar  = findViewById(R.id.progressBar);
        tvNoUpcoming = findViewById(R.id.tvNoUpcoming);
        tvNoPast     = findViewById(R.id.tvNoPast);
        bellBadge    = findViewById(R.id.bellBadge);
        isTestMode = getIntent().getBooleanExtra("TEST_MODE", false);

        rvUpcoming.setLayoutManager(new LinearLayoutManager(this));
        rvPast.setLayoutManager(new LinearLayoutManager(this));

        if (!isTestMode) {
            loadWelcomeName();
            attachUnreadBadge();
        } else {
            tvWelcome.setText(GreetingUtils.greetingWithName("Ahmad Raza"));
            tvNoUpcoming.setVisibility(View.VISIBLE);
            tvNoPast.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.topBarBell).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
        findViewById(R.id.topBarSettings).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        // Quick action buttons
        findViewById(R.id.btnBookAppointment).setOnClickListener(v ->
                startActivity(new Intent(this, CounselorListActivity.class)));
        findViewById(R.id.btnJournal).setOnClickListener(v ->
                startActivity(new Intent(this, MyJournalActivity.class)));
        findViewById(R.id.btnJournalEntriesList).setOnClickListener(v ->
                startActivity(new Intent(this, JournalEntriesListActivity.class)));
        findViewById(R.id.btnNotifications).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
        findViewById(R.id.btnHistory).setOnClickListener(v ->
                startActivity(new Intent(this, StudentAppointmentHistoryActivity.class)));
        findViewById(R.id.btnSessionNotesList).setOnClickListener(v ->
                startActivity(new Intent(this, StudentSessionNotesListActivity.class)));

        // Bottom nav
        findViewById(R.id.navHome).setOnClickListener(v -> {
            selectedBottomNavId = R.id.navHome;
            BottomNavUi.applyStudentNav(this, selectedBottomNavId);
        });
        findViewById(R.id.navCounsel).setOnClickListener(v -> {
            selectedBottomNavId = R.id.navCounsel;
            BottomNavUi.applyStudentNav(this, selectedBottomNavId);
            startActivity(new Intent(this, CounselorListActivity.class));
        });
        findViewById(R.id.navAppts).setOnClickListener(v -> {
            selectedBottomNavId = R.id.navAppts;
            BottomNavUi.applyStudentNav(this, selectedBottomNavId);
            startActivity(new Intent(this, AppointmentsActivity.class));
        });
        findViewById(R.id.navAlerts).setOnClickListener(v -> {
            selectedBottomNavId = R.id.navAlerts;
            BottomNavUi.applyStudentNav(this, selectedBottomNavId);
            startActivity(new Intent(this, NotificationsActivity.class));
        });
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            selectedBottomNavId = R.id.navProfile;
            BottomNavUi.applyStudentNav(this, selectedBottomNavId);
            startActivity(new Intent(this, ProfileActivity.class));
        });
    }

    private void attachUnreadBadge() {
        unreadListener = notificationController.listenUnreadCount((snap, e) -> {
            if (e != null || snap == null || bellBadge == null) {
                if (bellBadge != null) bellBadge.setVisibility(View.GONE);
                return;
            }
            bellBadge.setVisibility(snap.size() > 0 ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (unreadListener != null) {
            unreadListener.remove();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavUi.applyStudentNav(this, selectedBottomNavId);

        if (isTestMode) {
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("students").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        return;
                    }
                    loadAppointments();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("StudentDashboard",
                            "Session check failed: " + (e.getMessage() != null ? e.getMessage() : ""));
                    loadAppointments();
                });
    }

    private void loadWelcomeName() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("students").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("name") != null) {
                        tvWelcome.setText(GreetingUtils.greetingWithName(doc.getString("name")));
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
                            appt.getTimeSlotId(), appt.getTimeSlotId(),
                            new AppointmentController.AppointmentCallback() {
                                @Override
                                public void onSuccess() {
                                    String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                                            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                                    if (uid == null) {
                                        Toast.makeText(StudentDashboardActivity.this,
                                                "Appointment cancelled.", Toast.LENGTH_SHORT).show();
                                        loadAppointments();
                                        return;
                                    }
                                    FirebaseFirestore.getInstance().collection("students")
                                            .document(uid).get()
                                            .addOnSuccessListener(doc -> {
                                                String sn = doc.getString("name");
                                                notifyCounselorCancel(appt,
                                                        sn != null ? sn : "Student");
                                                Toast.makeText(StudentDashboardActivity.this,
                                                        "Appointment cancelled.", Toast.LENGTH_SHORT).show();
                                                loadAppointments();
                                            })
                                            .addOnFailureListener(e -> {
                                                notifyCounselorCancel(appt, "Student");
                                                Toast.makeText(StudentDashboardActivity.this,
                                                        "Appointment cancelled.", Toast.LENGTH_SHORT).show();
                                                loadAppointments();
                                            });
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

    private void notifyCounselorCancel(Appointment appt, String studentName) {
        String dateLine = "";
        if (appt.getDate() != null) {
            dateLine = new java.text.SimpleDateFormat(
                    com.fridge.caps.utils.DateUtils.DISPLAY_DATE, java.util.Locale.US)
                    .format(appt.getDate().toDate());
        }
        notificationController.sendStudentCancelledCounselor(
                appt.getCounselorId(), studentName, dateLine, appt.getTimeDisplay());
    }

    private void rescheduleAppointment(Appointment appt) {
        Intent i = new Intent(this, BookAppointmentActivity.class);
        i.putExtra(BookAppointmentActivity.EXTRA_COUNSELOR_ID, appt.getCounselorId());
        i.putExtra(BookAppointmentActivity.EXTRA_COUNSELOR_NAME, appt.getCounselorName());
        i.putExtra(BookAppointmentActivity.EXTRA_RESCHEDULE_APPOINTMENT_ID, appt.getAppointmentId());
        i.putExtra(BookAppointmentActivity.EXTRA_OLD_SLOT_ID, appt.getTimeSlotId());
        startActivity(i);
    }
}