package com.fridge.caps.controller;


/**
 * Purpose: Handles automated verification for application behavior.
 * Depends on: JUnit/Android test frameworks and app classes under test.
 * Notes: Provides regression coverage for key workflows.
 */
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ControllerClassSmokeTest {

    @Test
    public void testControllerClassesExist() throws Exception {
        assertNotNull(Class.forName("com.fridge.caps.controllers.AuthController"));
        assertNotNull(Class.forName("com.fridge.caps.controllers.CounselorController"));
        assertNotNull(Class.forName("com.fridge.caps.controllers.FeedbackController"));
        assertNotNull(Class.forName("com.fridge.caps.controllers.NotificationController"));
        assertNotNull(Class.forName("com.fridge.caps.controllers.AppointmentController"));
    }
}
