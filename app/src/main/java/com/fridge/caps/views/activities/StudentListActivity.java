package com.fridge.caps.views.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
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
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Admin: all registered students with search.
 */
public class StudentListActivity extends AppCompatActivity {

    private RecyclerView rv;
    private ProgressBar pb;
    private TextView tvEmpty;
    private EditText etSearch;
    private final List<Student> allStudents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        rv = findViewById(R.id.recyclerView);
        pb = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        etSearch = findViewById(R.id.etSearchStudents);
        ImageButton back = findViewById(R.id.topBarBack);
        back.setOnClickListener(v -> finish());

        rv.setLayoutManager(new LinearLayoutManager(this));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s != null ? s.toString() : "");
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        loadStudents();
    }

    private void loadStudents() {
        pb.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection("students")
                .get()
                .addOnSuccessListener(q -> {
                    pb.setVisibility(View.GONE);
                    allStudents.clear();
                    for (QueryDocumentSnapshot doc : q) {
                        Student s = doc.toObject(Student.class);
                        if (s != null) {
                            s.setUserId(doc.getId());
                            allStudents.add(s);
                        }
                    }
                    applyFilter(etSearch.getText() != null ? etSearch.getText().toString() : "");
                })
                .addOnFailureListener(e -> {
                    pb.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load students.", Toast.LENGTH_SHORT).show();
                });
    }

    private void applyFilter(String query) {
        String q = query.trim().toLowerCase(Locale.getDefault());
        List<Student> filtered = allStudents.stream()
                .filter(s -> {
                    if (q.isEmpty()) return true;
                    String name = s.getName() != null ? s.getName().toLowerCase(Locale.getDefault()) : "";
                    String id = s.getUserId() != null ? s.getUserId().toLowerCase(Locale.getDefault()) : "";
                    String dept = s.getDepartment() != null
                            ? s.getDepartment().toLowerCase(Locale.getDefault()) : "";
                    return name.contains(q) || id.contains(q) || dept.contains(q);
                })
                .collect(Collectors.toList());

        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rv.setAdapter(new StudentAdapter(filtered));
    }
}
