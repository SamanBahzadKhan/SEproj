package com.fridge.caps.views.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.R;
import com.fridge.caps.controllers.AppointmentController;
import com.fridge.caps.controllers.NotificationController;
import com.fridge.caps.workers.ReminderWorker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Confirms booking or reschedules using {@code timeslots} documents only.
 */
public class BookAppointmentActivity extends AppCompatActivity {

    public static final String EXTRA_COUNSELOR_ID   = "counselor_id";
    public static final String EXTRA_COUNSELOR_NAME = "counselor_name";
    public static final String EXTRA_SLOT_ID        = "slot_id";
    public static final String EXTRA_SLOT_TIME      = "slot_time";
    /** Storage format yyyy-MM-dd */
    public static final String EXTRA_SLOT_DATE      = "slot_date";
    public static final String EXTRA_SLOT_START_MS  = "slot_start_ms";
    public static final String EXTRA_RESCHEDULE_APPOINTMENT_ID = "reschedule_appointment_id";
    public static final String EXTRA_OLD_SLOT_ID    = "old_slot_id";

    private TextView    tvCounselorName, tvSelectedTime;
    private RadioGroup  rgType;
    private EditText    etNotes;
    private Button      btnConfirm;
    private ProgressBar progressBar;

    private AppointmentController  appointmentController;
    private NotificationController notificationController;

    private String counselorId, counselorName, slotId, slotTime, slotDate;
    private String rescheduleAppointmentId;
    private String oldSlotId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        appointmentController  = new AppointmentController();
        notificationController = new NotificationController();

        counselorId   = getIntent().getStringExtra(EXTRA_COUNSELOR_ID);
        counselorName = getIntent().getStringExtra(EXTRA_COUNSELOR_NAME);
        slotId        = getIntent().getStringExtra(EXTRA_SLOT_ID);
        slotTime      = getIntent().getStringExtra(EXTRA_SLOT_TIME);
        slotDate      = getIntent().getStringExtra(EXTRA_SLOT_DATE);
        rescheduleAppointmentId = getIntent().getStringExtra(EXTRA_RESCHEDULE_APPOINTMENT_ID);
        oldSlotId     = getIntent().getStringExtra(EXTRA_OLD_SLOT_ID);

        tvCounselorName = findViewById(R.id.tvCounselorName);
        tvSelectedTime  = findViewById(R.id.tvSelectedTime);
        rgType          = findViewById(R.id.rgType);
        etNotes         = findViewById(R.id.etNotes);
        btnConfirm      = findViewById(R.id.btnConfirm);
        progressBar     = findViewById(R.id.progressBar);

        tvCounselorName.setText(counselorName);
        tvSelectedTime.setText(slotTime);

        if (rescheduleAppointmentId != null && !rescheduleAppointmentId.isEmpty()) {
            btnConfirm.setText("RESCHEDULE");
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Book Appointment");
        }

        btnConfirm.setOnClickListener(v -> {
            if (rescheduleAppointmentId != null && !rescheduleAppointmentId.isEmpty()) {
                confirmReschedule();
            } else {
                confirmBooking();
            }
        });
    }

    private void confirmBooking() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) { Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show(); return; }

        int selectedId = rgType.getCheckedRadioButtonId();
        String type = selectedId == R.id.rbOnline ? "Online" : "In-Person";
        String notes = etNotes.getText().toString().trim();

        progressBar.setVisibility(View.VISIBLE);
        btnConfirm.setEnabled(false);

        FirebaseFirestore.getInstance().collection("students").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String studentName = doc.getString("name") != null
                            ? doc.getString("name") : "Student";

                    appointmentController.bookTimeslot(slotId, uid, notes, type,
                            counselorName, studentName,
                            new AppointmentController.AppointmentCallback() {
                                @Override
                                public void onSuccess() {
                                    String datePart = (slotDate != null ? slotDate + " " : "")
                                            + (slotTime != null ? slotTime : "");
                                    notificationController.sendBookingRequestNotifications(
                                            uid, studentName, counselorId, counselorName, datePart);
                                    ReminderWorker.scheduleIfFuture(BookAppointmentActivity.this,
                                            uid, counselorName, slotDate != null ? slotDate : "",
                                            slotTime != null ? slotTime : "");
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(BookAppointmentActivity.this,
                                            "Appointment request sent!", Toast.LENGTH_SHORT).show();
                                    finish();
                                }

                                @Override
                                public void onFailure(String error) {
                                    progressBar.setVisibility(View.GONE);
                                    btnConfirm.setEnabled(true);
                                    if ("This slot is no longer available.".equals(error)
                                            || (error != null && error.contains("no longer"))) {
                                        new AlertDialog.Builder(BookAppointmentActivity.this)
                                                .setTitle("Slot unavailable")
                                                .setMessage("This slot was just booked by someone else. Please select another time.")
                                                .setPositiveButton("OK", null)
                                                .show();
                                    } else {
                                        Toast.makeText(BookAppointmentActivity.this,
                                                error, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", e.getMessage() != null ? e.getMessage() : "student fetch");
                    progressBar.setVisibility(View.GONE);
                    btnConfirm.setEnabled(true);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmReschedule() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null || rescheduleAppointmentId == null || oldSlotId == null || slotId == null) {
            Toast.makeText(this, "Missing data for reschedule.", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = rgType.getCheckedRadioButtonId();
        String type = selectedId == R.id.rbOnline ? "Online" : "In-Person";
        String notes = etNotes.getText().toString().trim();

        progressBar.setVisibility(View.VISIBLE);
        btnConfirm.setEnabled(false);

        FirebaseFirestore.getInstance().collection("students").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String studentName = doc.getString("name") != null ? doc.getString("name") : "Student";
                    appointmentController.rescheduleAppointment(
                            rescheduleAppointmentId, oldSlotId, slotId, slotTime,
                            uid, studentName, notes, type,
                            new AppointmentController.AppointmentCallback() {
                                @Override
                                public void onSuccess() {
                                    notificationController.sendReschedule(uid, counselorName, slotTime);
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(BookAppointmentActivity.this,
                                            "Appointment rescheduled.", Toast.LENGTH_SHORT).show();
                                    finish();
                                }

                                @Override
                                public void onFailure(String error) {
                                    progressBar.setVisibility(View.GONE);
                                    btnConfirm.setEnabled(true);
                                    Toast.makeText(BookAppointmentActivity.this,
                                            error, Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", e.getMessage() != null ? e.getMessage() : "reschedule");
                    progressBar.setVisibility(View.GONE);
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
