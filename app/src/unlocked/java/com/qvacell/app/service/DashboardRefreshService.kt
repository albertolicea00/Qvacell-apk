package com.qvacell.app.service

import android.content.Context
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.data.FieldTypes
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

    private val _currentCodeId = MutableStateFlow<String?>(null)
    val currentCodeId: StateFlow<String?> = _currentCodeId

    fun setCurrentCode(codeId: String?) {
        _currentCodeId.value = codeId
    }

    fun refresh(context: Context, catalogRepository: CatalogRepository, dashboardRepository: DashboardDataRepository) {
        if (_isRefreshing.value) return
        if (!DialService.hasCallPermission(context)) return

        _isRefreshing.value = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var skipVoice = false
                var skipSms = false
                var skipData = false

                for (codeId in REFRESH_ORDER) {
                    if (skipVoice && codeId == "voice-balance") continue
                    if (skipSms && codeId == "sms-balance") continue
                    if (skipData && codeId == "data-plan") continue

                    _currentCodeId.value = codeId
                    val code = catalogRepository.findCodeById(codeId) ?: continue
                    val result = requestUssdSuspend(context, code)
                    if (result is UssdCaptureResult.Success) {
                        dashboardRepository.recordUssdParse(codeId, result.response)
                    }

                    if (codeId == "main-balance") {
                        val current = dashboardRepository.getCurrentValues()
                        skipVoice = current[FieldTypes.VOICE_MINUTES_REMAINING] == null
                        skipSms = current[FieldTypes.SMS_COUNT_REMAINING] == null
                        skipData = current[FieldTypes.DATA_DATOS_MB] == null
                    }
                }
            } finally {
                _currentCodeId.value = null
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
