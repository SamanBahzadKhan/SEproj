package com.fridge.caps.controller;


/**
 * Purpose: Handles automated verification for application behavior.
 * Depends on: JUnit/Android test frameworks and app classes under test.
 * Notes: Provides regression coverage for key workflows.
 */
import com.fridge.caps.controllers.AppointmentController;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AppointmentControllerTest {

    /**
     * Verifies testSlotDocumentIdStableFormat scenario.
     */
    @Test
    public void testSlotDocumentIdStableFormat() {
        String id = AppointmentController.slotDocumentId("c1", "2026-04-10", "10:00 AM");
        assertTrue(id.startsWith("c1_2026-04-10_"));
        assertEquals("c1_2026-04-10_1000AM", id);
    }
}
