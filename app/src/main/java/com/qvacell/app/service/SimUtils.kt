package com.qvacell.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Color
import com.qvacell.app.data.SimAppearance

import android.os.Build

object SimUtils {
    fun resolveSelectedSlot(context: Context, savedSlot: Int): Int {
        return resolveSelectedSlot(context, savedSlot, SubscriptionManager.INVALID_SUBSCRIPTION_ID)
    }

    fun resolveSelectedSlot(context: Context, savedSlot: Int, savedSubscriptionId: Int): Int {
        val sims = getActiveSubscriptions(context)
        if (savedSubscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            sims.firstOrNull { it.subscriptionId == savedSubscriptionId }
                ?.let { return it.simSlotIndex }
        }
        if (sims.any { it.simSlotIndex == savedSlot }) return savedSlot
        val defaultId = runCatching { SubscriptionManager.getDefaultVoiceSubscriptionId() }
            .getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        return (sims.firstOrNull { it.subscriptionId == defaultId } ?: sims.firstOrNull())
            ?.simSlotIndex ?: -1
    }

    data class DashboardLine(
        val subscriptionId: Int?,
        val isCorporate: Boolean,
        val needsSelection: Boolean = false
    ) {
        val balanceCodeId: String get() = if (isCorporate) "corp-balance" else "main-balance"
    }

    fun resolveDashboardLine(
        context: Context,
        selectedSlot: Int,
        appearances: Map<String, SimAppearance>
    ): DashboardLine {
        val sims = getActiveSubscriptions(context)
        if (sims.isEmpty()) return DashboardLine(null, false)
        val slot = resolveSelectedSlot(context, selectedSlot)
        val sim = sims.firstOrNull { it.simSlotIndex == slot }
            ?: return DashboardLine(null, false, needsSelection = true)
        return DashboardLine(
            subscriptionId = sim.subscriptionId,
            isCorporate = appearances[sim.subscriptionId.toString()]?.isCorporate ?: false
        )
    }

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

    fun simLabel(context: Context, info: SubscriptionInfo, appearance: SimAppearance? = null): String {
        val base = simLabel(info, appearance)
        val number = getPhoneNumber(context, info)
        return if (!number.isNullOrBlank()) "$base ($number)" else base
    }

    fun simLabel(info: SubscriptionInfo, appearance: SimAppearance? = null): String =
        appearance?.name?.trim()?.takeIf { it.isNotEmpty() } ?: baseLabel(info)

    private fun baseLabel(info: SubscriptionInfo): String {
        val display = info.displayName?.toString()?.takeIf { it.isNotBlank() }
        val carrier = info.carrierName?.toString()?.takeIf { it.isNotBlank() }
        return display ?: carrier ?: "SIM ${info.simSlotIndex + 1}"
    }

    /**
     * SIM colors: Amarillo para SIM 1 (slot 0) y Rosado para SIM 2 (slot 1).
     */
    fun simColor(slotIndex: Int, appearance: SimAppearance? = null): Color =
        appearance?.colorHex?.let { hex ->
            runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
        } ?: when (slotIndex) {
            0 -> Color(0xFFFFB300) // Amarillo cálido / ámbar visible
            1 -> Color(0xFFE91E63) // Rosado
            else -> Color(0xFFFFB300)
        }
}
