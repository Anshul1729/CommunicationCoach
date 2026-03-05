package com.communicationcoach.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.communicationcoach.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayData(
    val shortLabel: String,   // "Mon"
    val date: String,         // "2026-03-04"
    val conversationCount: Int
)

data class IssueFrequency(
    val label: String,
    val count: Int,
    val rate: Float            // 0..1 for bar fill proportion
)

data class WeeklyData(
    val days: List<DayData>,
    val issueFrequency: List<IssueFrequency>,
    val totalConversations: Int,
    val totalInsights: Int
)

class ProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val _weeklyData = MutableStateFlow<WeeklyData?>(null)
    val weeklyData: StateFlow<WeeklyData?> = _weeklyData.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true

            // Build last 7 days (oldest → newest)
            val days = (6 downTo 0).map { daysAgo ->
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -daysAgo) }
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                val shortLabel = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
                dateStr to shortLabel
            }

            val insightsLast7 = db.insightDao().getRecent(100)
            val conversationsLast7 = db.conversationDao().getRecent(100)

            // Count conversations per day
            val convsByDate = conversationsLast7.groupBy { conv ->
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(conv.startTime))
            }

            // Count insights per day
            val insightsByDate = insightsLast7.groupBy { insight ->
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(insight.createdAt))
            }

            val dayDataList = days.map { (dateStr, label) ->
                DayData(
                    shortLabel = label,
                    date = dateStr,
                    conversationCount = convsByDate[dateStr]?.size ?: 0
                )
            }

            // Aggregate issue counts across all insights from the last 7 days
            val issueCounts = mutableMapOf(
                "defensive"     to 0,
                "emotionalTone" to 0,
                "grammar"       to 0,
                "clarity"       to 0,
                "confidence"    to 0
            )

            insightsLast7.forEach { insight ->
                try {
                    val obj = JSONObject(insight.issuesJson)
                    issueCounts.keys.forEach { key ->
                        val issueObj = obj.optJSONObject(key) ?: return@forEach
                        // For "clarity" we check score < 6 as "problematic"
                        val detected = if (key == "clarity") {
                            issueObj.has("score") && issueObj.optInt("score", 10) < 6
                        } else {
                            issueObj.optBoolean("detected", false)
                        }
                        if (detected) issueCounts[key] = (issueCounts[key] ?: 0) + 1
                    }
                } catch (_: Exception) {}
            }

            val maxCount = issueCounts.values.maxOrNull()?.takeIf { it > 0 } ?: 1
            val issueFrequency = listOf(
                "emotionalTone" to "Emotional Tone",
                "defensive"     to "Defensive Language",
                "grammar"       to "Grammar",
                "confidence"    to "Confidence",
                "clarity"       to "Clarity"
            ).map { (key, label) ->
                val count = issueCounts[key] ?: 0
                IssueFrequency(label, count, count.toFloat() / maxCount)
            }

            _weeklyData.value = WeeklyData(
                days = dayDataList,
                issueFrequency = issueFrequency,
                totalConversations = convsByDate
                    .filterKeys { date -> days.any { it.first == date } }
                    .values.sumOf { it.size },
                totalInsights = insightsLast7.size
            )
            _isLoading.value = false
        }
    }
}
