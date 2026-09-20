package com.qvacell.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.qvacell.app.model.WrappedCaller

@Dao
interface WrappedCallerDao {
    @Query("SELECT * FROM wrapped_callers WHERE wrappedNumber = :number LIMIT 1")
    suspend fun findByNumber(number: Long): WrappedCaller?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(callers: List<WrappedCaller>)

    @Query("DELETE FROM wrapped_callers")
    suspend fun clear()
}
