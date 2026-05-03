package com.fridge.caps.views.activities;


/**
 * Purpose: Handles screen flow, UI state coordination, and user interactions.
 * Depends on: Android UI toolkit, app controllers/viewmodels, and navigation intents.
 * Notes: Focuses on presentation logic while delegating business rules to controllers.
 */
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.fridge.caps.R;
import com.fridge.caps.controllers.SessionNotesController;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentSessionNotesViewActivity extends AppCompatActivity {

    public static final String EXTRA_TIME_SLOT_ID      = "time_slot_id";
    
    public static final String EXTRA_APPOINTMENT_ID    = "appointment_id";
    public static final String EXTRA_COUNSELOR_NAME    = "counselor_name";
    public static final String EXTRA_SESSION_DATE_LINE = "session_date_line";

    private String studentId;
    private String timeSlotId;
    
    private String alternateNoteDocId;
    private String counselorNameHint;
    private String sessionDateHint;

    private ProgressBar progressBar;
    private NestedScrollView scrollContent;
    private View cardNotSubmitted;
    private TextView tvNotSubmitted;
    private LinearLayout llSubmittedBody;
    private TextView tvMetaLine;
    private TextView tvDiagnosis;
    private TextView tvPrescriptions;
    private TextView tvRecommendations;
    private LinearLayout llAttachmentLinks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_session_notes_view);

        studentId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        String slot = getIntent().getStringExtra(EXTRA_TIME_SLOT_ID);
        String appt = getIntent().getStringExtra(EXTRA_APPOINTMENT_ID);
        if (slot != null && !slot.isEmpty()) {
            timeSlotId = slot;
        } else if (appt != null && !appt.isEmpty()) {
            timeSlotId = appt;
        } else {
            timeSlotId = null;
        }
        alternateNoteDocId = (appt != null && !appt.isEmpty() && !appt.equals(timeSlotId)) ? appt : null;
        counselorNameHint = getIntent().getStringExtra(EXTRA_COUNSELOR_NAME);
        sessionDateHint = getIntent().getStringExtra(EXTRA_SESSION_DATE_LINE);

        if (studentId == null || timeSlotId == null || timeSlotId.isEmpty()) {
            Toast.makeText(this, "Invalid session.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar = findViewById(R.id.progressBar);
        scrollContent = findViewById(R.id.scrollContent);
        cardNotSubmitted = findViewById(R.id.cardNotSubmitted);
        tvNotSubmitted = findViewById(R.id.tvNotSubmitted);
        llSubmittedBody = findViewById(R.id.llSubmittedBody);
        tvMetaLine = findViewById(R.id.tvMetaLine);
        tvDiagnosis = findViewById(R.id.tvDiagnosis);
        tvPrescriptions = findViewById(R.id.tvPrescriptions);
        tvRecommendations = findViewById(R.id.tvRecommendations);
        llAttachmentLinks = findViewById(R.id.llAttachmentLinks);

        findViewById(R.id.btnNotesBack).setOnClickListener(v -> finish());

        fetchNotes();
    }

    private void fetchNotes() {
        SessionNotesController controller = new SessionNotesController();
        controller.loadReceivedNotes(studentId, timeSlotId,
                doc -> {
                    if (SessionNotesController.isReceivedNotesVisibleToStudent(doc)) {
                        applyDocument(doc);
                        return;
                    }
                    if (alternateNoteDocId != null) {
                        controller.loadReceivedNotes(studentId, alternateNoteDocId,
                                doc2 -> {
                                    if (SessionNotesController.isReceivedNotesVisibleToStudent(doc2)) {
                                        applyDocument(doc2);
                                    } else {
                                        applyDocument(doc != null && doc.exists() ? doc : doc2);
                                    }
                                },
                                this::onNotesLoadFailed);
                    } else {
                        applyDocument(doc);
                    }
                },
                this::onNotesLoadFailed);
    }

    private void onNotesLoadFailed(Exception e) {
        progressBar.setVisibility(View.GONE);
        scrollContent.setVisibility(View.VISIBLE);
        Toast.makeText(this,
                e.getMessage() != null ? e.getMessage() : getString(R.string.session_notes_load_failed),
                Toast.LENGTH_LONG).show();
        showNotSubmitted(getString(R.string.session_notes_load_failed));
    }

    private void applyDocument(DocumentSnapshot doc) {
        progressBar.setVisibility(View.GONE);
        scrollContent.setVisibility(View.VISIBLE);

        boolean submitted = doc != null && SessionNotesController.isReceivedNotesVisibleToStudent(doc);
        if (!submitted) {
            showNotSubmitted(getString(R.string.session_notes_not_submitted));
            return;
        }

        cardNotSubmitted.setVisibility(View.GONE);
        llSubmittedBody.setVisibility(View.VISIBLE);

        String cName = doc.getString("counselorName");
        if (cName == null || cName.isEmpty()) {
            cName = counselorNameHint;
        }
        if (cName != null && !cName.toLowerCase(Locale.US).startsWith("dr.")) {
            cName = "Dr. " + cName;
        }
        String sessionLine = doc.getString("sessionDateLine");
        if (sessionLine == null || sessionLine.isEmpty()) {
            sessionLine = sessionDateHint != null ? sessionDateHint : "";
        }
        tvMetaLine.setText((cName != null ? cName : "Your counsellor")
                + (sessionLine.isEmpty() ? "" : " · " + sessionLine));

        String dx = doc.getString("diagnosis");
        tvDiagnosis.setText(dx != null && !dx.isEmpty() ? dx : "—");

        tvPrescriptions.setText(formatPrescriptions(doc.get("prescriptions")));

        String rec = doc.getString("recommendations");
        tvRecommendations.setText(rec != null && !rec.isEmpty() ? rec : "—");

        llAttachmentLinks.removeAllViews();
        Object urlObj = doc.get("attachmentUrls");
        if (urlObj instanceof List) {
            boolean any = false;
            for (Object o : (List<?>) urlObj) {
                if (o == null) {
                    continue;
                }
                String url = String.valueOf(o);
                if (url.isEmpty()) {
                    continue;
                }
                any = true;
                TextView link = new TextView(this);
                link.setTextColor(ContextCompat.getColor(this, R.color.caps_brut_blue_gray));
                link.setTextSize(15f);
                link.setPadding(0, 8, 0, 8);
                String label = shortFileLabel(url);
                SpannableString ss = new SpannableString(label + "\n" + url);
                ss.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(), 0);
                link.setText(ss);
                link.setOnClickListener(v -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception ignored) {
                        Toast.makeText(this, "Cannot open link.", Toast.LENGTH_SHORT).show();
                    }
                });
                llAttachmentLinks.addView(link);
            }
            if (!any) {
                addPlainLine("—");
            }
        } else {
            addPlainLine("—");
        }
    }

    private void addPlainLine(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(ContextCompat.getColor(this, R.color.caps_brut_navy));
        t.setTextSize(15f);
        llAttachmentLinks.addView(t);
    }

    private void showNotSubmitted(String message) {
        cardNotSubmitted.setVisibility(View.VISIBLE);
        tvNotSubmitted.setText(message);
        llSubmittedBody.setVisibility(View.GONE);
    }

    private static String shortFileLabel(String url) {
        int slash = url.lastIndexOf('/');
        String label = slash >= 0 ? url.substring(slash + 1) : url;
        int q = label.indexOf('?');
        if (q > 0) {
            label = label.substring(0, q);
        }
        return label.isEmpty() ? "Open file" : label;
    }

    @SuppressWarnings("unchecked")
    private static String formatPrescriptions(Object raw) {
        if (!(raw instanceof List)) {
            return "—";
        }
        List<?> list = (List<?>) raw;
        if (list.isEmpty()) {
            return "—";
        }
        StringBuilder sb = new StringBuilder();
        int n = 1;
        for (Object o : list) {
            if (!(o instanceof Map)) {
                continue;
            }
            Map<String, Object> m = (Map<String, Object>) o;
            String name = m.get("name") != null ? String.valueOf(m.get("name")).trim() : "";
            String dose = m.get("dosage") != null ? String.valueOf(m.get("dosage")).trim() : "";
            String ins = m.get("instructions") != null ? String.valueOf(m.get("instructions")).trim() : "";
            if (name.isEmpty() && dose.isEmpty() && ins.isEmpty()) {
                continue;
            }
            sb.append(n++).append(". ");
            if (!name.isEmpty()) {
                sb.append(name);
            }
            sb.append("\n");
            if (!dose.isEmpty()) {
                sb.append("   Dosage: ").append(dose).append("\n");
            }
            if (!ins.isEmpty()) {
                sb.append("   ").append(ins).append("\n");
            }
            sb.append("\n");
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? "—" : out;
    }
}
