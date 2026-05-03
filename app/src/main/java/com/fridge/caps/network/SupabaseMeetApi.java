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
import com.fridge.caps.AppConfig;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SupabaseMeetApi {

    @POST(AppConfig.SUPABASE_CREATE_MEET_PATH)
    Call<MeetResponse> createMeet(@Body MeetRequest request);
}
