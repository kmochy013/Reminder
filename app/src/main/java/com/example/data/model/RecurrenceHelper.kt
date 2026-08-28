package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class RecurrenceType(val displayName: String) {
    NONE("Does not repeat"),
    DAILY("Every Day"),
    WEEKLY("Every Week"),
    MONTHLY("Every Month"),
    YEARLY("Every Year (Birthday)")
}

object RecurrenceHelper {

    fun getNextOccurrence(
        currentTimestamp: Long,
        recurrence: RecurrenceType,
        repeatDayOfWeek: Int? = null
    ): Long {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentTimestamp
        }

        when (recurrence) {
            RecurrenceType.NONE -> return currentTimestamp

            RecurrenceType.DAILY -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                while (cal.timeInMillis <= now) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                return cal.timeInMillis
            }

            RecurrenceType.WEEKLY -> {
                if (repeatDayOfWeek != null) {
                    cal.set(Calendar.DAY_OF_WEEK, repeatDayOfWeek)
                }
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                while (cal.timeInMillis <= now) {
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                }
                return cal.timeInMillis
            }

            RecurrenceType.MONTHLY -> {
                cal.add(Calendar.MONTH, 1)
                while (cal.timeInMillis <= now) {
                    cal.add(Calendar.MONTH, 1)
                }
                return cal.timeInMillis
            }

            RecurrenceType.YEARLY -> {
                cal.add(Calendar.YEAR, 1)
                while (cal.timeInMillis <= now) {
                    cal.add(Calendar.YEAR, 1)
                }
                return cal.timeInMillis
            }
        }
    }

    fun getUpcomingDayOfWeekTimestamp(dayOfWeek: Int, hourOfDay: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        var daysToAdd = (dayOfWeek - currentDayOfWeek + 7) % 7
        if (daysToAdd == 0 && cal.timeInMillis <= System.currentTimeMillis()) {
            daysToAdd = 7
        }
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
        return cal.timeInMillis
    }

    fun getRecurrenceLabel(recurrence: RecurrenceType, repeatDayOfWeek: Int?): String {
        return when (recurrence) {
            RecurrenceType.NONE -> "Once"
            RecurrenceType.DAILY -> "Every Day"
            RecurrenceType.WEEKLY -> {
                val dayName = when (repeatDayOfWeek) {
                    Calendar.SUNDAY -> "Sunday"
                    Calendar.MONDAY -> "Monday"
                    Calendar.TUESDAY -> "Tuesday"
                    Calendar.WEDNESDAY -> "Wednesday"
                    Calendar.THURSDAY -> "Thursday"
                    Calendar.FRIDAY -> "Friday"
                    Calendar.SATURDAY -> "Saturday"
                    else -> "Week"
                }
                "Every $dayName"
            }
            RecurrenceType.MONTHLY -> "Every Month"
            RecurrenceType.YEARLY -> "Every Year (Birthday)"
        }
    }

    fun getCategoryIcon(category: String): String {
        return when {
            category.contains("Birthday", ignoreCase = true) -> "🎂"
            category.contains("Mosque", ignoreCase = true) || category.contains("Prayer", ignoreCase = true) -> "🕌"
            category.contains("Work", ignoreCase = true) -> "💼"
            category.contains("Personal", ignoreCase = true) -> "👤"
            category.contains("Health", ignoreCase = true) -> "💊"
            category.contains("Study", ignoreCase = true) -> "📚"
            category.contains("Finance", ignoreCase = true) -> "💰"
            category.contains("Urgent", ignoreCase = true) -> "🚨"
            else -> "📌"
        }
    }
}
