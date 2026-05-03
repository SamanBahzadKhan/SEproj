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

public final class MeetResponse {

    @SerializedName("meetLink")
    public String meetLink;

    @SerializedName("error")
    public String error;
}
