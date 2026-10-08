package com.qvacell.app.service

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.net.Uri
import android.provider.OpenableColumns
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

sealed class ImportResult {
    data object Success : ImportResult()
    data class Truncated(val copied: Long, val expected: Long) : ImportResult()
    data object InvalidSchema : ImportResult()
    data object Error : ImportResult()
}

/** Read-only reverse phone lookup, imported by the user as a raw SQLite file via SAF. */
class DirectoryDatabase(private val context: Context) {

    private val importedDbFile: File get() = File(context.filesDir, "directory.db")

    fun isImported(): Boolean = importedDbFile.exists()

    suspend fun importFrom(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val sourceSize = queryFileSize(uri)

            context.contentResolver.openInputStream(uri)?.use { input ->
                importedDbFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 8 * 1024 * 1024)
                }
            } ?: return@withContext ImportResult.Error

            val copiedSize = importedDbFile.length()
            if (sourceSize > 0 && copiedSize != sourceSize) {
                deleteImported()
                return@withContext ImportResult.Truncated(copiedSize, sourceSize)
            }

            if (!validate()) {
                deleteImported()
                return@withContext ImportResult.InvalidSchema
            }

            ImportResult.Success
        } catch (e: Exception) {
            deleteImported()
            ImportResult.Error
        }
    }

    private fun queryFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && cursor.moveToFirst()) cursor.getLong(sizeIndex) else -1L
            } ?: -1L
        } catch (e: Exception) {
            -1L
        }
    }

    fun deleteImported() {
        if (importedDbFile.exists()) importedDbFile.delete()
    }

    private fun validate(): Boolean {
        val db = openDatabase() ?: return false
        return try {
            db.use { detectSchema(it) != SchemaVersion.UNKNOWN }
        } catch (e: Exception) {
            false
        }
    }

    private fun openDatabase(): SQLiteDatabase? {
        if (!importedDbFile.exists()) return null
        return try {
            SQLiteDatabase.openDatabase(
                importedDbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun detectSchema(db: SQLiteDatabase): SchemaVersion {
        val tableNames = mutableSetOf<String>()
        db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { cursor ->
            while (cursor.moveToNext()) {
                tableNames.add(cursor.getString(0))
            }
        }
        return when {
            tableNames.contains("movil") || tableNames.contains("fix") -> SchemaVersion.V2_SPLIT_TABLES
            tableNames.contains("contacts") -> SchemaVersion.V1_SINGLE_TABLE
            else -> SchemaVersion.UNKNOWN
        }
    }

    suspend fun search(prefix: String): List<DirectoryEntry> = withContext(Dispatchers.IO) {
        val db = openDatabase() ?: return@withContext emptyList()
        val results = mutableListOf<DirectoryEntry>()

        try {
            db.use {
                when (detectSchema(it)) {
                    SchemaVersion.V1_SINGLE_TABLE -> {
                        it.rawQuery(
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
                        it.rawQuery(
                            "SELECT number, name FROM movil WHERE number LIKE ? LIMIT 200",
                            arrayOf("$prefix%")
                        ).use { cursor ->
                            while (cursor.moveToNext()) {
                                results += DirectoryEntry(cursor.getString(0), cursor.getString(1), isMobile = true)
                            }
                        }
                        it.rawQuery(
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
        } catch (e: SQLiteDatabaseCorruptException) {
            deleteImported()
        } catch (e: Exception) {
            // query failed but DB not necessarily corrupt
        }

        results
    }

    suspend fun findByNumber(number: String): DirectoryEntry? = search(number).firstOrNull { it.number == number }
}
