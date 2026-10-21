package com.qvacell.app.service

data class LineTypeSuggestion(
    val subscriptionId: Int,
    val isCorporate: Boolean,
    val failedCode: String
)
