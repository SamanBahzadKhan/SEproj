package com.fridge.caps.views.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.R;
import com.fridge.caps.controllers.AppointmentController;
import com.fridge.caps.controllers.NotificationController;
import com.fridge.caps.models.Appointment;
import com.fridge.caps.models.TimeSlot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * BookAppointmentActivity.java
 * Handles confirmation and booking of an appointment (US-6).
 * Receives selected time slot and counselor info via Intent.
 * View in the MVC pattern.
 */
public class BookAppointmentActivity extends AppCompatActivity {

    public static final String EXTRA_COUNSELOR_ID   = "counselor_id";
    public static final String EXTRA_COUNSELOR_NAME = "counselor_name";
    public static final String EXTRA_SLOT_ID        = "slot_id";
    public static final String EXTRA_SLOT_TIME      = "slot_time";

    private TextView    tvCounselorName, tvSelectedTime;
    private RadioGroup  rgType;
    private EditText    etNotes;
    private Button      btnConfirm;
    private ProgressBar progressBar;

    private AppointmentController  appointmentController;
    private NotificationController notificationController;

    private String counselorId, counselorName, slotId, slotTime;

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

        tvCounselorName = findViewById(R.id.tvCounselorName);
        tvSelectedTime  = findViewById(R.id.tvSelectedTime);
        rgType          = findViewById(R.id.rgType);
        etNotes         = findViewById(R.id.etNotes);
        btnConfirm      = findViewById(R.id.btnConfirm);
        progressBar     = findViewById(R.id.progressBar);

        tvCounselorName.setText(counselorName);
        tvSelectedTime.setText(slotTime);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Book Appointment");
        }

        btnConfirm.setOnClickListener(v -> confirmBooking());
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

        // Get student name first
        FirebaseFirestore.getInstance().collection("students").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String studentName = doc.getString("name") != null
                            ? doc.getString("name") : "Student";

                    Appointment appointment = new Appointment(
                            "", uid, counselorId, counselorName,
                            studentName, slotId, null, slotTime, type, notes);

                    appointmentController.bookAppointment(appointment,
                            new AppointmentController.AppointmentCallback() {
                                @Override
                                public void onSuccess() {
                                    // Send confirmation notification
                                    notificationController.sendConfirmation(
                                            uid, counselorName, slotTime);
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(BookAppointmentActivity.this,
                                            "Appointment booked!", Toast.LENGTH_SHORT).show();
                                    finish();
                                }

                                @Override
                                public void onFailure(String error) {
                                    progressBar.setVisibility(View.GONE);
                                    btnConfirm.setEnabled(true);
                                    Toast.makeText(BookAppointmentActivity.this,
                                            "Failed: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}