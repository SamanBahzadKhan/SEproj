package com.fridge.caps.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.R;
import com.fridge.caps.controllers.AuthController;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Self-service counsellor sign-up; lands on {@link CounselorDashboardActivity}.
 */
public class CounselorRegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etPasswordConfirm,
            etSpecialization, etDepartment, etPhone, etBio;
    private MaterialButton    btnRegister;
    private ProgressBar       progressBar;

    private AuthController authController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_register);

        authController = new AuthController();

        etName            = findViewById(R.id.etName);
        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        etSpecialization  = findViewById(R.id.etSpecialization);
        etDepartment      = findViewById(R.id.etDepartment);
        etPhone           = findViewById(R.id.etPhone);
        etBio             = findViewById(R.id.etBio);
        btnRegister       = findViewById(R.id.btnRegister);
        progressBar       = findViewById(R.id.progressBar);

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
        String p2 = text(etPasswordConfirm);
        String spec = text(etSpecialization);
        String dept = text(etDepartment);
        String phone = text(etPhone);
        String bio = text(etBio);

        if (name.isEmpty() || email.isEmpty() || p1.isEmpty() || spec.isEmpty()
                || dept.isEmpty() || phone.isEmpty() || bio.isEmpty()) {
            Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!p1.equals(p2)) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        authController.registerCounselorAccount(name, email, p1, spec, dept, phone, bio,
                new AuthController.RegisterCallback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                        Intent i = new Intent(CounselorRegisterActivity.this,
                                CounselorDashboardActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
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

    private static String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
