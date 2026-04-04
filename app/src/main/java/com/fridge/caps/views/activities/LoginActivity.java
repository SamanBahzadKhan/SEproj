package com.fridge.caps.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.R;
import com.fridge.caps.controllers.AuthController;

/**
 * LoginActivity.java
 * Login screen for student users (US-2).
 * Verifies credentials via AuthController and navigates to dashboard on success.
 * View in the MVC pattern.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText    etEmail, etPassword;
    private Button      btnLogin, btnCounselorLogin, btnAdminLogin;
    private ProgressBar progressBar;
    private TextView    tvRegisterLink;

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

        etEmail          = findViewById(R.id.etEmail);
        etPassword       = findViewById(R.id.etPassword);
        btnLogin         = findViewById(R.id.btnLogin);
        btnCounselorLogin= findViewById(R.id.btnCounselorLogin);
        btnAdminLogin    = findViewById(R.id.btnAdminLogin);
        progressBar      = findViewById(R.id.progressBar);
        tvRegisterLink   = findViewById(R.id.tvRegisterLink);

        btnLogin.setOnClickListener(v -> handleLogin());

        tvRegisterLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        btnCounselorLogin.setOnClickListener(v ->
                Toast.makeText(this, "Counsellor login coming soon.", Toast.LENGTH_SHORT).show());

        btnAdminLogin.setOnClickListener(v ->
                startActivity(new Intent(this, AdminDashboardActivity.class)));
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
        Intent intent = new Intent(this, AdminDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
    }
}