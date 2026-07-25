package com.mohgwatch.wear.data

import android.content.Context
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

    private val dao = GlucoseDatabase.getInstance(context).glucoseDao()

    companion object {
        private const val TWELVE_HOURS_MS = 12 * 60 * 60 * 1000L
        private const val TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L
        private const val FRESH_DATA_LIMIT_MS = 15 * 60 * 1000L  // 15 min
        private const val STALE_DATA_LIMIT_MS = 60 * 60 * 1000L  // 60 min
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
        dao.insert(GlucoseReadingEntity.fromGlucoseReading(reading))
    }

    /** Zapisz listę odczytów (historia) */
    suspend fun insertReadings(readings: List<GlucoseReading>) {
        dao.insertAll(readings.map { GlucoseReadingEntity.fromGlucoseReading(it) })
    }

    /** Usuń odczyty starsze niż 24h */
    suspend fun cleanup() {
        val before = System.currentTimeMillis() - TWENTY_FOUR_HOURS_MS
        dao.deleteOlderThan(before)
    }

    /** Minuty od ostatniego odczytu */
    suspend fun getMinutesSinceLastReading(): Long {
        val latest = dao.getLatest() ?: return Long.MAX_VALUE
        return (System.currentTimeMillis() - latest.timestamp) / 60_000L
    }

    /** Czy dane są świeże (< 15 min) */
    suspend fun isDataFresh(): Boolean {
        val latest = dao.getLatest() ?: return false
        return (System.currentTimeMillis() - latest.timestamp) < FRESH_DATA_LIMIT_MS
    }

    /** Czy dane są nieaktualne (> 60 min) */
    suspend fun isDataStale(): Boolean {
        val latest = dao.getLatest() ?: return true
        return (System.currentTimeMillis() - latest.timestamp) > STALE_DATA_LIMIT_MS
    }
}
