package com.fridge.caps.network;

import com.google.gson.annotations.SerializedName;

/**
 * JSON returned by Supabase Edge Function: {@code { meetLink } }.
 */
public final class MeetResponse {

    @SerializedName("meetLink")
    public String meetLink;

    @SerializedName("error")
    public String error;
}
