package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ReminderDatabase
import com.example.data.model.Priority
import com.example.data.model.ReminderItem
import com.example.data.repository.ReminderRepository
import com.example.notification.AlarmScheduler
import com.example.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FilterTab(val label: String) {
    ALL("All"),
    UPCOMING("Upcoming"),
    NAGGING("Active Alert"),
    COMPLETED("Read / Done")
}

data class ReminderUiState(
    val reminders: List<ReminderItem> = emptyList(),
    val totalCount: Int = 0,
    val upcomingCount: Int = 0,
    val naggingCount: Int = 0,
    val completedCount: Int = 0,
    val selectedTab: FilterTab = FilterTab.ALL,
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val availableCategories: List<String> = listOf(
        "All", "Birthday", "Mosque / Prayer", "General", "Work", "Personal", "Health", "Study", "Finance", "Urgent"
    )
)

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReminderRepository
    private val _selectedTab = MutableStateFlow(FilterTab.ALL)
    private val _selectedCategory = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")

    init {
        val db = ReminderDatabase.getDatabase(application)
        repository = ReminderRepository(db.reminderDao())
        NotificationHelper.createNotificationChannel(application)
    }

    val uiState: StateFlow<ReminderUiState> = combine(
        repository.allReminders,
        _selectedTab,
        _selectedCategory,
        _searchQuery
    ) { allReminders, tab, category, query ->
        val now = System.currentTimeMillis()
        val total = allReminders.size
        val upcoming = allReminders.count { !it.isRead && it.targetTimestamp > now }
        val nagging = allReminders.count { !it.isRead && it.targetTimestamp <= now }
        val completed = allReminders.count { it.isRead }

        val filtered = allReminders.filter { reminder ->
            val matchesTab = when (tab) {
                FilterTab.ALL -> true
                FilterTab.UPCOMING -> !reminder.isRead && reminder.targetTimestamp > now
                FilterTab.NAGGING -> !reminder.isRead && reminder.targetTimestamp <= now
                FilterTab.COMPLETED -> reminder.isRead
            }

            val matchesCategory = if (category == "All") true else reminder.category.equals(category, ignoreCase = true)

            val matchesQuery = if (query.isBlank()) true else {
                reminder.title.contains(query, ignoreCase = true) ||
                        reminder.description.contains(query, ignoreCase = true) ||
                        reminder.category.contains(query, ignoreCase = true)
            }

            matchesTab && matchesCategory && matchesQuery
        }

        val dynamicCategories = (listOf("All", "Birthday", "Mosque / Prayer", "General", "Work", "Personal", "Health", "Study", "Finance", "Urgent") +
                allReminders.map { it.category }).distinct()

        ReminderUiState(
            reminders = filtered,
            totalCount = total,
            upcomingCount = upcoming,
            naggingCount = nagging,
            completedCount = completed,
            selectedTab = tab,
            selectedCategory = category,
            searchQuery = query,
            availableCategories = dynamicCategories
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReminderUiState()
    )

    fun selectTab(tab: FilterTab) {
        _selectedTab.value = tab
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleReadStatus(reminder: ReminderItem, isRead: Boolean) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (isRead) {
                // If marked as Read/Done:
                // Stop 1-minute nag alerts and clear notification banner!
                AlarmScheduler.cancelReminderAlarm(context, reminder.id)
                NotificationHelper.cancelNotification(context, reminder.id)

                if (reminder.isRecurring) {
                    // Recurring reminder (e.g. Weekly Friday Mosque, Yearly Birthday, Daily, etc.)
                    // Automatically advance to the next occurrence!
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
                    repository.updateReminder(updated)
                    AlarmScheduler.scheduleReminderAlarm(context, updated.id, updated.targetTimestamp)
                } else {
                    repository.setReadStatus(reminder.id, true)
                }
            } else {
                // Unchecked/marked as unread:
                repository.setReadStatus(reminder.id, false)
                if (reminder.targetTimestamp <= System.currentTimeMillis()) {
                    // Overdue -> start repeating reminder
                    AlarmScheduler.scheduleOneMinuteRepeatAlarm(context, reminder.id)
                } else {
                    AlarmScheduler.scheduleReminderAlarm(context, reminder.id, reminder.targetTimestamp)
                }
            }
        }
    }

    fun addReminder(
        title: String,
        description: String,
        targetTimestamp: Long,
        category: String,
        priority: Priority,
        recurrence: com.example.data.model.RecurrenceType = com.example.data.model.RecurrenceType.NONE,
        repeatDayOfWeek: Int? = null
    ) {
        viewModelScope.launch {
            val colorTag = when (priority) {
                Priority.URGENT -> 0xFFEF4444
                Priority.HIGH -> 0xFFF97316
                Priority.MEDIUM -> 0xFF4F46E5
                Priority.LOW -> 0xFF0D9488
            }

            val newReminder = ReminderItem(
                title = title.trim(),
                description = description.trim(),
                targetTimestamp = targetTimestamp,
                category = category.trim().ifBlank { "General" },
                priority = priority,
                isRead = false,
                repeatCount = 0,
                colorTag = colorTag,
                recurrence = recurrence,
                repeatDayOfWeek = repeatDayOfWeek
            )

            val id = repository.insertReminder(newReminder)
            val context = getApplication<Application>()
            AlarmScheduler.scheduleReminderAlarm(context, id, targetTimestamp)
        }
    }

    fun updateReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
            val context = getApplication<Application>()
            if (!reminder.isRead) {
                AlarmScheduler.scheduleReminderAlarm(context, reminder.id, reminder.targetTimestamp)
            } else {
                AlarmScheduler.cancelReminderAlarm(context, reminder.id)
                NotificationHelper.cancelNotification(context, reminder.id)
            }
        }
    }

    fun deleteReminder(reminder: ReminderItem) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            val context = getApplication<Application>()
            AlarmScheduler.cancelReminderAlarm(context, reminder.id)
            NotificationHelper.cancelNotification(context, reminder.id)
        }
    }

    fun snoozeReminder(reminderId: Long, minutes: Int = 1) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            NotificationHelper.cancelNotification(context, reminderId)
            val nextTime = System.currentTimeMillis() + (minutes * 60_000L)
            AlarmScheduler.scheduleReminderAlarm(context, reminderId, nextTime)
        }
    }

    fun testInstantReminder(title: String = "🚨 Drink Water & Stand Up") {
        viewModelScope.launch {
            val targetTime = System.currentTimeMillis() + 3_000L // 3 seconds from now
            val reminder = ReminderItem(
                title = title,
                description = "This is a live test notification. If not checked as read, it will alert again in 1 minute!",
                targetTimestamp = targetTime,
                category = "Health",
                priority = Priority.HIGH,
                isRead = false
            )
            val id = repository.insertReminder(reminder)
            val context = getApplication<Application>()
            AlarmScheduler.scheduleReminderAlarm(context, id, targetTime)
        }
    }
}
