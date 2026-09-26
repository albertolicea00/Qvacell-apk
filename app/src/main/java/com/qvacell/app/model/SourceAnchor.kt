package com.qvacell.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per independent capture signal (a specific USSD code id, a specific SMS message kind) —
 * NOT a single global "last known date". [lastSeenAt]/[lastSeenRawText] advance whenever raw text
 * arrives for that signal, whether or not it parses. [lastSuccessfulParseAt]/[lastSuccessfulRawText]
 * only advance on a successful parse — this is the only anchor the estimation engine trusts, so
 * clearing the call log or SMS history can never break it the way a naive
 * "look back from now" delta would.
 */
@Entity(tableName = "source_anchors")
data class SourceAnchor(
    @PrimaryKey val signalId: String,
    val signalKind: String,
    val lastSeenAt: Long,
    val lastSeenRawText: String,
    val lastSuccessfulParseAt: Long? = null,
    val lastSuccessfulRawText: String? = null
)
