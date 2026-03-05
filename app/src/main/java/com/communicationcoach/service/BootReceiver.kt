package com.communicationcoach.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.communicationcoach.ui.MainActivity

/**
 * Fires after the phone reboots and sends a gentle reminder notification
 * so the user can re-open the app and resume coaching.
 *
 * We deliberately do NOT auto-start recording on boot — starting a mic
 * foreground service silently after a reboot would be unexpected and
 * intrusive. The user opts back in with one tap.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "boot_reminder"
        private const val NOTIF_ID = 4001
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val manager = context.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Boot Reminder",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Reminds you to re-open the app after a reboot" }
            )
        }

        val tapIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Communication Coach")
            .setContentText("Tap to resume your coaching session")
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIF_ID, notif)
    }
}
