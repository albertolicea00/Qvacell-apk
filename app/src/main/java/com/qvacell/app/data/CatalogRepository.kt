package com.qvacell.app.data

import android.content.Context
import com.qvacell.app.model.UssdCatalog
import com.qvacell.app.model.UssdCode
import com.qvacell.app.model.WifiProvince
import kotlinx.serialization.json.Json
import java.io.BufferedReader

private val json = Json { ignoreUnknownKeys = true }

class CatalogRepository(private val context: Context) {

    private var cachedCatalog: UssdCatalog? = null
    private var cachedWifi: List<WifiProvince>? = null

    fun loadCatalog(): UssdCatalog {
        cachedCatalog?.let { return it }
        val text = context.assets.open("codes.json").bufferedReader().use(BufferedReader::readText)
        return json.decodeFromString<UssdCatalog>(text).also { cachedCatalog = it }
    }

    fun loadWifiProvinces(): List<WifiProvince> {
        cachedWifi?.let { return it }
        val text = context.assets.open("wifi_navigation_rooms.json").bufferedReader().use(BufferedReader::readText)
        return json.decodeFromString<List<WifiProvince>>(text).also { cachedWifi = it }
    }

    fun findCodeById(id: String): UssdCode? =
        loadCatalog().categories
            .asSequence()
            .flatMap { it.groups.asSequence() }
            .flatMap { it.codes.asSequence() }
            .firstOrNull { it.id == id }
}
