package com.fridge.caps.network;

import android.content.Context;

import com.fridge.caps.R;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit client for Supabase Edge Functions (Meet link). Uses anon key + apikey headers.
 * Base URL is normalized to end with {@code /} so Retrofit resolves {@code functions/v1/...} correctly.
 */
public final class SupabaseMeetClient {

    private SupabaseMeetClient() {}

    /**
     * Retrofit requires the base URL to end with {@code /}; missing slash breaks path resolution.
     */
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
