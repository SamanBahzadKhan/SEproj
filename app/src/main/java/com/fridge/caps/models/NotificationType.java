package com.fridge.caps.models;

/**
 * Legacy notification categories; newer docs may use {@code typeKey} string instead.
 */
public enum NotificationType {
    CONFIRMATION,
    REMINDER,
    CANCELLATION,
    RESCHEDULE,
    COMPLETED,
    PENDING,
    FEEDBACK,
    OTHER
}
