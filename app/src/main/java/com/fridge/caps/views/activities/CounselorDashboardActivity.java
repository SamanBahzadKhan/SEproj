package com.fridge.caps.views.activities;

/**
 * CounselorDashboardActivity.java
 * Counselor home screen displaying weekly availability grid, pending appointment requests, and confirmed sessions.
 * Shows morning/afternoon availability for each day and appointment management options.
 * View in the MVC pattern.
 */
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
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
import com.fridge.caps.models.TimeSlot;
import com.fridge.caps.network.MeetRequest;
import com.fridge.caps.network.MeetResponse;
import com.fridge.caps.network.SupabaseMeetClient;
import com.fridge.caps.utils.DateUtils;
import com.fridge.caps.utils.MeetLinkTimeHelper;
import com.fridge.caps.views.BottomNavUi;
import com.fridge.caps.views.adapters.AppointmentAdapter;
import com.fridge.caps.views.adapters.PendingRequestAdapter;
import com.fridge.caps.views.fragments.AvailabilityBottomSheet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Counsellor home: availability grid, pending requests, today's confirmed sessions.
 */
public class CounselorDashboardActivity extends AppCompatActivity {

    private static final int[] DAY_HEADER_IDS = {
            R.id.dayHeader0, R.id.dayHeader1, R.id.dayHeader2, R.id.dayHeader3,
            R.id.dayHeader4, R.id.dayHeader5, R.id.dayHeader6
    };
    private static final int[] CELL_M_IDS = {
            R.id.cell_m0, R.id.cell_m1, R.id.cell_m2, R.id.cell_m3,
            R.id.cell_m4, R.id.cell_m5, R.id.cell_m6
    };
    private static final int[] CELL_A_IDS = {
            R.id.cell_a0, R.id.cell_a1, R.id.cell_a2, R.id.cell_a3,
            R.id.cell_a4, R.id.cell_a5, R.id.cell_a6
    };

    private TextView     tvWelcome, tvTodayCount, tvWeekCount, tvPending;
    private RecyclerView rvAppointments, rvPending;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;
    private TextView     labelPendingSection;
    private View         bellBadge;

    private final FrameLayout[][] availabilityCells = new FrameLayout[2][7];
    private final boolean[] morningAvailCache = new boolean[7];
    private final boolean[] afternoonAvailCache = new boolean[7];

    private AppointmentController  appointmentController;
    private NotificationController notificationController;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ListenerRegistration unreadListener;
    private ListenerRegistration timeslotsListener;
    private ListenerRegistration availabilityListener;

    private String counselorNameCache = "";
    private String counselorUid;
    private String[] weekDates = new String[7];

    private int selectedCounselorNavId = R.id.navHome;

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

        for (int i = 0; i < 7; i++) {
            availabilityCells[0][i] = findViewById(CELL_M_IDS[i]);
            availabilityCells[1][i] = findViewById(CELL_A_IDS[i]);
        }

        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        rvPending.setLayoutManager(new LinearLayoutManager(this));

        computeWeekDates();
        bindDayHeaders();

        loadWelcomeName();
        attachUnreadBadge();
        attachTimeslotsListener();
        attachAvailabilityListener();
        wireAvailabilityCellClicks();

        findViewById(R.id.topBarBell).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        findViewById(R.id.topBarSettings).setOnClickListener(this::showSettingsMenu);

        findViewById(R.id.btnEditAvailability).setOnClickListener(v -> {
            if (counselorUid == null) return;
            AvailabilityBottomSheet.newInstance(counselorUid)
                    .show(getSupportFragmentManager(), "availability_sheet");
        });

        findViewById(R.id.navHome).setOnClickListener(v -> {
            selectedCounselorNavId = R.id.navHome;
            BottomNavUi.applyCounselorNav(this, selectedCounselorNavId);
        });
        findViewById(R.id.navCounsel).setOnClickListener(v -> {
            selectedCounselorNavId = R.id.navCounsel;
            BottomNavUi.applyCounselorNav(this, selectedCounselorNavId);
            if (counselorUid == null) return;
            AvailabilityBottomSheet.newInstance(counselorUid)
                    .show(getSupportFragmentManager(), "availability_sheet");
        });
        findViewById(R.id.navAppts).setOnClickListener(v -> {
            selectedCounselorNavId = R.id.navAppts;
            BottomNavUi.applyCounselorNav(this, selectedCounselorNavId);
            startActivity(new Intent(this, CounselorAppointmentsActivity.class));
        });
        findViewById(R.id.navAlerts).setOnClickListener(v -> {
            selectedCounselorNavId = R.id.navAlerts;
            BottomNavUi.applyCounselorNav(this, selectedCounselorNavId);
            startActivity(new Intent(this, NotificationsActivity.class));
        });
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            selectedCounselorNavId = R.id.navProfile;
            BottomNavUi.applyCounselorNav(this, selectedCounselorNavId);
            openOwnProfile();
        });
    }

    /** Called from {@link AvailabilityBottomSheet} after save/delete. */
    public void refreshAvailabilityGrid() {
        fetchAvailabilityWeekSnapshot();
    }

    private void fetchAvailabilityWeekSnapshot() {
        if (counselorUid == null) return;
        String todayString = new SimpleDateFormat(DateUtils.STORAGE_DATE, Locale.US)
                .format(new Date());
        String we = DateUtils.getThisWeekSunday();
        db.collection("availability")
                .whereEqualTo("counselorId", counselorUid)
                .whereGreaterThanOrEqualTo("date", todayString)
                .whereLessThanOrEqualTo("date", we)
                .get()
                .addOnSuccessListener(q -> applyAvailabilityDocs(q.getDocuments()))
                .addOnFailureListener(e -> { });
    }

    private void computeWeekDates() {
        String mon = DateUtils.getThisWeekMonday();
        SimpleDateFormat fmt = new SimpleDateFormat(DateUtils.STORAGE_DATE, Locale.US);
        try {
            Calendar c = Calendar.getInstance(Locale.US);
            c.setTime(Objects.requireNonNull(fmt.parse(mon)));
            for (int i = 0; i < 7; i++) {
                weekDates[i] = fmt.format(c.getTime());
                c.add(Calendar.DAY_OF_YEAR, 1);
            }
        } catch (ParseException e) {
            weekDates = new String[7];
        }
    }

    private void bindDayHeaders() {
        SimpleDateFormat dow = new SimpleDateFormat("EEE", Locale.US);
        SimpleDateFormat fmt = new SimpleDateFormat(DateUtils.STORAGE_DATE, Locale.US);
        try {
            for (int i = 0; i < 7; i++) {
                TextView tv = findViewById(DAY_HEADER_IDS[i]);
                Calendar c = Calendar.getInstance(Locale.US);
                c.setTime(Objects.requireNonNull(fmt.parse(weekDates[i])));
                String label = dow.format(c.getTime()) + "\n" + c.get(Calendar.DAY_OF_MONTH);
                tv.setText(label);
            }
        } catch (ParseException ignored) {
        }
    }

    private int columnForDate(String dateYmd) {
        if (dateYmd == null) return -1;
        for (int i = 0; i < weekDates.length; i++) {
            if (dateYmd.equals(weekDates[i])) return i;
        }
        return -1;
    }

    private void attachAvailabilityListener() {
        if (counselorUid == null) return;
        String todayString = new SimpleDateFormat(DateUtils.STORAGE_DATE, Locale.US)
                .format(new Date());
        String we = DateUtils.getThisWeekSunday();
        availabilityListener = db.collection("availability")
                .whereEqualTo("counselorId", counselorUid)
                .whereGreaterThanOrEqualTo("date", todayString)
                .whereLessThanOrEqualTo("date", we)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) return;
                    applyAvailabilityDocs(snap.getDocuments());
                });
    }

    private void applyAvailabilityDocs(Iterable<DocumentSnapshot> docs) {
        boolean[] morning = new boolean[7];
        boolean[] afternoon = new boolean[7];
        for (DocumentSnapshot doc : docs) {
            String date = doc.getString("date");
            int col = columnForDate(date);
            if (col < 0) continue;
            if (Boolean.TRUE.equals(doc.getBoolean("morning"))) {
                morning[col] = true;
            }
            if (Boolean.TRUE.equals(doc.getBoolean("afternoon"))) {
                afternoon[col] = true;
            }
        }
        System.arraycopy(morning, 0, morningAvailCache, 0, 7);
        System.arraycopy(afternoon, 0, afternoonAvailCache, 0, 7);
        for (int i = 0; i < 7; i++) {
            availabilityCells[0][i].setBackgroundResource(
                    morning[i] ? R.drawable.bg_avail_cell_green : R.drawable.bg_avail_cell_grey);
            availabilityCells[1][i].setBackgroundResource(
                    afternoon[i] ? R.drawable.bg_avail_cell_green : R.drawable.bg_avail_cell_grey);
        }
    }

    private void wireAvailabilityCellClicks() {
        for (int i = 0; i < 7; i++) {
            final int col = i;
            availabilityCells[0][col].setOnClickListener(v -> onCellTapped(col, true));
            availabilityCells[1][col].setOnClickListener(v -> onCellTapped(col, false));
        }
    }

    private void onCellTapped(int col, boolean morning) {
        boolean avail = morning ? morningAvailCache[col] : afternoonAvailCache[col];
        if (!avail) return;
        String date = weekDates[col];
        db.collection("timeslots")
                .whereEqualTo("counselorId", counselorUid)
                .whereEqualTo("date", date)
                .whereEqualTo("isBooked", true)
                .get()
                .addOnSuccessListener(q -> {
                    List<String> lines = new ArrayList<>();
                    for (DocumentSnapshot d : q.getDocuments()) {
                        TimeSlot s = TimeSlot.fromSnapshot(d);
                        boolean slotM = "Morning".equalsIgnoreCase(s.getPeriod())
                                || (s.getPeriod() == null && DateUtils.isMorningSlot(s.getStartTime()));
                        boolean slotA = "Afternoon".equalsIgnoreCase(s.getPeriod())
                                || (s.getPeriod() == null && s.getStartTime() != null
                                && !DateUtils.isMorningSlot(s.getStartTime()));
                        if (morning && slotM) {
                            lines.add(s.getStartTime() != null ? s.getStartTime() : "?");
                        }
                        if (!morning && slotA) {
                            lines.add(s.getStartTime() != null ? s.getStartTime() : "?");
                        }
                    }
                    if (lines.isEmpty()) {
                        new AlertDialog.Builder(this)
                                .setTitle(morning ? "Morning" : "Afternoon")
                                .setMessage("No bookings yet in this period.")
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("Booked times")
                                .setItems(lines.toArray(new String[0]), null)
                                .setPositiveButton("OK", null)
                                .show();
                    }
                });
    }

    private void attachTimeslotsListener() {
        if (counselorUid == null) return;
        progressBar.setVisibility(View.VISIBLE);
        timeslotsListener = db.collection("timeslots")
                .whereEqualTo("counselorId", counselorUid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        progressBar.setVisibility(View.GONE);
                        return;
                    }
                    if (snap == null) return;
                    List<TimeSlot> all = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        all.add(TimeSlot.fromSnapshot(doc));
                    }
                    String todayStr = DateUtils.getTodayString();
                    String weekStart = DateUtils.getThisWeekMonday();
                    String weekEnd = DateUtils.getThisWeekSunday();

                    int todayStat = 0;
                    int weekStat = 0;
                    for (TimeSlot s : all) {
                        String st = s.getStatus();
                        if (todayStr.equals(s.getDate())) {
                            if ("BOOKED".equals(st) || "PENDING".equals(st) || "COMPLETED".equals(st)) {
                                todayStat++;
                            }
                        }
                        String d = s.getDate();
                        if (s.isBooked() && d != null
                                && d.compareTo(weekStart) >= 0 && d.compareTo(weekEnd) <= 0) {
                            weekStat++;
                        }
                    }

                    List<TimeSlot> pendingSlots = new ArrayList<>();
                    List<TimeSlot> todayBookedSlots = new ArrayList<>();
                    for (TimeSlot s : all) {
                        if ("PENDING".equals(s.getStatus())) {
                            pendingSlots.add(s);
                        }
                        if (todayStr.equals(s.getDate()) && "BOOKED".equals(s.getStatus())) {
                            todayBookedSlots.add(s);
                        }
                    }

                    tvTodayCount.setText(String.valueOf(todayStat));
                    tvWeekCount.setText(String.valueOf(weekStat));
                    tvPending.setText(String.valueOf(pendingSlots.size()));

                    Set<String> ids = new HashSet<>();
                    List<TimeSlot> forEnrich = new ArrayList<>();
                    for (TimeSlot s : pendingSlots) {
                        if (ids.add(s.getSlotId())) forEnrich.add(s);
                    }
                    for (TimeSlot s : todayBookedSlots) {
                        if (ids.add(s.getSlotId())) forEnrich.add(s);
                    }

                    if (forEnrich.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        bindPendingAndToday(new ArrayList<>(), new ArrayList<>());
                        return;
                    }

                    appointmentController.enrichSlotsToAppointments(forEnrich,
                            new AppointmentController.AppointmentListCallback() {
                                @Override
                                public void onSuccess(List<Appointment> appointments) {
                                    progressBar.setVisibility(View.GONE);
                                    Map<String, Appointment> byId = new HashMap<>();
                                    for (Appointment a : appointments) {
                                        byId.put(a.getTimeSlotId(), a);
                                    }
                                    List<Appointment> pApps = new ArrayList<>();
                                    for (TimeSlot s : pendingSlots) {
                                        Appointment a = byId.get(s.getSlotId());
                                        if (a != null) pApps.add(a);
                                    }
                                    List<Appointment> tApps = new ArrayList<>();
                                    for (TimeSlot s : todayBookedSlots) {
                                        Appointment a = byId.get(s.getSlotId());
                                        if (a != null) tApps.add(a);
                                    }
                                    bindPendingAndToday(pApps, tApps);
                                }

                                @Override
                                public void onFailure(String error) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(CounselorDashboardActivity.this,
                                            error, Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    private void bindPendingAndToday(List<Appointment> pendingList, List<Appointment> todayBooked) {
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

    private void openOwnProfile() {
        if (counselorUid == null) return;
        Intent i = new Intent(this, CounselorProfileActivity.class);
        i.putExtra(CounselorProfileActivity.EXTRA_COUNSELOR_ID, counselorUid);
        i.putExtra("counselorId", counselorUid);
        startActivity(i);
    }

    private void showSettingsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("My Profile");
        popup.getMenu().add("Sign Out");
        popup.setOnMenuItemClickListener(item -> {
            CharSequence title = item.getTitle();
            if ("My Profile".contentEquals(title)) {
                openOwnProfile();
            } else {
                signOut();
            }
            return true;
        });
        popup.show();
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        BottomNavUi.applyCounselorNav(this, selectedCounselorNavId);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        db.collection("counselors").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> Log.e("CounselorDashboard",
                        "Session check failed: " + (e.getMessage() != null ? e.getMessage() : "")));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (unreadListener != null) {
            unreadListener.remove();
        }
        if (timeslotsListener != null) {
            timeslotsListener.remove();
        }
        if (availabilityListener != null) {
            availabilityListener.remove();
        }
    }

    private void attachUnreadBadge() {
        if (counselorUid == null) return;
        unreadListener = notificationController.listenUnreadCount((snap, err) -> {
            if (err != null || snap == null) {
                if (bellBadge != null) bellBadge.setVisibility(View.GONE);
                return;
            }
            int n = snap.size();
            if (bellBadge != null) {
                bellBadge.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void loadWelcomeName() {
        if (counselorUid == null) return;

        db.collection("counselors").document(counselorUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("name") != null) {
                        counselorNameCache = doc.getString("name");
                        tvWelcome.setText("Dr. " + counselorNameCache);
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
                        generateMeetLinkIfOnline(appt);
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(CounselorDashboardActivity.this,
                                error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Creates a Google Meet link for online appointments after the slot is confirmed.
     */
    private void generateMeetLinkIfOnline(Appointment appt) {
        if (appt == null || !"Online".equals(appt.getType())) {
            return;
        }
        if (counselorUid == null) {
            return;
        }
        String supabaseUrl = getString(R.string.supabase_url).trim();
        String supabaseKey = getString(R.string.supabase_anon_key).trim();
        if (supabaseUrl.isEmpty() || supabaseKey.isEmpty()) {
            Toast.makeText(this,
                    "Add non-empty supabase_url and supabase_anon_key in strings.xml.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!supabaseUrl.startsWith("https://")) {
            Toast.makeText(this,
                    "supabase_url must start with https://",
                    Toast.LENGTH_LONG).show();
            return;
        }
        db.collection("counselors").document(counselorUid).get()
                .addOnSuccessListener(cDoc -> {
                    if (!cDoc.exists()) {
                        return;
                    }
                    String cEmail = cDoc.getString("email");
                    String cName = cDoc.getString("name");
                    if (cEmail == null || cEmail.isEmpty()) {
                        Toast.makeText(CounselorDashboardActivity.this,
                                "Your profile is missing an email address.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    db.collection("timeslots").document(appt.getTimeSlotId()).get()
                            .addOnSuccessListener(tDoc -> {
                                String dateStr = tDoc.getString("date");
                                String startStr = tDoc.getString("startTime");
                                if (startStr == null || startStr.isEmpty()) {
                                    startStr = appt.getTimeDisplay();
                                }
                                String[] iso = MeetLinkTimeHelper.buildStartEndIso(dateStr, startStr, 60);
                                if (iso == null) {
                                    Toast.makeText(CounselorDashboardActivity.this,
                                            "Could not determine session time for Meet link.",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }
                                db.collection("students").document(appt.getStudentId()).get()
                                        .addOnSuccessListener(sDoc -> {
                                            String sEmail = sDoc.getString("email");
                                            String sName = sDoc.getString("name");
                                            if (sEmail == null || sEmail.isEmpty()) {
                                                Toast.makeText(CounselorDashboardActivity.this,
                                                        "Student email not found.",
                                                        Toast.LENGTH_LONG).show();
                                                return;
                                            }
                                            MeetRequest req = new MeetRequest(
                                                    sEmail,
                                                    cEmail,
                                                    iso[0],
                                                    iso[1]);
                                            SupabaseMeetClient.create(CounselorDashboardActivity.this)
                                                    .createMeet(req)
                                                    .enqueue(new Callback<MeetResponse>() {
                                                        @Override
                                                        public void onResponse(Call<MeetResponse> call,
                                                                Response<MeetResponse> response) {
                                                            runOnUiThread(() -> handleSupabaseMeetResponse(
                                                                    appt, sName, cName, response));
                                                        }

                                                        @Override
                                                        public void onFailure(Call<MeetResponse> call,
                                                                Throwable t) {
                                                            runOnUiThread(() -> {
                                                                Log.e("MeetLink",
                                                                        t.getMessage() != null ? t.getMessage()
                                                                                : "onFailure",
                                                                        t);
                                                                Toast.makeText(CounselorDashboardActivity.this,
                                                                        t.getMessage() != null ? t.getMessage()
                                                                                : "Could not reach Supabase.",
                                                                        Toast.LENGTH_LONG).show();
                                                            });
                                                        }
                                                    });
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(CounselorDashboardActivity.this,
                                                        "Could not load student profile.",
                                                        Toast.LENGTH_SHORT).show());
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(CounselorDashboardActivity.this,
                                            "Could not load appointment slot.",
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(CounselorDashboardActivity.this,
                                "Could not load counsellor profile.",
                                Toast.LENGTH_SHORT).show());
    }

    private void handleSupabaseMeetResponse(Appointment appt, String studentName, String counselorName,
                                            Response<MeetResponse> response) {
        if (!response.isSuccessful()) {
            String detail = "Meet link failed (" + response.code() + ").";
            ResponseBody err = response.errorBody();
            if (err != null) {
                try {
                    String body = err.string();
                    if (body != null && !body.trim().isEmpty()) {
                        detail = detail + " " + body;
                    }
                } catch (Exception e) {
                    Log.e("MeetLink", "read error body", e);
                }
            }
            Toast.makeText(this, detail, Toast.LENGTH_LONG).show();
            return;
        }
        MeetResponse body = response.body();
        if (body == null || body.meetLink == null || body.meetLink.trim().isEmpty()) {
            String detail =
                    body != null && body.error != null && !body.error.trim().isEmpty()
                            ? body.error.trim()
                            : "Supabase returned no meet link. Check Edge Function logs and Calendar setup.";
            Toast.makeText(this, detail, Toast.LENGTH_LONG).show();
            return;
        }
        persistMeetLinkAndNotify(appt, studentName, counselorName, body.meetLink.trim());
    }

    private void persistMeetLinkAndNotify(Appointment appt, String studentName, String counselorName,
                                           String meetLink) {
        Map<String, Object> slotUpdate = new HashMap<>();
        slotUpdate.put("meetLink", meetLink);
        slotUpdate.put("meetLinkGeneratedAt", FieldValue.serverTimestamp());
        db.collection("timeslots").document(appt.getTimeSlotId())
                .update(slotUpdate)
                .addOnSuccessListener(v -> {
                    notificationController.sendMeetLinkReady(
                            appt.getStudentId(),
                            counselorUid,
                            studentName,
                            counselorName,
                            meetLink,
                            appt.getAppointmentId());
                    Toast.makeText(CounselorDashboardActivity.this,
                            "Google Meet link generated!",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("MeetLink", e.getMessage() != null ? e.getMessage() : "firestore", e);
                    Toast.makeText(CounselorDashboardActivity.this,
                            "Meet link received but saving failed: "
                                    + (e.getMessage() != null ? e.getMessage() : ""),
                            Toast.LENGTH_LONG).show();
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
                        "Dismiss"
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
