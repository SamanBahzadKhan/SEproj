package com.fridge.caps.views.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.models.Student;
import com.fridge.caps.views.adapters.StudentAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists all students from the {@code students} collection.
 */
public class StudentListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);

        RecyclerView rv = findViewById(R.id.recyclerView);
        ProgressBar pb = findViewById(R.id.progressBar);
        TextView tvEmpty = findViewById(R.id.tvEmpty);
        rv.setLayoutManager(new LinearLayoutManager(this));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Registered Students");
        }

        pb.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection("students")
                .get()
                .addOnSuccessListener(q -> {
                    pb.setVisibility(View.GONE);
                    List<Student> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : q) {
                        Student s = doc.toObject(Student.class);
                        if (s != null) {
                            s.setUserId(doc.getId());
                            list.add(s);
                        }
                    }
                    tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    rv.setAdapter(new StudentAdapter(list));
                })
                .addOnFailureListener(e -> {
                    pb.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load students.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
