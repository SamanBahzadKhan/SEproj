package com.fridge.caps.controllers;

import com.fridge.caps.models.UserReport;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportController {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface ReportListCallback {
        void onSuccess(List<UserReport> reports);
        void onFailure(String error);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface ReportCallback {
        void onSuccess(UserReport report);
        void onFailure(String error);
    }

    public ListenerRegistration listenPendingReports(ReportListCallback callback) {
        return db.collection("reports")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) {
                        callback.onFailure(e != null ? e.getMessage() : "Failed to load reports.");
                        return;
                    }
                    List<UserReport> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        list.add(UserReport.fromDocument(doc));
                    }
                    Collections.sort(list, (a, b) -> {
                        long ta = a.getTimestamp() != null ? a.getTimestamp().toDate().getTime() : 0L;
                        long tb = b.getTimestamp() != null ? b.getTimestamp().toDate().getTime() : 0L;
                        return Long.compare(tb, ta);
                    });
                    callback.onSuccess(list);
                });
    }

    public void getReportById(String reportId, ReportCallback callback) {
        db.collection("reports").document(reportId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure("Report not found.");
                        return;
                    }
                    callback.onSuccess(UserReport.fromDocument(doc));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void submitReport(String reportedUserId, String reportedUserName, String reportedUserRole,
                             String reportType, String reportSummary, ActionCallback callback) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            callback.onFailure("Not logged in.");
            return;
        }

        resolveReporter(uid, new ReporterResolved() {
            @Override
            public void onResolved(String reporterName, String reporterRole) {
                Map<String, Object> report = new HashMap<>();
                report.put("reportedUserId", reportedUserId);
                report.put("reportedUserName", reportedUserName);
                report.put("reportedUserRole", reportedUserRole);
                report.put("reporterUserId", uid);
                report.put("reporterUserName", reporterName);
                report.put("reporterRole", reporterRole);
                report.put("reportType", reportType);
                report.put("reportSummary", reportSummary);
                report.put("timestamp", Timestamp.now());
                report.put("status", "pending");

                db.collection("reports").add(report)
                        .addOnSuccessListener(ref -> {
                            ref.update("reportId", ref.getId());
                            db.collection("admins").get().addOnSuccessListener(admins -> {
                                for (DocumentSnapshot admin : admins.getDocuments()) {
                                    Map<String, Object> notif = new HashMap<>();
                                    notif.put("recipientId", admin.getId());
                                    notif.put("targetRole", "admin");
                                    notif.put("title", "New Report Received");
                                    notif.put("message", reporterName + " reported " + reportedUserName
                                            + " for " + reportType);
                                    notif.put("timestamp", System.currentTimeMillis());
                                    notif.put("read", false);
                                    notif.put("isRead", false);
                                    notif.put("type", "NEW_REPORT");
                                    notif.put("relatedReportId", ref.getId());
                                    db.collection("notifications").add(notif);
                                }
                                callback.onSuccess();
                            }).addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                        })
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    public void markReportStatus(String reportId, String status, ActionCallback callback) {
        db.collection("reports").document(reportId)
                .update("status", status)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void removeReportedUser(UserReport report, ActionCallback callback) {
        if (report == null || report.getReportedUserId() == null) {
            callback.onFailure("Invalid report.");
            return;
        }
        String collection = "student".equalsIgnoreCase(report.getReportedUserRole())
                ? "students" : "counselors";
        db.collection(collection).document(report.getReportedUserId())
                .delete()
                .addOnSuccessListener(v -> markReportStatus(report.getReportId(), "resolved", callback))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private interface ReporterResolved {
        void onResolved(String reporterName, String reporterRole);
        void onFailure(String error);
    }

    private void resolveReporter(String uid, ReporterResolved cb) {
        db.collection("students").document(uid).get()
                .addOnSuccessListener(stu -> {
                    if (stu.exists()) {
                        cb.onResolved(valueOr(stu, "name", "Student"), "student");
                        return;
                    }
                    db.collection("counselors").document(uid).get()
                            .addOnSuccessListener(c -> {
                                if (c.exists()) {
                                    cb.onResolved(valueOr(c, "name", "Counsellor"), "counsellor");
                                    return;
                                }
                                db.collection("admins").document(uid).get()
                                        .addOnSuccessListener(a -> {
                                            if (a.exists()) {
                                                cb.onResolved(valueOr(a, "name", "Admin"), "admin");
                                            } else {
                                                cb.onFailure("Reporter profile not found.");
                                            }
                                        })
                                        .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
                            })
                            .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    private String valueOr(DocumentSnapshot doc, String key, String fallback) {
        String val = doc.getString(key);
        return val != null && !val.trim().isEmpty() ? val : fallback;
    }
}
