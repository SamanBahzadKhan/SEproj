package com.fridge.caps.views.activities;
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

    public static final String EXTRA_COUNSELOR_ID = "counselor_id";

    private RecyclerView recyclerView;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;

    private CounselorController counselorController;
    private String counselorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_slots);

        counselorController = new CounselorController();
        counselorId = getIntent().getStringExtra(EXTRA_COUNSELOR_ID);

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
                                    slots, slot ->
                                    Toast.makeText(TimeSlotsActivity.this,
                                            "Selected: " + slot.getStartTime().toDate(),
                                            Toast.LENGTH_SHORT).show()
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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

