package com.fridge.caps.util;


/**
 * Purpose: Handles automated verification for application behavior.
 * Depends on: JUnit/Android test frameworks and app classes under test.
 * Notes: Provides regression coverage for key workflows.
 */
import com.fridge.caps.utils.DateUtils;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DateUtilsTest {

    /**
     * Verifies testGetTodayStringFormat scenario.
     */
    @Test
    public void testGetTodayStringFormat() {
        String today = DateUtils.getTodayString();
        assertNotNull(today);
        assertTrue(today.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    /**
     * Verifies testGetMorningSlotsCount scenario.
     */
    @Test
    public void testGetMorningSlotsCount() {
        assertEquals(8, DateUtils.getMorningSlots().size());
    }

    /**
     * Verifies testGetAfternoonSlotsCount scenario.
     */
    @Test
    public void testGetAfternoonSlotsCount() {
        assertEquals(8, DateUtils.getAfternoonSlots().size());
    }

    /**
     * Verifies testIsMorningTrueAndFalse scenario.
     */
    @Test
    public void testIsMorningTrueAndFalse() {
        assertTrue(DateUtils.isMorningSlot("8:00 AM"));
        assertFalse(DateUtils.isMorningSlot("4:00 PM"));
    }

    /**
     * Verifies testNext14DaysStartsToday scenario.
     */
    @Test
    public void testNext14DaysStartsToday() {
        List<String> dates = DateUtils.getNextFourteenDays();
        assertEquals(14, dates.size());
        assertEquals(DateUtils.getTodayString(), dates.get(0));
    }

    /**
     * Verifies isSlotStartInPast_trueForOldDate scenario.
     */
    @Test
    public void isSlotStartInPast_trueForOldDate() {
        assertTrue(DateUtils.isSlotStartInPast("2000-01-01", "11:30 PM"));
    }

    /**
     * Verifies isSlotStartInPast_falseForFarFutureDate scenario.
     */
    @Test
    public void isSlotStartInPast_falseForFarFutureDate() {
        assertFalse(DateUtils.isSlotStartInPast("2099-12-31", "8:00 AM"));
    }

    /**
     * Verifies isSlotStartInPast_falseForNullInputs scenario.
     */
    @Test
    public void isSlotStartInPast_falseForNullInputs() {
        assertFalse(DateUtils.isSlotStartInPast(null, "10:00 AM"));
        assertFalse(DateUtils.isSlotStartInPast("2026-01-01", null));
        assertFalse(DateUtils.isSlotStartInPast("", "10:00 AM"));
    }
}
