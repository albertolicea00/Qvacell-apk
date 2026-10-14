package com.qvacell.app.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.service.DashboardDataRepository
import com.qvacell.app.service.EstimationEngine
import com.qvacell.app.service.EstimationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class EstimationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            var enabled = false
            try {
                enabled = SettingsDataStore(context).smsCaptureEnabled.first() &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
                    PackageManager.PERMISSION_GRANTED
                if (enabled) {
                    val repository = DashboardDataRepository(context)
                    EstimationEngine.computeAndRecordEstimate(context, repository)
                }
            } finally {
                if (enabled) EstimationScheduler(context).schedule()
                pendingResult.finish()
            }
        }
    }
}
