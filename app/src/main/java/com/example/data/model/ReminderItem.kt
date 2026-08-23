package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val targetTimestamp: Long,
    val category: String = "General",
    val priority: Priority = Priority.MEDIUM,
    val isRead: Boolean = false,
    val repeatCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastNotifiedAt: Long? = null,
    val colorTag: Long = 0xFF4A6CF7
) {
    val isPassed: Boolean
        get() = System.currentTimeMillis() >= targetTimestamp

    val isNagging: Boolean
        get() = !isRead && isPassed
}
