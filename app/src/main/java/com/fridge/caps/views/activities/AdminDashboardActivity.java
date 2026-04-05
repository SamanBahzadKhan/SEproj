package com.fridge.caps.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.controllers.AppointmentController;
import com.fridge.caps.controllers.AuthController;
import com.fridge.caps.controllers.CounselorController;
import com.fridge.caps.models.Appointment;
import com.fridge.caps.models.AppointmentStatus;
import com.fridge.caps.models.Counselor;
import com.fridge.caps.views.adapters.AppointmentAdapter;
import com.fridge.caps.views.adapters.CounselorAdapter;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.List;

/**
 * Admin panel — overview stats, appointments, counsellors.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private TextView     tvTotalAppts, tvTotalCounselors, tvTotalStudents, tvNoShows;
    private RecyclerView rvAppointments, rvCounselors;
    private ProgressBar  progressBar;
    private Button       btnAddCounselor;

    private AppointmentController appointmentController;
    private CounselorController   counselorController;
    private AuthController        authController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        appointmentController = new AppointmentController();
        counselorController   = new CounselorController();
        authController        = new AuthController();

        tvTotalAppts      = findViewById(R.id.tvTotalAppts);
        tvTotalCounselors = findViewById(R.id.tvTotalCounselors);
        tvTotalStudents   = findViewById(R.id.tvTotalStudents);
        tvNoShows         = findViewById(R.id.tvNoShows);
        rvAppointments    = findViewById(R.id.rvAppointments);
        rvCounselors      = findViewById(R.id.rvCounselors);
        progressBar       = findViewById(R.id.progressBar);
        btnAddCounselor   = findViewById(R.id.btnAddCounselor);

        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        rvCounselors.setLayoutManager(new LinearLayoutManager(this));

        loadAll();

        findViewById(R.id.cardStudents).setOnClickListener(v ->
                startActivity(new Intent(this, StudentListActivity.class)));
        findViewById(R.id.cardCounselors).setOnClickListener(v ->
                startActivity(new Intent(this, CounselorListActivity.class)));

        btnAddCounselor.setOnClickListener(v -> showAddCounselorDialog());

        findViewById(R.id.btnSignOut).setOnClickListener(v -> {
            getSharedPreferences("caps_prefs", MODE_PRIVATE).edit().clear().apply();
            authController.logout();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }

    private void loadAll() {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance().collection("students").get()
                .addOnSuccessListener(q -> {
                    if (tvTotalStudents != null) {
                        tvTotalStudents.setText(String.valueOf(q.size()));
                    }
                });

        appointmentController.getAllBookedTimeslots(new AppointmentController.AppointmentListCallback() {
            @Override
            public void onSuccess(List<Appointment> appointments) {
                tvTotalAppts.setText(String.valueOf(appointments.size()));
                int noShowToday = 0;
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long start = cal.getTimeInMillis();
                long end = start + 86400000L;
                for (Appointment a : appointments) {
                    if (a.getStatus() == AppointmentStatus.NO_SHOW
                            && a.getDate() != null) {
                        long t = a.getDate().toDate().getTime();
                        if (t >= start && t < end) noShowToday++;
                    }
                }
                if (tvNoShows != null) {
                    tvNoShows.setText(String.valueOf(noShowToday));
                }
                rvAppointments.setAdapter(new AppointmentAdapter(appointments,
                        AppointmentAdapter.MODE_ADMIN,
                        null, null, null, null, null));
                progressBar.setVisibility(View.GONE);
            }
            @Override public void onFailure(String e) { progressBar.setVisibility(View.GONE); }
        });

        counselorController.getAllCounselors(new CounselorController.CounselorListCallback() {
            @Override
            public void onSuccess(List<Counselor> counselors) {
                tvTotalCounselors.setText(String.valueOf(counselors.size()));
                rvCounselors.setAdapter(new CounselorAdapter(counselors, counselor -> {
                    Intent i = new Intent(AdminDashboardActivity.this,
                            CounselorProfileActivity.class);
                    i.putExtra(CounselorProfileActivity.EXTRA_COUNSELOR_ID,
                            counselor.getUserId());
                    startActivity(i);
                }));
            }
            @Override public void onFailure(String e) {}
        });
    }

    private void showAddCounselorDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_counselor, null);
        EditText etName  = dialogView.findViewById(R.id.etName);
        EditText etEmail = dialogView.findViewById(R.id.etEmail);
        EditText etSpec  = dialogView.findViewById(R.id.etSpecialization);
        EditText etPass  = dialogView.findViewById(R.id.etPassword);

        new AlertDialog.Builder(this)
                .setTitle("Add New Counsellor")
                .setView(dialogView)
                .setPositiveButton("Add", (d, w) -> {
                    String name  = etName.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    String spec  = etSpec.getText().toString().trim();
                    String pass  = etPass.getText().toString().trim();
                    if (name.isEmpty() || email.isEmpty()) {
                        Toast.makeText(this, "Name and email required.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    authController.registerCounselor(name, email, pass, spec,
                            new AuthController.RegisterCallback() {
                                @Override public void onSuccess() {
                                    Toast.makeText(AdminDashboardActivity.this,
                                            "Counsellor added.", Toast.LENGTH_SHORT).show();
                                    loadAll();
                                }
                                @Override public void onFailure(String error) {
                                    Toast.makeText(AdminDashboardActivity.this,
                                            "Failed: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
