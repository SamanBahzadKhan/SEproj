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

import com.fridge.caps.R;
import com.fridge.caps.controllers.AuthController;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Login screen for students; counsellor and admin use separate flows from here.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText    etEmail, etPassword;
    private Button      btnLogin, btnCounselorLogin, btnAdminLogin;
    private ProgressBar progressBar;
    private TextView    tvRegisterLink;
    private TextView    tvForgotPassword;

    private AuthController authController;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private boolean isTestMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authController = new AuthController();

        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        btnLogin          = findViewById(R.id.btnLogin);
        btnCounselorLogin = findViewById(R.id.btnCounselorLogin);
        btnAdminLogin     = findViewById(R.id.btnAdminLogin);
        TextView tvCounselorRegister = findViewById(R.id.tvCounselorRegister);
        progressBar       = findViewById(R.id.progressBar);
        tvRegisterLink    = findViewById(R.id.tvRegisterLink);
        tvForgotPassword  = findViewById(R.id.tvForgotPassword);

        isTestMode = getIntent().getBooleanExtra("TEST_MODE", false);
        if (!isTestMode && authController.isLoggedIn()) {
            routeExistingSessionFromLogin();
            return;
        }

        btnLogin.setOnClickListener(v -> handleLogin());

        tvRegisterLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        btnCounselorLogin.setOnClickListener(v -> handleCounselorLogin());

        btnAdminLogin.setOnClickListener(v ->
                startActivity(new Intent(this, AdminLoginActivity.class)));

        if (tvCounselorRegister != null) {
            tvCounselorRegister.setOnClickListener(v ->
                    startActivity(new Intent(this, CounselorRegisterActivity.class)));
        }

        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    /**
     * If Auth has a session, verify Firestore before routing (same rules as {@link SplashActivity}).
     */
    private void routeExistingSessionFromLogin() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        db.collection("students").document(uid).get()
                .addOnSuccessListener(studentDoc -> {
                    if (studentDoc.exists()) {
                        goToStudentDashboard();
                        return;
                    }
                    db.collection("counselors").document(uid).get()
                            .addOnSuccessListener(counselorDoc -> {
                                if (counselorDoc.exists()) {
                                    goToCounselorDashboard();
                                    return;
                                }
                                db.collection("admins").document(uid).get()
                                        .addOnSuccessListener(adminDoc -> {
                                            if (adminDoc.exists()) {
                                                goToAdminDashboard();
                                            } else {
                                                auth.signOut();
                                                Toast.makeText(this,
                                                        "Account not found. Please sign in again.",
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            auth.signOut();
                                            Toast.makeText(this,
                                                    "Connection error. Please sign in again.",
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> auth.signOut());
                })
                .addOnFailureListener(e -> {
                    auth.signOut();
                    Toast.makeText(this, "Connection error. Please sign in again.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void goToStudentDashboard() {
        Intent intent = new Intent(this, StudentDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToCounselorDashboard() {
        Intent intent = new Intent(this, CounselorDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToAdminDashboard() {
        Intent intent = new Intent(this, AdminDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showForgotPasswordDialog() {
        final EditText input = new EditText(this);
        input.setHint("Email");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        new AlertDialog.Builder(this)
                .setTitle("Reset password")
                .setMessage("Enter your account email. We will send a reset link.")
                .setView(input)
                .setPositiveButton("Send Reset Link", (d, w) -> {
                    String email = input.getText() != null
                            ? input.getText().toString().trim() : "";
                    authController.sendPasswordResetEmail(email, new AuthController.PasswordResetCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(LoginActivity.this,
                                    "Password reset email sent", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password are required.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isTestMode) {
            goToStudentDashboard();
            return;
        }

        showLoading(true);

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() == null) {
                        showLoading(false);
                        Toast.makeText(this, "Login failed.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String uid = authResult.getUser().getUid();

                    db.collection("students").document(uid).get()
                            .addOnSuccessListener(doc -> {
                                showLoading(false);
                                if (doc.exists()) {
                                    goToStudentDashboard();
                                } else {
                                    db.collection("counselors").document(uid).get()
                                            .addOnSuccessListener(cDoc -> {
                                                if (cDoc.exists()) {
                                                    FirebaseAuth.getInstance().signOut();
                                                    Toast.makeText(this,
                                                            "Please use 'Sign in as Counsellor' instead.",
                                                            Toast.LENGTH_LONG).show();
                                                } else {
                                                    FirebaseAuth.getInstance().signOut();
                                                    Toast.makeText(this,
                                                            "Account not found. Please register first.",
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                FirebaseAuth.getInstance().signOut();
                                                Toast.makeText(this, "Login failed: connection error.",
                                                        Toast.LENGTH_SHORT).show();
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                FirebaseAuth.getInstance().signOut();
                                Toast.makeText(this, "Login failed: connection error.",
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Invalid email or password.", Toast.LENGTH_SHORT).show();
                });
    }

    private void handleCounselorLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password are required.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isTestMode) {
            goToCounselorDashboard();
            return;
        }
        showLoading(true);
        authController.loginCounselor(email, password, new AuthController.LoginCallback() {
            @Override
            public void onSuccess() {
                showLoading(false);
                goToCounselorDashboard();
            }

            @Override
            public void onFailure(String errorMessage) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
    }
}
