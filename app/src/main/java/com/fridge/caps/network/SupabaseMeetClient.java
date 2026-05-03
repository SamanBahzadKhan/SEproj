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
import android.content.Context;

import com.fridge.caps.R;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class SupabaseMeetClient {

    private SupabaseMeetClient() {}

    
    static String normalizeSupabaseBaseUrl(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return s;
        }
        return s.endsWith("/") ? s : s + "/";
    }

    public static SupabaseMeetApi create(Context context) {
        Context app = context.getApplicationContext();
        String baseUrl = normalizeSupabaseBaseUrl(app.getString(R.string.supabase_url));
        String anon = app.getString(R.string.supabase_anon_key).trim();

        OkHttpClient http = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    okhttp3.Request req = chain.request().newBuilder()
                            .header("Authorization", "Bearer " + anon)
                            .header("apikey", anon)
                            .build();
                    return chain.proceed(req);
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(SupabaseMeetApi.class);
    }
}
