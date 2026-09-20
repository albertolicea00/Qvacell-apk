package com.qvacell.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qvacell.app.service.ReminderRepository
import com.qvacell.app.service.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AlarmManager alarms are cleared on reboot (unlike iOS's persisted local
 * notifications), so all enabled reminders must be rescheduled here.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = ReminderRepository(context)
                val scheduler = ReminderScheduler(context)
                repository.getEnabled().forEach { scheduler.schedule(it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
