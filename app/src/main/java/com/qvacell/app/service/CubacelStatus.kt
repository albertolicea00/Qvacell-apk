package com.qvacell.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.qvacell.app.R

/** Operator status of the selected SIM, independent of Wi-Fi or the visited screen. */
data class CubacelStatus(val subscriptionId: Int? = null, val warningRes: Int? = null) {
    val isCubacel: Boolean get() = subscriptionId != null && warningRes == null

    // Only confirmed missing/foreign SIMs block dynamic-dashboard updates.
    val blocksDashboardRefresh: Boolean
        get() = warningRes == R.string.sim_cubacel_missing || warningRes == R.string.sim_cubacel_unsupported

    companion object {
        fun read(context: Context, selectedSlot: Int): CubacelStatus {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
                PackageManager.PERMISSION_GRANTED
            ) return CubacelStatus(warningRes = R.string.sim_cubacel_permission)
            val slot = SimUtils.resolveSelectedSlot(context, selectedSlot)
            val sim = SimUtils.getActiveSubscriptions(context).firstOrNull { it.simSlotIndex == slot }
                ?: return CubacelStatus(warningRes = R.string.sim_cubacel_missing)
            return forSubscription(context, sim.subscriptionId)
        }

        fun forSubscription(context: Context, subscriptionId: Int?): CubacelStatus {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
                PackageManager.PERMISSION_GRANTED
            ) return CubacelStatus(warningRes = R.string.sim_cubacel_permission)
            if (subscriptionId == null || SimUtils.getActiveSubscriptions(context).none {
                    it.subscriptionId == subscriptionId
                }) return CubacelStatus(warningRes = R.string.sim_cubacel_missing)
            val operator = runCatching {
                (context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)
                    ?.createForSubscriptionId(subscriptionId)?.simOperator
            }.getOrNull()
            val warning = when {
                operator.isNullOrBlank() -> R.string.sim_cubacel_unknown
                operator != "36801" -> R.string.sim_cubacel_unsupported
                else -> null
            }
            return CubacelStatus(subscriptionId, warning)
        }
    }
}
