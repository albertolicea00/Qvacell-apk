package com.qvacell.app.service

import android.content.Context
import com.qvacell.app.data.CatalogRepository

object OnboardingDataPrefetch {
    fun startBackgroundFetch(context: Context) {
        val catalogRepository = CatalogRepository(context)
        val dashboardRepository = DashboardDataRepository(context)
        DashboardRefreshService.refresh(context, catalogRepository, dashboardRepository)
    }
}
