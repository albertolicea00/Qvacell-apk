package com.qvacell.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.core.app.NotificationCompat
import com.qvacell.app.data.QvacellDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ETECSA wraps *99 collect calls as "99" + "53" + 8-digit-number + "99". This service
 * unwraps that format and looks up the real contact name from a locally built map.
 *
 * IMPORTANT PLATFORM GAP: unlike iOS CallKit (CXCallDirectoryExtension), Android's
 * CallScreeningService cannot inject a custom display name into the system in-call UI
 * unless this app is the default Phone/Dialer app. We never block/reject calls — we
 * only surface the resolved name via a heads-up notification as a best-effort caller ID.
 */
class CallerIdScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val incoming = callDetails.handle?.schemeSpecificPart

        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)

        if (incoming.isNullOrBlank()) return

        val wrapped = unwrapCollectCallNumber(incoming) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val dao = QvacellDatabase.get(applicationContext).wrappedCallerDao()
            val match = dao.findByNumber(wrapped)
            if (match != null) {
                showResolvedCallerNotification(match.name, wrapped.toString())
            }
        }
    }

    /** Unwraps "99" + "53" + 8-digit-number + "99" into the raw 8-digit number as a Long. */
    private fun unwrapCollectCallNumber(raw: String): Long? {
        val digits = raw.filter { it.isDigit() }
        if (!digits.startsWith("9953") || !digits.endsWith("99")) return null
        val middle = digits.removePrefix("9953").removeSuffix("99")
        if (middle.length != 8) return null
        return middle.toLongOrNull()
    }

    private fun showResolvedCallerNotification(name: String, number: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Identificador de Llamadas", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("Llamada por cobrar de $name")
            .setContentText(number)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .build()
        manager.notify(number.hashCode(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "caller_id"
    }
}
