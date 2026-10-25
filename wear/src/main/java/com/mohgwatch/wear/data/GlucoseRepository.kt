package com.mohgwatch.wear.data

import android.content.Context
import androidx.room.withTransaction
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.wear.data.db.GlucoseDatabase
import com.mohgwatch.wear.data.db.GlucoseReadingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repozytorium danych glukozy na zegarku.
 * Zarządza lokalną bazą Room z odczytami otrzymanymi z telefonu.
 */
class GlucoseRepository(context: Context) {

    private val database = GlucoseDatabase.getInstance(context)
    private val dao = database.glucoseDao()

    companion object {
        private const val TWELVE_HOURS_MS = 12 * 60 * 60 * 1000L
        private const val TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L
    }

    /** Obserwuj najnowszy odczyt (Flow) */
    fun observeLatest(): Flow<GlucoseReading?> =
        dao.observeLatest().map { it?.toGlucoseReading() }

    /** Pobierz najnowszy odczyt (one-shot) */
    suspend fun getLatest(): GlucoseReading? =
        dao.getLatest()?.toGlucoseReading()

    /** Pobierz odczyty z ostatnich 12h */
    suspend fun getLast12Hours(): List<GlucoseReading> {
        val since = System.currentTimeMillis() - TWELVE_HOURS_MS
        return dao.getReadingsSince(since).map { it.toGlucoseReading() }
    }

    /** Zapisz pojedynczy odczyt */
    suspend fun insertReading(reading: GlucoseReading) {
        insertReadings(listOf(reading))
    }

    /** Zapisz listę odczytów (historia) */
    suspend fun insertReadings(readings: List<GlucoseReading>) {
        val now = System.currentTimeMillis()
        val since = now - TWENTY_FOUR_HOURS_MS
        val retained = readings.filter { it.timestamp in since..now }
            .distinctBy { it.timestamp }
            .map { GlucoseReadingEntity.fromGlucoseReading(it) }
        database.withTransaction {
            dao.insertAll(retained)
            dao.deleteOlderThan(since)
        }
    }

}
