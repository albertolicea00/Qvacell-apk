package com.qvacell.app.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qvacell.app.service.ReminderRepository
import com.qvacell.app.service.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID) ?: return
        val pendingResult = goAsync()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(reminderId.hashCode())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = ReminderRepository(context)
                val scheduler = ReminderScheduler(context)
                val reminder = repository.getById(reminderId) ?: return@launch

                when (intent.action) {
                    ACTION_MARK_DONE -> {
                        scheduler.cancel(reminderId)
                        val next = scheduler.nextOccurrence(reminder)
                        if (next == null) {
                            repository.setEnabled(reminder, false)
                        } else {
                            val updated = reminder.copy(date = next)
                            repository.save(updated)
                            scheduler.schedule(updated)
                        }
                    }
                    ACTION_SNOOZE -> {
                        val snoozed = reminder.copy(date = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1))
                        repository.save(snoozed)
                        scheduler.schedule(snoozed)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "com.qvacell.app.action.MARK_DONE"
        const val ACTION_SNOOZE = "com.qvacell.app.action.SNOOZE"
    }
}
