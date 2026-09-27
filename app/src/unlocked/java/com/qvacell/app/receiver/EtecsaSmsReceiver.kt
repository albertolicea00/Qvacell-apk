package com.qvacell.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.qvacell.app.service.DashboardDataRepository
import com.qvacell.app.service.EstimationEngine
import com.qvacell.app.service.EtecsaSmsFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Live incoming-SMS capture. Mirrors BootCompletedReceiver's goAsync()+CoroutineScope idiom. */
class EtecsaSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = DashboardDataRepository(context)
                messages.forEach { message ->
                    val sender = message.originatingAddress ?: return@forEach
                    val body = message.messageBody ?: return@forEach
                    if (EtecsaSmsFilter.isEtecsaMessage(sender, body)) {
                        repository.recordSmsBody(sender, body, message.timestampMillis)
                    }
                }
                EstimationEngine.computeAndRecordEstimate(context, repository)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
