package com.qvacell.app.service

import android.provider.CallLog
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageEstimateAccumulatorTest {
    @Test
    fun countsOnlyOutgoingCallsToValidCubanMobiles() {
        val estimate = UsageEstimateAccumulator()
        estimate.addCall(type = CallLog.Calls.OUTGOING_TYPE, outgoingType = CallLog.Calls.OUTGOING_TYPE, number = "+53 5123-4567", durationSeconds = 125)
        estimate.addCall(type = CallLog.Calls.INCOMING_TYPE, outgoingType = CallLog.Calls.OUTGOING_TYPE, number = "61234567", durationSeconds = 600)
        estimate.addCall(type = CallLog.Calls.OUTGOING_TYPE, outgoingType = CallLog.Calls.OUTGOING_TYPE, number = "21234567", durationSeconds = 300)
        estimate.addCall(type = CallLog.Calls.OUTGOING_TYPE, outgoingType = CallLog.Calls.OUTGOING_TYPE, number = null, durationSeconds = 300)

        assertEquals(1, estimate.callsMade)
        assertEquals(2L, estimate.voiceMinutes)
    }

    @Test
    fun countsOnlySentSmsWithValidCubanMobileAddresses() {
        val estimate = UsageEstimateAccumulator()
        estimate.addSentSms("+53 6123-4567")
        estimate.addSentSms("12345678")
        estimate.addSentSms(null)

        assertEquals(1, estimate.smsSent)
    }

    @Test
    fun emptyLogsProduceZeroDeltas() {
        val estimate = UsageEstimateAccumulator()
        assertEquals(0, estimate.callsMade)
        assertEquals(0L, estimate.voiceMinutes)
        assertEquals(0, estimate.smsSent)
    }
}
