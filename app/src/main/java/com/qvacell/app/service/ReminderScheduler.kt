package com.qvacell.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.qvacell.app.model.Reminder
import com.qvacell.app.model.ReminderRecurrence
import com.qvacell.app.receiver.ReminderAlarmReceiver
import java.util.Calendar

/**
 * Schedules reminder alarms with AlarmManager.setExactAndAllowWhileIdle. Recurring
 * reminders are rescheduled from the receiver when they fire. AlarmManager alarms are
 * cleared on reboot (unlike iOS local notifications), so BootCompletedReceiver
 * re-registers all enabled reminders after the device restarts.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: Reminder) {
        if (!reminder.isEnabled) return
        val pendingIntent = pendingIntentFor(reminder.id)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.date, pendingIntent)
    }

    fun cancel(reminderId: String) {
        alarmManager.cancel(pendingIntentFor(reminderId))
    }

    fun nextOccurrence(reminder: Reminder): Long? {
        if (reminder.recurrence == ReminderRecurrence.NONE) return null
        val calendar = Calendar.getInstance().apply { timeInMillis = reminder.date }
        when (reminder.recurrence) {
            ReminderRecurrence.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            ReminderRecurrence.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            ReminderRecurrence.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            ReminderRecurrence.CUSTOM -> calendar.add(Calendar.DAY_OF_YEAR, reminder.customIntervalDays.coerceAtLeast(1))
            ReminderRecurrence.NONE -> return null
        }
        return calendar.timeInMillis
    }

    private fun pendingIntentFor(reminderId: String): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
