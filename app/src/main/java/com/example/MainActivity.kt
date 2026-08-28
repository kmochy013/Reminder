package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.notification.NotificationHelper
import com.example.ui.screens.ReminderListScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ReminderViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ReminderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create the high priority notification channel
        NotificationHelper.createNotificationChannel(this)

        // Initialize update state from storage
        com.example.update.AppUpdateManager.initUpdateState(this)

        // Schedule periodic update checks & check once on launch
        com.example.update.AppUpdateManager.schedulePeriodicUpdateCheck(this)
        com.example.update.AppUpdateManager.checkForUpdates(
            context = this,
            notifyIfAvailable = true,
            onResult = { /* Handled via notification & dialog */ }
        )

        setContent {
            MyApplicationTheme {
                ReminderListScreen(viewModel = viewModel)
            }
        }
    }
}
