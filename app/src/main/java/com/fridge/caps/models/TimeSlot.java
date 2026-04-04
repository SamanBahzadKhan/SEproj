package com.fridge.caps.models;

import com.google.firebase.Timestamp;

/**
 * TimeSlot.java
 * Represents a single bookable time slot for a counselor.
 * Maps to the Firestore "timeslots" collection.
 */
public class TimeSlot {

    private String    slotId;
    private String    counselorId;
    private Timestamp startTime;
    private Timestamp endTime;
    private boolean   isAvailable;

    public TimeSlot() {}

    public TimeSlot(String slotId, String counselorId,
                    Timestamp startTime, Timestamp endTime,
                    boolean isAvailable) {
        this.slotId      = slotId;
        this.counselorId = counselorId;
        this.startTime   = startTime;
        this.endTime     = endTime;
        this.isAvailable = isAvailable;
    }

    /** Marks this slot as booked. */
    public void reserve() { this.isAvailable = false; }

    /** Marks this slot as available again. */
    public void release() { this.isAvailable = true; }

    /**
     * Checks if this slot falls within a given time window.
     *
     * @param windowStart Start of the window.
     * @param windowEnd   End of the window.
     * @return true if slot is within the window.
     */
    public boolean isWithinWindow(Timestamp windowStart, Timestamp windowEnd) {
        return startTime.compareTo(windowStart) >= 0
                && endTime.compareTo(windowEnd) <= 0;
    }

    public String getSlotId()       { return slotId; }
    public String getCounselorId()  { return counselorId; }
    public Timestamp getStartTime() { return startTime; }
    public Timestamp getEndTime()   { return endTime; }
    public boolean isAvailable()    { return isAvailable; }

    public void setSlotId(String slotId)           { this.slotId = slotId; }
    public void setCounselorId(String counselorId) { this.counselorId = counselorId; }
    public void setStartTime(Timestamp startTime)  { this.startTime = startTime; }
    public void setEndTime(Timestamp endTime)      { this.endTime = endTime; }
    public void setAvailable(boolean available)    { this.isAvailable = available; }
}
