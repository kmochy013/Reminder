package com.example.data.repository

import com.example.data.local.ReminderDao
import com.example.data.model.ReminderItem
import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val reminderDao: ReminderDao) {

    val allReminders: Flow<List<ReminderItem>> = reminderDao.getAllReminders()

    suspend fun getReminderById(id: Long): ReminderItem? = reminderDao.getReminderById(id)

    suspend fun getUnreadReminders(): List<ReminderItem> = reminderDao.getUnreadReminders()

    suspend fun insertReminder(reminder: ReminderItem): Long = reminderDao.insertReminder(reminder)

    suspend fun updateReminder(reminder: ReminderItem) = reminderDao.updateReminder(reminder)

    suspend fun deleteReminder(reminder: ReminderItem) = reminderDao.deleteReminder(reminder)

    suspend fun deleteReminderById(id: Long) = reminderDao.deleteReminderById(id)

    suspend fun setReadStatus(id: Long, isRead: Boolean) = reminderDao.setReadStatus(id, isRead)

    suspend fun recordNotificationSent(id: Long, notifiedAt: Long) =
        reminderDao.recordNotificationSent(id, notifiedAt)
}
