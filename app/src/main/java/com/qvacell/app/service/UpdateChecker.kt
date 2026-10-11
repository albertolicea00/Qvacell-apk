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
    val storeApkUrl: String?
)

object UpdateChecker {

    private const val RELEASES_URL =
        "https://api.github.com/repos/albertolicea00/Qvacell-apk/releases"

    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
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

            if (!isNewer(tagName, BuildConfig.VERSION_NAME)) return@withContext null

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

            UpdateInfo(tagName, htmlUrl, unlockedUrl, storeUrl)
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
