package com.fridge.caps.util;

import com.fridge.caps.utils.DateUtils;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DateUtilsTest {

    @Test
    public void testGetTodayStringFormat() {
        String today = DateUtils.getTodayString();
        assertNotNull(today);
        assertTrue(today.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testGetMorningSlotsCount() {
        assertEquals(8, DateUtils.getMorningSlots().size());
    }

    @Test
    public void testGetAfternoonSlotsCount() {
        assertEquals(8, DateUtils.getAfternoonSlots().size());
    }

    @Test
    public void testIsMorningTrueAndFalse() {
        assertTrue(DateUtils.isMorningSlot("8:00 AM"));
        assertFalse(DateUtils.isMorningSlot("4:00 PM"));
    }

    @Test
    public void testNext14DaysStartsToday() {
        List<String> dates = DateUtils.getNextFourteenDays();
        assertEquals(14, dates.size());
        assertEquals(DateUtils.getTodayString(), dates.get(0));
    }

    @Test
    public void isSlotStartInPast_trueForOldDate() {
        assertTrue(DateUtils.isSlotStartInPast("2000-01-01", "11:30 PM"));
    }

    @Test
    public void isSlotStartInPast_falseForFarFutureDate() {
        assertFalse(DateUtils.isSlotStartInPast("2099-12-31", "8:00 AM"));
    }

    @Test
    public void isSlotStartInPast_falseForNullInputs() {
        assertFalse(DateUtils.isSlotStartInPast(null, "10:00 AM"));
        assertFalse(DateUtils.isSlotStartInPast("2026-01-01", null));
        assertFalse(DateUtils.isSlotStartInPast("", "10:00 AM"));
    }
}
