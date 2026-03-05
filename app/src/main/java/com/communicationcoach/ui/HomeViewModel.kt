package com.communicationcoach.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.communicationcoach.data.db.AppDatabase
import com.communicationcoach.service.ACTION_START
import com.communicationcoach.service.ACTION_STOP
import com.communicationcoach.service.RecordingForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    // Initialise from actual service state so the UI is correct on re-open
    private val _isRecording = MutableStateFlow(RecordingForegroundService.isRunning)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _todayStats = MutableStateFlow(TodayStats())
    val todayStats: StateFlow<TodayStats> = _todayStats.asStateFlow()

    private val _latestTips = MutableStateFlow<List<String>>(emptyList())
    val latestTips: StateFlow<List<String>> = _latestTips.asStateFlow()

    // Bridge the LiveData from the service so Compose can observe it
    val liveTranscript = RecordingForegroundService.transcriptLiveData

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val allConversations = db.conversationDao().getAll()
            val todayConversations = allConversations.filter { conv ->
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date(conv.startTime)) == today
            }

            val latestInsight = db.insightDao().getRecent(1).firstOrNull()
            val tips = latestInsight?.let { parseTips(it.tipsJson) } ?: emptyList()

            val topIssue = latestInsight?.issuesJson?.let { extractTopIssue(it) }

            _todayStats.value = TodayStats(
                conversationCount = todayConversations.size,
                topIssue = topIssue
            )
            _latestTips.value = tips
        }
    }

    fun startRecording() {
        val context = getApplication<Application>()
        val intent = Intent(context, RecordingForegroundService::class.java).apply {
            action = ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        _isRecording.value = true
        RecordingForegroundService.transcriptLiveData.postValue("")
    }

    fun stopRecording() {
        val context = getApplication<Application>()
        val intent = Intent(context, RecordingForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        context.startService(intent)
        _isRecording.value = false
    }

    private fun extractTopIssue(issuesJson: String): String? = try {
        val obj = JSONObject(issuesJson)
        listOf("defensive", "emotionalTone", "grammar", "confidence")
            .firstOrNull { key -> obj.optJSONObject(key)?.optBoolean("detected", false) == true }
            ?.let { issueKeyToLabel(it) }
    } catch (e: Exception) {
        null
    }
}
