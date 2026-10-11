package com.qvacell.app.service

import android.content.Context
import com.qvacell.app.data.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object DashboardRefreshService {
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _currentCodeId = MutableStateFlow<String?>(null)
    val currentCodeId: StateFlow<String?> = _currentCodeId

    fun setCurrentCode(codeId: String?) {
        _currentCodeId.value = codeId
    }

    fun refresh(context: Context, catalogRepository: CatalogRepository, dashboardRepository: DashboardDataRepository) = Unit
}
