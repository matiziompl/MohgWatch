package com.mohgwatch.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class GlucoseReadingFreshnessTest {
    private val reading = GlucoseReading(120f, TrendArrow.RISING, MeasurementColor.IN_RANGE, 1_000_000L, false, false)

    @Test fun expiresExactlyTenMinutesAfterMeasurement() {
        assertTrue(reading.isFreshAt(1_000_000L))
        assertTrue(reading.isFreshAt(1_599_999L))
        assertFalse(reading.isFreshAt(1_600_000L))
    }

    @Test fun invalidAndFutureMeasurementTimesAreNotFresh() {
        assertFalse(reading.isFreshAt(999_999L))
        assertFalse(reading.copy(timestamp = 0L).isFreshAt(100L))
    }

    @Test fun expiresExactlyAtConfiguredLimit() {
        for (minutes in listOf(1, 3, 15, 60)) {
            val settings = UserSettings(staleDataMinutes = minutes)
            val expiry = reading.timestamp + settings.staleAfterMillis
            assertTrue(reading.isFreshAt(expiry - 1L, settings.staleAfterMillis))
            assertFalse(reading.isFreshAt(expiry, settings.staleAfterMillis))
        }
    }

    @Test fun legacyDefaultsAndInvalidSettingsHaveBoundedTimeout() {
        assertEquals(10 * 60_000L, UserSettings().staleAfterMillis)
        assertEquals(60_000L, UserSettings(staleDataMinutes = 0).staleAfterMillis)
        assertEquals(60 * 60_000L, UserSettings(staleDataMinutes = Int.MAX_VALUE).staleAfterMillis)
    }
}
