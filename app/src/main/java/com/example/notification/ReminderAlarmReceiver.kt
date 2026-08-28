package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.ReminderDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = ReminderDatabase.getDatabase(context)
                val dao = db.reminderDao()

                when (action) {
                    NotificationHelper.ACTION_TRIGGER_REMINDER -> {
                        if (reminderId != -1L) {
                            val reminder = dao.getReminderById(reminderId)
                            if (reminder != null) {
                                if (!reminder.isRead) {
                                    // 1. Show Heads-Up Notification on screen and panel
                                    NotificationHelper.showReminderNotification(context, reminder)

                                    // 2. Record notification timestamp & increment count
                                    dao.recordNotificationSent(reminderId, System.currentTimeMillis())

                                    // 3. User hasn't read it yet -> Schedule 1-minute repeat alarm!
                                    AlarmScheduler.scheduleOneMinuteRepeatAlarm(context, reminderId)
                                } else {
                                    // Marked as read -> Cancel alarm & dismiss notification
                                    AlarmScheduler.cancelReminderAlarm(context, reminderId)
                                    NotificationHelper.cancelNotification(context, reminderId)
                                }
                            }
                        }
                    }

                    NotificationHelper.ACTION_MARK_READ -> {
                        if (reminderId != -1L) {
                            val reminder = dao.getReminderById(reminderId)
                            AlarmScheduler.cancelReminderAlarm(context, reminderId)
                            NotificationHelper.cancelNotification(context, reminderId)

                            if (reminder != null && reminder.isRecurring) {
                                // Recurring reminder (e.g. Weekly Friday Mosque or Birthday)
                                val nextOccurrence = com.example.data.model.RecurrenceHelper.getNextOccurrence(
                                    reminder.targetTimestamp,
                                    reminder.recurrence,
                                    reminder.repeatDayOfWeek
                                )
                                val updated = reminder.copy(
                                    targetTimestamp = nextOccurrence,
                                    isRead = false,
                                    repeatCount = 0,
                                    lastNotifiedAt = null
                                )
                                dao.updateReminder(updated)
                                AlarmScheduler.scheduleReminderAlarm(context, updated.id, updated.targetTimestamp)
                                Log.d("ReminderReceiver", "Recurring reminder $reminderId advanced to next occurrence: $nextOccurrence")
                            } else {
                                dao.setReadStatus(reminderId, true)
                                Log.d("ReminderReceiver", "Reminder $reminderId marked as read. Repeat alarms cancelled.")
                            }
                        }
                    }

                    NotificationHelper.ACTION_SNOOZE_1MIN -> {
                        if (reminderId != -1L) {
                            // User clicked Snooze 1 Min
                            NotificationHelper.cancelNotification(context, reminderId)
                            AlarmScheduler.scheduleOneMinuteRepeatAlarm(context, reminderId)
                            Log.d("ReminderReceiver", "Reminder $reminderId snoozed for 1 minute.")
                        }
                    }

                    Intent.ACTION_BOOT_COMPLETED -> {
                        AlarmScheduler.rescheduleAllActiveReminders(context)
                        com.example.update.AppUpdateManager.schedulePeriodicUpdateCheck(context)
                        com.example.update.AppUpdateManager.checkForUpdates(
                            context = context,
                            notifyIfAvailable = true,
                            onResult = { /* Handled via notification */ }
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Error handling broadcast action $action: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
