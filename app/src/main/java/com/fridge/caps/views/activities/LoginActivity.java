package com.fridge.caps.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.AppConfig;
import com.fridge.caps.R;
import com.fridge.caps.controllers.AuthController;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Login screen for students and counsellors (same credentials); admin uses a separate flow.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText    etEmail, etPassword;
    private Button      btnLogin, btnAdminLogin;
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
        btnAdminLogin     = findViewById(R.id.btnAdminLogin);
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

        btnAdminLogin.setOnClickListener(v ->
                startActivity(new Intent(this, AdminLoginActivity.class)));

        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    /**
     * If Auth has a session, verify email (where applicable) and verify Firestore before routing.
     */
    private void routeExistingSessionFromLogin() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return;
        }
        user.reload().addOnCompleteListener(t -> {
            FirebaseUser u = auth.getCurrentUser();
            if (u == null) {
                return;
            }
            if (AppConfig.REQUIRE_EMAIL_VERIFICATION && !u.isEmailVerified()) {
                auth.signOut();
                Toast.makeText(this,
                        "Please verify your email before signing in.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            String uid = u.getUid();
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
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        showLoading(false);
                        Toast.makeText(this, "Login failed.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    user.reload().addOnCompleteListener(rt -> {
                        showLoading(false);
                        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                        if (u == null) {
                            Toast.makeText(this, "Login failed.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (AppConfig.REQUIRE_EMAIL_VERIFICATION && !u.isEmailVerified()) {
                            FirebaseAuth.getInstance().signOut();
                            showUnverifiedLoginDialog(email, password);
                            return;
                        }
                        routeAfterSuccessfulLogin(u.getUid());
                    });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Invalid email or password.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showUnverifiedLoginDialog(String email, String password) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_login_email_unverified, null);
        Button btnResend = content.findViewById(R.id.btnResendVerificationFromLogin);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        dialog.show();

        btnResend.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(ar -> {
                        FirebaseUser u = ar.getUser();
                        if (u == null) {
                            Toast.makeText(this, "Could not resend.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        u.sendEmailVerification().addOnCompleteListener(task -> {
                            FirebaseAuth.getInstance().signOut();
                            if (task.isSuccessful()) {
                                Toast.makeText(this,
                                        "Verification email resent",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this,
                                        task.getException() != null
                                                ? task.getException().getMessage()
                                                : "Could not send email.",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    e.getMessage() != null ? e.getMessage() : "Failed.",
                                    Toast.LENGTH_SHORT).show());
        });
    }

    private void routeAfterSuccessfulLogin(String uid) {
        db.collection("students").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        goToStudentDashboard();
                        return;
                    }
                    db.collection("counselors").document(uid).get()
                            .addOnSuccessListener(cDoc -> {
                                if (cDoc.exists()) {
                                    Boolean isActive = cDoc.getBoolean("isActive");
                                    if (isActive != null && !isActive) {
                                        FirebaseAuth.getInstance().signOut();
                                        Toast.makeText(this,
                                                "This counsellor account has been disabled.",
                                                Toast.LENGTH_LONG).show();
                                    } else {
                                        goToCounselorDashboard();
                                    }
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
                })
                .addOnFailureListener(e -> {
                    FirebaseAuth.getInstance().signOut();
                    Toast.makeText(this, "Login failed: connection error.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
    }
}
