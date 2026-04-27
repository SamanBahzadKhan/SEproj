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
 * Admin email/password sign-in; verifies {@code admins} collection after Firebase Auth.
 */
public class AdminLoginActivity extends AppCompatActivity {

    private EditText    etEmail, etPassword;
    private Button      btnAdminSignIn;
    private TextView    tvBackToStudent;
    private ProgressBar progressBar;

    private AuthController authController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        authController = new AuthController();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnAdminSignIn = findViewById(R.id.btnAdminSignIn);
        tvBackToStudent = findViewById(R.id.tvBackToStudent);
        progressBar = findViewById(R.id.progressBar);

        btnAdminSignIn.setOnClickListener(v -> signIn());
        tvBackToStudent.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
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
