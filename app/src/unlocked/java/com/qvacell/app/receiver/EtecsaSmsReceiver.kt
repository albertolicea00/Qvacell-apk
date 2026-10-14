package com.qvacell.app.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.service.DashboardDataRepository
import com.qvacell.app.service.EstimationEngine
import com.qvacell.app.service.EtecsaSmsFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/** Live incoming-SMS capture. Mirrors BootCompletedReceiver's goAsync()+CoroutineScope idiom. */
class EtecsaSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!SettingsDataStore(context).smsCaptureEnabled.first()) return@launch
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                val repository = DashboardDataRepository(context)
                messages.forEach { message ->
                    val sender = message.originatingAddress ?: return@forEach
                    val body = message.messageBody ?: return@forEach
                    if (EtecsaSmsFilter.isEtecsaMessage(sender, body)) {
                        repository.recordSmsBody(sender, body, message.timestampMillis)
                    }
                }
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
                    PackageManager.PERMISSION_GRANTED
                ) EstimationEngine.computeAndRecordEstimate(context, repository)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
