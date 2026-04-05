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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authController = new AuthController();

        if (authController.isLoggedIn()) {
            goToDashboard();
            return;
        }

        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        btnLogin          = findViewById(R.id.btnLogin);
        btnCounselorLogin = findViewById(R.id.btnCounselorLogin);
        btnAdminLogin     = findViewById(R.id.btnAdminLogin);
        TextView tvCounselorRegister = findViewById(R.id.tvCounselorRegister);
        progressBar       = findViewById(R.id.progressBar);
        tvRegisterLink    = findViewById(R.id.tvRegisterLink);
        tvForgotPassword  = findViewById(R.id.tvForgotPassword);

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

        showLoading(true);

        authController.loginStudent(email, password, new AuthController.LoginCallback() {
            @Override
            public void onSuccess() {
                showLoading(false);
                goToDashboard();
            }

            @Override
            public void onFailure(String errorMessage) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToDashboard() {
        Intent intent = new Intent(this, StudentDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleCounselorLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password are required.", Toast.LENGTH_SHORT).show();
            return;
        }
        showLoading(true);
        authController.loginCounselor(email, password, new AuthController.LoginCallback() {
            @Override
            public void onSuccess() {
                showLoading(false);
                Intent intent = new Intent(LoginActivity.this, CounselorDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
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
