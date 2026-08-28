package com.example.update

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class AppVersionInfo(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val isMandatory: Boolean = true
)

data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"
}

sealed class UpdateCheckResult {
    data class UpdateAvailable(val versionInfo: AppVersionInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String, val latestTag: String = currentVersion) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object AppUpdateManager {

    const val CURRENT_VERSION_NAME = "1.0.0"
    const val CURRENT_VERSION_CODE = 1

    private const val PREFS_NAME = "reminder_update_prefs"
    private const val KEY_GITHUB_REPO = "github_repo" // e.g. "username/reminder-app"
    private const val KEY_CUSTOM_FEED_URL = "custom_feed_url"
    private const val KEY_LAST_CHECK_TIME = "last_check_time"

    private const val KEY_MANDATORY_ACTIVE = "mandatory_update_active"
    private const val KEY_MANDATORY_VERSION_CODE = "mandatory_version_code"
    private const val KEY_MANDATORY_VERSION_NAME = "mandatory_version_name"
    private const val KEY_MANDATORY_NOTES = "mandatory_notes"
    private const val KEY_MANDATORY_URL = "mandatory_url"

    private val _mandatoryUpdateFlow = MutableStateFlow<AppVersionInfo?>(null)
    val mandatoryUpdateFlow: StateFlow<AppVersionInfo?> = _mandatoryUpdateFlow.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    fun parseSemVer(versionString: String): SemVer {
        val clean = versionString.trim().removePrefix("v").removePrefix("V")
        val parts = clean.split(".").mapNotNull { part ->
            part.takeWhile { it.isDigit() }.toIntOrNull()
        }
        val major = parts.getOrElse(0) { 0 }
        val minor = parts.getOrElse(1) { 0 }
        val patch = parts.getOrElse(2) { 0 }
        return SemVer(major, minor, patch)
    }

    fun isRemoteVersionNewer(remoteVersion: String, currentVersion: String = CURRENT_VERSION_NAME): Boolean {
        val remote = parseSemVer(remoteVersion)
        val current = parseSemVer(currentVersion)
        return remote > current
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Initializes update state from stored preferences.
     * If the app was updated to a newer version than the saved mandatory update,
     * it automatically clears the mandatory lock.
     */
    fun initUpdateState(context: Context) {
        val prefs = getPrefs(context)
        val isActive = prefs.getBoolean(KEY_MANDATORY_ACTIVE, false)
        val savedCode = prefs.getInt(KEY_MANDATORY_VERSION_CODE, 0)
        val savedName = prefs.getString(KEY_MANDATORY_VERSION_NAME, "") ?: ""

        val isNewer = if (savedName.isNotBlank()) {
            isRemoteVersionNewer(savedName, CURRENT_VERSION_NAME)
        } else {
            savedCode > CURRENT_VERSION_CODE
        }

        if (isActive && isNewer) {
            val info = AppVersionInfo(
                versionCode = if (savedCode > 0) savedCode else 2,
                versionName = if (savedName.isNotBlank()) savedName else "1.1.0",
                releaseNotes = prefs.getString(KEY_MANDATORY_NOTES, "Critical updates and reliability fixes.") ?: "",
                downloadUrl = prefs.getString(KEY_MANDATORY_URL, "https://github.com/${getGitHubRepo(context)}/releases") ?: "",
                isMandatory = true
            )
            _mandatoryUpdateFlow.value = info
        } else if (isActive && !isNewer) {
            clearMandatoryUpdate(context)
        }
    }

    fun getPendingMandatoryUpdate(context: Context): AppVersionInfo? {
        val prefs = getPrefs(context)
        val isActive = prefs.getBoolean(KEY_MANDATORY_ACTIVE, false)
        val savedCode = prefs.getInt(KEY_MANDATORY_VERSION_CODE, 0)
        val savedName = prefs.getString(KEY_MANDATORY_VERSION_NAME, "") ?: ""
        val isNewer = if (savedName.isNotBlank()) {
            isRemoteVersionNewer(savedName, CURRENT_VERSION_NAME)
        } else {
            savedCode > CURRENT_VERSION_CODE
        }

        if (isActive && isNewer) {
            return AppVersionInfo(
                versionCode = if (savedCode > 0) savedCode else 2,
                versionName = if (savedName.isNotBlank()) savedName else "1.1.0",
                releaseNotes = prefs.getString(KEY_MANDATORY_NOTES, "") ?: "",
                downloadUrl = prefs.getString(KEY_MANDATORY_URL, "") ?: "",
                isMandatory = true
            )
        }
        return null
    }

    fun setMandatoryUpdate(context: Context, info: AppVersionInfo) {
        getPrefs(context).edit()
            .putBoolean(KEY_MANDATORY_ACTIVE, true)
            .putInt(KEY_MANDATORY_VERSION_CODE, info.versionCode)
            .putString(KEY_MANDATORY_VERSION_NAME, info.versionName)
            .putString(KEY_MANDATORY_NOTES, info.releaseNotes)
            .putString(KEY_MANDATORY_URL, info.downloadUrl)
            .apply()
        _mandatoryUpdateFlow.value = info
    }

    fun clearMandatoryUpdate(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_MANDATORY_ACTIVE, false)
            .remove(KEY_MANDATORY_VERSION_CODE)
            .remove(KEY_MANDATORY_VERSION_NAME)
            .remove(KEY_MANDATORY_NOTES)
            .remove(KEY_MANDATORY_URL)
            .apply()
        _mandatoryUpdateFlow.value = null
        NotificationHelper.cancelNotification(context, NotificationHelper.NOTIFICATION_ID_UPDATE.toLong())
    }

    fun sanitizeGitHubRepo(input: String): String {
        var cleaned = input.trim()
        cleaned = cleaned.removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removePrefix("github.com/")
        cleaned = cleaned.removeSuffix("/releases")
            .removeSuffix("/releases/")
            .removeSuffix("/")
        return cleaned.trim()
    }

    const val DEFAULT_GITHUB_REPO = "kmochy013/Reminder"

    fun isPlaceholderRepo(repo: String): Boolean {
        val sanitized = sanitizeGitHubRepo(repo)
        return sanitized.isBlank() ||
                sanitized.equals("reminder-app", ignoreCase = true) ||
                sanitized.equals("reminder-app/release", ignoreCase = true) ||
                sanitized.equals("reminder-app/releases", ignoreCase = true) ||
                sanitized.equals("username/repository", ignoreCase = true) ||
                sanitized.equals("owner/repository", ignoreCase = true)
    }

    fun getGitHubRepo(context: Context): String {
        val stored = getPrefs(context).getString(KEY_GITHUB_REPO, DEFAULT_GITHUB_REPO) ?: DEFAULT_GITHUB_REPO
        val sanitized = sanitizeGitHubRepo(stored)
        if (isPlaceholderRepo(sanitized)) {
            setGitHubRepo(context, DEFAULT_GITHUB_REPO)
            return DEFAULT_GITHUB_REPO
        }
        return sanitized
    }

    fun setGitHubRepo(context: Context, repo: String) {
        val sanitized = sanitizeGitHubRepo(repo)
        getPrefs(context).edit().putString(KEY_GITHUB_REPO, sanitized).apply()
    }

    fun getCustomFeedUrl(context: Context): String {
        return getPrefs(context).getString(KEY_CUSTOM_FEED_URL, "") ?: ""
    }

    fun setCustomFeedUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_CUSTOM_FEED_URL, url.trim()).apply()
    }

    /**
     * Checks remote source (GitHub Releases API or fallback webpage / direct JSON feed) for a newer version.
     * When a newer version is detected, it enforces mandatory update and posts an auto-update notification.
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

                // If user hasn't configured their actual GitHub repo yet, guide them directly
                if (customUrl.isBlank() && isPlaceholderRepo(repo)) {
                    withContext(Dispatchers.Main) {
                        onResult(
                            UpdateCheckResult.Error(
                                "No releases found on GitHub repo ($repo). That was a sample placeholder. To check real updates, enter your GitHub 'username/repo' below, or tap 'Test Mandatory Update' to simulate an update right now."
                            )
                        )
                    }
                    return@launch
                }

                var tagName = ""
                var releaseNotes = ""
                var htmlUrl = "https://github.com/$repo/releases"
                var downloadUrl = "https://github.com/$repo/releases"
                var parsedVersionCode: Int? = null

                if (customUrl.isNotBlank()) {
                    // Custom JSON feed
                    val request = Request.Builder()
                        .url(customUrl)
                        .header("User-Agent", "ReminderApp-Android")
                        .build()
                    val response = httpClient.newCall(request).execute()
                    if (!response.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            onResult(UpdateCheckResult.Error("Custom feed returned HTTP ${response.code}"))
                        }
                        return@launch
                    }
                    val bodyString = response.body?.string() ?: ""
                    val json = JSONObject(bodyString)
                    tagName = json.optString("version_name", json.optString("tag_name", "1.0.0"))
                    releaseNotes = json.optString("release_notes", json.optString("body", "Stability improvements."))
                    downloadUrl = json.optString("download_url", json.optString("html_url", customUrl))
                    if (json.has("version_code")) {
                        parsedVersionCode = json.getInt("version_code")
                    }
                } else {
                    // Query GitHub API first
                    val targetUrl = "https://api.github.com/repos/$repo/releases/latest"
                    val request = Request.Builder()
                        .url(targetUrl)
                        .header("User-Agent", "ReminderApp-Android")
                        .header("Accept", "application/vnd.github.v3+json")
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val responseCode = response.code
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val json = JSONObject(responseBody)
                        tagName = json.optString("tag_name", "").removePrefix("v").removePrefix("V")
                        releaseNotes = json.optString("body", "Important stability, alarm accuracy, and notification improvements.")
                        htmlUrl = json.optString("html_url", "https://github.com/$repo/releases")
                        downloadUrl = htmlUrl

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
                    } else if (responseCode == 404) {
                        withContext(Dispatchers.Main) {
                            onResult(
                                UpdateCheckResult.Error(
                                    "Connected to GitHub repository '$repo' successfully!\n\nNo Releases have been published yet on this repository. To publish your first update, draft a release on GitHub with tag v1.1 and attach your APK file."
                                )
                            )
                        }
                        return@launch
                    } else {
                        // Rate limit (403) or other error -> Fallback to scraping public release page
                        val fallbackRequest = Request.Builder()
                            .url("https://github.com/$repo/releases")
                            .header("User-Agent", "Mozilla/5.0 (Android)")
                            .build()
                        val fallbackResponse = httpClient.newCall(fallbackRequest).execute()
                        if (fallbackResponse.isSuccessful) {
                            val html = fallbackResponse.body?.string() ?: ""
                            val tagMatcher = Pattern.compile("/$repo/releases/tag/([^\"/\\s]+)").matcher(html)
                            if (tagMatcher.find()) {
                                tagName = tagMatcher.group(1).removePrefix("v").removePrefix("V")
                                htmlUrl = "https://github.com/$repo/releases/tag/v$tagName"
                                downloadUrl = "https://github.com/$repo/releases/download/v$tagName/Reminder.apk"
                                releaseNotes = "Update version $tagName released on GitHub."
                            } else {
                                withContext(Dispatchers.Main) {
                                    onResult(
                                        UpdateCheckResult.Error(
                                            "Connected to GitHub repository '$repo'. No releases found yet. Draft a release on GitHub with tag v1.1 to publish an update."
                                        )
                                    )
                                }
                                return@launch
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                onResult(UpdateCheckResult.Error("GitHub returned code $responseCode. Please verify network connection."))
                            }
                            return@launch
                        }
                    }
                }

                val remoteVersionName = if (tagName.isNotBlank()) tagName else "1.0.0"
                val isNewer = if (parsedVersionCode != null) {
                    parsedVersionCode > CURRENT_VERSION_CODE
                } else {
                    isRemoteVersionNewer(remoteVersionName, CURRENT_VERSION_NAME)
                }

                getPrefs(context).edit().putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis()).apply()

                if (isNewer) {
                    val info = AppVersionInfo(
                        versionCode = parsedVersionCode ?: (CURRENT_VERSION_CODE + 1),
                        versionName = remoteVersionName,
                        releaseNotes = releaseNotes,
                        downloadUrl = downloadUrl,
                        isMandatory = true
                    )

                    setMandatoryUpdate(context, info)

                    if (notifyIfAvailable) {
                        NotificationHelper.showUpdateNotification(
                            context = context,
                            versionName = info.versionName,
                            releaseNotes = info.releaseNotes,
                            downloadUrl = info.downloadUrl,
                            isMandatory = true
                        )
                    }

                    withContext(Dispatchers.Main) {
                        onResult(UpdateCheckResult.UpdateAvailable(info))
                    }
                } else {
                    clearMandatoryUpdate(context)
                    withContext(Dispatchers.Main) {
                        onResult(UpdateCheckResult.UpToDate(CURRENT_VERSION_NAME, remoteVersionName))
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(UpdateCheckResult.Error("Could not check updates: ${e.localizedMessage ?: "Network error"}"))
                }
            }
        }
    }

    /**
     * Activates a mandatory update simulation immediately.
     * Posts the auto update heads-up notification and locks the app with the Mandatory Update screen.
     */
    fun simulateMandatoryUpdate(context: Context): AppVersionInfo {
        val sampleInfo = AppVersionInfo(
            versionCode = CURRENT_VERSION_CODE + 1,
            versionName = "1.1.0",
            releaseNotes = "• Mosque / Friday recurring reminders with auto-reschedule\n• Birthday & yearly anniversary alert engine\n• 1-minute alert repetition reliability\n• Mandatory security and alarm scheduling update",
            downloadUrl = "https://github.com/${getGitHubRepo(context)}/releases",
            isMandatory = true
        )

        setMandatoryUpdate(context, sampleInfo)

        NotificationHelper.showUpdateNotification(
            context = context,
            versionName = sampleInfo.versionName,
            releaseNotes = sampleInfo.releaseNotes,
            downloadUrl = sampleInfo.downloadUrl,
            isMandatory = true
        )

        return sampleInfo
    }

    /**
     * Broadcasts / posts a sample update notification so the developer and user can
     * immediately experience how the update notification looks and works.
     */
    fun triggerSampleUpdateNotification(context: Context) {
        simulateMandatoryUpdate(context)
    }

    /**
     * Schedules periodic background update checks via AlarmManager so installed phones
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

        val triggerTime = System.currentTimeMillis() + 6 * 3600_000L // First check in 6h
        val interval = AlarmManager.INTERVAL_HALF_DAY // Twice a day

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            interval,
            pendingIntent
        )
    }
}
