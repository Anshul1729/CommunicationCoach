package com.communicationcoach.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.communicationcoach.data.api.ApiClient
import com.communicationcoach.data.db.AppDatabase
import com.communicationcoach.data.db.entity.ConversationStatus
import com.communicationcoach.data.db.entity.InsightEntity
import com.communicationcoach.data.db.entity.UserProfileEntity
import com.communicationcoach.util.CostTracker
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AnalysisWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "AnalysisWorker"
        private const val KEY_CONVERSATION_ID = "conversation_id"
        private const val COACHING_CHANNEL_ID = "coaching_insights"
        private const val COACHING_NOTIF_ID = 2001

        fun enqueue(context: Context, conversationId: Long) {
            val data = workDataOf(KEY_CONVERSATION_ID to conversationId)
            val request = OneTimeWorkRequestBuilder<AnalysisWorker>()
                .setInputData(data)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueue(request)
            Log.d(TAG, "AnalysisWorker enqueued for conversation $conversationId")
        }
    }

    override suspend fun doWork(): Result {
        val conversationId = inputData.getLong(KEY_CONVERSATION_ID, -1L)
        if (conversationId == -1L) {
            Log.e(TAG, "No conversation ID in input data")
            return Result.failure()
        }

        val db = AppDatabase.getInstance(applicationContext)

        db.conversationDao().updateStatus(conversationId, ConversationStatus.ANALYZING)
        Log.d(TAG, "Analyzing conversation $conversationId")

        return try {
            val transcript = db.transcriptChunkDao().getFullTranscriptForConversation(conversationId)

            if (transcript.isNullOrBlank()) {
                Log.w(TAG, "Empty transcript for conversation $conversationId — skipping analysis")
                db.conversationDao().updateStatus(conversationId, ConversationStatus.ANALYZED)
                return Result.success()
            }

            val wordCount = transcript.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
            if (wordCount < 20) {
                Log.d(TAG, "Transcript too short ($wordCount words) — skipping Gemini call")
                db.conversationDao().updateStatus(conversationId, ConversationStatus.ANALYZED)
                return Result.success()
            }

            val costTracker = CostTracker(applicationContext)
            if (costTracker.getTotalUsd() >= CostTracker.SPEND_LIMIT_USD) {
                Log.w(TAG, "Spend limit reached (\$${CostTracker.SPEND_LIMIT_USD}) — skipping analysis")
                db.conversationDao().updateStatus(conversationId, ConversationStatus.ANALYZED)
                sendLimitNotification()
                return Result.success()
            }

            val profileJson = db.userProfileDao().get()?.profileJson ?: "{}"

            val apiClient = ApiClient(applicationContext)
            val response = apiClient.analyzeConversation(
                fullTranscript = transcript,
                userProfileJson = profileJson
            )

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API failed: ${response.errorBody()?.string()}")
                db.conversationDao().updateStatus(conversationId, ConversationStatus.FAILED)
                return Result.retry()
            }

            val rawText = response.body()?.let { ApiClient.extractText(it) }
            if (rawText.isNullOrBlank()) {
                Log.e(TAG, "Empty Gemini response body")
                return Result.retry()
            }

            Log.d(TAG, "Gemini response: $rawText")

            val json = JSONObject(ApiClient.cleanJson(rawText))
            val tipsJson = json.optJSONArray("tips")?.toString() ?: "[]"
            val issuesJson = json.optJSONObject("issues")?.toString() ?: "{}"
            val summary = json.optString("summary", "")

            db.insightDao().insert(
                InsightEntity(
                    conversationId = conversationId,
                    createdAt = System.currentTimeMillis(),
                    tipsJson = tipsJson,
                    issuesJson = issuesJson,
                    summary = summary
                )
            )

            val profileUpdates = json.optJSONObject("profileUpdates")
            if (profileUpdates != null) {
                val updatedProfile = mergeUserProfile(profileJson, profileUpdates)
                db.userProfileDao().upsert(UserProfileEntity(profileJson = updatedProfile))
                Log.d(TAG, "User profile updated")
            }

            db.conversationDao().updateStatus(conversationId, ConversationStatus.ANALYZED)

            // Track costs (costTracker already created above for spend-limit check)
            val conversation = db.conversationDao().getById(conversationId)
            if (conversation != null) {
                costTracker.addSpeechCost(conversation.durationSeconds)
            }
            val usage = response.body()?.usageMetadata
            if (usage != null) {
                costTracker.addGeminiCost(usage.promptTokenCount, usage.candidatesTokenCount)
                Log.d(TAG, "Cost tracked — STT: ${conversation?.durationSeconds}s, Gemini: ${usage.promptTokenCount} in / ${usage.candidatesTokenCount} out tokens")
            }

            sendCoachingNotification(tipsJson, summary)

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Analysis failed for conversation $conversationId", e)
            db.conversationDao().updateStatus(conversationId, ConversationStatus.FAILED)
            Result.retry()
        }
    }

    /**
     * Merges Claude's profileUpdates into the existing JSON profile document.
     */
    private fun mergeUserProfile(existingJson: String, updates: JSONObject): String {
        return try {
            val profile = JSONObject(existingJson)

            profile.put("totalConversations", profile.optInt("totalConversations", 0) + 1)

            updates.optJSONArray("recurringPatterns")?.let { profile.put("recurringPatterns", it) }
            updates.optJSONArray("improvements")?.let { profile.put("improvements", it) }
            updates.optJSONArray("focusAreas")?.let { profile.put("focusAreas", it) }

            profile.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to merge user profile", e)
            existingJson
        }
    }

    /**
     * Fires a post-conversation notification with the top 2–3 coaching tips.
     */
    private fun sendCoachingNotification(tipsJson: String, summary: String) {
        val tips = try {
            val arr = JSONArray(tipsJson)
            (0 until minOf(arr.length(), 3)).map { "• ${arr.getString(it)}" }
        } catch (e: Exception) {
            listOf(summary)
        }

        val notificationManager =
            applicationContext.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                COACHING_CHANNEL_ID,
                "Coaching Insights",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Post-conversation coaching tips"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val bodyText = tips.joinToString("\n")
        val notif = NotificationCompat.Builder(applicationContext, COACHING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Conversation Insights")
            .setContentText(tips.firstOrNull() ?: summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(COACHING_NOTIF_ID, notif)
        Log.d(TAG, "Coaching notification sent")
    }

    /** Notifies the user that the spend limit has been reached and analysis is paused. */
    private fun sendLimitNotification() {
        val notificationManager =
            applicationContext.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                COACHING_CHANNEL_ID,
                "Coaching Insights",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Post-conversation coaching tips" }
            notificationManager.createNotificationChannel(channel)
        }

        val notif = NotificationCompat.Builder(applicationContext, COACHING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Spend limit reached")
            .setContentText("API budget of \$${CostTracker.SPEND_LIMIT_USD} used. Coaching analysis paused.")
            .setAutoCancel(true)
            .build()

        notificationManager.notify(COACHING_NOTIF_ID + 1, notif)
        Log.w(TAG, "Spend limit notification sent")
    }
}
