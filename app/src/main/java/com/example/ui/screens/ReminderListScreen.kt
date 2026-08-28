package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Priority
import com.example.data.model.ReminderItem
import androidx.compose.material.icons.filled.SystemUpdate
import com.example.ui.screens.UpdateDialog
import com.example.ui.screens.MandatoryUpdateScreen
import com.example.ui.components.FilterTabs
import com.example.ui.components.PermissionRationaleCard
import com.example.ui.components.QuickStatsHeader
import com.example.ui.components.ReminderCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.PrimaryGradientEnd
import com.example.ui.theme.PrimaryGradientStart
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.FilterTab
import com.example.ui.viewmodel.ReminderViewModel
import com.example.update.AppUpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    viewModel: ReminderViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mandatoryUpdate by AppUpdateManager.mandatoryUpdateFlow.collectAsStateWithLifecycle()

    if (mandatoryUpdate != null) {
        MandatoryUpdateScreen(
            versionInfo = mandatoryUpdate!!,
            onDismissSimulation = {
                AppUpdateManager.clearMandatoryUpdate(context)
            }
        )
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var reminderToEdit by remember { mutableStateOf<ReminderItem?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(PrimaryGradientStart, PrimaryGradientEnd)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Reminders",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (uiState.naggingCount > 0) "${uiState.naggingCount} Active Nag Alert(s)" else "Smart 1-Min Repeating Alerts",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.naggingCount > 0) AlertRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Check for App Updates button
                    IconButton(
                        onClick = { showUpdateDialog = true },
                        modifier = Modifier.testTag("check_updates_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Check for App Updates",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Quick Test Alert button
                    Button(
                        onClick = {
                            viewModel.testInstantReminder("⏰ Test Reminder (Repeats 1m)")
                            scope.launch {
                                snackbarHostState.showSnackbar("Test alert scheduled for 3s! Pop notification will appear.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("test_alert_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Test Notification",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Test Alert", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    reminderToEdit = null
                    showAddEditDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add reminder") },
                text = { Text("New Reminder", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_reminder_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Notification Permission Card
            PermissionRationaleCard()

            // Quick Stats Counter Cards
            QuickStatsHeader(
                uiState = uiState,
                onTabSelected = { viewModel.selectTab(it) }
            )

            // Search, Status tabs & Category filters
            FilterTabs(
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.selectTab(it) },
                categories = uiState.availableCategories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) },
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) }
            )

            // Reminders List or Empty State
            if (uiState.reminders.isEmpty()) {
                EmptyStateView(
                    selectedTab = uiState.selectedTab,
                    onAddClick = {
                        reminderToEdit = null
                        showAddEditDialog = true
                    },
                    onTestClick = {
                        viewModel.testInstantReminder("💧 Drink Water Reminder")
                        scope.launch {
                            snackbarHostState.showSnackbar("Test alert scheduled for 3s!")
                        }
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("reminders_list"),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                ) {
                    items(
                        items = uiState.reminders,
                        key = { it.id }
                    ) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onToggleRead = { isRead ->
                                viewModel.toggleReadStatus(reminder, isRead)
                                scope.launch {
                                    val msg = if (isRead) {
                                        if (reminder.isRecurring) {
                                            val label = com.example.data.model.RecurrenceHelper.getRecurrenceLabel(reminder.recurrence, reminder.repeatDayOfWeek)
                                            "✓ Acknowledged for today! Rescheduled for next cycle ($label)"
                                        } else {
                                            "✓ Marked as read (repeat alerts stopped)"
                                        }
                                    } else {
                                        "Marked as unread"
                                    }
                                    snackbarHostState.showSnackbar(msg)
                                }
                            },
                            onEdit = {
                                reminderToEdit = reminder
                                showAddEditDialog = true
                            },
                            onDelete = {
                                viewModel.deleteReminder(reminder)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Reminder deleted")
                                }
                            },
                            onSnooze = { minutes ->
                                viewModel.snoozeReminder(reminder.id, minutes)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Snoozed for $minutes min")
                                }
                            }
                        )
                    }
                }
            }
        }

        // Add/Edit Dialog
        if (showAddEditDialog) {
            AddEditReminderDialog(
                reminderToEdit = reminderToEdit,
                onDismiss = {
                    showAddEditDialog = false
                    reminderToEdit = null
                },
                onSave = { title, description, targetTimestamp, category, priority, recurrence, repeatDayOfWeek ->
                    if (reminderToEdit != null) {
                        viewModel.updateReminder(
                            reminderToEdit!!.copy(
                                title = title,
                                description = description,
                                targetTimestamp = targetTimestamp,
                                category = category,
                                priority = priority,
                                recurrence = recurrence,
                                repeatDayOfWeek = repeatDayOfWeek,
                                isRead = false
                            )
                        )
                        scope.launch { snackbarHostState.showSnackbar("Reminder updated") }
                    } else {
                        viewModel.addReminder(title, description, targetTimestamp, category, priority, recurrence, repeatDayOfWeek)
                        val recurMsg = if (recurrence != com.example.data.model.RecurrenceType.NONE) {
                            " (${com.example.data.model.RecurrenceHelper.getRecurrenceLabel(recurrence, repeatDayOfWeek)})"
                        } else ""
                        scope.launch { snackbarHostState.showSnackbar("Reminder scheduled!$recurMsg") }
                    }
                    showAddEditDialog = false
                    reminderToEdit = null
                }
            )
        }

        if (showUpdateDialog) {
            UpdateDialog(
                onDismiss = { showUpdateDialog = false },
                onNotificationSent = { msg ->
                    scope.launch {
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            )
        }
    }
}

@Composable
private fun EmptyStateView(
    selectedTab: FilterTab,
    onAddClick: () -> Unit,
    onTestClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("empty_state_view"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (selectedTab) {
                            FilterTab.NAGGING -> Icons.Default.CheckCircle
                            FilterTab.COMPLETED -> Icons.Default.CheckCircle
                            else -> Icons.Outlined.Alarm
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = when (selectedTab) {
                        FilterTab.NAGGING -> "No Active Repeating Alerts"
                        FilterTab.COMPLETED -> "No Completed Reminders Yet"
                        FilterTab.UPCOMING -> "No Upcoming Reminders"
                        FilterTab.ALL -> "No Reminders Found"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = when (selectedTab) {
                        FilterTab.NAGGING -> "All current reminders have been checked and acknowledged! Great job."
                        FilterTab.COMPLETED -> "Check off reminders when done. If left unchecked, they will repeat every 1 minute."
                        else -> "Create a reminder with date and time. When it alerts, it will pop on your screen and repeat every 1 minute until you mark it as read."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onTestClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("empty_state_test_button")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Instant Test (3s)")
                    }

                    Button(
                        onClick = onAddClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("empty_state_add_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Add Reminder")
                    }
                }
            }
        }
    }
}
