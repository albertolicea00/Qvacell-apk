package com.qvacell.app.service

import android.content.Context
import com.qvacell.app.model.UssdCode

/**
 * Play-flavor stand-in — same package + object name + function signatures as the full-flavor
 * implementation in app/src/full, so app/src/main call sites compile unchanged against either
 * flavor. No capture, no new permissions: always falls back to the plain system-dialer flow.
 */
object DashboardCapture {
    fun captureOrDial(
        context: Context,
        code: UssdCode,
        repository: DashboardDataRepository,
        captureEnabled: Boolean
    ) {
        DialService.dial(context, code.resolvedCode())
    }

    fun scheduleEstimationIfEnabled(context: Context, enabled: Boolean) = Unit

    fun triggerSmsBackfill(context: Context, repository: DashboardDataRepository) = Unit
}
