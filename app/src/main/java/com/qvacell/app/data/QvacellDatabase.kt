package com.qvacell.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.qvacell.app.model.Reminder
import com.qvacell.app.model.WrappedCaller

@Database(entities = [Reminder::class, WrappedCaller::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class QvacellDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun wrappedCallerDao(): WrappedCallerDao

    companion object {
        @Volatile private var instance: QvacellDatabase? = null

        fun get(context: Context): QvacellDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    QvacellDatabase::class.java,
                    "qvacell.db"
                ).build().also { instance = it }
            }
    }
}
