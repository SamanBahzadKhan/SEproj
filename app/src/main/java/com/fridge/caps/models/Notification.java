package com.fridge.caps.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Notification.java
 * Represents a system notification for appointment confirmations, reminders, and status updates.
 * Maps to Firestore "notifications" collection with read status and timestamp tracking.
 * Supports multiple notification types (reminder, confirmation, cancellation, etc).
 */
public class Notification {

    private String           notificationId;
    private String           recipientId;
    private String           title;
    private String           message;
    private NotificationType type;
    /** Firestore string type for theming (CONFIRMATION, NEW_BOOKING, …). */
    private String           typeKey;
    private Timestamp        sentAt;
    private long               timestampMillis;
    private boolean          read;
    private String relatedReportId;
    private String relatedSignupId;

    public Notification() {}

    public Notification(String notificationId, String recipientId,
                        String title, String message,
                        NotificationType type, Timestamp sentAt) {
        this.notificationId = notificationId;
        this.recipientId    = recipientId;
        this.title          = title;
        this.message        = message;
        this.type           = type;
        this.sentAt         = sentAt;
        this.read           = false;
    }

    public static Notification fromDocument(DocumentSnapshot doc) {
        Notification n = new Notification();
        n.setNotificationId(doc.getId());
        n.setRecipientId(doc.getString("recipientId"));
        n.setTitle(doc.getString("title"));
        n.setMessage(doc.getString("message"));
        Long ts = doc.getLong("timestamp");
        if (ts != null) {
            n.setTimestampMillis(ts);
            n.setSentAt(new Timestamp(ts / 1000, (int) ((ts % 1000) * 1_000_000)));
        } else if (doc.getTimestamp("sentAt") != null) {
            n.setSentAt(doc.getTimestamp("sentAt"));
            n.setTimestampMillis(doc.getTimestamp("sentAt").toDate().getTime());
        }
        Boolean r = doc.getBoolean("read");
        if (r == null) {
            r = doc.getBoolean("isRead");
        }
        n.setRead(r != null && r);
        String tk = doc.getString("type");
        n.setTypeKey(tk);
        n.setType(mapTypeKey(tk));
        n.setRelatedReportId(doc.getString("relatedReportId"));
        n.setRelatedSignupId(doc.getString("relatedSignupId"));
        return n;
    }

    private static NotificationType mapTypeKey(String tk) {
        if (tk == null) return NotificationType.CONFIRMATION;
        switch (tk) {
            case "REMINDER":
                return NotificationType.REMINDER;
            case "CONFIRMATION":
            case "NEW_BOOKING":
                return NotificationType.CONFIRMATION;
            case "CANCELLED":
            case "CANCELLATION":
                return NotificationType.CANCELLATION;
            case "RESCHEDULE":
                return NotificationType.RESCHEDULE;
            case "COMPLETED":
                return NotificationType.COMPLETED;
            case "PENDING":
                return NotificationType.PENDING;
            default:
                return NotificationType.CONFIRMATION;
        }
    }

    public long getTimestampMillis() {
        if (timestampMillis > 0) return timestampMillis;
        if (sentAt != null) return sentAt.toDate().getTime();
        return 0L;
    }

    public String getTypeKey() {
        return typeKey != null ? typeKey : "";
    }

    public void markAsRead() { this.read = true; }

    public String getNotificationId()   { return notificationId; }
    public String getRecipientId()      { return recipientId; }
    public String getTitle()            { return title; }
    public String getMessage()          { return message; }
    public NotificationType getType()   { return type; }
    public Timestamp getSentAt()        { return sentAt; }
    public boolean isRead()             { return read; }
    public String getRelatedReportId()  { return relatedReportId; }
    public String getRelatedSignupId()  { return relatedSignupId; }

    public void setNotificationId(String id)        { this.notificationId = id; }
    public void setRecipientId(String id)           { this.recipientId = id; }
    public void setTitle(String title)              { this.title = title; }
    public void setMessage(String message)          { this.message = message; }
    public void setType(NotificationType type)      { this.type = type; }
    public void setTypeKey(String typeKey)          { this.typeKey = typeKey; }
    public void setSentAt(Timestamp sentAt)         { this.sentAt = sentAt; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }
    public void setRead(boolean read)               { this.read = read; }
    public void setRelatedReportId(String relatedReportId) { this.relatedReportId = relatedReportId; }
    public void setRelatedSignupId(String relatedSignupId) { this.relatedSignupId = relatedSignupId; }
}