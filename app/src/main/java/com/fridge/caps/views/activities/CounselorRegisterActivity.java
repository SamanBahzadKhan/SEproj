package com.fridge.caps.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.R;
import com.fridge.caps.controllers.AuthController;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Self-service counsellor sign-up; lands on {@link CounselorDashboardActivity}.
 */
public class CounselorRegisterActivity extends AppCompatActivity {

    private EditText      etName, etEmail, etPassword, etSpecialization, etDepartment, etPhone, etBio;
    private Button        btnRegister;
    private ProgressBar   progressBar;

    private AuthController authController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_register);

        authController = new AuthController();

        etName           = findViewById(R.id.etName);
        etEmail          = findViewById(R.id.etEmail);
        etPassword       = findViewById(R.id.etPassword);
        etSpecialization = findViewById(R.id.etSpecialization);
        etDepartment     = findViewById(R.id.etDepartment);
        etPhone          = findViewById(R.id.etPhone);
        etBio            = findViewById(R.id.etBio);
        btnRegister      = findViewById(R.id.btnRegister);
        progressBar      = findViewById(R.id.progressBar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Counsellor Sign Up");
        }

        btnRegister.setOnClickListener(v -> submit());
    }

    private void submit() {
        String name = text(etName);
        String email = text(etEmail);
        String p1 = text(etPassword);
        String spec = text(etSpecialization);
        String dept = text(etDepartment);
        String phone = text(etPhone);
        String bio = text(etBio);

        if (name.isEmpty() || email.isEmpty() || p1.isEmpty() || spec.isEmpty()
                || dept.isEmpty() || phone.isEmpty() || bio.isEmpty()) {
            Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        authController.registerCounselorAccount(name, email, p1, spec, dept, phone, bio,
                new AuthController.RegisterCallback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                        FirebaseAuth.getInstance().signOut();
                        Toast.makeText(CounselorRegisterActivity.this,
                                "Signup submitted. Wait for admin approval.", Toast.LENGTH_LONG).show();
                        Intent i = new Intent(CounselorRegisterActivity.this, LoginActivity.class);
                        finish();
                        startActivity(i);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        Toast.makeText(CounselorRegisterActivity.this,
                                errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private static String text(EditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
