package com.qvacell.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.qvacell.app.model.DashboardValueSnapshot
import com.qvacell.app.model.Reminder
import com.qvacell.app.model.SourceAnchor
import com.qvacell.app.model.WrappedCaller

@Database(
    entities = [Reminder::class, WrappedCaller::class, DashboardValueSnapshot::class, SourceAnchor::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class QvacellDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun wrappedCallerDao(): WrappedCallerDao
    abstract fun dashboardValueSnapshotDao(): DashboardValueSnapshotDao
    abstract fun sourceAnchorDao(): SourceAnchorDao

    companion object {
        @Volatile private var instance: QvacellDatabase? = null

        // Hand-written, not @AutoMigration — the app shipped version 1 with exportSchema = false,
        // so there's no captured schema baseline to auto-diff against. Existing Reminder/
        // WrappedCaller data must survive, so this only adds the two new tables.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS dashboard_value_snapshots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        fieldType TEXT NOT NULL,
                        numericValue REAL,
                        textValue TEXT,
                        dateValue INTEGER,
                        unit TEXT,
                        capturedAt INTEGER NOT NULL,
                        sourceSignalId TEXT NOT NULL,
                        sourceKind TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_dashboard_value_snapshots_fieldType_capturedAt " +
                        "ON dashboard_value_snapshots(fieldType, capturedAt)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS source_anchors (
                        signalId TEXT PRIMARY KEY NOT NULL,
                        signalKind TEXT NOT NULL,
                        lastSeenAt INTEGER NOT NULL,
                        lastSeenRawText TEXT NOT NULL,
                        lastSuccessfulParseAt INTEGER,
                        lastSuccessfulRawText TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        fun get(context: Context): QvacellDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    QvacellDatabase::class.java,
                    "qvacell.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
