package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.Priority
import com.example.data.model.ReminderItem
import com.example.ui.components.ReminderCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleReminder = ReminderItem(
      id = 1,
      title = "Take Daily Medicine",
      description = "Take 1 tablet after lunch. Will alert until marked read.",
      targetTimestamp = System.currentTimeMillis() + 3600000L,
      category = "Health",
      priority = Priority.HIGH,
      isRead = false
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        ReminderCard(
          reminder = sampleReminder,
          onToggleRead = {},
          onEdit = {},
          onDelete = {},
          onSnooze = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

