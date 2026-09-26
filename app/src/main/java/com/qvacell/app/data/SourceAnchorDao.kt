package com.qvacell.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.qvacell.app.model.SourceAnchor
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceAnchorDao {
    @Query("SELECT * FROM source_anchors WHERE signalId = :signalId")
    suspend fun get(signalId: String): SourceAnchor?

    @Query("SELECT * FROM source_anchors")
    fun observeAll(): Flow<List<SourceAnchor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(anchor: SourceAnchor)

    /** Most recent confirmed (non-estimated) capture across every signal — the only timestamp
     *  the estimation engine trusts as "since when do we estimate usage". */
    @Query("SELECT MAX(lastSuccessfulParseAt) FROM source_anchors")
    suspend fun latestSuccessfulParseAt(): Long?
}
