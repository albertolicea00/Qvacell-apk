package com.qvacell.app.service

import android.content.Context
import android.provider.Telephony
import com.qvacell.app.model.UssdCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Full/sideload-flavor implementation — same package + object name + function signatures as the
 * play-flavor stand-in in app/src/play, so app/src/main call sites compile against either flavor
 * without any shared interface. This is the single seam between the capture mechanism and the
 * rest of the app.
 */
object DashboardCapture {

    /** Preserves today's exact tap-to-dial UX unless the experimental toggle is on AND
     *  CALL_PHONE is granted — any capture failure/timeout falls back to [DialService.dial]. */
    fun captureOrDial(
        context: Context,
        code: UssdCode,
        repository: DashboardDataRepository,
        captureEnabled: Boolean
    ) {
        if (captureEnabled && DialService.hasCallPermission(context)) {
            UssdCaptureService.requestUssd(context, code) { result ->
                when (result) {
                    is UssdCaptureResult.Success -> CoroutineScope(Dispatchers.IO).launch {
                        repository.recordUssdParse(code.id, result.response)
                    }
                    is UssdCaptureResult.Failure -> DialService.dial(context, code.resolvedCode())
                }
            }
        } else {
            DialService.dial(context, code.resolvedCode())
        }
    }

    fun scheduleEstimationIfEnabled(context: Context, enabled: Boolean) {
        if (enabled) EstimationScheduler(context).schedule() else EstimationScheduler(context).cancel()
    }

    /** One-time historical SMS scan — call right after READ_SMS is newly granted. */
    fun triggerSmsBackfill(context: Context, repository: DashboardDataRepository) {
        CoroutineScope(Dispatchers.IO).launch {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                null,
                null,
                "${Telephony.Sms.DATE} ASC"
            )?.use { cursor ->
                val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
                while (cursor.moveToNext()) {
                    val sender = cursor.getString(addressIdx) ?: continue
                    val body = cursor.getString(bodyIdx) ?: continue
                    val date = cursor.getLong(dateIdx)
                    if (EtecsaSmsFilter.isEtecsaMessage(sender, body)) {
                        repository.recordSmsBody(sender, body, date)
                    }
                }
            }
            EstimationEngine.computeAndRecordEstimate(context, repository)
        }
    }
}
