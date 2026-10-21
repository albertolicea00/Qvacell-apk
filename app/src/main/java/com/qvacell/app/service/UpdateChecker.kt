package com.qvacell.app.service

import android.content.Context
import android.os.Build
import com.qvacell.app.BuildConfig
import com.qvacell.app.data.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val htmlUrl: String,
    val unlockedApkUrl: String?,
    val storeApkUrl: String?,
    val isFlavorUpgrade: Boolean = false
)

sealed interface UpdateCheckResult {
    data class Available(val info: UpdateInfo) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data object Failed : UpdateCheckResult
}

object UpdateChecker {

    private const val CHECK_INTERVAL_MILLIS = 24 * 60 * 60 * 1000L

    fun isPlayStoreInstall(context: Context): Boolean =
        !BuildConfig.DASHBOARD_CAPTURE_ENABLED && runCatching {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
            installer == "com.android.vending"
        }.getOrDefault(false)

    /** Runs at app open when the selected channel has not been checked successfully today. */
    suspend fun checkIfDue(settings: SettingsDataStore, targetMode: String): UpdateCheckResult? {
        if (targetMode == "none") return null
        val now = System.currentTimeMillis()
        val lastCheck = settings.lastSuccessfulUpdateCheckAt.first()
        if (lastCheck in 1..now && now - lastCheck < CHECK_INTERVAL_MILLIS) return null

        val result = check(targetMode)
        if (result != UpdateCheckResult.Failed) {
            settings.setLastSuccessfulUpdateCheckAt(System.currentTimeMillis())
        }
        return result
    }

    private const val RELEASES_URL =
        "https://api.github.com/repos/albertolicea00/Qvacell-apk/releases"
    private const val LATEST_RELEASE_URL = "$RELEASES_URL/latest"

    /**
     * Resolves the full APK using this fallback order:
     * latest public release -> installed version's release -> caller-provided releases page.
     */
    suspend fun preferredUnlockedApkUrl(): String? = withContext(Dispatchers.IO) {
        fetchUnlockedApkUrl(LATEST_RELEASE_URL)
            ?: fetchUnlockedApkUrl("$RELEASES_URL/tags/v${BuildConfig.VERSION_NAME}")
    }

    private fun fetchUnlockedApkUrl(releaseUrl: String): String? {
        try {
            val conn = URL(releaseUrl).openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000

            if (conn.responseCode != 200) {
                conn.disconnect()
                return null
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val assets = org.json.JSONObject(body).getJSONArray("assets")

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name").lowercase()
                if ("unlocked" in name && name.endsWith(".apk")) {
                    return asset.getString("browser_download_url")
                }
            }
            return null
        } catch (_: Exception) {
            return null
        }
    }

    suspend fun check(targetMode: String = "unlocked"): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val conn = URL(RELEASES_URL).openConnection() as HttpURLConnection
            val body = try {
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext UpdateCheckResult.Failed
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }

            val releases = JSONArray(body)
            if (releases.length() == 0) return@withContext UpdateCheckResult.Failed

            val latest = releases.getJSONObject(0)
            val tagName = latest.getString("tag_name").removePrefix("v")

            val htmlUrl = latest.getString("html_url")
            var unlockedUrl: String? = null
            var storeUrl: String? = null

            val assets = latest.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name").lowercase()
                val downloadUrl = asset.getString("browser_download_url")
                when {
                    "unlocked" in name && name.endsWith(".apk") -> unlockedUrl = downloadUrl
                    "store" in name && name.endsWith(".apk") -> storeUrl = downloadUrl
                }
            }

            val newerVersion = isNewer(tagName, BuildConfig.VERSION_NAME)
            val isSameVersion = tagName == BuildConfig.VERSION_NAME

            // If the user is currently on the "store" build (DASHBOARD_CAPTURE_ENABLED is false / even versionCode)
            // and checking for "unlocked", the unlocked build is a functional upgrade even on the same release.
            val isFlavorUpgrade = isSameVersion && !BuildConfig.DASHBOARD_CAPTURE_ENABLED && targetMode == "unlocked" && unlockedUrl != null

            if (!newerVersion && !isFlavorUpgrade) return@withContext UpdateCheckResult.UpToDate

            UpdateCheckResult.Available(UpdateInfo(
                versionName = tagName,
                htmlUrl = htmlUrl,
                unlockedApkUrl = unlockedUrl,
                storeApkUrl = storeUrl,
                isFlavorUpgrade = isFlavorUpgrade
            ))
        } catch (_: Exception) {
            UpdateCheckResult.Failed
        }
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split(".").mapNotNull { it.toIntOrNull() }
        val l = local.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        return false
    }
}
