package com.qvacell.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReminderRecurrence { NONE, DAILY, WEEKLY, MONTHLY, CUSTOM }

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val iconName: String,
    val ussdCodeId: String?,
    val phoneNumber: String,
    val date: Long,
    val recurrence: ReminderRecurrence,
    val customIntervalDays: Int,
    val isEnabled: Boolean,
    val templateKey: String?
)
