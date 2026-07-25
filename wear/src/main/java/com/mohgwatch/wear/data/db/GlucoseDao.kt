package com.mohgwatch.wear.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GlucoseDao {
    @Query("SELECT * FROM readings ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): GlucoseReadingEntity?

    @Query("SELECT * FROM readings ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(): Flow<GlucoseReadingEntity?>

    @Query("SELECT * FROM readings WHERE timestamp > :since ORDER BY timestamp ASC")
    suspend fun getReadingsSince(since: Long): List<GlucoseReadingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<GlucoseReadingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: GlucoseReadingEntity)

    @Query("DELETE FROM readings WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("SELECT COUNT(*) FROM readings")
    suspend fun count(): Int
}
