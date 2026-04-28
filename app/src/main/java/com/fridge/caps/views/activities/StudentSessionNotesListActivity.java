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
import com.fridge.caps.controllers.SessionNotesController;
import com.fridge.caps.views.adapters.StudentReceivedNotesListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

/**
 * Lists all received session-note documents for the student, grouped under each counsellor.
 */
public class StudentSessionNotesListActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvEmpty;
    private RecyclerView recyclerView;
    private StudentReceivedNotesListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_session_notes_list);

        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        recyclerView = findViewById(R.id.rvSessionNotes);
        findViewById(R.id.btnSessionNotesListBack).setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentReceivedNotesListAdapter(this::openNote);
        recyclerView.setAdapter(adapter);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Please sign in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        new SessionNotesController().loadAllReceivedSessionNotes(uid,
                this::onLoaded,
                e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            e.getMessage() != null ? e.getMessage() : getString(R.string.session_notes_load_failed),
                            Toast.LENGTH_SHORT).show();
                    tvEmpty.setVisibility(View.VISIBLE);
                });
    }

    private void onLoaded(List<DocumentSnapshot> docs) {
        progressBar.setVisibility(View.GONE);
        if (docs == null || docs.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }
        tvEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter.setDocuments(docs);
    }

    private void openNote(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return;
        }
        String slot = doc.getString("timeSlotId");
        if (slot == null || slot.isEmpty()) {
            slot = doc.getId();
        }
        String cname = doc.getString("counselorName");
        String sessionLine = doc.getString("sessionDateLine");
        Intent i = new Intent(this, StudentSessionNotesViewActivity.class);
        i.putExtra(StudentSessionNotesViewActivity.EXTRA_TIME_SLOT_ID, slot);
        i.putExtra(StudentSessionNotesViewActivity.EXTRA_APPOINTMENT_ID, doc.getId());
        i.putExtra(StudentSessionNotesViewActivity.EXTRA_COUNSELOR_NAME, cname != null ? cname : "");
        i.putExtra(StudentSessionNotesViewActivity.EXTRA_SESSION_DATE_LINE, sessionLine != null ? sessionLine : "");
        startActivity(i);
    }
}
