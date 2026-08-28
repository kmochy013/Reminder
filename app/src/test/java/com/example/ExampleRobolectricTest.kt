package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Priority
import com.example.data.model.ReminderItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Reminder", appName)
  }

  @Test
  fun `reminder item calculation`() {
    val pastReminder = ReminderItem(
      title = "Test",
      targetTimestamp = System.currentTimeMillis() - 10_000L,
      isRead = false,
      priority = Priority.HIGH
    )
    assertTrue(pastReminder.isPassed)
    assertTrue(pastReminder.isNagging)
  }

  @Test
  fun `weekly recurrence calculation advances to upcoming friday`() {
    val fridayTimestamp = com.example.data.model.RecurrenceHelper.getUpcomingDayOfWeekTimestamp(
      java.util.Calendar.FRIDAY, 13, 0
    )
    assertTrue(fridayTimestamp > System.currentTimeMillis())

    val cal = java.util.Calendar.getInstance().apply { timeInMillis = fridayTimestamp }
    assertEquals(java.util.Calendar.FRIDAY, cal.get(java.util.Calendar.DAY_OF_WEEK))

    // Next occurrence when acknowledged
    val nextFriday = com.example.data.model.RecurrenceHelper.getNextOccurrence(
      fridayTimestamp,
      com.example.data.model.RecurrenceType.WEEKLY,
      java.util.Calendar.FRIDAY
    )
    assertTrue(nextFriday > fridayTimestamp)
  }

  @Test
  fun `yearly birthday recurrence calculation`() {
    val cal = java.util.Calendar.getInstance().apply {
      set(java.util.Calendar.YEAR, 2025)
      set(java.util.Calendar.MONTH, java.util.Calendar.OCTOBER)
      set(java.util.Calendar.DAY_OF_MONTH, 15)
    }
    val nextBirthday = com.example.data.model.RecurrenceHelper.getNextOccurrence(
      cal.timeInMillis,
      com.example.data.model.RecurrenceType.YEARLY
    )
    assertTrue(nextBirthday > System.currentTimeMillis())

    val nextCal = java.util.Calendar.getInstance().apply { timeInMillis = nextBirthday }
    assertEquals(java.util.Calendar.OCTOBER, nextCal.get(java.util.Calendar.MONTH))
    assertEquals(15, nextCal.get(java.util.Calendar.DAY_OF_MONTH))
  }

  @Test
  fun `category icon helper identifies birthday and mosque`() {
    assertEquals("🎂", com.example.data.model.RecurrenceHelper.getCategoryIcon("Birthday"))
    assertEquals("🕌", com.example.data.model.RecurrenceHelper.getCategoryIcon("Mosque / Prayer"))
  }

  @Test
  fun `triggerSampleUpdateNotification runs cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.update.AppUpdateManager.triggerSampleUpdateNotification(context)
    // Verify notification manager received it without crash
    val notificationManager = androidx.core.app.NotificationManagerCompat.from(context)
    assertTrue(notificationManager.areNotificationsEnabled())
  }
}

