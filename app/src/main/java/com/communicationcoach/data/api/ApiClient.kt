package com.communicationcoach.data.api

import android.content.Context
import android.util.Base64
import android.util.Log
import com.communicationcoach.BuildConfig
import com.communicationcoach.data.model.GeminiContent
import com.communicationcoach.data.model.GeminiGenerationConfig
import com.communicationcoach.data.model.GeminiPart
import com.communicationcoach.data.model.GeminiRequest
import com.communicationcoach.data.model.GeminiResponse
import com.communicationcoach.data.model.SpeechAudio
import com.communicationcoach.data.model.SpeechConfig
import com.communicationcoach.data.model.SpeechRequest
import com.communicationcoach.data.model.SpeechResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class ApiClient(context: Context) {

    private val authHelper = GoogleAuthHelper(context)

    private val vertexUrl =
        "https://${BuildConfig.VERTEX_REGION}-aiplatform.googleapis.com/" +
        "v1/projects/${BuildConfig.GCP_PROJECT_ID}/locations/${BuildConfig.VERTEX_REGION}/" +
        "publishers/google/models/${BuildConfig.GEMINI_MODEL}:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val speechService = Retrofit.Builder()
        .baseUrl("https://speech.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SpeechToTextApiService::class.java)

    private val vertexService = Retrofit.Builder()
        .baseUrl("https://us-central1-aiplatform.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(VertexAiApiService::class.java)

    // ── Transcription ─────────────────────────────────────────────────────────

    suspend fun transcribeAudio(file: File): Response<SpeechResponse> {
        val token = authHelper.getAccessToken()

        // Strip 44-byte WAV header → raw LINEAR16 PCM
        val pcmBytes = file.readBytes().drop(44).toByteArray()
        val base64Audio = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)

        val request = SpeechRequest(
            config = SpeechConfig(
                encoding = "LINEAR16",
                sampleRateHertz = 16000,
                languageCode = "en-IN",
                alternativeLanguageCodes = listOf("hi-IN"),
                model = "latest_long",
                enableAutomaticPunctuation = true
            ),
            audio = SpeechAudio(content = base64Audio)
        )

        return speechService.recognize("Bearer $token", request)
    }

    // ── Conversation analysis ─────────────────────────────────────────────────

    suspend fun analyzeConversation(
        fullTranscript: String,
        userProfileJson: String
    ): Response<GeminiResponse> {
        val token = authHelper.getAccessToken()
        val prompt = buildConversationPrompt(fullTranscript, userProfileJson)
        return vertexService.generate(
            vertexUrl,
            "Bearer $token",
            GeminiRequest(
                contents = listOf(GeminiContent("user", listOf(GeminiPart(prompt)))),
                generationConfig = GeminiGenerationConfig(maxOutputTokens = 4096)
            )
        )
    }

    // ── Daily digest ──────────────────────────────────────────────────────────

    suspend fun generateDailyDigest(
        insightSummaries: String,
        userProfileJson: String
    ): Response<GeminiResponse> {
        val token = authHelper.getAccessToken()
        val prompt = buildDigestPrompt(insightSummaries, userProfileJson)
        return vertexService.generate(
            vertexUrl,
            "Bearer $token",
            GeminiRequest(
                contents = listOf(GeminiContent("user", listOf(GeminiPart(prompt)))),
                generationConfig = GeminiGenerationConfig(maxOutputTokens = 1024)
            )
        )
    }

    // ── Prompts ───────────────────────────────────────────────────────────────

    private fun buildConversationPrompt(transcript: String, profileJson: String): String = """
        You are a personal communication coach with memory of past sessions.
        The user is based in India and may speak in English, Hindi, or a mix of both (Hinglish).
        Analyze the full conversation regardless of language. For grammar/vocabulary, focus only on English portions.
        For tone, clarity, and confidence — analyze across all languages.

        USER PROFILE (learned from previous conversations):
        $profileJson

        FULL CONVERSATION TRANSCRIPT:
        "$transcript"

        Analyze this conversation for the issues this user is specifically working on:
        1. Defensive communication — deflecting blame, justifying instead of listening, dismissive phrases (in any language)
        2. Emotional tone / short temper — agitation, irritation, harsh or dismissive language (in any language)
        3. Grammar and vocabulary — wrong English word choices, repeated mistakes, unclear English phrasing (skip Hindi-only parts)
        4. Clarity and structure — are ideas expressed clearly and logically, regardless of language?
        5. Confidence signals — filler words (um, like, you know, matlab, bas), hedging, lack of assertiveness

        Write all tips and notes in English, even if the transcript is in Hindi or Hinglish.

        Respond ONLY in this exact JSON format, no other text:
        {
            "tips": ["actionable tip 1", "actionable tip 2", "actionable tip 3"],
            "issues": {
                "defensive": {"detected": true, "note": "brief example from transcript"},
                "emotionalTone": {"detected": false, "note": ""},
                "grammar": {"detected": true, "examples": ["mistake → correction"]},
                "clarity": {"score": 7, "note": "feedback on structure"},
                "confidence": {"detected": false, "note": ""}
            },
            "summary": "2 sentence overall coaching feedback",
            "profileUpdates": {
                "recurringPatterns": ["pattern seen in this conversation"],
                "improvements": ["something better than before"],
                "focusAreas": ["top 1-2 areas to keep working on"]
            }
        }
    """.trimIndent()

    private fun buildDigestPrompt(insightSummaries: String, profileJson: String): String = """
        You are a communication coach reviewing today's conversation sessions.
        The user is based in India and speaks English, Hindi, and Hinglish. Write all feedback in English.

        USER PROFILE (learned over time):
        $profileJson

        TODAY'S CONVERSATION SUMMARIES:
        $insightSummaries

        Generate a concise end-of-day digest. Respond ONLY in this exact JSON format:
        {
            "summary": "2-3 sentence overview of today's communication patterns",
            "topPatterns": ["pattern1", "pattern2"],
            "progressNote": "1 sentence comparing today vs. known past patterns (improvement or regression)",
            "tomorrowTip": "one specific, actionable focus for tomorrow"
        }
    """.trimIndent()

    companion object {
        private const val TAG = "ApiClient"

        // Extract text from Gemini response
        fun extractText(response: GeminiResponse): String? =
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

        // Strip markdown code fences Gemini sometimes wraps JSON in (```json ... ```)
        fun cleanJson(raw: String): String {
            val trimmed = raw.trim()
            return when {
                trimmed.startsWith("```json") -> trimmed.removePrefix("```json").trimStart('\n').removeSuffix("```").trimEnd()
                trimmed.startsWith("```") -> trimmed.removePrefix("```").trimStart('\n').removeSuffix("```").trimEnd()
                else -> trimmed
            }
        }
    }
}
