package com.communicationcoach.data.model

import com.google.gson.annotations.SerializedName
import java.io.File

// ── Cloudflare Worker requests ────────────────────────────────────────────────

data class WorkerTranscribeRequest(val audio: String)
data class WorkerGeminiRequest(val prompt: String, val maxTokens: Int)

// ── Speech transcription response (SpeechResponse) ───────────────────────────

data class SpeechRequest(
    val config: SpeechConfig,
    val audio: SpeechAudio
)

data class SpeechConfig(
    val encoding: String = "LINEAR16",
    val sampleRateHertz: Int = 16000,
    val languageCode: String = "en-IN",
    val alternativeLanguageCodes: List<String> = listOf("hi-IN"),
    val model: String = "latest_long",
    val enableAutomaticPunctuation: Boolean = true
)

data class SpeechAudio(
    val content: String  // base64-encoded raw PCM (no WAV header)
)

data class SpeechResponse(
    val results: List<SpeechResult>?
)

data class SpeechResult(
    val alternatives: List<SpeechAlternative>?
)

data class SpeechAlternative(
    val transcript: String = "",
    val confidence: Float = 0f
)

// ── Groq LLM response (shaped like Gemini for compatibility) ─────────────────

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
)

data class GeminiPart(val text: String)

data class GeminiThinkingConfig(
    @SerializedName("thinkingBudget") val thinkingBudget: Int = 0
)

data class GeminiGenerationConfig(
    @SerializedName("maxOutputTokens") val maxOutputTokens: Int,
    @SerializedName("thinkingConfig") val thinkingConfig: GeminiThinkingConfig = GeminiThinkingConfig()
)

data class GeminiUsageMetadata(
    @SerializedName("promptTokenCount") val promptTokenCount: Int = 0,
    @SerializedName("candidatesTokenCount") val candidatesTokenCount: Int = 0
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?,
    val usageMetadata: GeminiUsageMetadata? = null
)

data class GeminiCandidate(
    val content: GeminiContent?
)

// ── Audio (unchanged) ─────────────────────────────────────────────────────────

data class AudioFeatures(
    val volumeRms: Float,
    val volumeDb: Float,
    val zeroCrossingRate: Float,
    val pitchCategory: String
)

data class AudioChunk(
    val file: File,
    val features: AudioFeatures
)

// Legacy — kept for BehaviorAnalyzer compatibility
data class BehaviorAnalysis(
    val isHarshTone: Boolean = false,
    val isTalkingFast: Boolean = false,
    val wordsPerMinute: Int = 0,
    val isMonologuing: Boolean = false,
    val continuousSpeechSeconds: Int = 0,
    val summary: String = "",
    val rawAnalysis: String = "",
    val volumeDb: Float = 0f,
    val pitchCategory: String = ""
)
