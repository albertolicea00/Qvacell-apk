package com.qvacell.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qvacell.app.service.DashboardDataRepository
import com.qvacell.app.service.EstimationEngine
import com.qvacell.app.service.EstimationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EstimationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = DashboardDataRepository(context)
                EstimationEngine.computeAndRecordEstimate(context, repository)
            } finally {
                // Re-arm the next occurrence — AlarmManager alarms don't repeat on their own here.
                EstimationScheduler(context).schedule()
                pendingResult.finish()
            }
        }
    }
}
