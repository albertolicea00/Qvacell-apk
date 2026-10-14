package com.qvacell.app.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.service.DashboardCapture
import com.qvacell.app.service.ReminderRepository
import com.qvacell.app.service.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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

                // AlarmManager alarms are cleared on reboot the same way reminders are — re-arm
                // the estimation engine's alarm too if the user has it enabled (full flavor only;
                // a no-op on the play flavor).
                val smsCaptureEnabled = SettingsDataStore(context).smsCaptureEnabled.first()
                val hasCallLogPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_CALL_LOG
                ) == PackageManager.PERMISSION_GRANTED
                DashboardCapture.scheduleEstimationIfEnabled(context, smsCaptureEnabled && hasCallLogPermission)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
