package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Priority
import com.example.data.model.ReminderItem

object NotificationHelper {

    const val CHANNEL_ID = "channel_reminders_high_priority"
    const val CHANNEL_NAME = "Reminder Alerts"
    const val CHANNEL_DESCRIPTION = "High priority heads-up reminders with 1-minute repeat alerts"

    const val CHANNEL_ID_UPDATES = "channel_app_updates"
    const val CHANNEL_NAME_UPDATES = "App Updates & Releases"
    const val CHANNEL_DESCRIPTION_UPDATES = "Notifications when a new app update is released and available to download"

    const val NOTIFICATION_ID_UPDATE = 999001

    const val ACTION_TRIGGER_REMINDER = "com.example.ACTION_TRIGGER_REMINDER"
    const val ACTION_MARK_READ = "com.example.ACTION_MARK_READ"
    const val ACTION_SNOOZE_1MIN = "com.example.ACTION_SNOOZE_1MIN"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val reminderChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = Color.parseColor("#4A6CF7")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            val updateChannel = NotificationChannel(
                CHANNEL_ID_UPDATES,
                CHANNEL_NAME_UPDATES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION_UPDATES
                enableLights(true)
                lightColor = Color.parseColor("#10B981")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(updateChannel)
        }
    }

    fun showReminderNotification(context: Context, reminder: ReminderItem) {
        createNotificationChannel(context)

        // Intent to open app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark as Read (Done) -> Stops 1-min loop!
        val markReadIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_MARK_READ
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id * 10 + 1).toInt(),
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 1 Min
        val snoozeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_1MIN
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val categoryIcon = com.example.data.model.RecurrenceHelper.getCategoryIcon(reminder.category)
        val recurrenceInfo = if (reminder.isRecurring) {
            " • 🔁 ${com.example.data.model.RecurrenceHelper.getRecurrenceLabel(reminder.recurrence, reminder.repeatDayOfWeek)}"
        } else ""

        val repeatText = if (reminder.repeatCount > 0) {
            " (Repeated ${reminder.repeatCount}x • Will ring again in 1 min if unread)"
        } else {
            " (Will remind every 1 min until marked read)"
        }

        val priorityPrefix = when (reminder.priority) {
            Priority.URGENT -> "🚨 [URGENT] "
            Priority.HIGH -> "⚡ [HIGH] "
            Priority.MEDIUM -> "🔔 "
            Priority.LOW -> "📌 "
        }

        val fullTitle = "$priorityPrefix$categoryIcon ${reminder.title}"
        val bodyContent = if (reminder.description.isNotBlank()) {
            "${reminder.description}\n$categoryIcon ${reminder.category}$recurrenceInfo$repeatText"
        } else {
            "$categoryIcon Category: ${reminder.category}$recurrenceInfo$repeatText"
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(fullTitle)
            .setContentText(if (reminder.description.isNotBlank()) reminder.description else "Reminder: ${reminder.category}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bodyContent)
                    .setBigContentTitle(fullTitle)
                    .setSummaryText("Tap 'Mark Read' when done")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(false)
            .setOngoing(!reminder.isRead)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 350, 200, 350))
            .addAction(
                android.R.drawable.checkbox_on_background,
                "✓ Mark as Read",
                markReadPendingIntent
            )
            .addAction(
                android.R.drawable.ic_popup_sync,
                "⏰ Snooze 1 Min",
                snoozePendingIntent
            )

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(reminder.id.toInt(), notificationBuilder.build())
        } catch (e: SecurityException) {
            // Handled when POST_NOTIFICATIONS permission not granted
        }
    }

    fun cancelNotification(context: Context, reminderId: Long) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(reminderId.toInt())
    }

    fun showUpdateNotification(
        context: Context,
        versionName: String,
        releaseNotes: String,
        downloadUrl: String,
        isMandatory: Boolean = true
    ) {
        createNotificationChannel(context)

        // Intent to open download link or GitHub release
        val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val downloadPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_UPDATE,
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to open app directly
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_UPDATE + 1,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val title = if (isMandatory) {
            "⚠️ Mandatory Update Required: v$versionName"
        } else {
            "🚀 New Update Available: v$versionName"
        }

        val message = if (isMandatory) {
            "A necessary update is required to continue using the app. Tap to download and install version $versionName now."
        } else {
            "A new version of the Reminder app is ready to download! Tap to get the latest features and improvements."
        }

        val fullContent = if (releaseNotes.isNotBlank()) {
            "$message\n\nWhat's New in v$versionName:\n$releaseNotes"
        } else message

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_UPDATES)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(fullContent)
                    .setBigContentTitle(title)
                    .setSummaryText(if (isMandatory) "Update required to continue" else "Update available")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(downloadPendingIntent)
            .setAutoCancel(!isMandatory)
            .setOngoing(isMandatory)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 150, 300, 150, 300))
            .addAction(
                android.R.drawable.stat_sys_download,
                "Download & Update",
                downloadPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_info_details,
                "Open App",
                appPendingIntent
            )
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID_UPDATE, notification)
        } catch (e: SecurityException) {
            // Handled when permission is missing
        }
    }
}
