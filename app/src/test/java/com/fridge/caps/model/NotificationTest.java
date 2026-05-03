package com.fridge.caps.model;


/**
 * Purpose: Handles automated verification for application behavior.
 * Depends on: JUnit/Android test frameworks and app classes under test.
 * Notes: Provides regression coverage for key workflows.
 */
import com.fridge.caps.models.Notification;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class NotificationTest {

    private Notification notif;

    @Before
    public void setUp() {
        notif = new Notification();
        notif.setRecipientId("student_001");
        notif.setTitle("Appointment Confirmed");
        notif.setMessage("Your appointment is confirmed.");
        notif.setTypeKey("CONFIRMATION");
        notif.setTimestampMillis(System.currentTimeMillis());
        notif.setRead(false);
    }

    /**
     * Verifies testNotificationFieldsSet scenario.
     */
    @Test
    public void testNotificationFieldsSet() {
        assertEquals("student_001", notif.getRecipientId());
        assertEquals("Appointment Confirmed", notif.getTitle());
        assertEquals("CONFIRMATION", notif.getTypeKey());
        assertFalse(notif.isRead());
    }

    /**
     * Verifies testNotificationMarkRead scenario.
     */
    @Test
    public void testNotificationMarkRead() {
        assertFalse(notif.isRead());
        notif.setRead(true);
        assertTrue(notif.isRead());
    }
}
