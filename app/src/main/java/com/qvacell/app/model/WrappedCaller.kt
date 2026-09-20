package com.qvacell.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Maps an ETECSA *99-wrapped collect-call number to a locally known contact name. */
@Entity(tableName = "wrapped_callers")
data class WrappedCaller(
    @PrimaryKey val wrappedNumber: Long,
    val name: String
) {
    companion object {
        /**
         * ETECSA wraps a *99 collect call's caller ID as "99" + "53" + the 8-digit local
         * number + "99" (e.g. "51234567" -> "99535123456799"). [localNumber] must already be
         * the normalized 8-digit form (country code stripped) from CubanPhoneNumber.normalize.
         */
        fun wrappedNumber(localNumber: String): Long? {
            if (localNumber.length != 8) return null
            return ("99" + "53" + localNumber + "99").toLongOrNull()
        }
    }
}
