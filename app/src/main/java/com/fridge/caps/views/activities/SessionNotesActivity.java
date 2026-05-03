package com.fridge.caps.views.activities;


/**
 * Purpose: Handles screen flow, UI state coordination, and user interactions.
 * Depends on: Android UI toolkit, app controllers/viewmodels, and navigation intents.
 * Notes: Focuses on presentation logic while delegating business rules to controllers.
 */
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.controllers.SessionNotesController;
import com.fridge.caps.views.adapters.SessionAttachmentAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionNotesActivity extends AppCompatActivity {

    public static final String EXTRA_TIME_SLOT_ID      = "time_slot_id";
    public static final String EXTRA_STUDENT_ID        = "student_id";
    public static final String EXTRA_STUDENT_NAME      = "student_name";
    public static final String EXTRA_SESSION_DATE_LINE = "session_date_line";

    private String timeSlotId;
    private String studentId;
    private String studentDisplayName;
    private String sessionDateLine;
    private String counselorUid;
    private String counselorName = "";

    private TextView tvPatientName;
    private TextView tvStudentIdLine;
    private TextView tvSessionWhen;
    private EditText etDiagnosis;
    private EditText etRecommendations;
    private LinearLayout llPrescriptions;
    private MaterialButton btnSaveSubmit;
    private MaterialButton btnSaveDraft;
    private RecyclerView rvAttachments;

    private SessionNotesController controller;
    private SessionAttachmentAdapter attachmentAdapter;
    private ActivityResultLauncher<String> pickDocsLauncher;
    private boolean alreadySubmitted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_notes);

        Intent in = getIntent();
        timeSlotId = in.getStringExtra(EXTRA_TIME_SLOT_ID);
        studentId = in.getStringExtra(EXTRA_STUDENT_ID);
        studentDisplayName = in.getStringExtra(EXTRA_STUDENT_NAME);
        sessionDateLine = in.getStringExtra(EXTRA_SESSION_DATE_LINE);
        counselorUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (timeSlotId == null || timeSlotId.isEmpty() || studentId == null || studentId.isEmpty()
                || counselorUid == null) {
            Toast.makeText(this, "Missing session data.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        controller = new SessionNotesController();

        tvPatientName = findViewById(R.id.tvPatientName);
        tvStudentIdLine = findViewById(R.id.tvStudentIdLine);
        tvSessionWhen = findViewById(R.id.tvSessionWhen);
        etDiagnosis = findViewById(R.id.etDiagnosis);
        etRecommendations = findViewById(R.id.etRecommendations);
        llPrescriptions = findViewById(R.id.llPrescriptions);
        btnSaveSubmit = findViewById(R.id.btnSaveSubmit);
        btnSaveDraft = findViewById(R.id.btnSaveDraft);
        rvAttachments = findViewById(R.id.rvAttachments);

        tvPatientName.setText(studentDisplayName != null && !studentDisplayName.isEmpty()
                ? studentDisplayName : "Student");
        tvStudentIdLine.setText("Student ID: " + shortenId(studentId));
        tvSessionWhen.setText(sessionDateLine != null ? sessionDateLine : "");

        findViewById(R.id.btnSessionNotesBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddMedication).setOnClickListener(v -> addEmptyPrescriptionRow());
        findViewById(R.id.btnBrowseFiles).setOnClickListener(v -> openPicker());

        rvAttachments.setLayoutManager(new LinearLayoutManager(this));
        attachmentAdapter = new SessionAttachmentAdapter(() -> { });
        rvAttachments.setAdapter(attachmentAdapter);
        rvAttachments.setNestedScrollingEnabled(false);

        pickDocsLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                this::onDocumentsPickedList);

        FirebaseFirestore.getInstance().collection("counselors").document(counselorUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("name") != null) {
                        counselorName = doc.getString("name");
                    }
                });

        controller.checkAlreadySubmitted(studentId, timeSlotId, submitted -> {
            alreadySubmitted = submitted;
            if (submitted) {
                new AlertDialog.Builder(this)
                        .setTitle("Already submitted")
                        .setMessage("Session notes for this appointment were already sent to the student.")
                        .setPositiveButton("OK", (d, w) -> finish())
                        .setCancelable(false)
                        .show();
                btnSaveSubmit.setEnabled(false);
                btnSaveDraft.setEnabled(false);
                return;
            }
            loadDraft();
        });

        btnSaveDraft.setOnClickListener(v -> saveDraft());
        btnSaveSubmit.setOnClickListener(v -> saveSubmit());
    }

    private static String shortenId(String uid) {
        if (uid == null) {
            return "—";
        }
        if (uid.length() <= 12) {
            return uid;
        }
        return uid.substring(0, 8) + "…" + uid.substring(uid.length() - 4);
    }

    private void openPicker() {
        if (alreadySubmitted) {
            return;
        }
        pickDocsLauncher.launch("*/*");
    }

    private void onDocumentsPickedList(List<Uri> uris) {
        if (uris == null) {
            return;
        }
        for (Uri u : uris) {
            if (u == null) {
                continue;
            }
            String name = u.getLastPathSegment();
            if (name == null || name.isEmpty()) {
                name = "attachment";
            }
            attachmentAdapter.addRow(new SessionAttachmentAdapter.Row(name, u, null));
        }
    }

    private void addEmptyPrescriptionRow() {
        addPrescriptionRow("", "", "");
    }

    private void addPrescriptionRow(String name, String dosage, String instructions) {
        View block = LayoutInflater.from(this).inflate(R.layout.item_prescription_block, llPrescriptions, false);
        EditText etN = block.findViewById(R.id.etMedName);
        EditText etD = block.findViewById(R.id.etDosage);
        EditText etI = block.findViewById(R.id.etMedInstructions);
        etN.setText(name);
        etD.setText(dosage);
        etI.setText(instructions);
        llPrescriptions.addView(block);
    }

    private void loadDraft() {
        controller.loadDraft(counselorUid, timeSlotId,
                doc -> {
                    if (doc == null || !doc.exists()) {
                        if (llPrescriptions.getChildCount() == 0) {
                            addEmptyPrescriptionRow();
                        }
                        return;
                    }
                    String dx = doc.getString("diagnosis");
                    if (dx != null) {
                        etDiagnosis.setText(dx);
                    }
                    String rec = doc.getString("recommendations");
                    if (rec != null) {
                        etRecommendations.setText(rec);
                    }
                    llPrescriptions.removeAllViews();
                    Object prObj = doc.get("prescriptions");
                    boolean addedRx = false;
                    if (prObj instanceof List) {
                        List<?> raw = (List<?>) prObj;
                        for (Object o : raw) {
                            if (!(o instanceof Map)) {
                                continue;
                            }
                            @SuppressWarnings("unchecked")
                            Map<String, Object> m = (Map<String, Object>) o;
                            String n = m.get("name") != null ? String.valueOf(m.get("name")) : "";
                            String d = m.get("dosage") != null ? String.valueOf(m.get("dosage")) : "";
                            String ins = m.get("instructions") != null ? String.valueOf(m.get("instructions")) : "";
                            if (n.isEmpty() && d.isEmpty() && ins.isEmpty()) {
                                continue;
                            }
                            addPrescriptionRow(n, d, ins);
                            addedRx = true;
                        }
                    }
                    if (!addedRx) {
                        addEmptyPrescriptionRow();
                    }
                    Object urlObj = doc.get("attachmentUrls");
                    if (urlObj instanceof List) {
                        List<SessionAttachmentAdapter.Row> rows = new ArrayList<>();
                        for (Object o : (List<?>) urlObj) {
                            if (o == null) {
                                continue;
                            }
                            String url = String.valueOf(o);
                            String label = url;
                            int slash = url.lastIndexOf('/');
                            if (slash >= 0 && slash < url.length() - 1) {
                                label = url.substring(slash + 1);
                            }
                            int q = label.indexOf('?');
                            if (q > 0) {
                                label = label.substring(0, q);
                            }
                            rows.add(new SessionAttachmentAdapter.Row(label, null, url));
                        }
                        attachmentAdapter.setItems(rows);
                    }
                },
                e -> {
                    if (llPrescriptions.getChildCount() == 0) {
                        addEmptyPrescriptionRow();
                    }
                });
    }

    private List<Map<String, String>> collectPrescriptions() {
        List<Map<String, String>> out = new ArrayList<>();
        for (int i = 0; i < llPrescriptions.getChildCount(); i++) {
            View row = llPrescriptions.getChildAt(i);
            EditText etN = row.findViewById(R.id.etMedName);
            EditText etD = row.findViewById(R.id.etDosage);
            EditText etI = row.findViewById(R.id.etMedInstructions);
            String n = etN.getText() != null ? etN.getText().toString().trim() : "";
            String d = etD.getText() != null ? etD.getText().toString().trim() : "";
            String ins = etI.getText() != null ? etI.getText().toString().trim() : "";
            if (n.isEmpty() && d.isEmpty() && ins.isEmpty()) {
                continue;
            }
            Map<String, String> m = new HashMap<>();
            m.put("name", n);
            m.put("dosage", d);
            m.put("instructions", ins);
            out.add(m);
        }
        return out;
    }

    private List<Uri> collectNewLocalUris() {
        List<Uri> uris = new ArrayList<>();
        for (SessionAttachmentAdapter.Row r : attachmentAdapter.getItems()) {
            if (r.isPendingUpload()) {
                uris.add(r.localUri);
            }
        }
        return uris;
    }

    private List<String> collectExistingUrls() {
        List<String> urls = new ArrayList<>();
        for (SessionAttachmentAdapter.Row r : attachmentAdapter.getItems()) {
            if (!r.isPendingUpload() && r.remoteUrl != null && !r.remoteUrl.isEmpty()) {
                urls.add(r.remoteUrl);
            }
        }
        return urls;
    }

    private void saveDraft() {
        if (alreadySubmitted) {
            return;
        }
        btnSaveDraft.setEnabled(false);
        controller.saveDraft(counselorUid, timeSlotId, studentId, studentDisplayName, counselorName,
                sessionDateLine,
                textOf(etDiagnosis), textOf(etRecommendations),
                collectPrescriptions(),
                collectNewLocalUris(),
                collectExistingUrls(),
                new SessionNotesController.VoidCallback() {
                    @Override
                    public void onSuccess() {
                        btnSaveDraft.setEnabled(true);
                        Toast.makeText(SessionNotesActivity.this, "Draft saved.", Toast.LENGTH_SHORT).show();
                        loadDraft();
                    }

                    @Override
                    public void onFailure(String message) {
                        btnSaveDraft.setEnabled(true);
                        Toast.makeText(SessionNotesActivity.this,
                                message != null ? message : "Could not save draft.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveSubmit() {
        if (alreadySubmitted) {
            return;
        }
        String dx = textOf(etDiagnosis);
        if (dx.isEmpty()) {
            Toast.makeText(this, "Please enter a diagnosis before submitting.", Toast.LENGTH_SHORT).show();
            return;
        }
        btnSaveSubmit.setEnabled(false);
        btnSaveDraft.setEnabled(false);
        controller.submitToStudent(counselorUid, counselorName, timeSlotId, studentId, studentDisplayName,
                sessionDateLine,
                dx, textOf(etRecommendations),
                collectPrescriptions(),
                collectNewLocalUris(),
                collectExistingUrls(),
                new SessionNotesController.VoidCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(SessionNotesActivity.this, "Sent to student.", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailure(String message) {
                        btnSaveSubmit.setEnabled(true);
                        btnSaveDraft.setEnabled(true);
                        Toast.makeText(SessionNotesActivity.this,
                                message != null ? message : "Submit failed.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private static String textOf(EditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }
}
