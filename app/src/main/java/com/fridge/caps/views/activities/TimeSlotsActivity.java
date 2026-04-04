package com.fridge.caps.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.controllers.CounselorController;
import com.fridge.caps.models.TimeSlot;
import com.fridge.caps.views.adapters.TimeSlotsAdapter;

import java.util.List;

/**
 * TimeSlotsActivity.java
 * Displays available time slots for a selected counselor (US-5).
 * Fetches only available slots from Firestore via CounselorController.
 * View in the MVC pattern.
 *
 * Outstanding issues: Slot booking to be implemented in US-6.
 */
public class TimeSlotsActivity extends AppCompatActivity {

    public static final String EXTRA_COUNSELOR_ID              = "counselor_id";
    public static final String EXTRA_COUNSELOR_NAME          = "counselor_name";
    public static final String EXTRA_RESCHEDULE_APPOINTMENT_ID = "rescheduleAppointmentId";
    public static final String EXTRA_OLD_SLOT_ID             = "oldSlotId";

    private RecyclerView recyclerView;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;

    private CounselorController counselorController;
    private String counselorId;
    private String counselorName;
    private String rescheduleAppointmentId;
    private String oldSlotId;

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

        recyclerView = findViewById(R.id.recyclerViewSlots);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Available Slots");
        }

        loadSlots();
    }

    private void loadSlots() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        counselorController.getAvailableTimeSlots(counselorId,
                new CounselorController.TimeSlotsCallback() {
                    @Override
                    public void onSuccess(List<TimeSlot> slots) {
                        progressBar.setVisibility(View.GONE);
                        if (slots.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            tvEmpty.setText("No available slots at the moment.");
                        } else {
                            TimeSlotsAdapter adapter = new TimeSlotsAdapter(
                                    slots, slot -> openBooking(slot)
                            );
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

    private void openBooking(com.fridge.caps.models.TimeSlot slot) {
        String name = counselorName != null ? counselorName : "Counsellor";
        java.text.SimpleDateFormat tf = new java.text.SimpleDateFormat("hh:mm a",
                java.util.Locale.getDefault());
        String timeStr = tf.format(slot.getStartTime().toDate());
        Intent i = new Intent(this, BookAppointmentActivity.class);
        i.putExtra(BookAppointmentActivity.EXTRA_COUNSELOR_ID, counselorId);
        i.putExtra(BookAppointmentActivity.EXTRA_COUNSELOR_NAME, name);
        i.putExtra(BookAppointmentActivity.EXTRA_SLOT_ID, slot.getSlotId());
        i.putExtra(BookAppointmentActivity.EXTRA_SLOT_TIME, timeStr);
        i.putExtra(BookAppointmentActivity.EXTRA_SLOT_START_MS, slot.getStartTime().toDate().getTime());
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

