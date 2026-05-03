package com.fridge.caps.network;



/**
 * Purpose: Handles external service request/response contracts.
 * Depends on: HTTP client abstractions and app meeting integration flow.
 * Notes: Encapsulates network payloads and API interfaces.
 */
/**
 * Purpose: Handles external service request/response contracts.
 * Depends on: HTTP client abstractions and app meeting integration flow.
 * Notes: Encapsulates network payloads and API interfaces.
 */
import com.google.gson.annotations.SerializedName;

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
