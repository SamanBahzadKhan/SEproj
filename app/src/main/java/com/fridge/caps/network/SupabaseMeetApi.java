package com.fridge.caps.network;

import com.fridge.caps.AppConfig;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SupabaseMeetApi {

    @POST(AppConfig.SUPABASE_CREATE_MEET_PATH)
    Call<MeetResponse> createMeet(@Body MeetRequest request);
}
