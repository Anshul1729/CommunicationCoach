package com.communicationcoach.util

import android.content.Context

class CostTracker(context: Context) {

    private val prefs = context.getSharedPreferences("cost_tracker", Context.MODE_PRIVATE)

    companion object {
        // ── Spend limit ───────────────────────────────────────────────────────
        // Set to Double.MAX_VALUE for owner build (no limit).
        // Set to 2.0 for friend/guest build ($2 cap).
        const val SPEND_LIMIT_USD = Double.MAX_VALUE

        // Google Cloud Speech-to-Text: latest_long model — $0.006 per 15-second block
        private const val STT_COST_PER_15S = 0.006

        // Vertex AI Gemini 2.5 Flash
        private const val GEMINI_INPUT_PER_1M  = 0.075   // $0.075 per 1M input tokens
        private const val GEMINI_OUTPUT_PER_1M = 0.30    // $0.30 per 1M output tokens

        private const val KEY_STT_USD          = "stt_usd"
        private const val KEY_GEMINI_USD       = "gemini_usd"
        private const val KEY_GEMINI_IN_TOKENS = "gemini_in_tokens"
        private const val KEY_GEMINI_OUT_TOKENS= "gemini_out_tokens"
    }

    /** Call once per analyzed conversation. durationSeconds = length of that conversation. */
    fun addSpeechCost(durationSeconds: Int) {
        val blocks = Math.ceil(durationSeconds / 15.0)
        val cost = blocks * STT_COST_PER_15S
        val prev = prefs.getFloat(KEY_STT_USD, 0f)
        prefs.edit().putFloat(KEY_STT_USD, (prev + cost).toFloat()).apply()
    }

    /** Call with token counts from Gemini usageMetadata after each successful API call. */
    fun addGeminiCost(promptTokens: Int, outputTokens: Int) {
        val cost = (promptTokens  / 1_000_000.0 * GEMINI_INPUT_PER_1M) +
                   (outputTokens / 1_000_000.0 * GEMINI_OUTPUT_PER_1M)

        val prevUsd  = prefs.getFloat(KEY_GEMINI_USD, 0f)
        val prevIn   = prefs.getLong(KEY_GEMINI_IN_TOKENS, 0L)
        val prevOut  = prefs.getLong(KEY_GEMINI_OUT_TOKENS, 0L)

        prefs.edit()
            .putFloat(KEY_GEMINI_USD, (prevUsd + cost).toFloat())
            .putLong(KEY_GEMINI_IN_TOKENS,  prevIn  + promptTokens)
            .putLong(KEY_GEMINI_OUT_TOKENS, prevOut + outputTokens)
            .apply()
    }

    fun getSttUsd(): Double    = prefs.getFloat(KEY_STT_USD, 0f).toDouble()
    fun getGeminiUsd(): Double = prefs.getFloat(KEY_GEMINI_USD, 0f).toDouble()
    fun getTotalUsd(): Double  = getSttUsd() + getGeminiUsd()

    fun getGeminiInputTokens(): Long  = prefs.getLong(KEY_GEMINI_IN_TOKENS, 0L)
    fun getGeminiOutputTokens(): Long = prefs.getLong(KEY_GEMINI_OUT_TOKENS, 0L)
}
