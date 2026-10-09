package com.qvacell.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

object SimUtils {
    fun getActiveSubscriptions(context: Context): List<SubscriptionInfo> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) return emptyList()

        val manager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
            as? SubscriptionManager ?: return emptyList()

        return try {
            manager.activeSubscriptionInfoList
                ?.sortedBy { it.simSlotIndex }
                ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    fun simLabel(info: SubscriptionInfo): String {
        val display = info.displayName?.toString()?.takeIf { it.isNotBlank() }
        val carrier = info.carrierName?.toString()?.takeIf { it.isNotBlank() }
        return display ?: carrier ?: "SIM ${info.simSlotIndex + 1}"
    }
}
