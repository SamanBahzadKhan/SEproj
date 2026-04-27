package com.fridge.caps.views.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.controllers.AppointmentController;
import com.fridge.caps.models.Appointment;
import com.fridge.caps.models.AppointmentStatus;
import com.fridge.caps.models.TimeSlot;
import com.fridge.caps.views.adapters.AppointmentAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CounselorAppointmentsActivity extends AppCompatActivity {
    private RecyclerView rvUpcoming;
    private RecyclerView rvPast;
    private TextView tvEmptyUpcoming;
    private TextView tvEmptyPast;
    private ProgressBar progressBar;
    private final AppointmentController appointmentController = new AppointmentController();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_appointments);

        rvUpcoming = findViewById(R.id.rvUpcoming);
        rvPast = findViewById(R.id.rvPast);
        tvEmptyUpcoming = findViewById(R.id.tvNoUpcoming);
        tvEmptyPast = findViewById(R.id.tvNoPast);
        progressBar = findViewById(R.id.progressBar);

        rvUpcoming.setLayoutManager(new LinearLayoutManager(this));
        rvPast.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.topBarBack).setOnClickListener(v -> finish());
        loadAppointments();
    }

    private void loadAppointments() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            finish();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection("timeslots")
                .whereEqualTo("counselorId", uid)
                .get()
                .addOnSuccessListener(query -> {
                    List<TimeSlot> slots = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        TimeSlot slot = TimeSlot.fromSnapshot(doc);
                        if (slot.getStudentId() != null && !slot.getStudentId().isEmpty()) {
                            slots.add(slot);
                        } else if (slot.getStatus() != null) {
                            String st = slot.getStatus().toUpperCase();
                            if ("CANCELLED".equals(st) || "NO_SHOW".equals(st)
                                    || "COMPLETED".equals(st)) {
                                slots.add(slot);
                            }
                        }
                    }
                    appointmentController.enrichSlotsToAppointments(slots,
                            new AppointmentController.AppointmentListCallback() {
                                @Override
                                public void onSuccess(List<Appointment> appointments) {
                                    progressBar.setVisibility(View.GONE);
                                    bind(appointments);
                                }

                                @Override
                                public void onFailure(String error) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(CounselorAppointmentsActivity.this,
                                            error, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void bind(List<Appointment> appointments) {
        List<Appointment> upcoming = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING
                        || a.getStatus() == AppointmentStatus.CONFIRMED)
                .collect(Collectors.toList());
        List<Appointment> past = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED
                        || a.getStatus() == AppointmentStatus.CANCELLED
                        || a.getStatus() == AppointmentStatus.NO_SHOW)
                .collect(Collectors.toList());

        tvEmptyUpcoming.setVisibility(upcoming.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyPast.setVisibility(past.isEmpty() ? View.VISIBLE : View.GONE);

        rvUpcoming.setAdapter(new AppointmentAdapter(upcoming,
                AppointmentAdapter.MODE_COUNSELOR_APPOINTMENT_LIST,
                null, null, null, null, null));
        rvPast.setAdapter(new AppointmentAdapter(past,
                AppointmentAdapter.MODE_COUNSELOR_APPOINTMENT_LIST,
                null, null, null, null, null));
    }
}
