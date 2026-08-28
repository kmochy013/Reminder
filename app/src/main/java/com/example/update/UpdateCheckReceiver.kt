package com.example.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UpdateCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Run update check in background and post notification if new update is found
        AppUpdateManager.checkForUpdates(
            context = context,
            notifyIfAvailable = true,
            onResult = { /* Handled via notification */ }
        )
    }
}
