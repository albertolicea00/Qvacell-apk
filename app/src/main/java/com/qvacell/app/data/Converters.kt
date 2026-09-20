package com.qvacell.app.data

import androidx.room.TypeConverter
import com.qvacell.app.model.ReminderRecurrence

class Converters {
    @TypeConverter
    fun fromRecurrence(value: ReminderRecurrence): String = value.name

    @TypeConverter
    fun toRecurrence(value: String): ReminderRecurrence = ReminderRecurrence.valueOf(value)
}
