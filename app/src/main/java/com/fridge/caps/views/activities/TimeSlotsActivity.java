package com.fridge.caps.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.controllers.CounselorController;
import com.fridge.caps.models.TimeSlot;
import com.fridge.caps.utils.DateUtils;
import com.fridge.caps.views.adapters.TimeSlotsAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Browse available slots for a counsellor on a chosen day (next 14 days).
 */
public class TimeSlotsActivity extends AppCompatActivity {

    public static final String EXTRA_COUNSELOR_ID              = "counselor_id";
    public static final String EXTRA_COUNSELOR_NAME            = "counselor_name";
    public static final String EXTRA_RESCHEDULE_APPOINTMENT_ID = "rescheduleAppointmentId";
    public static final String EXTRA_OLD_SLOT_ID               = "oldSlotId";

    private Spinner      spinnerDate;
    private RecyclerView recyclerView;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;

    private CounselorController counselorController;
    private String              counselorId;
    private String              counselorName;
    private String              rescheduleAppointmentId;
    private String              oldSlotId;

    private final List<String> dateValues = new ArrayList<>();
    private final List<String> dateLabels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_slots);

        counselorController = new CounselorController();
        counselorId = getIntent().getStringExtra(EXTRA_COUNSELOR_ID);
        counselorName = getIntent().getStringExtra(EXTRA_COUNSELOR_NAME);
        rescheduleAppointmentId = getIntent().getStringExtra(EXTRA_RESCHEDULE_APPOINTMENT_ID);
        oldSlotId = getIntent().getStringExtra(EXTRA_OLD_SLOT_ID);

        if (counselorId == null || counselorId.isEmpty()) {
            Toast.makeText(this, "Invalid counselor.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        spinnerDate  = findViewById(R.id.spinnerDate);
        recyclerView = findViewById(R.id.recyclerViewSlots);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Available Slots");
        }

        buildDateSpinner();
        spinnerDate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadSlotsForDate(dateValues.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        if (!dateValues.isEmpty()) {
            loadSlotsForDate(dateValues.get(0));
        }
    }

    private void buildDateSpinner() {
        Calendar base = Calendar.getInstance();
        SimpleDateFormat labelFmt = new SimpleDateFormat("EEE, MMM d", Locale.US);
        SimpleDateFormat storeFmt = new SimpleDateFormat(DateUtils.STORAGE_DATE, Locale.US);
        dateValues.clear();
        dateLabels.clear();
        for (int i = 0; i < 14; i++) {
            Calendar day = (Calendar) base.clone();
            day.add(Calendar.DAY_OF_YEAR, i);
            dateValues.add(storeFmt.format(day.getTime()));
            dateLabels.add(labelFmt.format(day.getTime()));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, dateLabels);
        spinnerDate.setAdapter(adapter);
    }

    private void loadSlotsForDate(String dateYmd) {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        counselorController.getAvailableSlotsForDate(counselorId, dateYmd,
                new CounselorController.TimeSlotsCallback() {
                    @Override
                    public void onSuccess(List<TimeSlot> slots) {
                        progressBar.setVisibility(View.GONE);
                        if (slots.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            tvEmpty.setText("No available slots for this date");
                            recyclerView.setAdapter(null);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                            TimeSlotsAdapter adapter = new TimeSlotsAdapter(slots, slot -> openBooking(slot, dateYmd));
                            recyclerView.setAdapter(adapter);
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(TimeSlotsActivity.this,
                                "Failed to load slots: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void openBooking(TimeSlot slot, String dateYmd) {
        String name = counselorName != null ? counselorName : "Counsellor";
        String timeStr;
        long startMs = 0L;
        if (slot.getStartTime() != null && !slot.getStartTime().isEmpty()) {
            timeStr = slot.getStartTime();
        } else if (slot.getLegacyStartTime() != null) {
            java.text.SimpleDateFormat tf = new java.text.SimpleDateFormat("hh:mm a",
                    java.util.Locale.getDefault());
            timeStr = tf.format(slot.getLegacyStartTime().toDate());
            startMs = slot.getLegacyStartTime().toDate().getTime();
        } else {
            timeStr = "";
        }
        if (slot.getLegacyStartTime() != null && startMs == 0L) {
            startMs = slot.getLegacyStartTime().toDate().getTime();
        }
        Intent i = new Intent(this, BookAppointmentActivity.class);
        i.putExtra(BookAppointmentActivity.EXTRA_COUNSELOR_ID, counselorId);
        i.putExtra(BookAppointmentActivity.EXTRA_COUNSELOR_NAME, name);
        i.putExtra(BookAppointmentActivity.EXTRA_SLOT_ID, slot.getSlotId());
        i.putExtra(BookAppointmentActivity.EXTRA_SLOT_TIME, timeStr);
        i.putExtra(BookAppointmentActivity.EXTRA_SLOT_DATE, dateYmd != null ? dateYmd : slot.getDate());
        i.putExtra(BookAppointmentActivity.EXTRA_SLOT_START_MS, startMs);
        if (rescheduleAppointmentId != null && oldSlotId != null) {
            i.putExtra(BookAppointmentActivity.EXTRA_RESCHEDULE_APPOINTMENT_ID, rescheduleAppointmentId);
            i.putExtra(BookAppointmentActivity.EXTRA_OLD_SLOT_ID, oldSlotId);
        }
        startActivity(i);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
