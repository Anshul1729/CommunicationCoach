package com.communicationcoach.util

import android.util.Log

/**
 * Tracks silence across audio chunks to detect when a conversation has ended.
 *
 * After each 30-second chunk, call [onChunkRms] with the normalized RMS volume (0.0–1.0).
 * If RMS stays below [silenceThresholdRms] for [silenceDurationMs] cumulative milliseconds,
 * [onConversationEnded] fires exactly once. Speech resuming resets the timer.
 *
 * With 30s chunks:
 *   - 90s silence = 3 consecutive silent chunks → conversation ended
 */
class SilenceDetector(
    private val silenceThresholdRms: Float = 0.015f,
    private val silenceDurationMs: Long = 90_000L,
    private val onConversationEnded: () -> Unit
) {
    companion object {
        private const val TAG = "SilenceDetector"
    }

    // Timestamp when the current silence window started; null means we're in speech
    private var silenceStartMs: Long? = null

    // Prevent firing the callback multiple times for the same silence window
    private var triggered = false

    /**
     * Call after each audio chunk with the chunk's normalized RMS (from AudioFeatures.volumeRms).
     * [chunkDurationMs] is the actual duration of that chunk (default 30 000 ms).
     */
    fun onChunkRms(rms: Float, chunkDurationMs: Long = 30_000L) {
        val now = System.currentTimeMillis()

        if (rms < silenceThresholdRms) {
            // Silent chunk
            if (silenceStartMs == null) {
                silenceStartMs = now - chunkDurationMs  // count the chunk itself as silence
                Log.d(TAG, "Silence started (rms=${"%.4f".format(rms)})")
            }

            val silentForMs = now - (silenceStartMs ?: now)
            Log.d(TAG, "Silence ongoing: ${silentForMs / 1000}s / ${silenceDurationMs / 1000}s threshold")

            if (!triggered && silentForMs >= silenceDurationMs) {
                triggered = true
                Log.d(TAG, "Conversation ended after ${silentForMs / 1000}s of silence")
                onConversationEnded()
            }
        } else {
            // Speech detected — reset silence window
            if (silenceStartMs != null) {
                Log.d(TAG, "Speech resumed, resetting silence timer (rms=${"%.4f".format(rms)})")
            }
            silenceStartMs = null
            triggered = false
        }
    }

    /** Call when starting a brand-new recording session or after a conversation boundary. */
    fun reset() {
        silenceStartMs = null
        triggered = false
        Log.d(TAG, "SilenceDetector reset")
    }
}
