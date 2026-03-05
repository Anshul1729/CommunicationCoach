package com.communicationcoach.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.communicationcoach.data.api.ApiClient
import com.communicationcoach.data.db.AppDatabase
import com.communicationcoach.data.db.entity.DailyDigestEntity
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DailyDigestWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "DailyDigestWorker"
        private const val WORK_NAME = "daily_digest"
        private const val DIGEST_CHANNEL_ID = "daily_digest"
        private const val DIGEST_NOTIF_ID = 3001

        /**
         * Schedules the worker to fire once daily at 22:00.
         * Uses KEEP so re-scheduling on every app launch doesn't reset the timer.
         */
        fun schedule(context: Context) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 22)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // If 10 PM already passed today, fire tomorrow
                if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
            }
            val initialDelayMs = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<DailyDigestWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "DailyDigestWorker scheduled. First fire in ${initialDelayMs / 60_000} min")
        }
    }

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        Log.d(TAG, "Running daily digest for $today")

        return try {
            val recentInsights = db.insightDao().getRecent(20)
            val todayInsights = recentInsights.filter { insight ->
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date(insight.createdAt)) == today
            }

            if (todayInsights.isEmpty()) {
                Log.d(TAG, "No conversations today — skipping digest")
                return Result.success()
            }

            // Build a readable summary of today's insights for Claude
            val insightText = todayInsights.joinToString("\n\n") { insight ->
                "Conversation summary: ${insight.summary}"
            }

            val profileJson = db.userProfileDao().get()?.profileJson ?: "{}"

            val apiClient = ApiClient(applicationContext)
            val response = apiClient.generateDailyDigest(
                insightSummaries = insightText,
                userProfileJson = profileJson
            )

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini digest failed: ${response.errorBody()?.string()}")
                return Result.retry()
            }

            val rawText = response.body()?.let { ApiClient.extractText(it) }
            if (rawText.isNullOrBlank()) return Result.retry()

            Log.d(TAG, "Digest response: $rawText")

            val json = JSONObject(ApiClient.cleanJson(rawText))
            val summary = json.optString("summary", "")
            val topPatternsJson = json.optJSONArray("topPatterns")?.toString() ?: "[]"
            val progressNote = json.optString("progressNote", "")
            val tomorrowTip = json.optString("tomorrowTip", "")

            // Combine progressNote + tomorrowTip into progressNotes
            val progressNotes = buildString {
                if (progressNote.isNotBlank()) append(progressNote)
                if (tomorrowTip.isNotBlank()) {
                    if (isNotEmpty()) append("\n\nTomorrow: ")
                    append(tomorrowTip)
                }
            }

            db.dailyDigestDao().upsert(
                DailyDigestEntity(
                    date = today,
                    summary = summary,
                    topPatternsJson = topPatternsJson,
                    progressNotes = progressNotes
                )
            )

            sendDigestNotification(
                summary = summary,
                tomorrowTip = tomorrowTip,
                conversationCount = todayInsights.size
            )

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Daily digest failed", e)
            Result.retry()
        }
    }

    private fun sendDigestNotification(
        summary: String,
        tomorrowTip: String,
        conversationCount: Int
    ) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    DIGEST_CHANNEL_ID,
                    "Daily Digest",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "End-of-day coaching summary" }
            )
        }

        val bodyText = buildString {
            append(summary)
            if (tomorrowTip.isNotBlank()) append("\n\nTomorrow: $tomorrowTip")
        }

        val notif = NotificationCompat.Builder(applicationContext, DIGEST_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Today's Coaching Digest · $conversationCount conversations")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setAutoCancel(true)
            .build()

        manager.notify(DIGEST_NOTIF_ID, notif)
        Log.d(TAG, "Daily digest notification sent")
    }
}
