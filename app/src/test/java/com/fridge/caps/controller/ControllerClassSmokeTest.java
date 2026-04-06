package com.fridge.caps.controller;

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
