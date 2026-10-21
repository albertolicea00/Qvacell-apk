package com.qvacell.app.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MmiErrorClassifierTest {

    @Test
    fun recognizesReportedSpanishAndEnglishCarrierMessages() {
        listOf(
            "Se ha producido un problema de conexión o el código MMI no es válido.",
            "Problema de conexión o código incorrecto de MMI.",
            "Connection problem or invalid MMI code."
        ).forEach { response ->
            assertTrue("Expected MMI failure: $response", MmiErrorClassifier.isMmiErrorResponse(response))
        }
    }

    @Test
    fun recognizesAccentCaseAndPunctuationVariants() {
        listOf(
            "PROBLEMA DE CONEXION: codigo mmi incorrecto",
            "Error: código MMI no es válido",
            "Connection problem — invalid mMi code!",
            "The MMI code is not valid"
        ).forEach { response ->
            assertTrue("Expected MMI failure: $response", MmiErrorClassifier.isMmiErrorResponse(response))
        }
    }

    @Test
    fun doesNotClassifyUnrelatedOrSuccessfulResponsesAsMmiFailures() {
        listOf(
            "No hay señal de red. Inténtalo más tarde.",
            "Connection problem. Please try again.",
            "The MMI request was completed successfully.",
            "The MMI code is valid."
        ).forEach { response ->
            assertFalse("Unexpected MMI failure: $response", MmiErrorClassifier.isMmiErrorResponse(response))
        }
    }
}
