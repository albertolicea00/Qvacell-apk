package com.qvacell.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UssdCatalog(
    val version: Int,
    val carrier: String,
    val categories: List<UssdCategory>
)

@Serializable
data class UssdCategory(
    val id: String,
    val name: String,
    val icon: String,
    val groups: List<UssdCodeGroup>
)

@Serializable
data class UssdCodeGroup(
    val name: String? = null,
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
    val label: String,
    val smsBody: String
)

@Serializable
data class UssdCode(
    val id: String,
    val code: String,
    val title: String,
    val details: String,
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
