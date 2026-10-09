package com.qvacell.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

import android.os.Build

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

    fun getPhoneNumber(context: Context, info: SubscriptionInfo): String? {
        val manager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
            as? SubscriptionManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && manager != null) {
            try {
                val num = manager.getPhoneNumber(info.subscriptionId)
                if (!num.isNullOrBlank()) return num.trim()
            } catch (_: SecurityException) {
            } catch (_: Exception) {}
        }
        @Suppress("DEPRECATION")
        val fallback = try { info.number } catch (_: Exception) { null }
        return fallback?.takeIf { it.isNotBlank() }?.trim()
    }

    fun simLabel(context: Context, info: SubscriptionInfo): String {
        val base = baseLabel(info)
        val number = getPhoneNumber(context, info)
        return if (!number.isNullOrBlank()) "$base ($number)" else base
    }

    fun simLabel(info: SubscriptionInfo): String = baseLabel(info)

    private fun baseLabel(info: SubscriptionInfo): String {
        val display = info.displayName?.toString()?.takeIf { it.isNotBlank() }
        val carrier = info.carrierName?.toString()?.takeIf { it.isNotBlank() }
        return display ?: carrier ?: "SIM ${info.simSlotIndex + 1}"
    }

    /**
     * SIM colors: Amarillo para SIM 1 (slot 0) y Rosado para SIM 2 (slot 1).
     */
    fun simColor(slotIndex: Int): androidx.compose.ui.graphics.Color {
        return when (slotIndex) {
            0 -> androidx.compose.ui.graphics.Color(0xFFFFB300) // Amarillo cálido / ámbar visible
            1 -> androidx.compose.ui.graphics.Color(0xFFE91E63) // Rosado
            else -> androidx.compose.ui.graphics.Color(0xFFFFB300)
        }
    }
}
