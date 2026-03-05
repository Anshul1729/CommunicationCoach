package com.communicationcoach.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.communicationcoach.R
import com.communicationcoach.data.api.ApiClient
import com.communicationcoach.data.db.AppDatabase
import com.communicationcoach.data.db.entity.ConversationEntity
import com.communicationcoach.data.db.entity.ConversationStatus
import com.communicationcoach.data.db.entity.TranscriptChunkEntity
import com.communicationcoach.ui.MainActivity
import com.communicationcoach.util.AudioRecorder
import com.communicationcoach.util.SilenceDetector
import com.communicationcoach.worker.AnalysisWorker
import kotlinx.coroutines.*
import java.io.File

class RecordingForegroundService : Service() {

    companion object {
        private const val TAG = "RecordingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "recording_channel"
        private const val CHANNEL_NAME = "Recording Service"

        // Conversations shorter than this are discarded — no DB entry, no API call
        private const val MIN_CONVERSATION_SECONDS = 30

        // True while the service is running — read by HomeViewModel on startup
        @Volatile var isRunning = false

        // Observed by HomeScreen to show live transcript
        val transcriptLiveData = MutableLiveData<String>()
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val processingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var audioRecorder: AudioRecorder
    private lateinit var chunkFile: File
    private lateinit var database: AppDatabase
    private lateinit var silenceDetector: SilenceDetector
    private lateinit var apiClient: ApiClient

    // -1L means no active conversation; set on first non-silent chunk
    private var currentConversationId: Long = -1L
    private var conversationStartTime: Long = 0L
    private var chunkNumber: Int = 0

    @Volatile
    private var isProcessing = false

    private var recordingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        Log.d(TAG, "Service created")
        audioRecorder = AudioRecorder()
        database = AppDatabase.getInstance(this)
        apiClient = ApiClient(this)
        chunkFile = File(cacheDir, "audio_chunk.wav")
        silenceDetector = SilenceDetector(onConversationEnded = ::handleConversationEnded)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_STICKY
    }

    // ── Recording Loop ──────────────────────────────────────────────────────

    private fun startRecording() {
        startForeground(NOTIFICATION_ID, createNotification())

        recordingJob = serviceScope.launch {
            chunkNumber = 0
            Log.d(TAG, "Recording started")

            try {
                if (!audioRecorder.startRecording()) {
                    Log.e(TAG, "Failed to initialize AudioRecord")
                    stopSelf()
                    return@launch
                }

                while (audioRecorder.isRecording()) {
                    val result = audioRecorder.recordChunk(chunkFile)

                    result.onSuccess { audioChunk ->
                        // Feed RMS to silence detector (chunk duration = CHUNK_DURATION_MS)
                        silenceDetector.onChunkRms(
                            rms = audioChunk.features.volumeRms,
                            chunkDurationMs = AudioRecorder.CHUNK_DURATION_MS
                        )

                        if (!isProcessing) {
                            processChunk()
                        } else {
                            Log.w(TAG, "Skipping chunk #$chunkNumber — previous chunk still processing")
                            chunkNumber++
                        }
                    }

                    result.onFailure { error ->
                        Log.e(TAG, "Failed to record chunk #$chunkNumber", error)
                    }
                }

            } catch (e: CancellationException) {
                Log.d(TAG, "Recording loop cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in recording loop", e)
                stopSelf()
            } finally {
                recordingJob = null
            }
        }
    }

    // ── Chunk Processing ─────────────────────────────────────────────────────

    private fun processChunk() {
        isProcessing = true

        processingScope.launch {
            try {
                val speechResponse = apiClient.transcribeAudio(chunkFile)

                if (!speechResponse.isSuccessful) {
                    Log.e(TAG, "Speech-to-Text error (chunk #$chunkNumber): ${speechResponse.errorBody()?.string()}")
                    return@launch
                }

                val transcript = speechResponse.body()?.results
                    ?.flatMap { it.alternatives ?: emptyList() }
                    ?.joinToString(" ") { it.transcript }
                    ?.trim() ?: ""
                Log.d(TAG, "Chunk #$chunkNumber: \"$transcript\"")

                if (transcript.isNotBlank()) {
                    // Lazily create a new conversation on first speech
                    if (currentConversationId == -1L) {
                        conversationStartTime = System.currentTimeMillis()
                        currentConversationId = database.conversationDao().insert(
                            ConversationEntity(startTime = conversationStartTime)
                        )
                        Log.d(TAG, "Conversation created: id=$currentConversationId")
                    }

                    // Append to live transcript display
                    val current = transcriptLiveData.value ?: ""
                    transcriptLiveData.postValue(
                        if (current.isBlank()) transcript else "$current $transcript"
                    )

                    // Persist chunk
                    database.transcriptChunkDao().insertChunk(
                        TranscriptChunkEntity(
                            conversationId = currentConversationId,
                            chunkNumber = chunkNumber,
                            timestamp = System.currentTimeMillis(),
                            transcript = transcript
                        )
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing chunk #$chunkNumber", e)
            } finally {
                isProcessing = false
                chunkNumber++
            }
        }
    }

    // ── Conversation Boundary ─────────────────────────────────────────────────

    /**
     * Called by SilenceDetector when 90s of near-silence has been detected.
     * Closes the current conversation and kicks off background analysis.
     */
    private fun handleConversationEnded() {
        if (currentConversationId == -1L) {
            Log.d(TAG, "Silence threshold reached but no active conversation — ignoring")
            silenceDetector.reset()
            return
        }

        serviceScope.launch {
            val closedId = currentConversationId
            val duration = ((System.currentTimeMillis() - conversationStartTime) / 1000).toInt()

            // Reset state before DB writes so a new conversation can start cleanly
            currentConversationId = -1L
            chunkNumber = 0
            transcriptLiveData.postValue("")

            if (duration < MIN_CONVERSATION_SECONDS) {
                // Too short — discard cleanly, no analysis
                database.conversationDao().deleteChunksForConversation(closedId)
                database.conversationDao().deleteById(closedId)
                Log.d(TAG, "Conversation $closedId discarded (${duration}s < ${MIN_CONVERSATION_SECONDS}s minimum)")
            } else {
                database.conversationDao().update(
                    id = closedId,
                    endTime = System.currentTimeMillis(),
                    durationSeconds = duration,
                    status = ConversationStatus.READY
                )
                Log.d(TAG, "Conversation $closedId closed (${duration}s). Queuing analysis.")
                AnalysisWorker.enqueue(this@RecordingForegroundService, closedId)
            }

            silenceDetector.reset()
        }
    }

    // ── Stop ────────────────────────────────────────────────────────────────

    private fun stopRecording() {
        Log.d(TAG, "Stopping recording…")

        serviceScope.launch {
            audioRecorder.stopRecording()
            recordingJob?.join()

            // Wait up to 30s for the last Whisper call to finish
            val deadline = System.currentTimeMillis() + 30_000L
            while (isProcessing && System.currentTimeMillis() < deadline) {
                delay(300)
            }

            // Close any open conversation
            if (currentConversationId != -1L) {
                val duration = ((System.currentTimeMillis() - conversationStartTime) / 1000).toInt()
                if (duration < MIN_CONVERSATION_SECONDS) {
                    database.conversationDao().deleteChunksForConversation(currentConversationId)
                    database.conversationDao().deleteById(currentConversationId)
                    Log.d(TAG, "Conversation $currentConversationId discarded on stop (${duration}s too short)")
                } else {
                    database.conversationDao().update(
                        id = currentConversationId,
                        endTime = System.currentTimeMillis(),
                        durationSeconds = duration,
                        status = ConversationStatus.READY
                    )
                    Log.d(TAG, "Closed conversation $currentConversationId on stop (${duration}s)")
                    AnalysisWorker.enqueue(this@RecordingForegroundService, currentConversationId)
                }
            }

            processingScope.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    // ── Notification ────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when microphone is actively recording"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        Log.d(TAG, "Service destroyed")
        audioRecorder.stopRecording()
        processingScope.cancel()
        serviceScope.cancel()
    }
}

const val ACTION_START = "com.communicationcoach.START"
const val ACTION_STOP = "com.communicationcoach.STOP"
