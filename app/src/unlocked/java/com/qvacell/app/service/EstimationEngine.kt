package com.qvacell.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.qvacell.app.data.FieldTypes
import com.qvacell.app.model.CubanPhoneNumber
import com.qvacell.app.parsing.ParsedDashboardValue

/**
 * Estimates calls-made/minutes-talked/SMS-sent since the last confirmed (non-estimated) anchor,
 * Cuban numbers only ([CubanPhoneNumber.normalize], already used elsewhere in the app for the
 * same validation). Writes via [DashboardDataRepository.recordEstimate] — a distinct write path
 * from confirmed USSD/SMS snapshots, so an estimate can never be conflated with real data.
 */
object EstimationEngine {
    suspend fun computeAndRecordEstimate(context: Context, repository: DashboardDataRepository) {
        val anchor = repository.latestConfirmedAnchorTimestamp() ?: return
        val now = System.currentTimeMillis()

        val callsMade = mutableListOf<Int>()
        var minutesTalked = 0L
        if (hasPermission(context, Manifest.permission.READ_CALL_LOG)) {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DURATION, CallLog.Calls.TYPE),
                "${CallLog.Calls.DATE} > ?",
                arrayOf(anchor.toString()),
                null
            )?.use { cursor ->
                val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                while (cursor.moveToNext()) {
                    // ETECSA plans bill outgoing minutes, not incoming — count outgoing only.
                    if (cursor.getInt(typeIdx) != CallLog.Calls.OUTGOING_TYPE) continue
                    val number = cursor.getString(numberIdx) ?: continue
                    if (CubanPhoneNumber.normalize(number) == null) continue
                    callsMade.add(1)
                    minutesTalked += cursor.getLong(durationIdx) / 60
                }
            }
        }

        var smsSent = 0
        if (hasPermission(context, Manifest.permission.READ_SMS)) {
            context.contentResolver.query(
                Telephony.Sms.Sent.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS),
                "${Telephony.Sms.DATE} > ?",
                arrayOf(anchor.toString()),
                null
            )?.use { cursor ->
                val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                while (cursor.moveToNext()) {
                    val address = cursor.getString(addressIdx) ?: continue
                    if (CubanPhoneNumber.normalize(address) == null) continue
                    smsSent++
                }
            }
        }

        repository.recordEstimate(
            listOf(
                ParsedDashboardValue(fieldType = FieldTypes.CALLS_MADE_ESTIMATE, numericValue = callsMade.size.toDouble()),
                ParsedDashboardValue(fieldType = FieldTypes.VOICE_MINUTES_ESTIMATE, numericValue = minutesTalked.toDouble()),
                ParsedDashboardValue(fieldType = FieldTypes.SMS_SENT_ESTIMATE, numericValue = smsSent.toDouble())
            ),
            now
        )
    }

    private fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
