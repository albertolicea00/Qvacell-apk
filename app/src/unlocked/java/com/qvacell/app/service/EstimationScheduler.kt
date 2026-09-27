package com.qvacell.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.qvacell.app.receiver.EstimationAlarmReceiver

/**
 * Periodic call-log/SMS-sent delta estimation. Mirrors [ReminderScheduler]'s AlarmManager pattern
 * exactly (one background-scheduling idiom in the app, no WorkManager dependency) — re-arms
 * itself each firing from [EstimationAlarmReceiver], and cleared alarms are re-registered by
 * BootCompletedReceiver.
 */
class EstimationScheduler(private val context: Context) {

    private val alarmManager get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule() {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + INTERVAL_MILLIS,
            pendingIntent()
        )
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, EstimationAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        // Every 12h — balances battery cost against dashboard staleness; revisit once real usage
        // shows whether that cadence is too aggressive or too stale.
        private const val INTERVAL_MILLIS = 12 * 60 * 60 * 1000L
        private const val REQUEST_CODE = 9001
    }
}
