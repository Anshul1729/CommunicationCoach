package com.communicationcoach.util

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.communicationcoach.data.model.AudioChunk
import com.communicationcoach.data.model.AudioFeatures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.log10
import kotlin.math.sqrt

class AudioRecorder {
    companion object {
        private const val TAG = "AudioRecorder"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val CHUNK_DURATION_MS = 30000L

        private fun getMinBufferSize(): Int {
            return AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        }
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val bufferSize = getMinBufferSize() * 2

    fun startRecording(): Boolean {
        return try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                return false
            }

            audioRecord?.startRecording()
            isRecording = true
            Log.d(TAG, "Recording started")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            false
        }
    }

    suspend fun recordChunk(outputFile: File): Result<AudioChunk> = withContext(Dispatchers.IO) {
        if (!isRecording || audioRecord == null) {
            return@withContext Result.failure(IllegalStateException("Recording not started"))
        }

        try {
            val readBuffer = ByteArray(bufferSize)
            val allPcmData = ByteArrayOutputStream()

            val startTime = System.currentTimeMillis()
            while (isRecording && System.currentTimeMillis() - startTime < CHUNK_DURATION_MS) {
                val bytesRead = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: -1
                when {
                    bytesRead > 0 -> allPcmData.write(readBuffer, 0, bytesRead)
                    bytesRead == AudioRecord.ERROR_INVALID_OPERATION -> {
                        Log.e(TAG, "Invalid read operation")
                        break
                    }
                }
            }

            val pcmBytes = allPcmData.toByteArray()

            // Write WAV file
            convertPcmToWav(pcmBytes, outputFile)

            // Compute audio features from raw PCM
            val features = computeAudioFeatures(pcmBytes)

            Log.d(TAG, "Chunk recorded: ${outputFile.length()} bytes | volume: ${features.volumeDb} dB | pitch: ${features.pitchCategory}")
            Result.success(AudioChunk(outputFile, features))
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write audio file", e)
            Result.failure(e)
        }
    }

    private fun computeAudioFeatures(pcmData: ByteArray): AudioFeatures {
        if (pcmData.size < 4) {
            return AudioFeatures(0f, -96f, 0f, "low")
        }

        val numSamples = pcmData.size / 2
        var sumSquares = 0.0
        var zeroCrossings = 0
        var prevSample = 0

        for (i in pcmData.indices step 2) {
            if (i + 1 >= pcmData.size) break
            // PCM 16-bit little-endian to signed int
            val sample = (pcmData[i + 1].toInt() shl 8) or (pcmData[i].toInt() and 0xFF)
            sumSquares += sample.toDouble() * sample.toDouble()

            // Zero crossing: sign changed from previous sample
            if (prevSample != 0 && (sample >= 0) != (prevSample >= 0)) {
                zeroCrossings++
            }
            prevSample = sample
        }

        val rms = sqrt(sumSquares / numSamples).toFloat()
        val normalizedRms = (rms / 32768f).coerceIn(0f, 1f)
        val volumeDb = if (normalizedRms > 0) (20 * log10(normalizedRms.toDouble())).toFloat() else -96f

        // ZCR per sample — typical voiced speech at 16kHz:
        // low pitch (~100Hz): ZCR ~0.0125 | medium (~200Hz): ~0.025 | high (~300Hz): ~0.0375
        val zcr = zeroCrossings.toFloat() / numSamples
        val pitchCategory = when {
            zcr < 0.015f -> "low"
            zcr < 0.030f -> "medium"
            else -> "high"
        }

        return AudioFeatures(
            volumeRms = normalizedRms,
            volumeDb = volumeDb,
            zeroCrossingRate = zcr,
            pitchCategory = pitchCategory
        )
    }

    private fun convertPcmToWav(pcmData: ByteArray, wavFile: File) {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val bytesPerSample = 2
        val byteRate = SAMPLE_RATE * channels * bytesPerSample

        FileOutputStream(wavFile).use { fos ->
            val header = ByteArray(44)

            header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()

            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()

            header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()

            header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()

            header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
            header[20] = 1; header[21] = 0  // PCM format
            header[22] = channels.toByte(); header[23] = 0

            header[24] = (SAMPLE_RATE and 0xff).toByte()
            header[25] = ((SAMPLE_RATE shr 8) and 0xff).toByte()
            header[26] = ((SAMPLE_RATE shr 16) and 0xff).toByte()
            header[27] = ((SAMPLE_RATE shr 24) and 0xff).toByte()

            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()

            header[32] = (channels * bytesPerSample).toByte(); header[33] = 0
            header[34] = (bytesPerSample * 8).toByte(); header[35] = 0

            header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()

            header[40] = (totalAudioLen and 0xff).toByte()
            header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
            header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
            header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

            fos.write(header)
            fos.write(pcmData)
        }
    }

    fun stopRecording() {
        try {
            isRecording = false
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.d(TAG, "Recording stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
    }

    fun isRecording(): Boolean = isRecording
}
