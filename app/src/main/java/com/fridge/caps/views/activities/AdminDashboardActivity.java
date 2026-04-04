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
import com.fridge.caps.models.Counselor;
import com.fridge.caps.views.adapters.AppointmentAdapter;
import com.fridge.caps.views.adapters.CounselorAdapter;

import java.util.List;

/**
 * AdminDashboardActivity.java
 * Admin panel for viewing all appointments and managing counselors (US-16, US-17).
 * View in the MVC pattern.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private TextView     tvTotalAppts, tvTotalCounselors;
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

        tvTotalAppts     = findViewById(R.id.tvTotalAppts);
        tvTotalCounselors= findViewById(R.id.tvTotalCounselors);
        rvAppointments   = findViewById(R.id.rvAppointments);
        rvCounselors     = findViewById(R.id.rvCounselors);
        progressBar      = findViewById(R.id.progressBar);
        btnAddCounselor  = findViewById(R.id.btnAddCounselor);

        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        rvCounselors.setLayoutManager(new LinearLayoutManager(this));

        loadAll();

        btnAddCounselor.setOnClickListener(v -> showAddCounselorDialog());

        findViewById(R.id.btnSignOut).setOnClickListener(v -> {
            authController.logout();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }

    private void loadAll() {
        progressBar.setVisibility(View.VISIBLE);

        appointmentController.getAllAppointments(new AppointmentController.AppointmentListCallback() {
            @Override
            public void onSuccess(List<Appointment> appointments) {
                tvTotalAppts.setText(String.valueOf(appointments.size()));
                rvAppointments.setAdapter(new AppointmentAdapter(
                        appointments, false, null, null, null));
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
                .setTitle("Add New Counselor")
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
                    // Register counselor via Firebase Auth + Firestore
                    authController.registerStudent(name, email, pass, "", spec, "", new AuthController.RegisterCallback() {
                        @Override public void onSuccess() {
                            Toast.makeText(AdminDashboardActivity.this,
                                    "Counselor added!", Toast.LENGTH_SHORT).show();
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