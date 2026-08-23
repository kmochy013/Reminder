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
}

