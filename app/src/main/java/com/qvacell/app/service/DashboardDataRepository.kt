package com.qvacell.app.service

import android.content.Context
import com.qvacell.app.data.QvacellDatabase
import com.qvacell.app.model.DashboardValueSnapshot
import com.qvacell.app.model.SourceAnchor
import com.qvacell.app.model.SourceKind
import com.qvacell.app.parsing.ParseResult
import com.qvacell.app.parsing.ParsedDashboardValue
import com.qvacell.app.parsing.SmsParsers
import com.qvacell.app.parsing.UssdParsers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reconciliation rule: a real (USSD/SMS-confirmed) value always wins over an estimate, regardless
 * of which is newer — estimates only fill a field that has no real value at all yet. This is a
 * deliberate simplification of "most-recent-wins across both": since estimates are heuristic
 * (call-log/SMS-log deltas), a confirmed reading should never be silently displaced by a guess.
 *
 * [database] is overridable so tests can pass an in-memory Room instance instead of the real
 * disk-backed singleton.
 */
class DashboardDataRepository(
    context: Context,
    database: QvacellDatabase = QvacellDatabase.get(context)
) {
    private val valueDao = database.dashboardValueSnapshotDao()
    private val anchorDao = database.sourceAnchorDao()

    fun observeCurrentValues(): Flow<Map<String, DashboardValueSnapshot>> =
        valueDao.observeAllLatestPerField().map(::reconcile)

    fun observeHistory(fieldType: String): Flow<List<DashboardValueSnapshot>> =
        valueDao.observeHistory(fieldType)

    fun observeAnchors(): Flow<List<SourceAnchor>> = anchorDao.observeAll()

    suspend fun latestConfirmedAnchorTimestamp(): Long? = anchorDao.latestSuccessfulParseAt()

    suspend fun recordUssdParse(codeId: String, rawText: String, capturedAt: Long = System.currentTimeMillis()) {
        val signalId = "ussd:$codeId"
        upsertAnchorSeen(signalId, "USSD", rawText, capturedAt)
        val result = UssdParsers.forCode(codeId).parse(rawText)
        if (result is ParseResult.Success) {
            result.value.forEach { valueDao.insert(it.toSnapshot(signalId, SourceKind.USSD_REAL, capturedAt)) }
            upsertAnchorParsed(signalId, "USSD", rawText, capturedAt)
        }
    }

    /** [messageTimestamp] MUST be the SMS's own timestamp (e.g. Telephony.Sms.DATE), never
     *  wall-clock "now" — a historical backfill import would otherwise collapse all history onto
     *  the import date, and freshness/reconciliation comparisons would be wrong. */
    suspend fun recordSmsBody(sender: String, rawBody: String, messageTimestamp: Long) {
        SmsParsers.all().forEach { parser ->
            if (parser.matches(rawBody, sender)) {
                val signalId = "sms:${parser.smsSignalId}"
                upsertAnchorSeen(signalId, "SMS", rawBody, messageTimestamp)
                val result = parser.parse(rawBody)
                if (result is ParseResult.Success) {
                    result.value.forEach { valueDao.insert(it.toSnapshot(signalId, SourceKind.SMS_REAL, messageTimestamp)) }
                    upsertAnchorParsed(signalId, "SMS", rawBody, messageTimestamp)
                }
            }
        }
    }

    /** Distinct write path from [recordUssdParse]/[recordSmsBody] — an estimate can never be
     *  mistaken for a confirmed reading by a caller. */
    suspend fun recordEstimate(values: List<ParsedDashboardValue>, estimatedAt: Long) {
        values.forEach { valueDao.insert(it.toSnapshot("estimation-engine", SourceKind.ESTIMATED, estimatedAt)) }
    }

    private suspend fun upsertAnchorSeen(signalId: String, kind: String, rawText: String, seenAt: Long) {
        val existing = anchorDao.get(signalId)
        anchorDao.upsert(
            SourceAnchor(
                signalId = signalId,
                signalKind = kind,
                lastSeenAt = seenAt,
                lastSeenRawText = rawText,
                lastSuccessfulParseAt = existing?.lastSuccessfulParseAt,
                lastSuccessfulRawText = existing?.lastSuccessfulRawText
            )
        )
    }

    private suspend fun upsertAnchorParsed(signalId: String, kind: String, rawText: String, parsedAt: Long) {
        val existing = anchorDao.get(signalId)
        anchorDao.upsert(
            SourceAnchor(
                signalId = signalId,
                signalKind = kind,
                lastSeenAt = existing?.lastSeenAt ?: parsedAt,
                lastSeenRawText = existing?.lastSeenRawText ?: rawText,
                lastSuccessfulParseAt = parsedAt,
                lastSuccessfulRawText = rawText
            )
        )
    }

    private fun reconcile(rows: List<DashboardValueSnapshot>): Map<String, DashboardValueSnapshot> =
        rows.groupBy { it.fieldType }.mapNotNull { (fieldType, snapshots) ->
            val real = snapshots.filter { it.sourceKind != SourceKind.ESTIMATED }.maxByOrNull { it.capturedAt }
            val chosen = real ?: snapshots.filter { it.sourceKind == SourceKind.ESTIMATED }.maxByOrNull { it.capturedAt }
            chosen?.let { fieldType to it }
        }.toMap()

    private fun ParsedDashboardValue.toSnapshot(sourceSignalId: String, sourceKind: String, capturedAt: Long) =
        DashboardValueSnapshot(
            fieldType = fieldType,
            numericValue = numericValue,
            textValue = textValue,
            dateValue = dateValue,
            unit = unit,
            capturedAt = capturedAt,
            sourceSignalId = sourceSignalId,
            sourceKind = sourceKind
        )
}
