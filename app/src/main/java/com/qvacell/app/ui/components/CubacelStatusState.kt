package com.qvacell.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.qvacell.app.service.CubacelStatus
import kotlinx.coroutines.delay

/** Recheck on selection changes and SIM insertion/removal, even when the network banner is off. */
@Composable
fun rememberCubacelStatus(selectedSlot: Int): State<CubacelStatus> {
    val context = LocalContext.current
    return produceState(CubacelStatus.read(context, selectedSlot), context, selectedSlot) {
        while (true) {
            value = CubacelStatus.read(context, selectedSlot)
            delay(2000)
        }
    }
}
