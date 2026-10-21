package com.qvacell.app.ui.components

import android.os.SystemClock
import android.view.ViewConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

val LocalDashboardCardTapMode = compositionLocalOf { "tap_confirm" }

@Composable
fun Modifier.dashboardQueryGesture(onQuery: () -> Unit): Modifier {
    val mode = LocalDashboardCardTapMode.current
    val currentOnQuery by rememberUpdatedState(onQuery)
    return when (mode) {
        "long", "long_confirm" -> pointerInput(mode) {
            detectTapGestures(onLongPress = { currentOnQuery() })
        }.semantics { onClick { currentOnQuery(); true } }

        "double_tap", "triple_tap" -> pointerInput(mode) {
            val requiredTaps = if (mode == "double_tap") 2 else 3
            val timeout = ViewConfiguration.getDoubleTapTimeout().toLong()
            val maxDistance = 48.dp.toPx()
            var tapCount = 0
            var previousTapTime = 0L
            var previousTapPosition = Offset.Zero
            detectTapGestures(onTap = { position ->
                val now = SystemClock.uptimeMillis()
                tapCount = if (now - previousTapTime <= timeout &&
                    (position - previousTapPosition).getDistance() <= maxDistance
                ) tapCount + 1 else 1
                previousTapTime = now
                previousTapPosition = position
                if (tapCount == requiredTaps) {
                    tapCount = 0
                    currentOnQuery()
                }
            })
        }.semantics { onClick { currentOnQuery(); true } }

        else -> clickable { currentOnQuery() }
    }
}
