package com.fridge.caps.views.activities;


/**
 * Purpose: Handles screen flow, UI state coordination, and user interactions.
 * Depends on: Android UI toolkit, app controllers/viewmodels, and navigation intents.
 * Notes: Focuses on presentation logic while delegating business rules to controllers.
 */
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

public class LoginActivity extends AppCompatActivity {

    private EditText    etEmail, etPassword;
    private ImageButton btnTogglePassword;
    private Button      btnLogin, btnAdminLogin;
    private ProgressBar progressBar;
    private TextView    tvRegisterLink;
    private TextView    tvForgotPassword;

    private AuthController authController;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private boolean isTestMode;
    private boolean   passwordVisible;

    
    private String lastEnteredEmail    = "";
    private String lastEnteredPassword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authController = new AuthController();

        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
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

        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());

        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        passwordVisible = false;
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        int selStart = etPassword.getSelectionStart();
        int selEnd = etPassword.getSelectionEnd();
        if (passwordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_off_24);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_24);
        }
        etPassword.setTypeface(etEmail.getTypeface());
        CharSequence text = etPassword.getText();
        int len = text != null ? text.length() : 0;
        int a = selStart >= 0 ? Math.min(selStart, len) : len;
        int b = selEnd >= 0 ? Math.min(selEnd, len) : len;
        if (a <= b) {
            etPassword.setSelection(a, b);
        } else {
            etPassword.setSelection(b, a);
        }
    }

    private void handleForgotPassword() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        if (email.isEmpty()) {
            Toast.makeText(this, R.string.forgot_password_enter_email_first, Toast.LENGTH_LONG).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.forgot_password_invalid_email, Toast.LENGTH_LONG).show();
            return;
        }
        authController.sendPasswordResetEmail(email, new AuthController.PasswordResetCallback() {
            @Override
            public void onSuccess() {
                new AlertDialog.Builder(LoginActivity.this)
                        .setTitle(R.string.forgot_reset_sent_title)
                        .setMessage(getString(R.string.forgot_reset_sent_message, email))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(LoginActivity.this,
                        errorMessage != null ? errorMessage : getString(R.string.forgot_password_invalid_email),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    
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
            final String uid = u.getUid();
            db.collection("counselors").document(uid).get()
                    .addOnSuccessListener(cDoc -> {
                        if (cDoc.exists()) {
                            Boolean isActive = cDoc.getBoolean("isActive");
                            if (isActive != null && !isActive) {
                                auth.signOut();
                                Toast.makeText(this,
                                        "This counsellor account has been disabled.",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }
                            goToCounselorDashboard();
                            return;
                        }
                        db.collection("admins").document(uid).get()
                                .addOnSuccessListener(aDoc -> {
                                    if (aDoc.exists()) {
                                        goToAdminDashboard();
                                        return;
                                    }
                                    if (AppConfig.REQUIRE_EMAIL_VERIFICATION
                                            && !u.isEmailVerified()) {
                                        auth.signOut();
                                        Toast.makeText(this,
                                                "Please verify your email before signing in.",
                                                Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    db.collection("students").document(uid).get()
                                            .addOnSuccessListener(sDoc -> {
                                                if (sDoc.exists()) {
                                                    goToStudentDashboard();
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
                                .addOnFailureListener(e -> {
                                    auth.signOut();
                                    Toast.makeText(this,
                                            "Connection error. Please sign in again.",
                                            Toast.LENGTH_SHORT).show();
                                });
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

        lastEnteredEmail = email;
        lastEnteredPassword = password;

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
                        maybeRouteAfterLogin(u.getUid(), email, password);
                    });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Invalid email or password.", Toast.LENGTH_SHORT).show();
                });
    }

    
    private void maybeRouteAfterLogin(String uid, String email, String password) {
        db.collection("counselors").document(uid).get()
                .addOnSuccessListener(cDoc -> {
                    if (cDoc.exists()) {
                        Boolean isActive = cDoc.getBoolean("isActive");
                        if (isActive != null && !isActive) {
                            FirebaseAuth.getInstance().signOut();
                            Toast.makeText(this,
                                    "This counsellor account has been disabled.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        goToCounselorDashboard();
                        return;
                    }
                    db.collection("admins").document(uid).get()
                            .addOnSuccessListener(aDoc -> {
                                if (aDoc.exists()) {
                                    goToAdminDashboard();
                                    return;
                                }
                                if (AppConfig.REQUIRE_EMAIL_VERIFICATION) {
                                    FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                                    if (u != null && !u.isEmailVerified()) {
                                        FirebaseAuth.getInstance().signOut();
                                        showUnverifiedLoginDialog(email, password);
                                        return;
                                    }
                                }
                                db.collection("students").document(uid).get()
                                        .addOnSuccessListener(sDoc -> {
                                            if (sDoc.exists()) {
                                                goToStudentDashboard();
                                            } else {
                                                FirebaseAuth.getInstance().signOut();
                                                Toast.makeText(this,
                                                        "Account not found. Please register first.",
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            FirebaseAuth.getInstance().signOut();
                                            Toast.makeText(this,
                                                    "Login failed: connection error.",
                                                    Toast.LENGTH_SHORT).show();
                                        });
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

    private void showUnverifiedLoginDialog(String email, String password) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_login_email_unverified, null);
        Button btnResend = content.findViewById(R.id.btnResendVerificationFromLogin);
        Button btnOk     = content.findViewById(R.id.btnOkUnverified);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();

        btnOk.setOnClickListener(v -> dialog.dismiss());

        btnResend.setOnClickListener(v -> {
            String em = email != null ? email : lastEnteredEmail;
            String pw = password != null ? password : lastEnteredPassword;
            FirebaseAuth.getInstance().signInWithEmailAndPassword(em, pw)
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
                                        "Verification email sent!",
                                        Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
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

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
    }
}
