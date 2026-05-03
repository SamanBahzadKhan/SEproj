package com.fridge.caps.model;


/**
 * Purpose: Handles automated verification for application behavior.
 * Depends on: JUnit/Android test frameworks and app classes under test.
 * Notes: Provides regression coverage for key workflows.
 */
import com.fridge.caps.models.Counselor;
import com.fridge.caps.models.UserRole;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CounselorTest {

    private Counselor counselor;

    @Before
    public void setUp() {
        counselor = new Counselor();
        counselor.setUserId("counselor_001");
        counselor.setName("Dr. Sara Khan");
        counselor.setEmail("sara.khan@lums.edu.pk");
        counselor.setSpecialization("Anxiety & Stress Management");
        counselor.setDepartment("Psychology");
        counselor.setPhone("+92 333 9876543");
        counselor.setBio("Experienced counsellor with 8 years in student mental health.");
        counselor.setRating(4.3f);
        counselor.setRatingCount(12);
        counselor.setAcceptingClients(true);
        counselor.setRole(UserRole.COUNSELOR);
    }

    /**
     * Verifies testCounselorFieldsSetCorrectly scenario.
     */
    @Test
    public void testCounselorFieldsSetCorrectly() {
        assertEquals("Dr. Sara Khan", counselor.getName());
        assertEquals("Anxiety & Stress Management", counselor.getSpecialization());
        assertTrue(counselor.isAcceptingClients());
        assertEquals(UserRole.COUNSELOR, counselor.getRole());
    }

    /**
     * Verifies testCounselorRatingBounds scenario.
     */
    @Test
    public void testCounselorRatingBounds() {
        double rating = counselor.getRating();
        assertTrue(rating >= 0.0);
        assertTrue(rating <= 5.0);
    }

    /**
     * Verifies testCounselorRatingCountNonNegative scenario.
     */
    @Test
    public void testCounselorRatingCountNonNegative() {
        assertTrue(counselor.getRatingCount() >= 0);
    }

    /**
     * Verifies testCounselorAcceptingClientsDefaultBehavior scenario.
     */
    @Test
    public void testCounselorAcceptingClientsDefaultBehavior() {
        Counselor c = new Counselor();
        assertTrue(c.isAcceptingClients());
    }
}
