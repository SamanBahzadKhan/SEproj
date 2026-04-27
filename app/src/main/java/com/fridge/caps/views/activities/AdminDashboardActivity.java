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
import com.fridge.caps.controllers.CounselorSignupController;
import com.fridge.caps.controllers.CounselorController;
import com.fridge.caps.controllers.ReportController;
import com.fridge.caps.models.Appointment;
import com.fridge.caps.models.Counselor;
import com.fridge.caps.models.PendingCounselorSignup;
import com.fridge.caps.models.UserReport;
import com.fridge.caps.views.adapters.CounselorAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * Admin panel — overview stats, appointments, counsellors.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private TextView     tvTotalAppts, tvTotalCounselors, tvTotalStudents, tvReportsCount;
    private RecyclerView rvAppointments, rvCounselors;
    private RecyclerView rvPendingSignups;
    private ProgressBar  progressBar;
    private Button       btnAddCounselor;
    private TextView     tvNoReports, tvNoSignups;
    private TextView     adminBellBadge;

    private AppointmentController appointmentController;
    private CounselorController   counselorController;
    private AuthController        authController;
    private ReportController      reportController;
    private CounselorSignupController signupController;
    private ListenerRegistration reportListener;
    private ListenerRegistration signupListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        appointmentController = new AppointmentController();
        counselorController   = new CounselorController();
        authController        = new AuthController();
        reportController      = new ReportController();
        signupController = new CounselorSignupController();

        tvTotalAppts      = findViewById(R.id.tvTotalAppts);
        tvTotalCounselors = findViewById(R.id.tvTotalCounselors);
        tvTotalStudents   = findViewById(R.id.tvTotalStudents);
        tvReportsCount    = findViewById(R.id.tvReportsCount);
        rvAppointments    = findViewById(R.id.rvAppointments);
        rvCounselors      = findViewById(R.id.rvCounselors);
        rvPendingSignups  = findViewById(R.id.rvPendingSignups);
        progressBar       = findViewById(R.id.progressBar);
        btnAddCounselor   = findViewById(R.id.btnAddCounselor);
        tvNoReports       = findViewById(R.id.tvNoReports);
        tvNoSignups       = findViewById(R.id.tvNoSignups);
        adminBellBadge    = findViewById(R.id.adminBellBadge);

        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        rvCounselors.setLayoutManager(new LinearLayoutManager(this));
        rvPendingSignups.setLayoutManager(new LinearLayoutManager(this));

        loadAll();
        listenPendingReports();
        listenPendingSignups();

        findViewById(R.id.topBarBell).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        findViewById(R.id.cardStudents).setOnClickListener(v ->
                startActivity(new Intent(this, StudentListActivity.class)));
        findViewById(R.id.cardCounselors).setOnClickListener(v -> {
            Intent i = new Intent(this, CounselorListActivity.class);
            i.putExtra(CounselorListActivity.EXTRA_ADMIN_COUNSELOR_LIST, true);
            startActivity(i);
        });

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
                progressBar.setVisibility(View.GONE);
            }
            @Override public void onFailure(String e) { progressBar.setVisibility(View.GONE); }
        });

        counselorController.getAllCounselorsForAdmin(new CounselorController.CounselorListCallback() {
            @Override
            public void onSuccess(List<Counselor> counselors) {
                tvTotalCounselors.setText(String.valueOf(counselors.size()));
                rvCounselors.setAdapter(new CounselorAdapter(counselors,
                        counselor -> openCounselor(counselor.getUserId()),
                        new CounselorAdapter.OnCounselorActionListener() {
                            @Override
                            public void onEdit(Counselor counselor) {
                                showEditCounselorDialog(counselor);
                            }

                            @Override
                            public void onDelete(Counselor counselor) {
                                confirmDeleteCounselor(counselor);
                            }
                        }));
            }
            @Override public void onFailure(String e) {}
        });
    }

    private void listenPendingReports() {
        reportListener = reportController.listenPendingReports(new ReportController.ReportListCallback() {
            @Override
            public void onSuccess(List<UserReport> reports) {
                int pendingCount = reports != null ? reports.size() : 0;
                if (tvReportsCount != null) {
                    tvReportsCount.setText(formatStatCount(pendingCount));
                }
                updateAdminBellBadge(pendingCount);
                if (tvNoReports != null) {
                    tvNoReports.setVisibility(reports.isEmpty() ? View.VISIBLE : View.GONE);
                }
                rvAppointments.setAdapter(new com.fridge.caps.views.adapters.ReportedUserAdapter(
                        reports, new com.fridge.caps.views.adapters.ReportedUserAdapter.ActionListener() {
                    @Override
                    public void onOpen(UserReport report) {
                        Intent i = new Intent(AdminDashboardActivity.this, ReportDetailActivity.class);
                        i.putExtra(ReportDetailActivity.EXTRA_REPORT_ID, report.getReportId());
                        startActivity(i);
                    }

                    @Override
                    public void onRemove(UserReport report) {
                        progressBar.setVisibility(View.VISIBLE);
                        reportController.removeReportedUser(report, new ReportController.ActionCallback() {
                            @Override public void onSuccess() { progressBar.setVisibility(View.GONE); }
                            @Override public void onFailure(String error) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(AdminDashboardActivity.this,
                                        error != null ? error : "Failed to remove user.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onIgnore(UserReport report) {
                        progressBar.setVisibility(View.VISIBLE);
                        reportController.markReportStatus(report.getReportId(), "ignored",
                                new ReportController.ActionCallback() {
                            @Override public void onSuccess() { progressBar.setVisibility(View.GONE); }
                            @Override public void onFailure(String error) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(AdminDashboardActivity.this,
                                        error != null ? error : "Failed to ignore report.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }));
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AdminDashboardActivity.this,
                        error != null ? error : "Failed to load reports.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenPendingSignups() {
        signupListener = signupController.listenPendingSignups(new CounselorSignupController.SignupListCallback() {
            @Override
            public void onSuccess(List<PendingCounselorSignup> signups) {
                tvNoSignups.setVisibility(signups.isEmpty() ? View.VISIBLE : View.GONE);
                rvPendingSignups.setAdapter(new com.fridge.caps.views.adapters.PendingCounselorSignupAdapter(
                        signups, new com.fridge.caps.views.adapters.PendingCounselorSignupAdapter.ActionListener() {
                    @Override
                    public void onOpen(PendingCounselorSignup signup) {
                        Intent i = new Intent(AdminDashboardActivity.this, PendingCounselorSignupDetailActivity.class);
                        i.putExtra(PendingCounselorSignupDetailActivity.EXTRA_SIGNUP_ID, signup.getSignupId());
                        startActivity(i);
                    }

                    @Override
                    public void onApprove(PendingCounselorSignup signup) {
                        progressBar.setVisibility(View.VISIBLE);
                        signupController.approveSignup(signup, new CounselorSignupController.ActionCallback() {
                            @Override public void onSuccess() { progressBar.setVisibility(View.GONE); }
                            @Override public void onFailure(String error) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(AdminDashboardActivity.this,
                                        error != null ? error : "Failed to approve signup.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onReject(PendingCounselorSignup signup) {
                        progressBar.setVisibility(View.VISIBLE);
                        signupController.rejectSignup(signup, new CounselorSignupController.ActionCallback() {
                            @Override public void onSuccess() { progressBar.setVisibility(View.GONE); }
                            @Override public void onFailure(String error) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(AdminDashboardActivity.this,
                                        error != null ? error : "Failed to reject signup.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }));
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AdminDashboardActivity.this,
                        error != null ? error : "Failed to load signups.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String formatStatCount(int n) {
        if (n > 99) return "99+";
        return String.valueOf(n);
    }

    private void updateAdminBellBadge(int pendingReports) {
        if (adminBellBadge == null) return;
        if (pendingReports <= 0) {
            adminBellBadge.setVisibility(View.GONE);
            return;
        }
        adminBellBadge.setVisibility(View.VISIBLE);
        adminBellBadge.setText(formatStatCount(pendingReports));
    }

    private void openCounselor(String counselorId) {
        Intent i = new Intent(AdminDashboardActivity.this, CounselorProfileActivity.class);
        i.putExtra(CounselorProfileActivity.EXTRA_COUNSELOR_ID, counselorId);
        i.putExtra("counselorId", counselorId);
        startActivity(i);
    }

    private void showEditCounselorDialog(Counselor counselor) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_counselor_admin, null);
        EditText etName  = dialogView.findViewById(R.id.etName);
        EditText etSpec  = dialogView.findViewById(R.id.etSpecialization);
        EditText etDept  = dialogView.findViewById(R.id.etDepartment);
        EditText etPhone = dialogView.findViewById(R.id.etPhone);
        android.widget.Switch swAccepting = dialogView.findViewById(R.id.switchAcceptingClients);

        etName.setText(counselor.getName());
        etSpec.setText(counselor.getSpecialization());
        swAccepting.setChecked(counselor.isAcceptingClients());

        new AlertDialog.Builder(this)
                .setTitle("Edit Counsellor")
                .setView(dialogView)
                .setPositiveButton("Save changes", (d, w) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    FirebaseFirestore.getInstance().collection("counselors")
                            .document(counselor.getUserId())
                            .update(
                                    "name", etName.getText().toString().trim(),
                                    "specialization", etSpec.getText().toString().trim(),
                                    "department", etDept.getText().toString().trim(),
                                    "phone", etPhone.getText().toString().trim(),
                                    "isAcceptingClients", swAccepting.isChecked()
                            )
                            .addOnSuccessListener(v -> {
                                progressBar.setVisibility(View.GONE);
                                loadAll();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteCounselor(Counselor counselor) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Counsellor")
                .setMessage("Disable " + (counselor.getName() != null ? counselor.getName() : "this counsellor")
                        + "? Existing appointment history will remain.")
                .setPositiveButton("Delete", (d, w) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    FirebaseFirestore.getInstance().collection("counselors")
                            .document(counselor.getUserId())
                            .update(
                                    "isActive", false,
                                    "isDeleted", true,
                                    "isAcceptingClients", false
                            )
                            .addOnSuccessListener(v -> {
                                progressBar.setVisibility(View.GONE);
                                loadAll();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reportListener != null) reportListener.remove();
        if (signupListener != null) signupListener.remove();
    }
}
