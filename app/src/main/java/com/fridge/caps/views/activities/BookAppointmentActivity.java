package com.fridge.caps.views.activities;

/**
 * BookAppointmentActivity.java
 * Appointment booking interface for students to select date and time from available slots.
 * Displays available time slots in a grid, sends booking request notification, and manages confirmations.
 * View in the MVC pattern.
 */
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.R;
import com.fridge.caps.controllers.AppointmentController;
import com.fridge.caps.controllers.NotificationController;
import com.fridge.caps.utils.DateUtils;
import com.fridge.caps.workers.ReminderWorker;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Book from counsellor availability (morning/afternoon) with 1-hour slots; or reschedule.
 */
public class BookAppointmentActivity extends AppCompatActivity {

    public static final String EXTRA_COUNSELOR_ID   = "counselor_id";
    public static final String EXTRA_COUNSELOR_NAME = "counselor_name";
    public static final String EXTRA_RESCHEDULE_APPOINTMENT_ID = "reschedule_appointment_id";
    public static final String EXTRA_OLD_SLOT_ID    = "old_slot_id";

    private TextView tvCounselorName, tvNoAvailability, labelSelectTime;
    private LinearLayout llDateChips, llPeriods;
    private MaterialCardView cardPickMorning, cardPickAfternoon;
    private GridLayout gridTimes;
    private RadioGroup rgType;
    private android.widget.EditText etNotes;
    private com.google.android.material.button.MaterialButton btnConfirm;
    private android.widget.ProgressBar progressBar;

    private AppointmentController  appointmentController;
    private NotificationController notificationController;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String counselorId;
    private String counselorName;
    private String rescheduleAppointmentId;
    private String oldSlotId;

    private String selectedDate;
    private String selectedPeriod;
    private String selectedTime;
    private TextView selectedTimeView;
    private boolean isTestMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        appointmentController  = new AppointmentController();
        notificationController = new NotificationController();

        counselorId   = getIntent().getStringExtra(EXTRA_COUNSELOR_ID);
        counselorName = getIntent().getStringExtra(EXTRA_COUNSELOR_NAME);
        rescheduleAppointmentId = getIntent().getStringExtra(EXTRA_RESCHEDULE_APPOINTMENT_ID);
        oldSlotId     = getIntent().getStringExtra(EXTRA_OLD_SLOT_ID);
        isTestMode = getIntent().getBooleanExtra("TEST_MODE", false);

        tvCounselorName = findViewById(R.id.tvCounselorName);
        tvNoAvailability = findViewById(R.id.tvNoAvailability);
        llDateChips = findViewById(R.id.llDateChips);
        llPeriods = findViewById(R.id.llPeriods);
        cardPickMorning = findViewById(R.id.cardPickMorning);
        cardPickAfternoon = findViewById(R.id.cardPickAfternoon);
        labelSelectTime = findViewById(R.id.labelSelectTime);
        gridTimes = findViewById(R.id.gridTimes);
        rgType = findViewById(R.id.rgType);
        etNotes = findViewById(R.id.etNotes);
        btnConfirm = findViewById(R.id.btnConfirm);
        progressBar = findViewById(R.id.progressBar);

        tvCounselorName.setText(counselorName != null ? counselorName : "Counsellor");

        if (rescheduleAppointmentId != null && !rescheduleAppointmentId.isEmpty()) {
            btnConfirm.setText("RESCHEDULE");
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Reschedule");
            }
        } else if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Book Appointment");
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        List<String> days = DateUtils.getNextFourteenDays();
        selectedDate = days.get(0);
        buildDateChips(days);

        cardPickMorning.setOnClickListener(v -> selectPeriod("Morning"));
        cardPickAfternoon.setOnClickListener(v -> selectPeriod("Afternoon"));

        btnConfirm.setOnClickListener(v -> {
            boolean rescheduling = rescheduleAppointmentId != null && !rescheduleAppointmentId.isEmpty();
            if (rescheduling) {
                confirmReschedule();
            } else {
                confirmBooking();
            }
        });

        if (isTestMode) {
            selectedDate = null;
            llPeriods.setVisibility(android.view.View.VISIBLE);
            cardPickMorning.setVisibility(android.view.View.VISIBLE);
            cardPickAfternoon.setVisibility(android.view.View.VISIBLE);
            progressBar.setVisibility(android.view.View.GONE);
            tvNoAvailability.setVisibility(android.view.View.GONE);
            return;
        }

        loadAvailabilityForDate(selectedDate);
    }

    private void buildDateChips(List<String> days) {
        llDateChips.removeAllViews();
        SimpleDateFormat in = new SimpleDateFormat(DateUtils.STORAGE_DATE, Locale.US);
        for (String ymd : days) {
            TextView chip = new TextView(this);
            try {
                Calendar c = Calendar.getInstance(Locale.US);
                c.setTime(Objects.requireNonNull(in.parse(ymd)));
                String dayName = new SimpleDateFormat("EEE", Locale.US).format(c.getTime());
                int dom = c.get(Calendar.DAY_OF_MONTH);
                chip.setText(dayName + "\n" + dom);
            } catch (ParseException e) {
                chip.setText(ymd);
            }
            chip.setPadding(dp(16), dp(12), dp(16), dp(12));
            chip.setGravity(Gravity.CENTER);
            chip.setTag(ymd);
            styleDateChip(chip, ymd.equals(selectedDate));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(6), 0, dp(6), 0);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                selectedDate = (String) v.getTag();
                for (int i = 0; i < llDateChips.getChildCount(); i++) {
                    TextView ch = (TextView) llDateChips.getChildAt(i);
                    styleDateChip(ch, selectedDate.equals(ch.getTag()));
                }
                selectedPeriod = null;
                selectedTime = null;
                selectedTimeView = null;
                loadAvailabilityForDate(selectedDate);
            });
            llDateChips.addView(chip);
        }
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    private void styleDateChip(TextView chip, boolean selected) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.bg_chip_date_selected);
            chip.setTextColor(Color.WHITE);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_date_unselected);
            chip.setTextColor(Color.parseColor("#2D2D2D"));
        }
    }

    private void loadAvailabilityForDate(String date) {
        progressBar.setVisibility(android.view.View.VISIBLE);
        tvNoAvailability.setVisibility(android.view.View.GONE);
        llPeriods.setVisibility(android.view.View.GONE);
        labelSelectTime.setVisibility(android.view.View.GONE);
        gridTimes.setVisibility(android.view.View.GONE);
        gridTimes.removeAllViews();

        String docId = counselorId + "_" + date;
        db.collection("availability").document(docId).get()
                .addOnSuccessListener(doc -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    if (!doc.exists()) {
                        tvNoAvailability.setVisibility(android.view.View.VISIBLE);
                        tvNoAvailability.setText("Dr. "
                                + (counselorName != null ? counselorName : "Counsellor")
                                + " is not available on this date.");
                        return;
                    }
                    boolean morning = Boolean.TRUE.equals(doc.getBoolean("morning"));
                    boolean afternoon = Boolean.TRUE.equals(doc.getBoolean("afternoon"));
                    if (!morning && !afternoon) {
                        tvNoAvailability.setVisibility(android.view.View.VISIBLE);
                        tvNoAvailability.setText("No availability on this date.");
                        return;
                    }
                    tvNoAvailability.setVisibility(android.view.View.GONE);
                    llPeriods.setVisibility(android.view.View.VISIBLE);
                    cardPickMorning.setVisibility(morning ? android.view.View.VISIBLE : android.view.View.GONE);
                    cardPickAfternoon.setVisibility(afternoon ? android.view.View.VISIBLE : android.view.View.GONE);
                    resetPeriodCards();
                    if (morning && !afternoon) {
                        selectPeriod("Morning");
                    } else if (!morning && afternoon) {
                        selectPeriod("Afternoon");
                    } else {
                        selectedPeriod = null;
                        selectedTime = null;
                        selectedTimeView = null;
                        resetPeriodCards();
                        labelSelectTime.setVisibility(android.view.View.GONE);
                        gridTimes.setVisibility(android.view.View.GONE);
                        gridTimes.removeAllViews();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, e.getMessage() != null ? e.getMessage() : "Load failed",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void resetPeriodCards() {
        stylePeriodCard(cardPickMorning, false);
        stylePeriodCard(cardPickAfternoon, false);
    }

    private void stylePeriodCard(MaterialCardView card, boolean on) {
        TextView tv = (TextView) card.getChildAt(0);
        if (on) {
            card.setCardBackgroundColor(Color.parseColor("#CEAFA6"));
            card.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
            card.setStrokeColor(Color.parseColor("#2D2D2D"));
            tv.setTextColor(Color.parseColor("#2D2D2D"));
        } else {
            card.setCardBackgroundColor(Color.WHITE);
            card.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
            card.setStrokeColor(Color.parseColor("#2D2D2D"));
            tv.setTextColor(Color.parseColor("#2D2D2D"));
        }
    }

    private void selectPeriod(String period) {
        selectedPeriod = period;
        if ("Morning".equals(period)) {
            stylePeriodCard(cardPickMorning, true);
            stylePeriodCard(cardPickAfternoon, false);
        } else {
            stylePeriodCard(cardPickMorning, false);
            stylePeriodCard(cardPickAfternoon, true);
        }
        selectedTime = null;
        selectedTimeView = null;
        loadBookedTimesAndRenderGrid();
    }

    private void loadBookedTimesAndRenderGrid() {
        if (selectedDate == null || selectedPeriod == null || counselorId == null) return;
        progressBar.setVisibility(android.view.View.VISIBLE);
        db.collection("timeslots")
                .whereEqualTo("counselorId", counselorId)
                .whereEqualTo("date", selectedDate)
                .whereEqualTo("isBooked", true)
                .get()
                .addOnSuccessListener(q -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Set<String> booked = new HashSet<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot d : q) {
                        String t = d.getString("startTime");
                        if (t != null) booked.add(t);
                    }
                    List<String> slots = "Morning".equals(selectedPeriod)
                            ? DateUtils.getMorningSlots()
                            : DateUtils.getAfternoonSlots();
                    renderTimeGrid(slots, booked);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, e.getMessage() != null ? e.getMessage() : "Query failed",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void renderTimeGrid(List<String> slots, Set<String> booked) {
        gridTimes.removeAllViews();
        labelSelectTime.setVisibility(android.view.View.VISIBLE);
        gridTimes.setVisibility(android.view.View.VISIBLE);

        int col = 0;
        int row = 0;
        for (String time : slots) {
            TextView b = new TextView(this);
            b.setText(time);
            b.setGravity(Gravity.CENTER);
            b.setPadding(dp(8), dp(10), dp(8), dp(10));
            boolean isBooked = booked.contains(time);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.columnSpec = GridLayout.spec(col, 1f);
            lp.rowSpec = GridLayout.spec(row);
            lp.width = 0;
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            b.setLayoutParams(lp);

            if (isBooked) {
                b.setBackgroundColor(Color.parseColor("#EEEEEE"));
                b.setTextColor(Color.parseColor("#AAAAAA"));
                b.setClickable(false);
            } else {
                b.setBackgroundResource(R.drawable.bg_chip_date_unselected);
                b.setTextColor(Color.parseColor("#2D2D2D"));
                b.setClickable(true);
                final String timeF = time;
                b.setOnClickListener(v -> {
                    if (selectedTimeView != null) {
                        selectedTimeView.setBackgroundResource(R.drawable.bg_chip_date_unselected);
                        selectedTimeView.setTextColor(Color.parseColor("#2D2D2D"));
                    }
                    selectedTime = timeF;
                    selectedTimeView = b;
                    b.setBackgroundResource(R.drawable.bg_chip_date_selected);
                    b.setTextColor(Color.WHITE);
                });
            }
            gridTimes.addView(b);
            col++;
            if (col >= 3) {
                col = 0;
                row++;
            }
        }
    }

    private void confirmBooking() {
        if (isTestMode) {
            if (selectedDate == null) {
                Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedPeriod == null) {
                Toast.makeText(this, "Please select a period", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedTime == null || selectedTime.isEmpty()) {
                Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Appointment request sent!", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedDate == null) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedPeriod == null) {
            Toast.makeText(this, "Please select a period", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTime == null || selectedTime.isEmpty()) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = rgType.getCheckedRadioButtonId();
        String type = selectedId == R.id.rbOnline ? "Online" : "In-Person";
        String notes = etNotes.getText().toString().trim();

        progressBar.setVisibility(android.view.View.VISIBLE);
        btnConfirm.setEnabled(false);

        db.collection("students").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String studentName = doc.getString("name") != null
                            ? doc.getString("name") : "Student";

                    appointmentController.createBookingFromAvailability(
                            counselorId, selectedDate, selectedTime, selectedPeriod, type,
                            uid, studentName, notes,
                            new AppointmentController.AppointmentCallback() {
                                @Override
                                public void onSuccess() {
                                    String datePart = selectedDate + " " + selectedTime;
                                    notificationController.sendBookingRequestNotifications(
                                            uid, studentName, counselorId, counselorName, datePart);
                                    ReminderWorker.scheduleIfFuture(BookAppointmentActivity.this,
                                            uid, counselorName, selectedDate, selectedTime);
                                    progressBar.setVisibility(android.view.View.GONE);
                                    Toast.makeText(BookAppointmentActivity.this,
                                            "Appointment request sent!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(BookAppointmentActivity.this,
                                            StudentDashboardActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                    finish();
                                }

                                @Override
                                public void onFailure(String error) {
                                    progressBar.setVisibility(android.view.View.GONE);
                                    btnConfirm.setEnabled(true);
                                    if ("This slot is no longer available.".equals(error)
                                            || (error != null && error.contains("no longer"))) {
                                        new AlertDialog.Builder(BookAppointmentActivity.this)
                                                .setTitle("Slot unavailable")
                                                .setMessage("This slot was just booked. Please pick another time.")
                                                .setPositiveButton("OK", (d, w) ->
                                                        loadBookedTimesAndRenderGrid())
                                                .show();
                                    } else {
                                        Toast.makeText(BookAppointmentActivity.this,
                                                error, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    btnConfirm.setEnabled(true);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmReschedule() {
        if (isTestMode) {
            if (selectedDate == null) {
                Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedPeriod == null) {
                Toast.makeText(this, "Please select a period", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedTime == null) {
                Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Appointment rescheduled.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        String slotToRelease = (oldSlotId != null && !oldSlotId.isEmpty())
                ? oldSlotId
                : (rescheduleAppointmentId != null && !rescheduleAppointmentId.isEmpty()
                        ? rescheduleAppointmentId : null);
        if (uid == null || slotToRelease == null || slotToRelease.isEmpty()) {
            Toast.makeText(this, "Missing reschedule data.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedDate == null) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedPeriod == null) {
            Toast.makeText(this, "Please select a period", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTime == null) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = rgType.getCheckedRadioButtonId();
        String type = selectedId == R.id.rbOnline ? "Online" : "In-Person";
        String notes = etNotes.getText().toString().trim();

        progressBar.setVisibility(android.view.View.VISIBLE);
        btnConfirm.setEnabled(false);

        db.collection("students").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String studentName = doc.getString("name") != null ? doc.getString("name") : "Student";
                    appointmentController.rescheduleCreateNew(
                            slotToRelease, counselorId, selectedDate, selectedTime, selectedPeriod, type,
                            uid, studentName, notes,
                            new AppointmentController.AppointmentCallback() {
                                @Override
                                public void onSuccess() {
                                    notificationController.sendReschedule(uid, counselorName, selectedTime);
                                    progressBar.setVisibility(android.view.View.GONE);
                                    Toast.makeText(BookAppointmentActivity.this,
                                            "Appointment rescheduled.", Toast.LENGTH_SHORT).show();
                                    finish();
                                }

                                @Override
                                public void onFailure(String error) {
                                    progressBar.setVisibility(android.view.View.GONE);
                                    btnConfirm.setEnabled(true);
                                    if (error != null && error.contains("no longer")) {
                                        new AlertDialog.Builder(BookAppointmentActivity.this)
                                                .setTitle("Slot unavailable")
                                                .setMessage("Please choose another time.")
                                                .setPositiveButton("OK", (d, w) ->
                                                        loadBookedTimesAndRenderGrid())
                                                .show();
                                    } else {
                                        Toast.makeText(BookAppointmentActivity.this,
                                                error, Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    btnConfirm.setEnabled(true);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
