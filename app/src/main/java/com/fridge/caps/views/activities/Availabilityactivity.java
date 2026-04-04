package com.fridge.caps.views.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.R;
import com.fridge.caps.models.TimeSlot;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * AvailabilityActivity.java
 * Allows counselors to add available time slots (US-12).
 * View in the MVC pattern.
 */
public class AvailabilityActivity extends AppCompatActivity {

    private TextView tvSelectedDate, tvStartTime, tvEndTime;
    private Button   btnPickDate, btnPickStart, btnPickEnd, btnSave;

    private Calendar selectedDate  = Calendar.getInstance();
    private Calendar startTime     = Calendar.getInstance();
    private Calendar endTime       = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_availability);

        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvStartTime    = findViewById(R.id.tvStartTime);
        tvEndTime      = findViewById(R.id.tvEndTime);
        btnPickDate    = findViewById(R.id.btnPickDate);
        btnPickStart   = findViewById(R.id.btnPickStart);
        btnPickEnd     = findViewById(R.id.btnPickEnd);
        btnSave        = findViewById(R.id.btnSave);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Set Availability");
        }

        btnPickDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, day) -> {
                selectedDate.set(year, month, day);
                tvSelectedDate.setText(day + "/" + (month+1) + "/" + year);
            }, selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnPickStart.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) -> {
                startTime.set(Calendar.HOUR_OF_DAY, hour);
                startTime.set(Calendar.MINUTE, minute);
                tvStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            }, startTime.get(Calendar.HOUR_OF_DAY),
                    startTime.get(Calendar.MINUTE), true).show();
        });

        btnPickEnd.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) -> {
                endTime.set(Calendar.HOUR_OF_DAY, hour);
                endTime.set(Calendar.MINUTE, minute);
                tvEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            }, endTime.get(Calendar.HOUR_OF_DAY),
                    endTime.get(Calendar.MINUTE), true).show();
        });

        btnSave.setOnClickListener(v -> saveSlot());
    }

    private void saveSlot() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) { Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show(); return; }

        if (tvSelectedDate.getText().toString().equals("Not selected")
                || tvStartTime.getText().toString().equals("Not selected")) {
            Toast.makeText(this, "Please select date and time.", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedDate.set(Calendar.HOUR_OF_DAY, startTime.get(Calendar.HOUR_OF_DAY));
        selectedDate.set(Calendar.MINUTE, startTime.get(Calendar.MINUTE));
        Date start = selectedDate.getTime();

        selectedDate.set(Calendar.HOUR_OF_DAY, endTime.get(Calendar.HOUR_OF_DAY));
        selectedDate.set(Calendar.MINUTE, endTime.get(Calendar.MINUTE));
        Date end = selectedDate.getTime();

        String slotId = FirebaseFirestore.getInstance().collection("timeslots").document().getId();
        TimeSlot slot = new TimeSlot(slotId, uid,
                new Timestamp(start), new Timestamp(end), true);

        FirebaseFirestore.getInstance().collection("timeslots")
                .document(slotId)
                .set(slot)
                .addOnSuccessListener(u -> {
                    Toast.makeText(this, "Slot added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}