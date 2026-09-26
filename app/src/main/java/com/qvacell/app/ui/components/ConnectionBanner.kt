package com.qvacell.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

/** 0 (no service) through 3 (best) — mirrors iOS's `CellularMonitor.signalQuality`. */
private data class CellularSignalStatus(
    val hasService: Boolean = false,
    val networkType: String = "Buscando red...",
    val signalQuality: Int = 0
)

/**
 * USSD needs voice-network reachability, not data or Wi-Fi — this warns before a code is dialed
 * with no/weak signal. Polls `TelephonyManager` every 5s rather than registering a listener,
 * which is plenty responsive for a passive banner and avoids the API-level branching a
 * `TelephonyCallback`/`PhoneStateListener` split would need. Ported from iOS's ConnectionBannerView.
 */
@Composable
fun ConnectionBanner() {
    val context = LocalContext.current
    var status by remember { mutableStateOf(CellularSignalStatus()) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    // Signal quality reads TelephonyManager.dataNetworkType, which needs READ_PHONE_STATE —
    // requested here (not at app launch) since this only runs while the banner is switched on.
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
    }

    LaunchedEffect(hasPermission) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        while (true) {
            status = if (hasPermission) readCellularSignalStatus(telephonyManager) else CellularSignalStatus()
            delay(5000)
        }
    }

    val statusColor = when {
        !status.hasService -> Color(0xFFE53935)
        status.signalQuality == 3 -> Color(0xFF43A047)
        status.signalQuality == 2 -> MaterialTheme.colorScheme.primary
        status.signalQuality == 1 -> Color(0xFFFB8C00)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusText = when {
        !status.hasService -> "Sin señal — el USSD no funcionará"
        status.signalQuality == 3 -> "Señal óptima (${status.networkType})"
        status.signalQuality == 2 -> "Señal buena (${status.networkType})"
        status.signalQuality == 1 -> "Señal débil (riesgo de fallo)"
        else -> "Buscando red..."
    }
    val icon = when {
        !status.hasService -> Icons.Filled.SignalCellularOff
        status.signalQuality == 1 -> Icons.Filled.Warning
        else -> Icons.Filled.SignalCellularAlt
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(statusColor.copy(alpha = 0.15f))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
        Text(
            statusText,
            style = MaterialTheme.typography.labelMedium,
            color = statusColor,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

private fun readCellularSignalStatus(telephonyManager: TelephonyManager?): CellularSignalStatus {
    val networkType = try {
        telephonyManager?.dataNetworkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
    } catch (e: SecurityException) {
        TelephonyManager.NETWORK_TYPE_UNKNOWN
    }
    return when (networkType) {
        TelephonyManager.NETWORK_TYPE_NR ->
            CellularSignalStatus(hasService = true, networkType = "5G", signalQuality = 3)
        TelephonyManager.NETWORK_TYPE_LTE ->
            CellularSignalStatus(hasService = true, networkType = "4G / LTE", signalQuality = 3)
        TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_EVDO_0,
        TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_EVDO_B,
        TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_CDMA ->
            CellularSignalStatus(hasService = true, networkType = "3G", signalQuality = 2)
        TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS ->
            CellularSignalStatus(hasService = true, networkType = "2G / EDGE", signalQuality = 1)
        TelephonyManager.NETWORK_TYPE_UNKNOWN ->
            CellularSignalStatus(hasService = false, networkType = "Sin servicio celular", signalQuality = 0)
        else ->
            CellularSignalStatus(hasService = true, networkType = "Red celular", signalQuality = 2)
    }
}
