package com.qvacell.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Legacy schema retained for database compatibility. Identification belongs to LlamaCon99. */
@Entity(tableName = "wrapped_callers")
data class WrappedCaller(
    @PrimaryKey val wrappedNumber: Long,
    val name: String
)
