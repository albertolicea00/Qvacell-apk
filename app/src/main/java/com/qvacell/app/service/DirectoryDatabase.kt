package com.qvacell.app.service

import android.content.Context
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class DirectoryEntry(val number: String, val name: String, val isMobile: Boolean) {
    val displayName: String
        get() = name.trim().split(" ").joinToString(" ") { word ->
            if (word.isEmpty()) word
            else word.lowercase().replaceFirstChar { it.uppercase() }
        }
}

private enum class SchemaVersion { V1_SINGLE_TABLE, V2_SPLIT_TABLES, UNKNOWN }

/** Read-only reverse phone lookup, imported by the user as a raw SQLite file via SAF. */
class DirectoryDatabase(private val context: Context) {

    private val importedDbFile: File get() = File(context.filesDir, "directory.db")

    fun isImported(): Boolean = importedDbFile.exists()

    suspend fun importFrom(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                importedDbFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext false
            true
        } catch (e: Exception) {
            false
        }
    }

    fun deleteImported() {
        if (importedDbFile.exists()) importedDbFile.delete()
    }

    private fun openHelper(): SupportSQLiteOpenHelper? {
        if (!importedDbFile.exists()) return null
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(importedDbFile.absolutePath)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config)
    }

    private fun detectSchema(db: SupportSQLiteDatabase): SchemaVersion {
        val tableNames = mutableSetOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
            while (cursor.moveToNext()) {
                tableNames.add(cursor.getString(0))
            }
        }
        return when {
            tableNames.contains("movil") && tableNames.contains("fix") -> SchemaVersion.V2_SPLIT_TABLES
            tableNames.contains("contacts") -> SchemaVersion.V1_SINGLE_TABLE
            else -> SchemaVersion.UNKNOWN
        }
    }

    suspend fun search(prefix: String): List<DirectoryEntry> = withContext(Dispatchers.IO) {
        val helper = openHelper() ?: return@withContext emptyList()
        val results = mutableListOf<DirectoryEntry>()

        helper.readableDatabase.use { db ->
            when (detectSchema(db)) {
                SchemaVersion.V1_SINGLE_TABLE -> {
                    db.query(
                        "SELECT number, name, is_mobile FROM contacts WHERE number LIKE ? LIMIT 200",
                        arrayOf("$prefix%")
                    ).use { cursor ->
                        while (cursor.moveToNext()) {
                            results += DirectoryEntry(
                                number = cursor.getString(0),
                                name = cursor.getString(1),
                                isMobile = cursor.getInt(2) != 0
                            )
                        }
                    }
                }
                SchemaVersion.V2_SPLIT_TABLES -> {
                    db.query(
                        "SELECT number, name FROM movil WHERE number LIKE ? LIMIT 200",
                        arrayOf("$prefix%")
                    ).use { cursor ->
                        while (cursor.moveToNext()) {
                            results += DirectoryEntry(cursor.getString(0), cursor.getString(1), isMobile = true)
                        }
                    }
                    db.query(
                        "SELECT number, name FROM fix WHERE number LIKE ? LIMIT 200",
                        arrayOf("$prefix%")
                    ).use { cursor ->
                        while (cursor.moveToNext()) {
                            results += DirectoryEntry(cursor.getString(0), cursor.getString(1), isMobile = false)
                        }
                    }
                }
                SchemaVersion.UNKNOWN -> {}
            }
        }

        results
    }

    suspend fun findByNumber(number: String): DirectoryEntry? = search(number).firstOrNull { it.number == number }
}
