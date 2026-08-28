# 🔔 Reminder — Smart Persistent Task & Alarm Manager

A modern, high-precision Android reminder and task management application built entirely with **Kotlin** and **Jetpack Compose**. Engineered with a proactive alerting engine that ensures you never miss critical tasks by delivering high-priority heads-up notifications with automated 1-minute repeat cycles until explicitly acknowledged.

---

## ✨ Features

- ⏰ **Precise Alarm Scheduling**: Schedule tasks and alerts down to the exact second using Android's `AlarmManager` with `setExactAndAllowWhileIdle` support.
- 🔁 **Smart 1-Minute Repeat Loop**: If a reminder fires and remains unread, the app automatically re-alerts every 60 seconds with a repetition counter until you check it off.
- 🕌 **Recurring Events & Custom Schedules**: Built-in support for flexible repeating patterns:
  - **Every Friday (Mosque / Jumu'ah)**: Never miss Friday prayers or weekly gatherings.
  - **🎂 Yearly Birthdays & Anniversaries**: Automatically reschedules for next year upon acknowledgment.
  - **Daily & Monthly Cycles**: For medication routines, daily habits, and monthly bill payments.
- 🚀 **Automatic App Updates & Mandatory Enforcement**: 
  - **Auto-Update Notifications**: Every device where the app is installed automatically checks for updates in the background (on device reboot, periodic schedule, and app startup) and delivers a high-priority system notification when a new version is released.
  - **Mandatory Update Screen**: Updating is strictly required to continue using the application. Outdated versions are securely locked with an un-dismissible **App Update Required** screen that guides users directly to download and install the new release, preventing broken reminders or outdated alarm engines.
  - **One-Tap Update Simulation**: Built-in test trigger in the Update Center allows instant testing of both the auto-notification and the mandatory lock screen directly on device.
- 📬 **Interactive Heads-Up Notifications**: Rich system notifications featuring quick actions:
  - **✓ Mark as Read** — Instantly acknowledges the task and dismisses recurring alarms.
  - **⏰ Snooze (1 Min)** — Temporarily postpones the reminder for a quick grace period.
- 🎨 **Elegant Dark UI**: Designed according to Material Design 3 guidelines with deep charcoal surfaces, vibrant lavender highlights, and clear visual hierarchies.
- 🏷️ **Categorization & Priority Tags**: Organize reminders across categories (*Mosque*, *Birthday*, *Work*, *Personal*, *Health*, *Study*, *Finance*, *Urgent*) with dedicated emojis and 4 priority tiers (*Low*, *Medium*, *High*, *Urgent*).
- 🔍 **Fast Search & Filtering**: Real-time keyword filtering, status tabs (*All*, *Upcoming*, *Active Alerts*, *Read / Done*), and dynamic category chips.
- ⚡ **Instant Test Alert**: Built-in 3-second simulation trigger to test notification banners, sounds, and repeat behaviors on device.
- 🔒 **100% Offline & Private**: All data is securely stored on-device using SQLite via Room Database. No account creation or external server tracking required.
- 🔄 **Boot & Reboot Resilience**: Registered `BOOT_COMPLETED` receiver automatically reschedules all active alarms if the device restarts.

---


## 🛠️ Tech Stack & Architecture

This project follows modern Android architecture principles (**MVVM / Clean Architecture**):

- **Language**: [Kotlin](https://kotlinlang.org/) (100%)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **State Management**: `StateFlow`, `SharedFlow`, `ViewModel`, `collectAsStateWithLifecycle`
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) with Kotlin Coroutines & Flow
- **Background & Alarms**: `AlarmManager`, `BroadcastReceiver`, `NotificationManagerCompat`
- **Asynchronous Programming**: Kotlin Coroutines & Flow
- **Build System**: Gradle Kotlin DSL (`.gradle.kts`) with Version Catalog (`libs.versions.toml`)

---

## 📂 Project Structure

```text
com.example / com.aistudio.reminder.appkz
├── data/
│   ├── local/
│   │   ├── ReminderDao.kt         # Room Data Access Object (Reactive queries & updates)
│   │   └── ReminderDatabase.kt    # Room Database instance configuration
│   ├── model/
│   │   ├── Priority.kt            # Task priority enum (Urgent, High, Med, Low)
│   │   └── ReminderItem.kt        # Reminder entity model
│   └── repository/
│       └── ReminderRepository.kt  # Clean repository layer abstracting data sources
├── notification/
│   ├── AlarmScheduler.kt          # Exact alarm scheduling & repeating logic
│   ├── NotificationHelper.kt      # High-priority notification channels & builders
│   └── ReminderAlarmReceiver.kt   # System broadcast receiver for alarm triggers & actions
├── ui/
│   ├── components/
│   │   ├── FilterTabs.kt          # Search bar, tab selector & category filter chips
│   │   ├── PermissionRationaleCard.kt # Runtime notification permission request card
│   │   ├── QuickStatsHeader.kt    # Overview stat summary cards (Completed / Active alerts)
│   │   └── ReminderCard.kt        # Task card with checkbox, timer, and action buttons
│   ├── screens/
│   │   ├── AddEditReminderDialog.kt # Task creation dialog with date/time pickers
│   │   └── ReminderListScreen.kt  # Main dashboard screen
│   ├── theme/
│   │   ├── Color.kt               # Elegant Dark theme palette
│   │   ├── Theme.kt               # Dynamic & Material3 color schemes
│   │   └── Type.kt                # Typography system
│   └── viewmodel/
│       └── ReminderViewModel.kt   # UI state holder with reactive filters & actions
└── MainActivity.kt                # Edge-to-edge Compose container & notification init
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Ladybug / Hedgehog or newer
- **JDK**: Version 17 or higher
- **Android SDK**: `minSdk = 24` (Android 7.0+), `targetSdk = 36`

### Installation & Build

1. **Clone the repository**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/reminder-app.git
   cd reminder-app
   ```

2. **Open in Android Studio**:
   - Launch Android Studio and choose **Open an Existing Project**.
   - Select the cloned project root folder.

3. **Sync Gradle**:
   - Allow Gradle to download dependencies and sync the project.

4. **Run on Device / Emulator**:
   - Connect an Android device with USB Debugging enabled, or start an Android Virtual Device (AVD).
   - Click the **Run ▶** button (`Shift + F10`).

---

## 🔑 Permissions Used

- `android.permission.POST_NOTIFICATIONS`: Required on Android 13+ to display notification alerts.
- `android.permission.SCHEDULE_EXACT_ALARM` & `USE_EXACT_ALARM`: Enables precise alarm delivery.
- `android.permission.VIBRATE`: Provides haptic feedback when alerts ring.
- `android.permission.RECEIVE_BOOT_COMPLETED`: Automatically restores scheduled reminders upon device reboot.
- `android.permission.WAKE_LOCK`: Ensures the device processes alarms when in doze or sleep state.

---

## 📄 License

```text
MIT License

Copyright (c) 2026

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
