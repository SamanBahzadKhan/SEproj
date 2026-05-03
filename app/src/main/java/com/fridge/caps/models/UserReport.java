package com.fridge.caps.models;



/**
 * Purpose: Defines core domain data structures and status values.
 * Depends on: Firebase timestamp types and Java/Kotlin data accessors.
 * Notes: Used as transfer objects between controllers and screens.
 */
/**
 * Purpose: Defines core domain data structures and status values.
 * Depends on: Firebase timestamp types and Java/Kotlin data accessors.
 * Notes: Used as transfer objects between controllers and screens.
 */
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

public class UserReport {
    private String reportId;
    private String reportedUserId;
    private String reportedUserName;
    private String reportedUserRole;
    private String reporterUserId;
    private String reporterUserName;
    private String reporterRole;
    private String reportType;
    private String reportSummary;
    private Timestamp timestamp;
    private String status;

    public UserReport() {}

    public static UserReport fromDocument(DocumentSnapshot doc) {
        UserReport r = new UserReport();
        r.reportId = doc.getId();
        r.reportedUserId = doc.getString("reportedUserId");
        r.reportedUserName = doc.getString("reportedUserName");
        r.reportedUserRole = doc.getString("reportedUserRole");
        r.reporterUserId = doc.getString("reporterUserId");
        r.reporterUserName = doc.getString("reporterUserName");
        r.reporterRole = doc.getString("reporterRole");
        r.reportType = doc.getString("reportType");
        r.reportSummary = doc.getString("reportSummary");
        r.timestamp = doc.getTimestamp("timestamp");
        r.status = doc.getString("status");
        return r;
    }

    public String getReportId() { return reportId; }
    public String getReportedUserId() { return reportedUserId; }
    public String getReportedUserName() { return reportedUserName; }
    public String getReportedUserRole() { return reportedUserRole; }
    public String getReporterUserId() { return reporterUserId; }
    public String getReporterUserName() { return reporterUserName; }
    public String getReporterRole() { return reporterRole; }
    public String getReportType() { return reportType; }
    public String getReportSummary() { return reportSummary; }
    public Timestamp getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
}
