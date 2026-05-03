package com.fridge.caps.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.R;
import com.fridge.caps.controllers.AuthController;

/**
 * Admin email/password sign-in; verifies {@code admins} collection after Firebase Auth.
 */
public class AdminLoginActivity extends AppCompatActivity {

    private EditText    etEmail, etPassword;
    private ImageButton btnTogglePasswordAdmin;
    private Button      btnAdminSignIn;
    private TextView    tvBackToStudent;
    private ProgressBar progressBar;

    private AuthController authController;
    private boolean        passwordVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        authController = new AuthController();

        etEmail               = findViewById(R.id.etEmail);
        etPassword            = findViewById(R.id.etPassword);
        btnTogglePasswordAdmin = findViewById(R.id.btnTogglePasswordAdmin);
        btnAdminSignIn        = findViewById(R.id.btnAdminSignIn);
        tvBackToStudent       = findViewById(R.id.tvBackToStudent);
        progressBar           = findViewById(R.id.progressBar);

        btnAdminSignIn.setOnClickListener(v -> signIn());
        findViewById(R.id.tvForgotPasswordAdmin).setOnClickListener(v -> handleForgotPassword());
        tvBackToStudent.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        btnTogglePasswordAdmin.setOnClickListener(v -> togglePasswordVisibility());
        passwordVisible = false;
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        int selStart = etPassword.getSelectionStart();
        int selEnd = etPassword.getSelectionEnd();
        if (passwordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePasswordAdmin.setImageResource(R.drawable.ic_visibility_off_24);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePasswordAdmin.setImageResource(R.drawable.ic_visibility_24);
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
                new AlertDialog.Builder(AdminLoginActivity.this)
                        .setTitle(R.string.forgot_reset_sent_title)
                        .setMessage(getString(R.string.forgot_reset_sent_message, email))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(AdminLoginActivity.this,
                        errorMessage != null ? errorMessage
                                : getString(R.string.forgot_password_invalid_email),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void signIn() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnAdminSignIn.setEnabled(false);

        authController.loginAdmin(email, password, new AuthController.LoginCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                btnAdminSignIn.setEnabled(true);
                Intent intent = new Intent(AdminLoginActivity.this, AdminDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                btnAdminSignIn.setEnabled(true);
                Toast.makeText(AdminLoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
