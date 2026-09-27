package com.qvacell.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.qvacell.app.model.DashboardValueSnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardValueSnapshotDao {
    // No @Update/@Delete exposed — append-only by construction, history is never pruned.
    @Insert
    suspend fun insert(snapshot: DashboardValueSnapshot): Long

    @Query("SELECT * FROM dashboard_value_snapshots WHERE fieldType = :fieldType ORDER BY capturedAt ASC")
    fun observeHistory(fieldType: String): Flow<List<DashboardValueSnapshot>>

    // Grouped by MAX(capturedAt), not MAX(id): historical SMS backfill inserts old rows AFTER
    // (with a higher id than) a real-time capture that already exists, which would make MAX(id)
    // wrongly surface the older backfilled row as "latest".
    //
    // Also grouped by (fieldType, isReal) rather than fieldType alone: the repository's
    // reconciliation rule requires "real always wins over estimate" regardless of recency, which
    // only works if the latest REAL row and the latest ESTIMATED row for a field can both reach
    // the caller. Grouping by fieldType alone would return just the single most-recent row overall
    // — if an estimate happened to be newer, the real row would never surface at all, silently
    // breaking the precedence rule. This returns up to two rows per field (latest real + latest
    // estimated); `reconcile()` picks between them.
    @Query(
        """
        SELECT s.* FROM dashboard_value_snapshots s
        INNER JOIN (
            SELECT fieldType, (sourceKind != 'ESTIMATED') AS isReal, MAX(capturedAt) AS maxCapturedAt
            FROM dashboard_value_snapshots GROUP BY fieldType, isReal
        ) latest ON s.fieldType = latest.fieldType
            AND s.capturedAt = latest.maxCapturedAt
            AND (s.sourceKind != 'ESTIMATED') = latest.isReal
        """
    )
    fun observeAllLatestPerField(): Flow<List<DashboardValueSnapshot>>
}
