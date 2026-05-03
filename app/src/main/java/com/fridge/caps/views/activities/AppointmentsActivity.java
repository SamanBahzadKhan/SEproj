package com.fridge.caps.views.activities;


/**
 * Purpose: Handles screen flow, UI state coordination, and user interactions.
 * Depends on: Android UI toolkit, app controllers/viewmodels, and navigation intents.
 * Notes: Focuses on presentation logic while delegating business rules to controllers.
 */
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.controllers.AppointmentController;
import com.fridge.caps.controllers.NotificationController;
import com.fridge.caps.models.Appointment;
import com.fridge.caps.models.AppointmentStatus;
import com.fridge.caps.utils.DateUtils;
import com.fridge.caps.views.adapters.AppointmentAdapter;
import com.fridge.caps.views.adapters.StudentPastAppointmentsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AppointmentsActivity extends AppCompatActivity {

    private enum PastFilter { ALL, COMPLETED, CANCELLED, NO_SHOW }

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private View chipScroll;
    private TextView tabUpcoming, tabPast;
    private View underlineUpcoming, underlinePast;
    private TextView chipAll, chipCompleted, chipCancelled, chipNoShow;

    private final AppointmentController appointmentController = new AppointmentController();
    private final NotificationController notificationController = new NotificationController();
    private List<Appointment> upcoming = new ArrayList<>();
    private List<Appointment> pastAll = new ArrayList<>();
    private Map<String, StudentPastAppointmentsAdapter.Snippet> feedbackMap = new HashMap<>();

    private boolean showUpcoming = true;
    private PastFilter pastFilter = PastFilter.ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointments);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        chipScroll = findViewById(R.id.chipScroll);
        tabUpcoming = findViewById(R.id.tabUpcoming);
        tabPast = findViewById(R.id.tabPast);
        underlineUpcoming = findViewById(R.id.underlineUpcoming);
        underlinePast = findViewById(R.id.underlinePast);
        chipAll = findViewById(R.id.chipAll);
        chipCompleted = findViewById(R.id.chipCompleted);
        chipCancelled = findViewById(R.id.chipCancelled);
        chipNoShow = findViewById(R.id.chipNoShow);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.topBarBack).setOnClickListener(v -> finish());
        findViewById(R.id.tabUpcomingWrap).setOnClickListener(v -> selectTab(true));
        findViewById(R.id.tabPastWrap).setOnClickListener(v -> selectTab(false));

        chipAll.setOnClickListener(v -> setPastFilter(PastFilter.ALL));
        chipCompleted.setOnClickListener(v -> setPastFilter(PastFilter.COMPLETED));
        chipCancelled.setOnClickListener(v -> setPastFilter(PastFilter.CANCELLED));
        chipNoShow.setOnClickListener(v -> setPastFilter(PastFilter.NO_SHOW));

        selectTab(true);
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void selectTab(boolean upcoming) {
        showUpcoming = upcoming;
        chipScroll.setVisibility(upcoming ? View.GONE : View.VISIBLE);
        if (upcoming) {
            tabUpcoming.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tabPast.setTypeface(android.graphics.Typeface.DEFAULT);
            tabUpcoming.setTextColor(ContextCompat.getColor(this, R.color.caps_palette_neutral_dark));
            tabPast.setTextColor(ContextCompat.getColor(this, R.color.caps_palette_grey_warm));
            underlineUpcoming.setVisibility(View.VISIBLE);
            underlinePast.setVisibility(View.GONE);
        } else {
            tabPast.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tabUpcoming.setTypeface(android.graphics.Typeface.DEFAULT);
            tabPast.setTextColor(ContextCompat.getColor(this, R.color.caps_palette_neutral_dark));
            tabUpcoming.setTextColor(ContextCompat.getColor(this, R.color.caps_palette_grey_warm));
            underlinePast.setVisibility(View.VISIBLE);
            underlineUpcoming.setVisibility(View.GONE);
        }
        bindList();
    }

    private void setPastFilter(PastFilter f) {
        pastFilter = f;
        chipAll.setBackgroundResource(f == PastFilter.ALL
                ? R.drawable.bg_chip_filter_past_selected : R.drawable.bg_chip_filter_past);
        chipCompleted.setBackgroundResource(f == PastFilter.COMPLETED
                ? R.drawable.bg_chip_filter_past_selected : R.drawable.bg_chip_filter_past);
        chipCancelled.setBackgroundResource(f == PastFilter.CANCELLED
                ? R.drawable.bg_chip_filter_past_selected : R.drawable.bg_chip_filter_past);
        chipNoShow.setBackgroundResource(f == PastFilter.NO_SHOW
                ? R.drawable.bg_chip_filter_past_selected : R.drawable.bg_chip_filter_past);

        chipAll.setTypeface(f == PastFilter.ALL ? android.graphics.Typeface.DEFAULT_BOLD
                : android.graphics.Typeface.DEFAULT);
        chipCompleted.setTypeface(f == PastFilter.COMPLETED ? android.graphics.Typeface.DEFAULT_BOLD
                : android.graphics.Typeface.DEFAULT);
        chipCancelled.setTypeface(f == PastFilter.CANCELLED ? android.graphics.Typeface.DEFAULT_BOLD
                : android.graphics.Typeface.DEFAULT);
        chipNoShow.setTypeface(f == PastFilter.NO_SHOW ? android.graphics.Typeface.DEFAULT_BOLD
                : android.graphics.Typeface.DEFAULT);
        bindList();
    }

    private void load() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            finish();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        appointmentController.getStudentAppointments(new AppointmentController.AppointmentListCallback() {
            @Override
            public void onSuccess(List<Appointment> appointments) {
                upcoming = appointments.stream()
                        .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED
                                || a.getStatus() == AppointmentStatus.PENDING)
                        .collect(Collectors.toList());
                pastAll = appointments.stream()
                        .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED
                                || a.getStatus() == AppointmentStatus.CANCELLED
                                || a.getStatus() == AppointmentStatus.NO_SHOW)
                        .collect(Collectors.toList());
                loadFeedback(uid);
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AppointmentsActivity.this, "Failed to load appointments.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFeedback(String studentId) {
        FirebaseFirestore.getInstance().collection("feedback")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(q -> {
                    feedbackMap = new HashMap<>();
                    for (QueryDocumentSnapshot doc : q) {
                        String tid = doc.getString("timeslotId");
                        Long r = doc.getLong("rating");
                        String c = doc.getString("comment");
                        if (tid != null && r != null) {
                            feedbackMap.put(tid, new StudentPastAppointmentsAdapter.Snippet(
                                    r.intValue(), c != null ? c : ""));
                        }
                    }
                    progressBar.setVisibility(View.GONE);
                    bindList();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    feedbackMap = new HashMap<>();
                    bindList();
                });
    }

    private List<Appointment> filteredPast() {
        return pastAll.stream().filter(a -> {
            AppointmentStatus st = a.getStatus();
            switch (pastFilter) {
                case COMPLETED:
                    return st == AppointmentStatus.COMPLETED;
                case CANCELLED:
                    return st == AppointmentStatus.CANCELLED;
                case NO_SHOW:
                    return st == AppointmentStatus.NO_SHOW;
                default:
                    return true;
            }
        }).collect(Collectors.toList());
    }

    private void bindList() {
        if (showUpcoming) {
            tvEmpty.setVisibility(upcoming.isEmpty() ? View.VISIBLE : View.GONE);
            if (upcoming.isEmpty()) {
                recyclerView.setAdapter(null);
            } else {
                recyclerView.setAdapter(new AppointmentAdapter(upcoming,
                        AppointmentAdapter.MODE_STUDENT_UPCOMING,
                        this::cancelAppointment,
                        this::rescheduleAppointment,
                        null, null, null));
            }
        } else {
            List<Appointment> past = filteredPast();
            tvEmpty.setVisibility(past.isEmpty() ? View.VISIBLE : View.GONE);
            if (past.isEmpty()) {
                recyclerView.setAdapter(null);
            } else {
                recyclerView.setAdapter(new StudentPastAppointmentsAdapter(past, feedbackMap,
                        this::openFeedback));
            }
        }
    }

    private void openFeedback(Appointment a) {
        Intent i = new Intent(this, FeedbackActivity.class);
        i.putExtra(FeedbackActivity.EXTRA_TIMESLOT_ID, a.getTimeSlotId());
        i.putExtra(FeedbackActivity.EXTRA_APPOINTMENT_ID, a.getTimeSlotId());
        i.putExtra(FeedbackActivity.EXTRA_COUNSELOR_ID, a.getCounselorId());
        i.putExtra(FeedbackActivity.EXTRA_COUNSELOR_NAME, a.getCounselorName());
        i.putExtra(FeedbackActivity.EXTRA_COUNSELOR_SPECIALIZATION, "");
        String dateLine = "";
        if (a.getDate() != null) {
            dateLine = new java.text.SimpleDateFormat(DateUtils.DISPLAY_DATE, java.util.Locale.US)
                    .format(a.getDate().toDate());
            if (a.getTimeDisplay() != null) {
                dateLine = dateLine + " · " + a.getTimeDisplay();
            }
        }
        i.putExtra(FeedbackActivity.EXTRA_APPOINTMENT_DATE, dateLine);
        startActivity(i);
    }

    private void cancelAppointment(Appointment appt) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel your appointment with "
                        + appt.getCounselorName() + "?")
                .setPositiveButton("Yes, Cancel", (d, w) ->
                        appointmentController.cancelAppointment(
                                appt.getTimeSlotId(), appt.getTimeSlotId(),
                                new AppointmentController.AppointmentCallback() {
                                    @Override
                                    public void onSuccess() {
                                        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                                                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                                        if (uid == null) {
                                            Toast.makeText(AppointmentsActivity.this,
                                                    "Appointment cancelled.", Toast.LENGTH_SHORT).show();
                                            load();
                                            return;
                                        }
                                        FirebaseFirestore.getInstance().collection("students")
                                                .document(uid).get()
                                                .addOnSuccessListener(doc -> {
                                                    String sn = doc.getString("name");
                                                    notifyCounselorCancel(appt,
                                                            sn != null ? sn : "Student");
                                                    Toast.makeText(AppointmentsActivity.this,
                                                            "Appointment cancelled.", Toast.LENGTH_SHORT).show();
                                                    load();
                                                })
                                                .addOnFailureListener(e -> {
                                                    notifyCounselorCancel(appt, "Student");
                                                    Toast.makeText(AppointmentsActivity.this,
                                                            "Appointment cancelled.", Toast.LENGTH_SHORT).show();
                                                    load();
                                                });
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        Toast.makeText(AppointmentsActivity.this,
                                                "Failed: " + error, Toast.LENGTH_SHORT).show();
                                    }
                                }))
                .setNegativeButton("No", null)
                .show();
    }

    private void notifyCounselorCancel(Appointment appt, String studentName) {
        String dateLine = "";
        if (appt.getDate() != null) {
            dateLine = new java.text.SimpleDateFormat(
                    DateUtils.DISPLAY_DATE, java.util.Locale.US)
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
