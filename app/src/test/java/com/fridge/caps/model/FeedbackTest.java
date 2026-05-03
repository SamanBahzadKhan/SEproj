package com.fridge.caps.model;


/**
 * Purpose: Handles automated verification for application behavior.
 * Depends on: JUnit/Android test frameworks and app classes under test.
 * Notes: Provides regression coverage for key workflows.
 */
import com.fridge.caps.models.Feedback;
import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FeedbackTest {

    private Feedback feedback;

    @Before
    public void setUp() {
        feedback = new Feedback();
        feedback.setTimeslotId("slot_001");
        feedback.setStudentId("student_001");
        feedback.setCounselorId("counselor_001");
        feedback.setRating(4);
        feedback.setComment("Very helpful session. I feel much better now.");
        feedback.setSubmittedAt(new Timestamp(System.currentTimeMillis() / 1000, 0));
    }

    /**
     * Verifies testFeedbackRatingBounds scenario.
     */
    @Test
    public void testFeedbackRatingBounds() {
        int rating = feedback.getRating();
        assertTrue(rating >= 1);
        assertTrue(rating <= 5);
    }

    /**
     * Verifies testFeedbackLinkedToSlot scenario.
     */
    @Test
    public void testFeedbackLinkedToSlot() {
        assertEquals("slot_001", feedback.getTimeslotId());
        assertEquals("counselor_001", feedback.getCounselorId());
        assertEquals("student_001", feedback.getStudentId());
    }

    /**
     * Verifies testFeedbackDefaultConstructor scenario.
     */
    @Test
    public void testFeedbackDefaultConstructor() {
        Feedback empty = new Feedback();
        assertEquals(0, empty.getRating());
        assertNull(empty.getComment());
    }
}
