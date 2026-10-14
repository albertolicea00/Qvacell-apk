package com.qvacell.app.service

import android.content.Context
import android.provider.Telephony
import com.qvacell.app.model.UssdCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            DashboardRefreshService.setCurrentCode(code.id)
            UssdCaptureService.requestUssd(context, code) { result ->
                when (result) {
                    is UssdCaptureResult.Success -> CoroutineScope(Dispatchers.IO).launch {
                        repository.recordUssdParse(code.id, result.response)
                        DashboardRefreshService.setCurrentCode(null)
                    }
                    is UssdCaptureResult.Failure -> {
                        DialService.dialDirect(context, code.resolvedCode())
                        DashboardRefreshService.setCurrentCode(null)
                    }
                }
            }
        } else {
            DialService.dialDirect(context, code.resolvedCode())
        }
    }

    fun scheduleEstimationIfEnabled(context: Context, enabled: Boolean) {
        if (enabled) EstimationScheduler(context).schedule() else EstimationScheduler(context).cancel()
    }

    /** One-time historical SMS scan, independent of live incoming-SMS capture. */
    suspend fun triggerSmsBackfill(context: Context, repository: DashboardDataRepository): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val cursor = context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                    null,
                    null,
                    "${Telephony.Sms.DATE} ASC"
                ) ?: return@withContext false
                cursor.use {
                    val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                    val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                    val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
                    while (it.moveToNext()) {
                        val sender = it.getString(addressIdx) ?: continue
                        val body = it.getString(bodyIdx) ?: continue
                        val date = it.getLong(dateIdx)
                        if (EtecsaSmsFilter.isEtecsaMessage(sender, body)) {
                            repository.recordSmsBody(sender, body, date)
                        }
                    }
                }
                true
            } catch (_: SecurityException) {
                false
            }
        }
}
