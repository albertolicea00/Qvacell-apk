package com.qvacell.app.data

/**
 * String keys for [com.qvacell.app.model.DashboardValueSnapshot.fieldType] — plain strings, not
 * an enum, so a new metric discovered from real USSD/SMS text later is a one-line addition here,
 * with zero Room schema migration.
 */
object FieldTypes {
    const val MAIN_BALANCE = "main_balance"
    const val LINE_ACTIVE_UNTIL = "line_active_until"
    const val ACCOUNT_DUE_DATE = "account_due_date"
    const val BONUS_USD_AMOUNT = "bonus_usd_amount"
    const val DATA_PLAN_GB = "data_plan_gb"
    const val DATA_DAYS_REMAINING = "data_days_remaining"
    const val VOICE_MINUTES_REMAINING = "voice_minutes_remaining"
    const val VOICE_DAYS_REMAINING = "voice_days_remaining"
    const val SMS_COUNT_REMAINING = "sms_count_remaining"
    const val SMS_DAYS_REMAINING = "sms_days_remaining"
    const val NATIONAL_RECHARGE_LIMIT_AMOUNT = "national_recharge_limit_amount"
    const val NATIONAL_RECHARGE_LIMIT_AVAILABLE_FROM = "national_recharge_limit_available_from"

    // Estimation-engine-only fields — Cuban-numbers-only usage since the last confirmed anchor.
    const val CALLS_MADE_ESTIMATE = "calls_made_estimate"
    const val VOICE_MINUTES_ESTIMATE = "voice_minutes_estimate"
    const val SMS_SENT_ESTIMATE = "sms_sent_estimate"
}
