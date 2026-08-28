package com.example.update

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AppVersionInfo(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val isMandatory: Boolean = false
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val versionInfo: AppVersionInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object AppUpdateManager {

    const val CURRENT_VERSION_NAME = "1.0"
    const val CURRENT_VERSION_CODE = 1

    private const val PREFS_NAME = "reminder_update_prefs"
    private const val KEY_GITHUB_REPO = "github_repo" // e.g. "username/reminder-app"
    private const val KEY_CUSTOM_FEED_URL = "custom_feed_url"
    private const val KEY_LAST_CHECK_TIME = "last_check_time"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getGitHubRepo(context: Context): String {
        return getPrefs(context).getString(KEY_GITHUB_REPO, "reminder-app/release") ?: "reminder-app/release"
    }

    fun setGitHubRepo(context: Context, repo: String) {
        getPrefs(context).edit().putString(KEY_GITHUB_REPO, repo.trim()).apply()
    }

    fun getCustomFeedUrl(context: Context): String {
        return getPrefs(context).getString(KEY_CUSTOM_FEED_URL, "") ?: ""
    }

    fun setCustomFeedUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_CUSTOM_FEED_URL, url.trim()).apply()
    }

    /**
     * Checks remote source (GitHub Releases or direct JSON feed) for a newer version.
     * When a newer version is detected, it posts a high-priority system notification.
     */
    fun checkForUpdates(
        context: Context,
        notifyIfAvailable: Boolean = true,
        onResult: (UpdateCheckResult) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val customUrl = getCustomFeedUrl(context)
                val repo = getGitHubRepo(context)

                val targetUrl = if (customUrl.isNotBlank()) {
                    customUrl
                } else {
                    "https://api.github.com/repos/$repo/releases/latest"
                }

                val request = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "ReminderApp-Android")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    val code = response.code
                    withContext(Dispatchers.Main) {
                        onResult(
                            UpdateCheckResult.Error(
                                if (code == 404) "No releases found on GitHub repo ($repo). Make sure the repository exists and has a release."
                                else "Server returned HTTP $code while checking for updates."
                            )
                        )
                    }
                    return@launch
                }

                val bodyString = response.body?.string() ?: ""
                val json = JSONObject(bodyString)

                // Parse GitHub release format or standard version JSON
                val tag = json.optString("tag_name", "").removePrefix("v")
                val releaseNotes = json.optString("body", "Bug fixes and performance improvements.")
                val htmlUrl = json.optString("html_url", "https://github.com/$repo/releases")

                // Extract APK download URL if assets exist
                var downloadUrl = htmlUrl
                val assets = json.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", htmlUrl)
                            break
                        }
                    }
                }

                // Check version comparison
                val latestVersionCode = json.optInt("version_code", parseVersionCode(tag))
                val latestVersionName = if (tag.isNotBlank()) tag else json.optString("version_name", "1.1")

                getPrefs(context).edit().putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis()).apply()

                if (latestVersionCode > CURRENT_VERSION_CODE) {
                    val info = AppVersionInfo(
                        versionCode = latestVersionCode,
                        versionName = latestVersionName,
                        releaseNotes = releaseNotes,
                        downloadUrl = downloadUrl
                    )

                    if (notifyIfAvailable) {
                        NotificationHelper.showUpdateNotification(
                            context = context,
                            versionName = info.versionName,
                            releaseNotes = info.releaseNotes,
                            downloadUrl = info.downloadUrl
                        )
                    }

                    withContext(Dispatchers.Main) {
                        onResult(UpdateCheckResult.UpdateAvailable(info))
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(UpdateCheckResult.UpToDate(CURRENT_VERSION_NAME))
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(UpdateCheckResult.Error("Could not check updates: ${e.localizedMessage ?: "Network error"}"))
                }
            }
        }
    }

    private fun parseVersionCode(tag: String): Int {
        return try {
            val parts = tag.split(".").mapNotNull { it.toIntOrNull() }
            when (parts.size) {
                1 -> parts[0]
                2 -> parts[0] * 100 + parts[1]
                3 -> parts[0] * 10000 + parts[1] * 100 + parts[2]
                else -> 1
            }
        } catch (e: Exception) {
            1
        }
    }

    /**
     * Broadcasts / posts a sample update notification so the developer and user can
     * immediately experience how the update notification looks and works.
     */
    fun triggerSampleUpdateNotification(context: Context) {
        val sampleInfo = AppVersionInfo(
            versionCode = CURRENT_VERSION_CODE + 1,
            versionName = "1.1.0",
            releaseNotes = "• Added Mosque & Birthday recurring reminders\n• Improved repeating alert reliability\n• Custom alert ringtones\n• UI performance polish",
            downloadUrl = "https://github.com/${getGitHubRepo(context)}/releases"
        )

        NotificationHelper.showUpdateNotification(
            context = context,
            versionName = sampleInfo.versionName,
            releaseNotes = sampleInfo.releaseNotes,
            downloadUrl = sampleInfo.downloadUrl
        )
    }

    /**
     * Schedules a daily background update check via AlarmManager so installed phones
     * automatically check and notify when the developer publishes an update.
     */
    fun schedulePeriodicUpdateCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, UpdateCheckReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            888101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + 12 * 3600_000L // check in 12h
        val interval = AlarmManager.INTERVAL_DAY

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            interval,
            pendingIntent
        )
    }
}
