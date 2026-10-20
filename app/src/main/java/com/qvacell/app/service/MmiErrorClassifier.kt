package com.qvacell.app.service

import java.text.Normalizer
import java.util.Locale

/** Recognizes localized Android/carrier messages that indicate an invalid MMI request. */
internal object MmiErrorClassifier {
    fun isMmiErrorResponse(response: String): Boolean {
        val normalized = Normalizer.normalize(response, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
        val mentionsMmi = Regex("\\bmmi\\b").containsMatchIn(normalized)
        val indicatesFailure = listOf(
            "conexion", "connection", "problema", "problem", "incorrect", "wrong",
            "invalid", "no es valido", "not valid"
        ).any { it in normalized }
        return mentionsMmi && indicatesFailure
    }
}
