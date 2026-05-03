package com.fridge.caps.network;

import com.google.gson.annotations.SerializedName;

/**
 * Body for Supabase Edge Function that creates a Calendar event and Meet link.
 */
public final class MeetRequest {

    @SerializedName("studentEmail")
    public final String studentEmail;

    @SerializedName("counselorEmail")
    public final String counselorEmail;

    @SerializedName("start")
    public final String start;

    @SerializedName("end")
    public final String end;

    public MeetRequest(String studentEmail, String counselorEmail, String start, String end) {
        this.studentEmail = studentEmail;
        this.counselorEmail = counselorEmail;
        this.start = start;
        this.end = end;
    }
}
