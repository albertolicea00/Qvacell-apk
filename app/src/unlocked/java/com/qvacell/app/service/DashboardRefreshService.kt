package com.qvacell.app.service

import android.content.Context
import com.qvacell.app.data.CatalogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object DashboardRefreshService {

    private val REFRESH_ORDER = listOf(
        "main-balance",
        "bonus-usd-plans",
        "data-plan",
        "national-recharge-limit",
        "voice-balance",
        "sms-balance"
    )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    fun refresh(context: Context, catalogRepository: CatalogRepository, dashboardRepository: DashboardDataRepository) {
        if (_isRefreshing.value) return
        if (!DialService.hasCallPermission(context)) return

        _isRefreshing.value = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (codeId in REFRESH_ORDER) {
                    val code = catalogRepository.findCodeById(codeId) ?: continue
                    val result = requestUssdSuspend(context, code)
                    if (result is UssdCaptureResult.Success) {
                        dashboardRepository.recordUssdParse(codeId, result.response)
                    }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun requestUssdSuspend(
        context: Context,
        code: com.qvacell.app.model.UssdCode
    ): UssdCaptureResult = suspendCancellableCoroutine { cont ->
        UssdCaptureService.requestUssd(context, code) { result ->
            if (cont.isActive) cont.resume(result)
        }
    }
}
