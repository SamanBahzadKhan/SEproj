/**
 * Purpose: Handles AI assistant requests and response orchestration.
 * Depends on: Android ViewModel/repository layers and Supabase Edge endpoint.
 * Notes: Manages chat responses and safe fallback behavior.
 */

package com.fridge.caps.ai

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.fridge.caps.AppConfig
import com.fridge.caps.R
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiRepository(context: Context) {

    private val appContext = context.applicationContext

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mainHandler = Handler(Looper.getMainLooper())

    private val supabaseUrl: String
        get() = appContext.getString(R.string.supabase_url).trimEnd('/')

    private val supabaseAnonKey: String
        get() = appContext.getString(R.string.supabase_anon_key).trim()

    data class AiResponse(
        val answer: String,
        val counsellors: List<Counsellor>,
        val officeInfo: OfficeInfo?,
        val isCrisis: Boolean,
    )

    data class Counsellor(
        val counselorId: String,
        val name: String,
        val specialization: String,
        val rating: Double,
        val reviewCount: Int,
    )

    data class OfficeInfo(
        val office: String,
        val location: String,
        val contact: String,
    )

    private fun answerFromJsonOrNull(rawBody: String): String? {
        val trimmed = rawBody.trim()
        if (!trimmed.startsWith("{")) return null
        return try {
            val j = JSONObject(trimmed)
            val a = j.optString("answer", "").trim()
            if (a.isNotEmpty()) a else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseCounsellorsAndOffice(json: JSONObject): Pair<List<Counsellor>, OfficeInfo?> {
        val list = mutableListOf<Counsellor>()
        val counsellorsArray = json.optJSONArray("recommendedCounsellors")
        if (counsellorsArray != null) {
            for (i in 0 until counsellorsArray.length()) {
                try {
                    val c = counsellorsArray.getJSONObject(i)
                    list.add(
                        Counsellor(
                            counselorId = c.optString("counselorId", ""),
                            name = c.optString("name", ""),
                            specialization = c.optString("specialization", ""),
                            rating = c.optDouble("rating", 0.0),
                            reviewCount = c.optInt("reviewCount", 0),
                        ),
                    )
                } catch (e: Exception) {
                    Log.w("AI_DEBUG", "Skipping malformed counsellor entry", e)
                }
            }
        }
        val officeJson = json.optJSONObject("officeInfo")
        val office = if (officeJson != null) {
            try {
                OfficeInfo(
                    office = officeJson.optString("office", ""),
                    location = officeJson.optString("location", ""),
                    contact = officeJson.optString("contact", ""),
                )
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
        return Pair(list, office)
    }

    private fun fallbackResult(): Result<AiResponse> = Result.success(
        AiResponse(
            answer = FALLBACK_ERROR,
            counsellors = emptyList(),
            officeInfo = null,
            isCrisis = false,
        ),
    )

    
    fun getRecommendation(query: String, onResult: (Result<AiResponse>) -> Unit) {
        Log.d(
            "AI_DEBUG",
            "=== getRecommendation CALLED thread=${Thread.currentThread().name} query=$query ===",
        )
        val q = query.trim()
        if (q.isEmpty()) {
            onResult(Result.failure(IllegalArgumentException("empty query")))
            return
        }

        val functionUrl =
            "${supabaseUrl.trimEnd('/')}/${AppConfig.SUPABASE_RECOMMEND_COUNSELLOR_PATH}"
        Log.d("AI_DEBUG", "URL: $functionUrl")

        val request = try {
            val requestBodyJson = JSONObject().apply {
                put("query", q)
            }.toString()
            val body = requestBodyJson.toRequestBody(JSON_MEDIA)
            Request.Builder()
                .url(functionUrl)
                .post(body)
                .header("apikey", supabaseAnonKey)
                .header("Authorization", "Bearer $supabaseAnonKey")
                .build()
        } catch (e: Exception) {
            Log.e("AI_DEBUG", "Failed to build OkHttp Request", e)
            onResult(fallbackResult())
            return
        }

        Log.d("AI_DEBUG", "Starting background Thread for execute()")
        Thread({
            try {
                Log.d("AI_DEBUG", "execute() on ${Thread.currentThread().name}")
                client.newCall(request).execute().use { response ->
                    val rawBody = response.body?.string() ?: ""
                    val code = response.code
                    Log.d("AI_DEBUG", "HTTP $code; body len=${rawBody.length}")
                    Log.d(
                        "AI_DEBUG",
                        "Raw (truncated): ${rawBody.take(LOG_BODY_PREVIEW_CHARS)}",
                    )
                    val result = parseRecommendationResponse(code, rawBody)
                    mainHandler.post { onResult(result) }
                }
            } catch (e: Exception) {
                Log.e("AI_DEBUG", "OkHttp execute failed", e)
                mainHandler.post { onResult(fallbackResult()) }
            }
        }, "caps-ai-http").start()
    }

    private fun parseRecommendationResponse(code: Int, rawBody: String): Result<AiResponse> {
        try {
            if (!code.isSuccessfulHttp()) {
                Log.e("AI_DEBUG", "Non-2xx: $code ${rawBody.take(400)}")
                val parsed = try {
                    JSONObject(rawBody.trim())
                } catch (_: Exception) {
                    null
                }
                val answerText = parsed?.optString("answer", "")?.trim()?.takeIf { it.isNotEmpty() }
                    ?: answerFromJsonOrNull(rawBody)
                    ?: FALLBACK_ERROR
                val (counsellors, office) = if (parsed != null) {
                    parseCounsellorsAndOffice(parsed)
                } else {
                    Pair(emptyList(), null)
                }
                val crisis = parsed?.optBoolean("isCrisis", false) ?: false
                return Result.success(
                    AiResponse(
                        answer = answerText,
                        counsellors = counsellors,
                        officeInfo = office,
                        isCrisis = crisis,
                    ),
                )
            }

            if (rawBody.isBlank()) {
                Log.e("AI_DEBUG", "Empty response body")
                return fallbackResult()
            }

            val json = try {
                JSONObject(rawBody.trim())
            } catch (e: Exception) {
                Log.e("AI_DEBUG", "JSON parse failed: ${rawBody.take(400)}", e)
                val maybeAns = answerFromJsonOrNull(rawBody)
                return Result.success(
                    AiResponse(
                        answer = maybeAns ?: FALLBACK_ERROR,
                        counsellors = emptyList(),
                        officeInfo = null,
                        isCrisis = false,
                    ),
                )
            }

            val answer = json.optString("answer", "").trim().ifEmpty {
                FALLBACK_ERROR
            }
            val (counsellors, office) = parseCounsellorsAndOffice(json)

            return Result.success(
                AiResponse(
                    answer = answer,
                    counsellors = counsellors,
                    officeInfo = office,
                    isCrisis = json.optBoolean("isCrisis", false),
                ),
            )
        } catch (e: Exception) {
            Log.e("AI_DEBUG", "parseRecommendationResponse", e)
            return fallbackResult()
        }
    }

    private fun Int.isSuccessfulHttp(): Boolean = this in 200..299

    private companion object {
        private const val FALLBACK_ERROR =
            "Something went wrong. Please try again."
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        private const val LOG_BODY_PREVIEW_CHARS = 2000
    }
}
