package com.qvacell.app.service

import com.qvacell.app.model.CubanPhoneNumber

/** Pure counting rules shared by call-log and sent-SMS estimate collection. */
internal class UsageEstimateAccumulator {
    var callsMade: Int = 0
        private set
    var voiceMinutes: Long = 0
        private set
    var smsSent: Int = 0
        private set

    fun addCall(type: Int, outgoingType: Int, number: String?, durationSeconds: Long) {
        if (type != outgoingType || CubanPhoneNumber.normalize(number.orEmpty()) == null) return
        callsMade++
        voiceMinutes += durationSeconds / 60
    }

    fun addSentSms(address: String?) {
        if (CubanPhoneNumber.normalize(address.orEmpty()) != null) smsSent++
    }
}
