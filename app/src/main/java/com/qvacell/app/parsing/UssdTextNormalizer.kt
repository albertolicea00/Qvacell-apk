package com.qvacell.app.parsing

import java.text.Normalizer

/** Normalizes presentation-only differences while preserving line breaks and numeric punctuation. */
internal object UssdTextNormalizer {
    fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[\\u00A0\\u2000-\\u200A\\u202F\\u205F\\u3000]"), " ")
}
