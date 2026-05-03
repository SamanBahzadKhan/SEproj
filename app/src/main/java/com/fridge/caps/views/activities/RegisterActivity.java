package com.fridge.caps.views.activities;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

/*
MANUAL STEP IN FIREBASE CONSOLE:
Go to Authentication → Email Templates → Email address verification
Customize the email:
- Subject: "Verify your CAPs account"
- Body: include %LINK% which Firebase replaces with the verification button
This cannot be done in code — must be done in Firebase Console
*/

/**
 * Registration screen for new student accounts.
 * Email must be verified before the Firestore profile is written and dashboard opens.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText    etName, etEmail, etPassword, etPhone, etDepartment, etYear;
    private Button      btnRegister;
    private ProgressBar progressBar;
    private TextView    tvLoginLink;

    private AuthController authController;

    private AlertDialog    verifyDialog;
    private Handler        resendHandler = new Handler(Looper.getMainLooper());
    private Runnable       resendCountdownRunnable;
    private int            resendCooldownRemainingSec;
    private TextView       tvResendLink;

    private String pendingName;
    private String pendingEmail;
    private String pendingPhone;
    private String pendingDepartment;
    private String pendingYear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authController = new AuthController();

        etName       = findViewById(R.id.etName);
        etEmail      = findViewById(R.id.etEmail);
        etPassword   = findViewById(R.id.etPassword);
        etPhone      = findViewById(R.id.etPhone);
        etDepartment = findViewById(R.id.etDepartment);
        etYear       = findViewById(R.id.etYear);
        btnRegister  = findViewById(R.id.btnRegister);
        progressBar  = findViewById(R.id.progressBar);
        tvLoginLink  = findViewById(R.id.tvLoginLink);

        btnRegister.setOnClickListener(v -> handleRegister());
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
                                showVerifyEmailDialog(user);
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this,
                            e.getMessage() != null ? e.getMessage() : "Registration failed.",
                            Toast.LENGTH_LONG).show();
                });
    }

    /** Writes student doc and opens dashboard (no verification step). */
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

    private void showVerifyEmailDialog(FirebaseUser user) {
        View content = getLayoutInflater().inflate(R.layout.dialog_verify_email, null);
        TextView tvBody = content.findViewById(R.id.tvVerifyBody);
        Button btnContinue = content.findViewById(R.id.btnVerifiedContinue);
        tvResendLink = content.findViewById(R.id.tvResendEmail);
        tvResendLink.setPaintFlags(tvResendLink.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        String emailShown = pendingEmail != null ? pendingEmail : "";
        tvBody.setText(getString(R.string.verify_email_body_template, emailShown));

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setView(content)
                .setCancelable(false);
        verifyDialog = b.create();
        verifyDialog.setCanceledOnTouchOutside(false);
        verifyDialog.show();

        btnContinue.setOnClickListener(v -> onVerifiedContinueClicked(user));

        tvResendLink.setOnClickListener(v -> onResendVerification(user));
        startResendCooldown(0);
    }

    private void onVerifiedContinueClicked(FirebaseUser user) {
        if (user == null) {
            Toast.makeText(this, "Session expired. Please register again.", Toast.LENGTH_LONG).show();
            return;
        }
        user.reload().addOnCompleteListener(task -> {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            if (u == null || !u.getUid().equals(user.getUid())) {
                Toast.makeText(this, "Session expired. Please sign in.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!u.isEmailVerified()) {
                Toast.makeText(this,
                        "Email not verified yet. Please check your inbox.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            showLoading(true);
            authController.saveStudentProfile(
                    u.getUid(),
                    pendingName,
                    pendingEmail,
                    pendingPhone,
                    pendingDepartment,
                    pendingYear,
                    new AuthController.RegisterCallback() {
                        @Override
                        public void onSuccess() {
                            showLoading(false);
                            cancelResendCooldown();
                            if (verifyDialog != null && verifyDialog.isShowing()) {
                                verifyDialog.dismiss();
                            }
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
        });
    }

    private void onResendVerification(FirebaseUser user) {
        if (resendCooldownRemainingSec > 0) {
            return;
        }
        if (user == null) {
            Toast.makeText(this, "Session expired.", Toast.LENGTH_SHORT).show();
            return;
        }
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Verification email resent", Toast.LENGTH_SHORT).show();
                        startResendCooldown(60);
                    } else {
                        Toast.makeText(this,
                                task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Could not resend email.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void startResendCooldown(int seconds) {
        cancelResendCooldown();
        resendCooldownRemainingSec = seconds;
        if (tvResendLink == null) {
            return;
        }
        if (seconds <= 0) {
            tvResendLink.setEnabled(true);
            tvResendLink.setAlpha(1f);
            tvResendLink.setText("Resend Email");
            return;
        }
        tvResendLink.setEnabled(false);
        tvResendLink.setAlpha(0.5f);
        resendCountdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (tvResendLink == null) return;
                if (resendCooldownRemainingSec <= 0) {
                    tvResendLink.setEnabled(true);
                    tvResendLink.setAlpha(1f);
                    tvResendLink.setText("Resend Email");
                    return;
                }
                tvResendLink.setText(getString(R.string.resend_email_countdown, resendCooldownRemainingSec));
                resendCooldownRemainingSec--;
                resendHandler.postDelayed(this, 1000L);
            }
        };
        resendHandler.post(resendCountdownRunnable);
    }

    private void cancelResendCooldown() {
        if (resendCountdownRunnable != null) {
            resendHandler.removeCallbacks(resendCountdownRunnable);
            resendCountdownRunnable = null;
        }
        resendCooldownRemainingSec = 0;
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
