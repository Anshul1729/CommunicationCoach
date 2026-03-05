package com.communicationcoach.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.communicationcoach.data.db.AppDatabase
import com.communicationcoach.util.CostTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class ProfileData(
    val totalConversations: Int = 0,
    val recurringPatterns: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    val focusAreas: List<String> = emptyList(),
    val recentDigestSummary: String? = null,
    val recentDigestTip: String? = null
)

data class SpendData(
    val sttUsd: Double = 0.0,
    val geminiUsd: Double = 0.0,
    val geminiInputTokens: Long = 0L,
    val geminiOutputTokens: Long = 0L
) {
    val totalUsd: Double get() = sttUsd + geminiUsd
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val costTracker = CostTracker(application)

    private val _profile = MutableStateFlow<ProfileData?>(null)
    val profile: StateFlow<ProfileData?> = _profile.asStateFlow()

    private val _spend = MutableStateFlow(SpendData())
    val spend: StateFlow<SpendData> = _spend.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true

            val profileEntity = db.userProfileDao().get()
            val recentDigest = db.dailyDigestDao().getRecent(1).firstOrNull()

            val profileData = if (profileEntity != null) {
                parseProfile(profileEntity.profileJson)
            } else {
                ProfileData()
            }

            _profile.value = profileData.copy(
                recentDigestSummary = recentDigest?.summary,
                recentDigestTip = recentDigest?.progressNotes
                    ?.lines()
                    ?.firstOrNull { it.startsWith("Tomorrow:") }
                    ?.removePrefix("Tomorrow:")
                    ?.trim()
            )

            _spend.value = SpendData(
                sttUsd = costTracker.getSttUsd(),
                geminiUsd = costTracker.getGeminiUsd(),
                geminiInputTokens = costTracker.getGeminiInputTokens(),
                geminiOutputTokens = costTracker.getGeminiOutputTokens()
            )

            _isLoading.value = false
        }
    }

    private fun parseProfile(json: String): ProfileData {
        return try {
            val obj = JSONObject(json)
            ProfileData(
                totalConversations = obj.optInt("totalConversations", 0),
                recurringPatterns  = parseStringArray(obj.optJSONArray("recurringPatterns")),
                improvements       = parseStringArray(obj.optJSONArray("improvements")),
                focusAreas         = parseStringArray(obj.optJSONArray("focusAreas"))
            )
        } catch (e: Exception) {
            ProfileData()
        }
    }

    private fun parseStringArray(arr: JSONArray?): List<String> {
        arr ?: return emptyList()
        return (0 until arr.length()).map { arr.getString(it) }
    }
}
