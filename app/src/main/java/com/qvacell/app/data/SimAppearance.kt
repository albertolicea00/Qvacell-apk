package com.qvacell.app.data

import kotlinx.serialization.Serializable

@Serializable
data class SimAppearance(
    val name: String = "",
    val colorHex: String? = null,
    val icon: String = "number",
    val isCorporate: Boolean = false
)
