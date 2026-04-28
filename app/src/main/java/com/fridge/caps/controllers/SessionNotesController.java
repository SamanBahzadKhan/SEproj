package com.fridge.caps.controllers;

import android.net.Uri;

import com.fridge.caps.utils.NotificationUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Draft session notes on the counselor; submitted notes are written to the student's document tree.
 */
public class SessionNotesController {

    private static final String COUNSELORS = "counselors";
    private static final String DRAFTS    = "sessionNoteDrafts";
    private static final String STUDENTS  = "students";
    private static final String RECEIVED  = "receivedSessionNotes";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    public interface VoidCallback {
        void onSuccess();

        void onFailure(String message);
    }

    public interface BoolCallback {
        void onResult(boolean alreadySubmitted);
    }

    /** Whether the student already has a submitted note for this slot. */
    public void checkAlreadySubmitted(String studentId, String timeSlotId, BoolCallback callback) {
        if (studentId == null || timeSlotId == null) {
            callback.onResult(false);
            return;
        }
        CollectionReference col = db.collection(STUDENTS).document(studentId).collection(RECEIVED);
        col.document(timeSlotId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (isReceivedNotesVisibleToStudent(doc)) {
                        callback.onResult(true);
                        return;
                    }
                    col.whereEqualTo("timeSlotId", timeSlotId)
                            .limit(5)
                            .get()
                            .addOnSuccessListener(snap -> {
                                for (DocumentSnapshot q : snap.getDocuments()) {
                                    if (isReceivedNotesVisibleToStudent(q)) {
                                        callback.onResult(true);
                                        return;
                                    }
                                }
                                callback.onResult(false);
                            })
                            .addOnFailureListener(e -> callback.onResult(false));
                })
                .addOnFailureListener(e -> callback.onResult(false));
    }

    public void loadDraft(String counselorId, String timeSlotId,
                          com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot> onOk,
                          com.google.android.gms.tasks.OnFailureListener onFail) {
        if (counselorId == null || timeSlotId == null) {
            onOk.onSuccess(null);
            return;
        }
        db.collection(COUNSELORS).document(counselorId).collection(DRAFTS).document(timeSlotId)
                .get()
                .addOnSuccessListener(onOk)
                .addOnFailureListener(onFail);
    }

    public void saveDraft(String counselorId, String timeSlotId, String studentId, String studentName,
                          String counselorName, String sessionDateLine,
                          String diagnosis, String recommendations,
                          List<Map<String, String>> prescriptions,
                          List<Uri> newLocalFiles,
                          List<String> existingUrls,
                          VoidCallback callback) {
        if (counselorId == null || timeSlotId == null) {
            if (callback != null) {
                callback.onFailure("Missing counselor or slot.");
            }
            return;
        }
        uploadNewFiles(counselorId, timeSlotId, newLocalFiles, (uploadedUrls, err) -> {
            if (err != null) {
                if (callback != null) {
                    callback.onFailure(err);
                }
                return;
            }
            List<String> allUrls = new ArrayList<>();
            if (existingUrls != null) {
                allUrls.addAll(existingUrls);
            }
            allUrls.addAll(uploadedUrls);

            Map<String, Object> data = basePayload(timeSlotId, studentId, studentName, counselorId,
                    counselorName, sessionDateLine, diagnosis, recommendations, prescriptions, allUrls);
            data.put("status", "draft");
            data.put("updatedAt", FieldValue.serverTimestamp());

            db.collection(COUNSELORS).document(counselorId).collection(DRAFTS).document(timeSlotId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(v -> {
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) {
                            callback.onFailure(e.getMessage() != null ? e.getMessage() : "Draft save failed");
                        }
                    });
        });
    }

    public void submitToStudent(String counselorId, String counselorName, String timeSlotId,
                                String studentId, String studentName, String sessionDateLine,
                                String diagnosis, String recommendations,
                                List<Map<String, String>> prescriptions,
                                List<Uri> newLocalFiles,
                                List<String> existingUrls,
                                VoidCallback callback) {
        if (studentId == null || studentId.isEmpty()) {
            if (callback != null) {
                callback.onFailure("Missing student.");
            }
            return;
        }
        uploadNewFiles(counselorId, timeSlotId, newLocalFiles, (uploadedUrls, err) -> {
            if (err != null) {
                if (callback != null) {
                    callback.onFailure(err);
                }
                return;
            }
            List<String> allUrls = new ArrayList<>();
            if (existingUrls != null) {
                allUrls.addAll(existingUrls);
            }
            allUrls.addAll(uploadedUrls);

            Map<String, Object> studentDoc = basePayload(timeSlotId, studentId, studentName,
                    counselorId, counselorName, sessionDateLine, diagnosis, recommendations,
                    prescriptions, allUrls);
            studentDoc.put("submitted", true);
            studentDoc.put("submittedAt", FieldValue.serverTimestamp());

            DocumentReference studentRef = db.collection(STUDENTS).document(studentId)
                    .collection(RECEIVED).document(timeSlotId);
            DocumentReference draftRef = db.collection(COUNSELORS).document(counselorId)
                    .collection(DRAFTS).document(timeSlotId);

            db.runTransaction(transaction -> {
                        transaction.set(studentRef, studentDoc, SetOptions.merge());
                        transaction.delete(draftRef);
                        return null;
                    }).addOnSuccessListener(v -> {
                        NotificationUtils.writeNotification(db, studentId,
                                "Session notes from your counsellor",
                                "Dr. " + (counselorName != null ? counselorName : "Your counsellor")
                                        + " shared session notes"
                                        + (sessionDateLine != null && !sessionDateLine.isEmpty()
                                        ? " for " + sessionDateLine + "." : "."),
                                "SESSION_NOTES");
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) {
                            callback.onFailure(e.getMessage() != null ? e.getMessage() : "Submit failed");
                        }
                    });
        });
    }

    private interface UploadDone {
        void onDone(List<String> urls, String error);
    }

    private void uploadNewFiles(String counselorId, String timeSlotId, List<Uri> uris, UploadDone done) {
        if (uris == null || uris.isEmpty()) {
            done.onDone(new ArrayList<>(), null);
            return;
        }
        List<Task<String>> tasks = new ArrayList<>();
        for (Uri uri : uris) {
            if (uri == null) {
                continue;
            }
            String name = safeFileName(uri);
            StorageReference ref = storage.getReference()
                    .child("session_notes")
                    .child(counselorId)
                    .child(timeSlotId)
                    .child(UUID.randomUUID().toString() + "_" + name);
            tasks.add(ref.putFile(uri).continueWithTask(t -> {
                if (!t.isSuccessful()) {
                    Exception ex = t.getException();
                    throw ex != null ? ex : new Exception("Upload failed");
                }
                return ref.getDownloadUrl();
            }).continueWith(t -> {
                if (!t.isSuccessful() || t.getResult() == null) {
                    return null;
                }
                return t.getResult().toString();
            }));
        }
        if (tasks.isEmpty()) {
            done.onDone(new ArrayList<>(), null);
            return;
        }
        Tasks.whenAllComplete(tasks)
                .addOnSuccessListener(v -> {
                    List<String> urls = new ArrayList<>();
                    for (Task<String> t : tasks) {
                        if (t.isSuccessful() && t.getResult() != null) {
                            urls.add(t.getResult());
                        }
                    }
                    if (urls.size() != tasks.size()) {
                        done.onDone(urls, "Some files failed to upload.");
                    } else {
                        done.onDone(urls, null);
                    }
                })
                .addOnFailureListener(e ->
                        done.onDone(new ArrayList<>(), e.getMessage() != null ? e.getMessage() : "Upload failed"));
    }

    private static String safeFileName(Uri uri) {
        String last = uri.getLastPathSegment();
        if (last == null || last.isEmpty()) {
            return "file";
        }
        return last.replace("/", "_").substring(0, Math.min(last.length(), 80));
    }

    /**
     * Submitted notes visible to the student (read-only).
     * Resolves by document id first, then by {@code timeSlotId} field inside the subcollection.
     */
    public void loadReceivedNotes(String studentId, String timeSlotId,
                                  com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot> onOk,
                                  com.google.android.gms.tasks.OnFailureListener onFail) {
        if (studentId == null || studentId.isEmpty() || timeSlotId == null || timeSlotId.isEmpty()) {
            onOk.onSuccess(null);
            return;
        }
        CollectionReference col = db.collection(STUDENTS).document(studentId).collection(RECEIVED);
        col.document(timeSlotId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (isReceivedNotesVisibleToStudent(doc)) {
                        onOk.onSuccess(doc);
                        return;
                    }
                    col.whereEqualTo("timeSlotId", timeSlotId)
                            .limit(10)
                            .get()
                            .addOnSuccessListener(snap -> {
                                DocumentSnapshot chosen = null;
                                for (DocumentSnapshot q : snap.getDocuments()) {
                                    if (isReceivedNotesVisibleToStudent(q)) {
                                        chosen = q;
                                        break;
                                    }
                                }
                                if (chosen != null) {
                                    onOk.onSuccess(chosen);
                                } else if (doc.exists()) {
                                    onOk.onSuccess(doc);
                                } else if (!snap.isEmpty()) {
                                    onOk.onSuccess(snap.getDocuments().get(0));
                                } else {
                                    onOk.onSuccess(null);
                                }
                            })
                            .addOnFailureListener(onFail);
                })
                .addOnFailureListener(onFail);
    }

    /**
     * All documents under {@code students/{id}/receivedSessionNotes}, sorted by counsellor name
     * (A–Z) then newest {@code submittedAt} first within each counsellor.
     */
    public void loadAllReceivedSessionNotes(String studentId,
            com.google.android.gms.tasks.OnSuccessListener<List<DocumentSnapshot>> onOk,
            com.google.android.gms.tasks.OnFailureListener onFail) {
        if (studentId == null || studentId.isEmpty()) {
            onOk.onSuccess(new ArrayList<>());
            return;
        }
        db.collection(STUDENTS).document(studentId).collection(RECEIVED)
                .get()
                .addOnSuccessListener(q -> {
                    List<DocumentSnapshot> list = new ArrayList<>(q.getDocuments());
                    Collections.sort(list, RECEIVED_NOTES_LIST_ORDER);
                    onOk.onSuccess(list);
                })
                .addOnFailureListener(onFail);
    }

    private static final Comparator<DocumentSnapshot> RECEIVED_NOTES_LIST_ORDER = (a, b) -> {
        int c = counselorSortKey(a).compareToIgnoreCase(counselorSortKey(b));
        if (c != 0) {
            return c;
        }
        return Long.compare(submittedAtMillis(b), submittedAtMillis(a));
    };

    private static String counselorSortKey(DocumentSnapshot d) {
        String n = d.getString("counselorName");
        return n != null ? n.trim() : "";
    }

    private static long submittedAtMillis(DocumentSnapshot d) {
        com.google.firebase.Timestamp t = d.getTimestamp("submittedAt");
        if (t != null) {
            return t.toDate().getTime();
        }
        return 0L;
    }

    /** Whether this snapshot should show submitted session notes to the student. */
    public static boolean isReceivedNotesVisibleToStudent(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return false;
        }
        if (Boolean.FALSE.equals(doc.getBoolean("submitted"))) {
            return false;
        }
        if (truthySubmittedRaw(doc.get("submitted"))) {
            return true;
        }
        return hasMeaningfulNotesPayload(doc);
    }

    private static boolean truthySubmittedRaw(Object raw) {
        if (raw == null) {
            return false;
        }
        if (raw instanceof Boolean) {
            return (Boolean) raw;
        }
        if (raw instanceof Long) {
            return ((Long) raw) != 0L;
        }
        if (raw instanceof Double) {
            return ((Double) raw) != 0.0;
        }
        if (raw instanceof Integer) {
            return ((Integer) raw) != 0;
        }
        if (raw instanceof String) {
            String s = ((String) raw).trim().toLowerCase(Locale.US);
            return "true".equals(s) || "1".equals(s) || "yes".equals(s);
        }
        return false;
    }

    private static boolean hasMeaningfulNotesPayload(DocumentSnapshot doc) {
        if (nonEmpty(doc.getString("diagnosis"))) {
            return true;
        }
        if (nonEmpty(doc.getString("recommendations"))) {
            return true;
        }
        Object urls = doc.get("attachmentUrls");
        if (urls instanceof List && !((List<?>) urls).isEmpty()) {
            return true;
        }
        Object rx = doc.get("prescriptions");
        return rx instanceof List && !((List<?>) rx).isEmpty();
    }

    private static boolean nonEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static Map<String, Object> basePayload(String timeSlotId, String studentId, String studentName,
                                                    String counselorId, String counselorName,
                                                    String sessionDateLine, String diagnosis, String recommendations,
                                                    List<Map<String, String>> prescriptions, List<String> urls) {
        Map<String, Object> m = new HashMap<>();
        m.put("timeSlotId", timeSlotId);
        m.put("studentId", studentId);
        m.put("studentName", studentName != null ? studentName : "");
        m.put("counselorId", counselorId);
        m.put("counselorName", counselorName != null ? counselorName : "");
        m.put("sessionDateLine", sessionDateLine != null ? sessionDateLine : "");
        m.put("diagnosis", diagnosis != null ? diagnosis : "");
        m.put("recommendations", recommendations != null ? recommendations : "");
        m.put("prescriptions", prescriptions != null ? prescriptions : new ArrayList<>());
        m.put("attachmentUrls", urls != null ? urls : new ArrayList<>());
        return m;
    }
}
