package com.qvacell.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Wraps TelephonyManager to expose a simple description of the current network type. */
class CellularMonitor(private val context: Context) {

    private val _networkType = MutableStateFlow(currentNetworkType())
    val networkType: StateFlow<String> = _networkType

    fun refresh() {
        _networkType.value = currentNetworkType()
    }

    private fun hasPhoneStatePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    private fun currentNetworkType(): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return "Sin datos"

        if (!hasPhoneStatePermission()) return "Desconocido"

        @Suppress("MissingPermission")
        return when (tm.dataNetworkType) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA -> "2G"
            TelephonyManager.NETWORK_TYPE_UNKNOWN -> "Sin señal"
            else -> "Sin señal"
        }
    }

    fun isWeakOrNoSignal(type: String): Boolean = type == "Sin señal" || type == "2G" || type == "Sin datos"
}
