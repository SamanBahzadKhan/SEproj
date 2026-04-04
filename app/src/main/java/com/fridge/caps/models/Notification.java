package com.fridge.caps.models;

import com.google.firebase.Timestamp;

/**
 * Notification.java
 * Represents a system notification sent to a student.
 * Maps to the Firestore "notifications" collection.
 */
public class Notification {

    private String           notificationId;
    private String           recipientId;
    private String           title;
    private String           message;
    private NotificationType type;
    private Timestamp        sentAt;
    private boolean          isRead;

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
        this.isRead         = false;
    }

    /** Marks this notification as read. */
    public void markAsRead() { this.isRead = true; }

    public String getNotificationId()   { return notificationId; }
    public String getRecipientId()      { return recipientId; }
    public String getTitle()            { return title; }
    public String getMessage()          { return message; }
    public NotificationType getType()   { return type; }
    public Timestamp getSentAt()        { return sentAt; }
    public boolean isRead()             { return isRead; }

    public void setNotificationId(String id)        { this.notificationId = id; }
    public void setRecipientId(String id)           { this.recipientId = id; }
    public void setTitle(String title)              { this.title = title; }
    public void setMessage(String message)          { this.message = message; }
    public void setType(NotificationType type)      { this.type = type; }
    public void setSentAt(Timestamp sentAt)         { this.sentAt = sentAt; }
    public void setRead(boolean read)               { this.isRead = read; }
}