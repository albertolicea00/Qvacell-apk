package com.qvacell.app.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Append-only fact: one dashboard metric's value as of a point in time. Rows are never updated
 * or deleted — full history is kept indefinitely for future trend graphs. [sourceKind]
 * distinguishes a USSD/SMS-confirmed reading ([SourceKind.USSD_REAL]/[SourceKind.SMS_REAL]) from
 * a background-estimated guess ([SourceKind.ESTIMATED]) so the repository can prefer real data.
 */
@Entity(
    tableName = "dashboard_value_snapshots",
    indices = [Index(value = ["fieldType", "capturedAt"])]
)
data class DashboardValueSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fieldType: String,
    val numericValue: Double? = null,
    val textValue: String? = null,
    val dateValue: Long? = null,
    val unit: String? = null,
    val capturedAt: Long,
    val sourceSignalId: String,
    val sourceKind: String
)

object SourceKind {
    const val USSD_REAL = "USSD_REAL"
    const val SMS_REAL = "SMS_REAL"
    const val ESTIMATED = "ESTIMATED"
}
