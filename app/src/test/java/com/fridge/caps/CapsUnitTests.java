package com.fridge.caps;

import com.fridge.caps.models.Counselor;
import com.fridge.caps.models.Student;
import com.fridge.caps.models.TimeSlot;
import com.fridge.caps.models.UserRole;
import com.google.firebase.Timestamp;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Date;

/**
 * CapsUnitTests.java
 * Unit tests for Student, Counselor and TimeSlot models.
 * Covers US-1 (account creation), US-4 (counselor profiles), US-5 (time slots).
 * Runs locally on the development machine.
 *
 * Outstanding issues: Firestore integration tests require instrumented tests.
 */
public class CapsUnitTests {

    // US-1: Student Account Creation

    @Test
    public void testStudentCreation_fieldsSetCorrectly() {
        Student student = new Student(
                "uid123", "Ahmad Raza", "ahmad@lums.edu.pk",
                "+92 300 0000000", "Computer Science",
                "3rd Year", "2026-03-27T10:00:00"
        );
        assertEquals("uid123",           student.getUserId());
        assertEquals("Ahmad Raza",       student.getName());
        assertEquals("ahmad@lums.edu.pk",student.getEmail());
        assertEquals("Computer Science", student.getDepartment());
        assertEquals("3rd Year",         student.getYearOfStudy());
        assertEquals(UserRole.STUDENT,   student.getRole());
    }

    @Test
    public void testStudentRole_isAlwaysStudent() {
        Student student = new Student(
                "uid456", "Sara Ali", "sara@lums.edu.pk",
                "", "EE", "2nd Year", "2026-03-27T10:00:00"
        );
        assertEquals(UserRole.STUDENT, student.getRole());
    }

    @Test
    public void testStudentSetters_updateFieldsCorrectly() {
        Student student = new Student();
        student.setName("Bilal Khan");
        student.setEmail("bilal@lums.edu.pk");
        student.setDepartment("Physics");

        assertEquals("Bilal Khan",        student.getName());
        assertEquals("bilal@lums.edu.pk", student.getEmail());
        assertEquals("Physics",           student.getDepartment());
    }

    // US-4: View Counselor Profiles

    @Test
    public void testCounselorCreation_fieldsSetCorrectly() {
        Counselor counselor = new Counselor(
                "c001", "Dr. Sara Khan", "sara@caps.pk",
                "Anxiety & Stress", "Experienced therapist.",
                "https://photo.url", 4.5f, true, "2026-01-01T00:00:00"
        );
        assertEquals("c001",                   counselor.getUserId());
        assertEquals("Dr. Sara Khan",          counselor.getName());
        assertEquals("Anxiety & Stress",       counselor.getSpecialization());
        assertEquals(4.5f,                     counselor.getRating(), 0.01f);
        assertTrue(counselor.isAcceptingClients());
        assertEquals(UserRole.COUNSELOR,       counselor.getRole());
    }

    @Test
    public void testCounselor_notAcceptingClients() {
        Counselor counselor = new Counselor(
                "c002", "Dr. Ali", "ali@caps.pk",
                "Depression", "Senior counselor.",
                "", 4.0f, false, "2026-01-01T00:00:00"
        );
        assertFalse(counselor.isAcceptingClients());
    }

    @Test
    public void testCounselorRating_storedCorrectly() {
        Counselor counselor = new Counselor(
                "c003", "Dr. Zara", "zara@caps.pk",
                "Grief", "Bio.", "", 3.7f, true, "2026-01-01T00:00:00"
        );
        assertEquals(3.7f, counselor.getRating(), 0.01f);
    }

    // US-5: View Available Time Slots

    @Test
    public void testTimeSlot_isAvailableOnCreation() {
        Timestamp start = new Timestamp(new Date());
        Timestamp end   = new Timestamp(new Date(System.currentTimeMillis() + 3600000));
        TimeSlot slot   = new TimeSlot("slot001", "c001", start, end, true);
        assertTrue(slot.isAvailable());
    }

    @Test
    public void testTimeSlot_reserve_marksUnavailable() {
        Timestamp start = new Timestamp(new Date());
        Timestamp end   = new Timestamp(new Date(System.currentTimeMillis() + 3600000));
        TimeSlot slot   = new TimeSlot("slot002", "c001", start, end, true);
        slot.reserve();
        assertFalse(slot.isAvailable());
    }

    @Test
    public void testTimeSlot_release_marksAvailable() {
        Timestamp start = new Timestamp(new Date());
        Timestamp end   = new Timestamp(new Date(System.currentTimeMillis() + 3600000));
        TimeSlot slot   = new TimeSlot("slot003", "c001", start, end, false);
        slot.release();
        assertTrue(slot.isAvailable());
    }

    @Test
    public void testTimeSlot_isWithinWindow_returnsTrue() {
        long now           = System.currentTimeMillis();
        Timestamp start    = new Timestamp(new Date(now + 1000));
        Timestamp end      = new Timestamp(new Date(now + 3600000));
        Timestamp winStart = new Timestamp(new Date(now));
        Timestamp winEnd   = new Timestamp(new Date(now + 7200000));
        TimeSlot slot      = new TimeSlot("slot004", "c001", start, end, true);
        assertTrue(slot.isWithinWindow(winStart, winEnd));
    }

    @Test
    public void testTimeSlot_isWithinWindow_returnsFalse() {
        long now           = System.currentTimeMillis();
        Timestamp start    = new Timestamp(new Date(now + 10000000));
        Timestamp end      = new Timestamp(new Date(now + 13600000));
        Timestamp winStart = new Timestamp(new Date(now));
        Timestamp winEnd   = new Timestamp(new Date(now + 7200000));
        TimeSlot slot      = new TimeSlot("slot005", "c001", start, end, true);
        assertFalse(slot.isWithinWindow(winStart, winEnd));
    }

    @Test
    public void testTimeSlot_counselorId_storedCorrectly() {
        Timestamp start = new Timestamp(new Date());
        Timestamp end   = new Timestamp(new Date(System.currentTimeMillis() + 3600000));
        TimeSlot slot   = new TimeSlot("slot006", "counselor_abc", start, end, true);
        assertEquals("counselor_abc", slot.getCounselorId());
    }
}
