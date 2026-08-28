package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.example.ui.theme.AlertRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.update.AppUpdateManager
import com.example.update.AppVersionInfo
import com.example.update.UpdateCheckResult

@Composable
fun UpdateDialog(
    onDismiss: () -> Unit,
    onNotificationSent: (String) -> Unit
) {
    val context = LocalContext.current
    var isChecking by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var showGuide by remember { mutableStateOf(false) }
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    // Automatically perform a live check upon opening the dialog
    LaunchedEffect(Unit) {
        isChecking = true
        AppUpdateManager.checkForUpdates(
            context = context,
            notifyIfAvailable = true
        ) { res ->
            isChecking = false
            updateResult = res
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp)
                .testTag("update_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "App Updates",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Installed: v${AppUpdateManager.CURRENT_VERSION_NAME} • Built-in Auto Update",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_update_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Device Notification Permission Warning (if blocked)
                if (!notificationsEnabled) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AlertRed.copy(alpha = 0.12f)
                        ),
                        border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = AlertRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Notifications Disabled on Device",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertRed
                                )
                            }
                            Text(
                                text = "Notifications are disabled. Enable them to get automatic heads-up alerts whenever a new version is released.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = {
                                    val intent = Intent().apply {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        } else {
                                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enable Notifications in Settings", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Status Banner / Result Card
                when (val result = updateResult) {
                    null -> {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Checking for new releases on GitHub...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    is UpdateCheckResult.UpdateAvailable -> {
                        UpdateAvailableCard(
                            version = result.versionInfo,
                            onDownload = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.versionInfo.downloadUrl)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        )
                    }

                    is UpdateCheckResult.UpToDate -> {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = SuccessGreen.copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "App is Up to Date!",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SuccessGreen
                                        )
                                        Text(
                                            text = "Running v${result.currentVersion} • Built-in Auto Updater Active",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "Whenever you publish a newer release (e.g. tag 'v1.1' or 'v1.2') with an APK on GitHub, this app and all installed devices will automatically display a notification and lock screen to update immediately.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is UpdateCheckResult.Error -> {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "Auto Update Ready",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = result.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )

                                Button(
                                    onClick = {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://github.com/${AppUpdateManager.DEFAULT_GITHUB_REPO}/releases")
                                        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.OpenInBrowser,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open GitHub Releases", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Primary Action: Check for Updates Button
                Button(
                    onClick = {
                        isChecking = true
                        AppUpdateManager.checkForUpdates(
                            context = context,
                            notifyIfAvailable = true
                        ) { res ->
                            isChecking = false
                            updateResult = res
                        }
                    },
                    enabled = !isChecking,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("check_for_updates_button")
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking Releases...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check for Updates Now")
                    }
                }

                // Test Mandatory Update Notification & Lock Button
                Button(
                    onClick = {
                        AppUpdateManager.simulateMandatoryUpdate(context)
                        onDismiss()
                        onNotificationSent("🚨 Mandatory Update triggered! System notification sent & screen locked.")
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlertRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_mandatory_update_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Test Update Notification & Lock Screen",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // How to publish updates guide
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showGuide = !showGuide },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "How do auto-updates work?",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = if (showGuide) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (showGuide) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "1. Everything is built-in: The app is directly wired to your GitHub repository.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "2. When you want to release an update, publish a release tag (e.g. 'v1.1') with your APK at github.com/${AppUpdateManager.DEFAULT_GITHUB_REPO}/releases.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "3. Installed devices automatically check in the background. As soon as a newer version is seen, a high-priority update notification is shown and the mandatory update screen prompts the user to download the update.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateAvailableCard(
    version: AppVersionInfo,
    onDownload: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "🚀 New Version v${version.versionName} Available!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (version.releaseNotes.isNotBlank()) {
                Text(
                    text = version.releaseNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = onDownload,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Download & Install Update")
            }
        }
    }
}

