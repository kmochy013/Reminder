package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Priority
import com.example.data.model.RecurrenceHelper
import com.example.data.model.RecurrenceType
import com.example.data.model.ReminderItem
import com.example.ui.components.formatScheduledTime
import com.example.ui.theme.AlertRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AddEditReminderDialog(
    reminderToEdit: ReminderItem? = null,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        targetTimestamp: Long,
        category: String,
        priority: Priority,
        recurrence: RecurrenceType,
        repeatDayOfWeek: Int?
    ) -> Unit
) {
    val context = LocalContext.current
    val isEdit = reminderToEdit != null

    var title by remember { mutableStateOf(reminderToEdit?.title ?: "") }
    var description by remember { mutableStateOf(reminderToEdit?.description ?: "") }
    var category by remember { mutableStateOf(reminderToEdit?.category ?: "General") }
    var priority by remember { mutableStateOf(reminderToEdit?.priority ?: Priority.MEDIUM) }
    var recurrence by remember { mutableStateOf(reminderToEdit?.recurrence ?: RecurrenceType.NONE) }
    var repeatDayOfWeek by remember {
        mutableStateOf<Int?>(
            reminderToEdit?.repeatDayOfWeek ?: Calendar.FRIDAY
        )
    }

    var targetTimestamp by remember {
        mutableLongStateOf(
            reminderToEdit?.targetTimestamp ?: (System.currentTimeMillis() + 60_000L) // default: 1 min ahead
        )
    }

    var titleError by remember { mutableStateOf(false) }
    var isCustomCategoryMode by remember { mutableStateOf(false) }
    var customCategoryText by remember { mutableStateOf("") }

    val presetCategories = listOf(
        "Birthday",
        "Mosque / Prayer",
        "General",
        "Work",
        "Personal",
        "Health",
        "Study",
        "Finance",
        "Urgent"
    )

    val weekDays = listOf(
        "Mon" to Calendar.MONDAY,
        "Tue" to Calendar.TUESDAY,
        "Wed" to Calendar.WEDNESDAY,
        "Thu" to Calendar.THURSDAY,
        "Fri" to Calendar.FRIDAY,
        "Sat" to Calendar.SATURDAY,
        "Sun" to Calendar.SUNDAY
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("add_edit_reminder_dialog"),
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
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (isEdit) "Edit Reminder" else "New Reminder",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Quick Suggestion Templates (Mosque Friday, Birthday, Water, etc.)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Quick Suggestion Templates:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                title = "Go to Mosque (Jumu'ah Prayer)"
                                category = "Mosque / Prayer"
                                priority = Priority.HIGH
                                recurrence = RecurrenceType.WEEKLY
                                repeatDayOfWeek = Calendar.FRIDAY
                                targetTimestamp = RecurrenceHelper.getUpcomingDayOfWeekTimestamp(Calendar.FRIDAY, 12, 45)
                                description = "Prepare for Jumu'ah prayer & Friday sermon at the mosque."
                                titleError = false
                            },
                            label = { Text("🕌 Friday Mosque (Jumu'ah)", fontSize = 12.sp) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("suggestion_chip_friday_mosque")
                        )

                        FilterChip(
                            selected = false,
                            onClick = {
                                title = "Birthday Celebration"
                                category = "Birthday"
                                priority = Priority.MEDIUM
                                recurrence = RecurrenceType.YEARLY
                                description = "Wish happy birthday, gift & celebrate! (Repeats every year)"
                                titleError = false
                            },
                            label = { Text("🎂 Birthday Reminder", fontSize = 12.sp) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("suggestion_chip_birthday")
                        )

                        FilterChip(
                            selected = false,
                            onClick = {
                                title = "Drink Water & Hydrate"
                                category = "Health"
                                priority = Priority.LOW
                                recurrence = RecurrenceType.NONE
                                targetTimestamp = System.currentTimeMillis() + 30 * 60_000L
                                titleError = false
                            },
                            label = { Text("💧 Drink Water", fontSize = 12.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )

                        FilterChip(
                            selected = false,
                            onClick = {
                                title = "Daily Medication"
                                category = "Health"
                                priority = Priority.HIGH
                                recurrence = RecurrenceType.DAILY
                                titleError = false
                            },
                            label = { Text("💊 Daily Medicine", fontSize = 12.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )

                        FilterChip(
                            selected = false,
                            onClick = {
                                title = "Pay Monthly Bills"
                                category = "Finance"
                                priority = Priority.MEDIUM
                                recurrence = RecurrenceType.MONTHLY
                                titleError = false
                            },
                            label = { Text("⚡ Pay Bills", fontSize = 12.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Title Input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) titleError = false
                        },
                        label = { Text("Reminder Title *") },
                        placeholder = { Text("e.g. Go to Mosque, Rahim's Birthday, Doctor Appt") },
                        isError = titleError,
                        supportingText = if (titleError) {
                            { Text("Title cannot be empty", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reminder_title_input")
                    )
                }

                // Description (Optional)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Note / Description (Optional)") },
                    placeholder = { Text("Add any details, location, gifts, or instructions...") },
                    shape = RoundedCornerShape(14.dp),
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_description_input")
                )

                // Date and Time Section
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "📅 Schedule Date & Time",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Scheduled Time Display
                        Text(
                            text = "Set for: ${formatScheduledTime(targetTimestamp)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Quick Presets
                        Text(
                            text = "Quick Presets:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuickTimeChip("+1 min (Test)") {
                                targetTimestamp = System.currentTimeMillis() + 60_000L
                            }
                            QuickTimeChip("+15 mins") {
                                targetTimestamp = System.currentTimeMillis() + 15 * 60_000L
                            }
                            QuickTimeChip("+1 hour") {
                                targetTimestamp = System.currentTimeMillis() + 60 * 60_000L
                            }
                            QuickTimeChip("Tonight (8 PM)") {
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 20)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    if (timeInMillis <= System.currentTimeMillis()) {
                                        add(Calendar.DAY_OF_YEAR, 1)
                                    }
                                }
                                targetTimestamp = cal.timeInMillis
                            }
                            QuickTimeChip("This Friday 12:45 PM") {
                                targetTimestamp = RecurrenceHelper.getUpcomingDayOfWeekTimestamp(Calendar.FRIDAY, 12, 45)
                                recurrence = RecurrenceType.WEEKLY
                                repeatDayOfWeek = Calendar.FRIDAY
                            }
                            QuickTimeChip("Tomorrow 9 AM") {
                                val cal = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                    set(Calendar.HOUR_OF_DAY, 9)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                }
                                targetTimestamp = cal.timeInMillis
                            }
                        }

                        // Custom Pickers Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = targetTimestamp }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val updated = Calendar.getInstance().apply {
                                                timeInMillis = targetTimestamp
                                                set(Calendar.YEAR, year)
                                                set(Calendar.MONTH, month)
                                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                            }
                                            targetTimestamp = updated.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("select_date_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Pick Date")
                            }

                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = targetTimestamp }
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            val updated = Calendar.getInstance().apply {
                                                timeInMillis = targetTimestamp
                                                set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                set(Calendar.MINUTE, minute)
                                                set(Calendar.SECOND, 0)
                                            }
                                            targetTimestamp = updated.timeInMillis
                                        },
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("select_time_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Pick Time")
                            }
                        }
                    }
                }

                // Recurrence / Repetition Section
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "🔁 Repeat Frequency",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Recurrence Options
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = recurrence == RecurrenceType.NONE,
                                onClick = { recurrence = RecurrenceType.NONE },
                                label = { Text("Does Not Repeat (Once)") },
                                shape = RoundedCornerShape(10.dp)
                            )

                            FilterChip(
                                selected = recurrence == RecurrenceType.WEEKLY,
                                onClick = {
                                    recurrence = RecurrenceType.WEEKLY
                                    if (repeatDayOfWeek == null) repeatDayOfWeek = Calendar.FRIDAY
                                },
                                label = { Text("Weekly (e.g. Every Friday)") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("recurrence_chip_weekly")
                            )

                            FilterChip(
                                selected = recurrence == RecurrenceType.YEARLY,
                                onClick = { recurrence = RecurrenceType.YEARLY },
                                label = { Text("🎂 Yearly (Birthday / Annual)") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("recurrence_chip_yearly")
                            )

                            FilterChip(
                                selected = recurrence == RecurrenceType.DAILY,
                                onClick = { recurrence = RecurrenceType.DAILY },
                                label = { Text("Daily (Every Day)") },
                                shape = RoundedCornerShape(10.dp)
                            )

                            FilterChip(
                                selected = recurrence == RecurrenceType.MONTHLY,
                                onClick = { recurrence = RecurrenceType.MONTHLY },
                                label = { Text("Monthly") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        // Day of Week Selector if Weekly
                        if (recurrence == RecurrenceType.WEEKLY) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Select Day of Week:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    weekDays.forEach { (name, dayVal) ->
                                        val isSelected = (repeatDayOfWeek ?: Calendar.FRIDAY) == dayVal
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                repeatDayOfWeek = dayVal
                                                // Adjust targetTimestamp to the upcoming day of week
                                                val cal = Calendar.getInstance().apply { timeInMillis = targetTimestamp }
                                                val h = cal.get(Calendar.HOUR_OF_DAY)
                                                val m = cal.get(Calendar.MINUTE)
                                                targetTimestamp = RecurrenceHelper.getUpcomingDayOfWeekTimestamp(dayVal, h, m)
                                            },
                                            label = {
                                                Text(
                                                    text = if (dayVal == Calendar.FRIDAY) "🕌 $name" else name,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            modifier = Modifier.testTag("weekday_chip_${name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }

                        // Explanatory badge based on chosen recurrence
                        val helperNote = when (recurrence) {
                            RecurrenceType.NONE -> "• Alert triggers once at the scheduled date and time."
                            RecurrenceType.WEEKLY -> {
                                val label = RecurrenceHelper.getRecurrenceLabel(recurrence, repeatDayOfWeek)
                                "🔁 $label: Once marked as read, this reminder automatically reschedules for the following week at the same time!"
                            }
                            RecurrenceType.YEARLY -> "🎂 Yearly Birthday/Annual: When celebrated and marked read, it automatically advances to next year's date!"
                            RecurrenceType.DAILY -> "🔁 Daily: Automatically advances to tomorrow at the same time when marked read."
                            RecurrenceType.MONTHLY -> "🔁 Monthly: Automatically advances to next month on this day when marked read."
                        }

                        Text(
                            text = helperNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Category Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (!isCustomCategoryMode) {
                            OutlinedButton(
                                onClick = { isCustomCategoryMode = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Custom", fontSize = 11.sp)
                            }
                        }
                    }

                    if (isCustomCategoryMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customCategoryText,
                                onValueChange = { customCategoryText = it },
                                label = { Text("New Category Name") },
                                placeholder = { Text("e.g. Quran Study, Fitness") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (customCategoryText.isNotBlank()) {
                                        category = customCategoryText.trim()
                                        isCustomCategoryMode = false
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Use")
                            }
                            IconButton(onClick = { isCustomCategoryMode = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Custom")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetCategories.forEach { cat ->
                            val icon = RecurrenceHelper.getCategoryIcon(cat)
                            val isSelected = category.equals(cat, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    category = cat
                                    // Intelligent defaults
                                    if (cat == "Birthday" && recurrence == RecurrenceType.NONE) {
                                        recurrence = RecurrenceType.YEARLY
                                    } else if (cat == "Mosque / Prayer" && recurrence == RecurrenceType.NONE) {
                                        recurrence = RecurrenceType.WEEKLY
                                        repeatDayOfWeek = Calendar.FRIDAY
                                    }
                                },
                                label = { Text("$icon $cat") },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.testTag("category_preset_${cat.take(5).trim().lowercase()}")
                            )
                        }
                    }
                }

                // Priority Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Priority Level",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Priority.values().forEach { prio ->
                            FilterChip(
                                selected = priority == prio,
                                onClick = { priority = prio },
                                label = { Text(prio.displayName, fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("priority_chip_${prio.name.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (prio) {
                                        Priority.URGENT -> AlertRed.copy(alpha = 0.2f)
                                        Priority.HIGH -> MaterialTheme.colorScheme.secondaryContainer
                                        Priority.MEDIUM -> MaterialTheme.colorScheme.primaryContainer
                                        Priority.LOW -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            )
                        }
                    }
                }

                // 1-Minute Repeat Guarantee Notice Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "🔔 Active Alert: When the scheduled time arrives, you'll receive a heads-up notification. If not marked as read, it will repeat every 1 minute until acknowledged.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Save and Cancel Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("cancel_save_reminder_button")
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                            } else {
                                onSave(
                                    title,
                                    description,
                                    targetTimestamp,
                                    category,
                                    priority,
                                    recurrence,
                                    repeatDayOfWeek
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("confirm_save_reminder_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(if (isEdit) "Save Changes" else "Create Reminder")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTimeChip(
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
