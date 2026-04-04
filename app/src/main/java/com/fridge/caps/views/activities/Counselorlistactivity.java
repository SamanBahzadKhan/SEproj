package com.fridge.caps.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.controllers.CounselorController;
import com.fridge.caps.models.Counselor;
import com.fridge.caps.views.adapters.CounselorAdapter;

import java.util.List;

/**
 * CounselorListActivity.java
 * Displays all available counselors for students to browse (US-3).
 * Tapping a counselor opens their full profile.
 * View in the MVC pattern.
 */
public class CounselorListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;

    private CounselorController counselorController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_list);

        counselorController = new CounselorController();

        recyclerView = findViewById(R.id.recyclerViewCounselors);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Counselors");
        }

        loadCounselors();
    }

    private void loadCounselors() {
        progressBar.setVisibility(View.VISIBLE);

        counselorController.getAllCounselors(new CounselorController.CounselorListCallback() {
            @Override
            public void onSuccess(List<Counselor> counselors) {
                progressBar.setVisibility(View.GONE);
                if (counselors.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setAdapter(new CounselorAdapter(counselors, counselor -> {
                        Intent i = new Intent(CounselorListActivity.this,
                                CounselorProfileActivity.class);
                        i.putExtra(CounselorProfileActivity.EXTRA_COUNSELOR_ID,
                                counselor.getUserId());
                        startActivity(i);
                    }));
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CounselorListActivity.this,
                        "Failed to load counselors: " + errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}