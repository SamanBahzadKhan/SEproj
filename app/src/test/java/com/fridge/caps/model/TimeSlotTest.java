package com.fridge.caps.model;


/**
 * Purpose: Handles automated verification for application behavior.
 * Depends on: JUnit/Android test frameworks and app classes under test.
 * Notes: Provides regression coverage for key workflows.
 */
import com.fridge.caps.models.TimeSlot;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TimeSlotTest {

    private TimeSlot slot;

    @Before
    public void setUp() {
        slot = new TimeSlot();
        slot.setId("slot_001");
        slot.setCounselorId("counselor_001");
        slot.setDate("2026-04-10");
        slot.setStartTime("10:00 AM");
        slot.setPeriod("Morning");
        slot.setAppointmentType("In-Person");
        slot.setBooked(false);
        slot.setStatus(null);
    }

    /**
     * Verifies testSlotInitiallyNotBooked scenario.
     */
    @Test
    public void testSlotInitiallyNotBooked() {
        assertFalse(slot.isBooked());
    }

    /**
     * Verifies testSlotBooking scenario.
     */
    @Test
    public void testSlotBooking() {
        slot.setBooked(true);
        slot.setStudentId("student_001");
        slot.setStudentName("Ahmad Raza");
        slot.setStatus("PENDING");
        assertTrue(slot.isBooked());
        assertEquals("PENDING", slot.getStatus());
        assertEquals("student_001", slot.getStudentId());
    }

    /**
     * Verifies testSlotCancellationKeepsStudentId scenario.
     */
    @Test
    public void testSlotCancellationKeepsStudentId() {
        slot.setBooked(true);
        slot.setStudentId("student_001");
        slot.setStatus("PENDING");
        slot.setBooked(false);
        slot.setStatus("CANCELLED");
        assertFalse(slot.isBooked());
        assertEquals("CANCELLED", slot.getStatus());
        assertEquals("student_001", slot.getStudentId());
    }

    /**
     * Verifies testFeedbackSubmittedDefaultFalse scenario.
     */
    @Test
    public void testFeedbackSubmittedDefaultFalse() {
        TimeSlot newSlot = new TimeSlot();
        assertFalse(newSlot.isFeedbackSubmitted());
    }
}
