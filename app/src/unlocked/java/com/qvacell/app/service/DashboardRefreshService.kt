package com.qvacell.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.qvacell.app.R
import com.qvacell.app.data.CatalogRepository
import com.qvacell.app.data.FieldTypes
import com.qvacell.app.data.SettingsDataStore
import com.qvacell.app.model.UssdCode
import com.qvacell.app.ui.components.hasCellularService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object DashboardRefreshService {

    private val REFRESH_ORDER = listOf(
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

    private val _refreshFailure = MutableStateFlow<String?>(null)
    val refreshFailure: StateFlow<String?> = _refreshFailure

    private val _lineTypeSuggestion = MutableStateFlow<LineTypeSuggestion?>(null)
    val lineTypeSuggestion: StateFlow<LineTypeSuggestion?> = _lineTypeSuggestion

    fun clearLineTypeSuggestion() {
        _lineTypeSuggestion.value = null
        _refreshFailure.value = null
    }

    fun setCurrentCode(codeId: String?) {
        _currentCodeId.value = codeId
    }

    fun refresh(context: Context, catalogRepository: CatalogRepository, dashboardRepository: DashboardDataRepository) {
        if (_isRefreshing.value) return
        _lineTypeSuggestion.value = null
        if (!DialService.hasCallPermission(context)) {
            _refreshFailure.value = context.getString(R.string.dashboard_refresh_call_permission)
            return
        }
        if (!hasDashboardService(context)) {
            _refreshFailure.value = context.getString(R.string.dashboard_refresh_no_service)
            return
        }

        _isRefreshing.value = true
        _refreshFailure.value = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val line = selectedDashboardLine(context)
                if (line.needsSelection) {
                    _refreshFailure.value = context.getString(R.string.dashboard_choose_sim)
                    return@launch
                }
                var skipVoice = false
                var skipSms = false
                var skipData = false

                val refreshCodes = if (line.isCorporate) listOf(line.balanceCodeId)
                    else listOf(line.balanceCodeId) + REFRESH_ORDER
                for (codeId in refreshCodes) {
                    if (skipVoice && codeId == "voice-balance") continue
                    if (skipSms && codeId == "sms-balance") continue
                    if (skipData && codeId == "data-plan") continue

                    _currentCodeId.value = codeId
                    val code = catalogRepository.findCodeById(codeId) ?: break
                    val result = requestUssdSuspend(context, code, line.subscriptionId)
                    when (result) {
                        is UssdCaptureResult.Success -> {
                            if (codeId == line.balanceCodeId && MmiErrorClassifier.isMmiErrorResponse(result.response)) {
                                if (!suggestLineTypeChange(context, line, code)) {
                                    _refreshFailure.value = context.getString(R.string.dashboard_refresh_parse_failed_batch)
                                }
                                break
                            }
                            if (!dashboardRepository.recordUssdParse(
                                    codeId, result.response, subscriptionId = line.subscriptionId
                                )) {
                                _refreshFailure.value = context.getString(R.string.dashboard_refresh_parse_failed_batch)
                                break
                            }
                        }
                        is UssdCaptureResult.Failure -> {
                            reportFailure(context, result.reason, batch = true, line = line, code = code)
                            break
                        }
                    }

                    if (codeId == line.balanceCodeId) {
                        val current = dashboardRepository.getCurrentValues(line.subscriptionId)
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

    /** Refresh one dynamic-dashboard card without opening the system dialer on failure. */
    fun refreshCode(context: Context, code: UssdCode, dashboardRepository: DashboardDataRepository) {
        if (_isRefreshing.value) return
        _lineTypeSuggestion.value = null
        if (!DialService.hasCallPermission(context)) {
            _refreshFailure.value = context.getString(R.string.dashboard_refresh_call_permission)
            return
        }
        if (!hasDashboardService(context)) {
            _refreshFailure.value = context.getString(R.string.dashboard_refresh_no_service_query)
            return
        }

        _isRefreshing.value = true
        _refreshFailure.value = null
        _currentCodeId.value = code.id
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val line = selectedDashboardLine(context)
                if (line.needsSelection) {
                    _refreshFailure.value = context.getString(R.string.dashboard_choose_sim)
                    return@launch
                }
                if (code.id in setOf("main-balance", "corp-balance") && code.id != line.balanceCodeId) {
                    _refreshFailure.value = context.getString(R.string.dashboard_refresh_sim_not_applicable)
                    return@launch
                }
                when (val result = requestUssdSuspend(context, code, line.subscriptionId)) {
                    is UssdCaptureResult.Success -> {
                        if (code.id == line.balanceCodeId && MmiErrorClassifier.isMmiErrorResponse(result.response)) {
                            if (!suggestLineTypeChange(context, line, code)) {
                                _refreshFailure.value = context.getString(R.string.dashboard_refresh_parse_failed)
                            }
                            return@launch
                        }
                        if (!dashboardRepository.recordUssdParse(
                                code.id, result.response, subscriptionId = line.subscriptionId
                            )) {
                            _refreshFailure.value = context.getString(R.string.dashboard_refresh_parse_failed)
                        }
                    }
                    is UssdCaptureResult.Failure -> reportFailure(
                        context, result.reason, batch = false, line = line, code = code
                    )
                }
            } finally {
                _currentCodeId.value = null
                _isRefreshing.value = false
            }
        }
    }

    private fun reportFailure(
        context: Context,
        reason: String,
        batch: Boolean,
        line: SimUtils.DashboardLine,
        code: UssdCode
    ) {
        if (reason == "service_lost" || !hasDashboardService(context)) {
            _refreshFailure.value = context.getString(R.string.dashboard_refresh_service_lost)
            return
        }
        if (code.id == line.balanceCodeId &&
            reason == "carrier_failure_${TelephonyManager.USSD_RETURN_FAILURE}" &&
            suggestLineTypeChange(context, line, code)
        ) {
            return
        }
        val cause = context.getString(
            if (reason == "timeout") R.string.dashboard_refresh_ussd_timeout
            else R.string.dashboard_refresh_ussd_failed
        )
        val stopped = if (batch) context.getString(R.string.dashboard_refresh_stopped) else null
        val advice = context.getString(
            if (stillOnLte(context)) R.string.dashboard_refresh_advice_4g
            else R.string.dashboard_refresh_advice_signal
        )
        _refreshFailure.value = listOfNotNull(cause, stopped, advice).joinToString(" ")
    }

    private fun suggestLineTypeChange(
        context: Context,
        line: SimUtils.DashboardLine,
        code: UssdCode
    ): Boolean {
        val subscriptionId = line.subscriptionId ?: return false
        _lineTypeSuggestion.value = LineTypeSuggestion(
            subscriptionId = subscriptionId,
            isCorporate = !line.isCorporate,
            failedCode = code.resolvedCode()
        )
        _refreshFailure.value = context.getString(
            if (line.isCorporate) R.string.dashboard_refresh_possible_personal
            else R.string.dashboard_refresh_possible_corporate
        )
        return true
    }

    private suspend fun requestUssdSuspend(
        context: Context,
        code: UssdCode,
        subscriptionId: Int?
    ): UssdCaptureResult = suspendCancellableCoroutine { cont ->
        if (!hasDashboardService(context)) {
            cont.resume(UssdCaptureResult.Failure("service_lost"))
            return@suspendCancellableCoroutine
        }
        UssdCaptureService.requestUssd(context, code, subscriptionId) { result ->
            if (cont.isActive) cont.resume(result)
        }
    }

    private suspend fun selectedDashboardLine(context: Context): SimUtils.DashboardLine {
        val settings = SettingsDataStore(context)
        return SimUtils.resolveDashboardLine(
            context,
            settings.selectedSimSlot.first(),
            settings.simAppearances.first()
        )
    }

    private fun hasDashboardService(context: Context): Boolean {
        if (Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1) return false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val state = try {
                telephonyManager?.serviceState?.state
            } catch (_: SecurityException) {
                null
            } catch (_: UnsupportedOperationException) {
                null
            }
            if (state == ServiceState.STATE_OUT_OF_SERVICE || state == ServiceState.STATE_POWER_OFF ||
                state == ServiceState.STATE_EMERGENCY_ONLY
            ) return false
        }
        return hasCellularService(context)
    }

    private fun stillOnLte(context: Context): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED) return false
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return false
        return try {
            val voice = telephonyManager.voiceNetworkType
            val observed = if (voice != TelephonyManager.NETWORK_TYPE_UNKNOWN) voice
                else telephonyManager.dataNetworkType
            observed == TelephonyManager.NETWORK_TYPE_LTE || observed == TelephonyManager.NETWORK_TYPE_NR
        } catch (_: SecurityException) {
            false
        }
    }
}
