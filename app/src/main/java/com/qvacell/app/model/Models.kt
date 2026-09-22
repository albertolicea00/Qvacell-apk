package com.qvacell.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocalizedString(
    val es: String,
    val en: String
) {
    // App UI copy is Spanish-only for now (no locale switch wired up yet).
    val value: String get() = es

    override fun toString(): String = value
}

@Serializable
data class UssdCatalog(
    val version: Int,
    val carrier: String,
    val categories: List<UssdCategory>
)

@Serializable
data class UssdCategory(
    val id: String,
    val name: LocalizedString,
    val icon: String,
    val groups: List<UssdCodeGroup>
)

@Serializable
data class UssdCodeGroup(
    val name: LocalizedString? = null,
    val icon: String? = null,
    val codes: List<UssdCode>
)

@Serializable
enum class UssdActionType {
    @SerialName("ussd") USSD,
    @SerialName("call") CALL,
    @SerialName("sms") SMS
}

@Serializable
data class SmsVariant(
    val label: LocalizedString,
    val smsBody: String
)

@Serializable
data class UssdCode(
    val id: String,
    val code: String,
    val title: LocalizedString,
    val details: LocalizedString,
    val icon: String? = null,
    val price: String? = null,
    val compact: Boolean? = null,
    val showsNumber: Boolean? = null,
    val type: UssdActionType,
    val requiresInput: Boolean,
    val inputPlaceholder: String? = null,
    val noConfirmCode: String? = null,
    val smsBody: String? = null,
    val options: List<String>? = null,
    val isSubscription: Boolean? = null,
    val variants: List<SmsVariant>? = null
) {
    fun resolvedCode(input: String? = null): String =
        if (input != null) code.replace("{input}", input) else code

    fun resolvedSmsBody(input: String? = null): String {
        val body = smsBody ?: return ""
        return if (input != null) body.replace("{input}", input) else body
    }
}

@Serializable
data class WifiRoom(
    val name: String,
    val address: String? = null,
    val positions: Int? = null
)

@Serializable
data class WifiHotspotGroup(
    val municipality: String,
    val spots: List<String>
)

@Serializable
data class WifiProvince(
    val province: String,
    val rooms: List<WifiRoom> = emptyList(),
    val hotspots: List<WifiHotspotGroup> = emptyList()
)
