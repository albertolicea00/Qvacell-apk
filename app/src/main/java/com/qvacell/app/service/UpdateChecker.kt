package com.qvacell.app.service

import com.qvacell.app.BuildConfig
import kotlinx.coroutines.Dispatchers
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

object UpdateChecker {

    private const val RELEASES_URL =
        "https://api.github.com/repos/albertolicea00/Qvacell-apk/releases"

    suspend fun check(targetMode: String = "unlocked"): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(RELEASES_URL).openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000

            if (conn.responseCode != 200) return@withContext null

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val releases = JSONArray(body)
            if (releases.length() == 0) return@withContext null

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

            if (!newerVersion && !isFlavorUpgrade) return@withContext null

            UpdateInfo(
                versionName = tagName,
                htmlUrl = htmlUrl,
                unlockedApkUrl = unlockedUrl,
                storeApkUrl = storeUrl,
                isFlavorUpgrade = isFlavorUpgrade
            )
        } catch (_: Exception) {
            null
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
