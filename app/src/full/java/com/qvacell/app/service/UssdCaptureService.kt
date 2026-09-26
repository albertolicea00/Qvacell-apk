package com.qvacell.app.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import com.qvacell.app.model.UssdCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface UssdCaptureResult {
    data class Success(val response: String) : UssdCaptureResult
    data class Failure(val reason: String) : UssdCaptureResult
}

/**
 * Wraps TelephonyManager.sendUssdRequest (API 26+) — silent capture of a USSD response's reply
 * text, bypassing the system dialer. ETECSA's gateway reliability through this API is unverified
 * (some carriers route the reply as an OS-level dialog the callback never sees) — callers MUST
 * treat [UssdCaptureResult.Failure] as "fall back to DialService.dial", never as a dead end.
 * Requires CALL_PHONE (already granted for Compras).
 */
object UssdCaptureService {
    private const val TIMEOUT_MILLIS = 15_000L

    fun requestUssd(context: Context, code: UssdCode, onResult: (UssdCaptureResult) -> Unit) {
        if (!DialService.hasCallPermission(context)) {
            onResult(UssdCaptureResult.Failure("permission_denied"))
            return
        }

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val deferred = CompletableDeferred<UssdCaptureResult>()

        try {
            telephonyManager.sendUssdRequest(
                code.resolvedCode(),
                object : TelephonyManager.UssdResponseCallback() {
                    override fun onReceiveUssdResponse(
                        telephonyManager: TelephonyManager,
                        request: String,
                        response: CharSequence
                    ) {
                        deferred.complete(UssdCaptureResult.Success(response.toString()))
                    }

                    override fun onReceiveUssdResponseFailed(
                        telephonyManager: TelephonyManager,
                        request: String,
                        failureCode: Int
                    ) {
                        deferred.complete(UssdCaptureResult.Failure("carrier_failure_$failureCode"))
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: SecurityException) {
            onResult(UssdCaptureResult.Failure("security_exception"))
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val result = withTimeoutOrNull(TIMEOUT_MILLIS) { deferred.await() }
                ?: UssdCaptureResult.Failure("timeout")
            onResult(result)
        }
    }
}
