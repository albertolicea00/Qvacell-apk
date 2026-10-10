package com.qvacell.app.service

import android.content.Context
import com.qvacell.app.data.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object DashboardRefreshService {
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    fun refresh(context: Context, catalogRepository: CatalogRepository, dashboardRepository: DashboardDataRepository) = Unit
}
