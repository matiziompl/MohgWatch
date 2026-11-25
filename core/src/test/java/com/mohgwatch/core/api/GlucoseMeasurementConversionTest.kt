package com.mohgwatch.core.api

import com.mohgwatch.core.api.dto.GlucoseMeasurement
import org.junit.Assert.*
import org.junit.Test

class GlucoseMeasurementConversionTest {
    private fun measurement(timestamp: String? = null, factory: String? = null) =
        GlucoseMeasurement(100f, 100f, timestamp, factory, 3)

    @Test fun `missing or invalid measurement time cannot become a fresh reading`() {
        assertNull(measurement().toGlucoseReading())
        assertNull(measurement("invalid", "invalid").toGlucoseReading())
    }

    @Test fun `factory time preserves actual measurement age when local time is invalid`() {
        val reading = measurement("invalid", "2026-10-01T12:34:56").toGlucoseReading()!!
        assertEquals(1_790_858_096_000L, reading.timestamp)
        assertFalse(reading.isFreshAt(reading.timestamp + 10 * 60_000L))
    }
}
