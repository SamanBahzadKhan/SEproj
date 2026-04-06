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
 * RegisterActivity.java
 * Registration screen for new student accounts (US-1).
 * Collects student details and delegates to AuthController.
 * View in the MVC pattern.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText    etName, etEmail, etPassword, etPhone, etDepartment, etYear;
    private Button      btnRegister;
    private ProgressBar progressBar;
    private TextView    tvLoginLink;

    private AuthController authController;

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

        showLoading(true);

        authController.registerStudent(name, email, password, phone, department, year,
                new AuthController.RegisterCallback() {
                    @Override
                    public void onSuccess() {
                        showLoading(false);
                        Toast.makeText(RegisterActivity.this,
                                "Account created!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        showLoading(false);
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
    }
}
