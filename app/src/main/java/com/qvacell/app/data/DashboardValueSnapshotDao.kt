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
    @Query(
        """
        SELECT s.* FROM dashboard_value_snapshots s
        INNER JOIN (
            SELECT fieldType, MAX(capturedAt) AS maxCapturedAt
            FROM dashboard_value_snapshots GROUP BY fieldType
        ) latest ON s.fieldType = latest.fieldType AND s.capturedAt = latest.maxCapturedAt
        """
    )
    fun observeAllLatestPerField(): Flow<List<DashboardValueSnapshot>>
}
