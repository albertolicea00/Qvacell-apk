package com.qvacell.app.service

import android.content.Context

object OnboardingDataPrefetch {
    // Called when the user picks "dynamic" mode during onboarding.
    // Fires background data fetch while they read the rest of the tour, so the dashboard
    // is already warmed up when they land on the home screen.
    fun startBackgroundFetch(context: Context) {
        // TODO: wire to EstimationEngine / DashboardCapture pipeline once prefetch API is stable
        // e.g. DashboardCapture.scheduleEstimationIfEnabled(context, enabled = true)
    }
}
