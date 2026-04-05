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
import com.fridge.caps.controllers.NotificationController;
import com.fridge.caps.models.Appointment;
import com.fridge.caps.models.AppointmentStatus;
import com.fridge.caps.utils.DateUtils;
import com.fridge.caps.views.adapters.AppointmentAdapter;
import com.fridge.caps.views.adapters.PendingRequestAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Counsellor home: pending requests, today's confirmed sessions, availability.
 */
public class CounselorDashboardActivity extends AppCompatActivity {

    private TextView     tvWelcome, tvTodayCount, tvWeekCount, tvPending;
    private RecyclerView rvAppointments, rvPending;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;
    private TextView     labelPendingSection;
    private View         bellBadge;

    private AppointmentController  appointmentController;
    private NotificationController notificationController;
    private ListenerRegistration   unreadListener;

    private String counselorNameCache = "";
    private String counselorUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_dashboard);

        appointmentController  = new AppointmentController();
        notificationController = new NotificationController();

        counselorUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        tvWelcome       = findViewById(R.id.tvWelcome);
        tvTodayCount    = findViewById(R.id.tvTodayCount);
        tvWeekCount     = findViewById(R.id.tvWeekCount);
        tvPending       = findViewById(R.id.tvPending);
        rvAppointments  = findViewById(R.id.rvAppointments);
        rvPending       = findViewById(R.id.rvPending);
        progressBar     = findViewById(R.id.progressBar);
        tvEmpty         = findViewById(R.id.tvEmpty);
        labelPendingSection = findViewById(R.id.labelPendingSection);
        bellBadge       = findViewById(R.id.bellBadge);

        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        rvPending.setLayoutManager(new LinearLayoutManager(this));

        loadWelcomeName();
        loadAppointments();
        attachUnreadBadge();

        findViewById(R.id.topBarBell).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
        findViewById(R.id.topBarSettings).setOnClickListener(v -> openOwnProfile());

        findViewById(R.id.btnEditAvailability).setOnClickListener(v ->
                startActivity(new Intent(this, AvailabilityActivity.class)));

        findViewById(R.id.navHome).setOnClickListener(v -> { });
        findViewById(R.id.navCounsel).setOnClickListener(v ->
                startActivity(new Intent(this, CounselorListActivity.class)));
        findViewById(R.id.navAppts).setOnClickListener(v ->
                startActivity(new Intent(this, AppointmentsActivity.class)));
        findViewById(R.id.navAlerts).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v -> openOwnProfile());
    }

    private void openOwnProfile() {
        if (counselorUid == null) return;
        Intent i = new Intent(this, CounselorProfileActivity.class);
        i.putExtra(CounselorProfileActivity.EXTRA_COUNSELOR_ID, counselorUid);
        startActivity(i);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (unreadListener != null) {
            unreadListener.remove();
        }
    }

    private void attachUnreadBadge() {
        if (counselorUid == null) return;
        unreadListener = notificationController.listenUnreadCount((snap, e) -> {
            if (e != null || snap == null) {
                if (bellBadge != null) bellBadge.setVisibility(View.GONE);
                return;
            }
            int n = snap.size();
            if (bellBadge != null) {
                bellBadge.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppointments();
    }

    private void loadWelcomeName() {
        if (counselorUid == null) return;

        FirebaseFirestore.getInstance().collection("counselors").document(counselorUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("name") != null) {
                        counselorNameCache = doc.getString("name");
                        tvWelcome.setText("Good Morning,\nDr. " + counselorNameCache);
                    }
                });
    }

    private void loadAppointments() {
        if (counselorUid == null) return;
        progressBar.setVisibility(View.VISIBLE);

        appointmentController.getCounselorAppointments(counselorUid,
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

                        List<Appointment> pendingList = appointments.stream()
                                .filter(a -> a.getStatus() == AppointmentStatus.PENDING)
                                .collect(Collectors.toList());

                        List<Appointment> todayBooked = appointments.stream()
                                .filter(a -> {
                                    if (a.getStatus() != AppointmentStatus.CONFIRMED) return false;
                                    if (a.getDate() == null) return false;
                                    long t = a.getDate().toDate().getTime();
                                    return t >= startOfDay && t < endOfDay;
                                })
                                .collect(Collectors.toList());

                        int todayN = todayBooked.size();
                        int pendingN = pendingList.size();
                        int week = 0;
                        for (Appointment a : appointments) {
                            AppointmentStatus st = a.getStatus();
                            if (st != AppointmentStatus.CONFIRMED && st != AppointmentStatus.PENDING) {
                                continue;
                            }
                            if (a.getDate() != null) {
                                long t = a.getDate().toDate().getTime();
                                if (t >= System.currentTimeMillis() && t <= weekEnd) {
                                    week++;
                                }
                            }
                        }

                        tvTodayCount.setText(String.valueOf(todayN));
                        tvWeekCount.setText(String.valueOf(week));
                        tvPending.setText(String.valueOf(pendingN));

                        boolean showPending = !pendingList.isEmpty();
                        labelPendingSection.setVisibility(showPending ? View.VISIBLE : View.GONE);
                        rvPending.setVisibility(showPending ? View.VISIBLE : View.GONE);
                        if (showPending) {
                            rvPending.setAdapter(new PendingRequestAdapter(pendingList,
                                    new PendingRequestAdapter.Action() {
                                        @Override
                                        public void onConfirm(Appointment a) {
                                            confirmPending(a);
                                        }

                                        @Override
                                        public void onDecline(Appointment a) {
                                            declinePending(a);
                                        }
                                    }));
                        }

                        tvEmpty.setVisibility(todayBooked.isEmpty() ? View.VISIBLE : View.GONE);

                        rvAppointments.setAdapter(new AppointmentAdapter(todayBooked,
                                AppointmentAdapter.MODE_COUNSELOR,
                                appt -> confirmCancelSession(appt),
                                null, null,
                                appt -> showCompleteDialog(appt),
                                null));
                    }

                    @Override
                    public void onFailure(String error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CounselorDashboardActivity.this,
                                error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmPending(Appointment appt) {
        appointmentController.confirmPendingTimeslot(appt.getTimeSlotId(),
                new AppointmentController.AppointmentCallback() {
                    @Override
                    public void onSuccess() {
                        String dt = formatApptLine(appt);
                        notificationController.sendConfirmation(
                                appt.getStudentId(),
                                counselorNameCache != null ? counselorNameCache : "Counsellor",
                                dt);
                        Toast.makeText(CounselorDashboardActivity.this,
                                "Appointment confirmed.", Toast.LENGTH_SHORT).show();
                        loadAppointments();
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(CounselorDashboardActivity.this,
                                error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void declinePending(Appointment appt) {
        appointmentController.cancelAppointment(appt.getAppointmentId(), appt.getTimeSlotId(),
                new AppointmentController.AppointmentCallback() {
                    @Override
                    public void onSuccess() {
                        notificationController.sendDeclined(appt.getStudentId(),
                                counselorNameCache,
                                formatDateOnly(appt));
                        Toast.makeText(CounselorDashboardActivity.this,
                                "Request declined.", Toast.LENGTH_SHORT).show();
                        loadAppointments();
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(CounselorDashboardActivity.this,
                                error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showCompleteDialog(Appointment appt) {
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
                                        appt.getStudentId(), counselorNameCache);
                                Toast.makeText(CounselorDashboardActivity.this,
                                        "Marked complete.", Toast.LENGTH_SHORT).show();
                                loadAppointments();
                            }

                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(CounselorDashboardActivity.this,
                                        error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        appointmentController.markNoShow(tid, new AppointmentController.AppointmentCallback() {
                            @Override
                            public void onSuccess() {
                                notificationController.sendMissedSession(
                                        appt.getStudentId(), counselorNameCache, formatApptLine(appt));
                                Toast.makeText(CounselorDashboardActivity.this,
                                        "Marked no-show.", Toast.LENGTH_SHORT).show();
                                loadAppointments();
                            }

                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(CounselorDashboardActivity.this,
                                        error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .show();
    }

    private void confirmCancelSession(Appointment appt) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel appointment")
                .setMessage("Cancel this session with "
                        + (appt.getStudentName() != null ? appt.getStudentName() : "student") + "?")
                .setPositiveButton("Yes", (d, w) ->
                        appointmentController.cancelAppointment(
                                appt.getAppointmentId(), appt.getTimeSlotId(),
                                new AppointmentController.AppointmentCallback() {
                                    @Override
                                    public void onSuccess() {
                                        notificationController.sendCounselorCancelledStudent(
                                                appt.getStudentId(),
                                                counselorNameCache,
                                                formatDateOnly(appt),
                                                appt.getTimeDisplay());
                                        Toast.makeText(CounselorDashboardActivity.this,
                                                "Cancelled.", Toast.LENGTH_SHORT).show();
                                        loadAppointments();
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        Toast.makeText(CounselorDashboardActivity.this,
                                                error, Toast.LENGTH_SHORT).show();
                                    }
                                }))
                .setNegativeButton("No", null)
                .show();
    }

    private String formatApptLine(Appointment a) {
        if (a.getDate() == null) return a.getTimeDisplay() != null ? a.getTimeDisplay() : "";
        String day = new SimpleDateFormat(DateUtils.DISPLAY_DATE, Locale.US).format(a.getDate().toDate());
        String t = a.getTimeDisplay() != null ? a.getTimeDisplay() : "";
        return t.isEmpty() ? day : day + " · " + t;
    }

    private String formatDateOnly(Appointment a) {
        if (a.getDate() == null) return "";
        return new SimpleDateFormat(DateUtils.DISPLAY_DATE, Locale.US).format(a.getDate().toDate());
    }
}
