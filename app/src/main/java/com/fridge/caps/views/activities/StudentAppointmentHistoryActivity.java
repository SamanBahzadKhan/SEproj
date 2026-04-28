package com.fridge.caps.views.activities;

import android.content.Intent;
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
import com.fridge.caps.views.adapters.StudentCompletedHistoryAdapter;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * List of completed appointments for the signed-in student (History quick action).
 */
public class StudentAppointmentHistoryActivity extends AppCompatActivity
        implements StudentCompletedHistoryAdapter.Listener {

    private RecyclerView rvCompleted;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private final AppointmentController appointmentController = new AppointmentController();
    private final List<Appointment> completed = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_history);

        rvCompleted = findViewById(R.id.rvCompleted);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        findViewById(R.id.btnHistoryBack).setOnClickListener(v -> finish());

        rvCompleted.setLayoutManager(new LinearLayoutManager(this));
        rvCompleted.setAdapter(new StudentCompletedHistoryAdapter(completed, this));

        load();
    }

    private void load() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Please sign in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        appointmentController.getStudentAppointments(new AppointmentController.AppointmentListCallback() {
            @Override
            public void onSuccess(List<Appointment> appointments) {
                progressBar.setVisibility(View.GONE);
                completed.clear();
                completed.addAll(appointments.stream()
                        .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                        .collect(Collectors.toList()));
                rvCompleted.getAdapter().notifyDataSetChanged();
                boolean empty = completed.isEmpty();
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvCompleted.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(StudentAppointmentHistoryActivity.this,
                        error != null ? error : "Could not load history.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSessionClick(Appointment appointment) {
        Intent i = new Intent(this, StudentSessionNotesViewActivity.class);
        i.putExtra(StudentSessionNotesViewActivity.EXTRA_TIME_SLOT_ID, appointment.getTimeSlotId());
        i.putExtra(StudentSessionNotesViewActivity.EXTRA_APPOINTMENT_ID, appointment.getAppointmentId());
        i.putExtra(StudentSessionNotesViewActivity.EXTRA_COUNSELOR_NAME, appointment.getCounselorName());
        i.putExtra(StudentSessionNotesViewActivity.EXTRA_SESSION_DATE_LINE,
                StudentCompletedHistoryAdapter.formatSessionLine(appointment));
        startActivity(i);
    }
}
