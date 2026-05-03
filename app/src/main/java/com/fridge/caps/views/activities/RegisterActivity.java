package com.fridge.caps.views.activities;


/**
 * Purpose: Handles screen flow, UI state coordination, and user interactions.
 * Depends on: Android UI toolkit, app controllers/viewmodels, and navigation intents.
 * Notes: Focuses on presentation logic while delegating business rules to controllers.
 */
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.AppConfig;
import com.fridge.caps.R;
import com.fridge.caps.controllers.AuthController;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private static final int FLIP_FORM = 0;
    private static final int FLIP_VERIFY = 1;

    private ViewFlipper     registerFlipper;
    private EditText        etName, etEmail, etPassword, etPhone, etDepartment, etYear, etRollNumber;
    private Button          btnRegister;
    private Button          btnVerifiedContinue;
    private Button          btnResendVerification;
    private TextView        tvLoginLink;
    private TextView        tvVerificationEmail;
    private TextView        tvWrongEmailGoBack;
    private ProgressBar     progressBar;

    private AuthController authController;

    private CountDownTimer resendCooldownTimer;

    private String pendingName;
    private String pendingEmail;
    private String pendingPhone;
    private String pendingRollNumber;
    private String pendingDepartment;
    private String pendingYear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authController = new AuthController();

        registerFlipper      = findViewById(R.id.registerFlipper);
        etName               = findViewById(R.id.etName);
        etEmail              = findViewById(R.id.etEmail);
        etPassword           = findViewById(R.id.etPassword);
        etPhone              = findViewById(R.id.etPhone);
        etRollNumber         = findViewById(R.id.etRollNumber);
        etDepartment         = findViewById(R.id.etDepartment);
        etYear               = findViewById(R.id.etYear);
        btnRegister          = findViewById(R.id.btnRegister);
        btnVerifiedContinue  = findViewById(R.id.btnVerifiedContinue);
        btnResendVerification = findViewById(R.id.btnResendVerification);
        tvLoginLink          = findViewById(R.id.tvLoginLink);
        tvVerificationEmail  = findViewById(R.id.tvVerificationEmail);
        tvWrongEmailGoBack   = findViewById(R.id.tvWrongEmailGoBack);
        progressBar          = findViewById(R.id.progressBar);

        tvWrongEmailGoBack.setPaintFlags(
                tvWrongEmailGoBack.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        btnRegister.setOnClickListener(v -> handleRegister());
        btnVerifiedContinue.setOnClickListener(v -> onVerifiedContinueClicked());
        btnResendVerification.setOnClickListener(v -> onResendVerification());
        tvWrongEmailGoBack.setOnClickListener(v -> onWrongEmailGoBack());
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void handleRegister() {
        String name       = etName.getText().toString().trim();
        String email      = etEmail.getText().toString().trim();
        String password   = etPassword.getText().toString().trim();
        String phone      = etPhone.getText().toString().trim();
        String rollNumber = etRollNumber.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String year       = etYear.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Name, email and password are required.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rollNumber.isEmpty()) {
            Toast.makeText(this, "Roll number is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        showLoading(false);
                        Toast.makeText(this, "Account creation failed.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pendingName = name;
                    pendingEmail = email;
                    pendingPhone = phone;
                    pendingRollNumber = rollNumber;
                    pendingDepartment = department;
                    pendingYear = year;

                    if (!AppConfig.REQUIRE_EMAIL_VERIFICATION) {
                        completeRegistrationAfterAuth(user);
                        return;
                    }

                    user.sendEmailVerification()
                            .addOnCompleteListener(task -> {
                                showLoading(false);
                                if (!task.isSuccessful()) {
                                    Toast.makeText(this,
                                            task.getException() != null
                                                    ? task.getException().getMessage()
                                                    : "Could not send verification email.",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }
                                tvVerificationEmail.setText(pendingEmail);
                                registerFlipper.setDisplayedChild(FLIP_VERIFY);
                                startResendCooldown(60_000L);
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this,
                            e.getMessage() != null ? e.getMessage() : "Registration failed.",
                            Toast.LENGTH_LONG).show();
                });
    }

    private void completeRegistrationAfterAuth(FirebaseUser user) {
        if (user == null) {
            showLoading(false);
            return;
        }
        authController.saveStudentProfile(
                user.getUid(),
                pendingName,
                pendingEmail,
                pendingPhone,
                pendingRollNumber,
                pendingDepartment,
                pendingYear,
                new AuthController.RegisterCallback() {
                    @Override
                    public void onSuccess() {
                        showLoading(false);
                        Toast.makeText(RegisterActivity.this,
                                "Account created!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, StudentDashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        showLoading(false);
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void onVerifiedContinueClicked() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Session expired. Please register again.", Toast.LENGTH_LONG).show();
            return;
        }
        btnVerifiedContinue.setEnabled(false);
        btnVerifiedContinue.setText(R.string.checking_email);

        user.reload().addOnCompleteListener(task -> {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            if (u == null) {
                btnVerifiedContinue.setEnabled(true);
                btnVerifiedContinue.setText(R.string.ive_verified_continue);
                Toast.makeText(this, "Session expired. Please sign in.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!u.isEmailVerified()) {
                btnVerifiedContinue.setEnabled(true);
                btnVerifiedContinue.setText(R.string.ive_verified_continue);
                Toast.makeText(this,
                        "Email not verified yet. Please click the link in your inbox.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            showLoading(true);
            authController.saveStudentProfile(
                    u.getUid(),
                    pendingName,
                    pendingEmail,
                    pendingPhone,
                    pendingRollNumber,
                    pendingDepartment,
                    pendingYear,
                    new AuthController.RegisterCallback() {
                        @Override
                        public void onSuccess() {
                            showLoading(false);
                            cancelResendCooldown();
                            btnVerifiedContinue.setEnabled(true);
                            btnVerifiedContinue.setText(R.string.ive_verified_continue);
                            Toast.makeText(RegisterActivity.this,
                                    "Account created!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterActivity.this,
                                    StudentDashboardActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            showLoading(false);
                            btnVerifiedContinue.setEnabled(true);
                            btnVerifiedContinue.setText(R.string.ive_verified_continue);
                            Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void onResendVerification() {
        if (!btnResendVerification.isEnabled()) {
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Session expired.", Toast.LENGTH_SHORT).show();
            return;
        }
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Verification email resent!", Toast.LENGTH_SHORT).show();
                        startResendCooldown(60_000L);
                    } else {
                        Toast.makeText(this,
                                task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Could not resend email.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void onWrongEmailGoBack() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            registerFlipper.setDisplayedChild(FLIP_FORM);
            cancelResendCooldown();
            return;
        }
        showLoading(true);
        user.delete().addOnCompleteListener(task -> {
            showLoading(false);
            cancelResendCooldown();
            registerFlipper.setDisplayedChild(FLIP_FORM);
            if (task.isSuccessful()) {
                Toast.makeText(this, "You can sign up again with a different email.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        task.getException() != null ? task.getException().getMessage()
                                : "Could not reset. Try again.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startResendCooldown(long millisTotal) {
        cancelResendCooldown();
        btnResendVerification.setEnabled(false);
        updateResendCooldownLabel(millisTotal);
        resendCooldownTimer = new CountDownTimer(millisTotal, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateResendCooldownLabel(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                btnResendVerification.setEnabled(true);
                btnResendVerification.setText(R.string.resend_email_upper);
            }
        };
        resendCooldownTimer.start();
    }

    private void updateResendCooldownLabel(long millisUntilFinished) {
        long sec = (millisUntilFinished + 999L) / 1000L;
        btnResendVerification.setText(getString(R.string.resend_in_seconds, sec));
    }

    private void cancelResendCooldown() {
        if (resendCooldownTimer != null) {
            resendCooldownTimer.cancel();
            resendCooldownTimer = null;
        }
        if (btnResendVerification != null) {
            btnResendVerification.setEnabled(true);
            btnResendVerification.setText(R.string.resend_email_upper);
        }
    }

    @Override
    protected void onDestroy() {
        cancelResendCooldown();
        super.onDestroy();
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
    }
}
