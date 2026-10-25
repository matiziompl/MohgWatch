package com.mohgwatch.core.model

import org.junit.Assert.*
import org.junit.Test

class GlucoseHistoryPolicyTest {
    private val now = 100_000_000L
    private fun reading(timestamp: Long, value: Float = 100f) =
        GlucoseReading(value, TrendArrow.STABLE, MeasurementColor.IN_RANGE, timestamp, false, false)

    @Test fun deduplicatesTimestampsKeepingTheNewerValueAndSorts() {
        val merged = GlucoseHistoryPolicy.merge(
            listOf(reading(now - 2, 90f), reading(now - 1)),
            listOf(reading(now - 2, 110f), reading(now - 3)), now
        )
        assertEquals(listOf(now - 3, now - 2, now - 1), merged.map { it.timestamp })
        assertEquals(110f, merged[1].value)
    }

    @Test fun dropsOldAndInvalidFutureSamples() {
        val cutoff = now - 24 * 60 * 60_000L
        val merged = GlucoseHistoryPolicy.merge(emptyList(), listOf(
            reading(0), reading(cutoff - 1), reading(cutoff), reading(now), reading(now + 1)
        ), now)
        assertEquals(listOf(cutoff, now), merged.map { it.timestamp })
    }
}
