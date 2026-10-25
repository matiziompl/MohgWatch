package com.mohgwatch.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WristStatusTest {
    @Test fun sensorDistinguishesWornRemovedAndMissingData() {
        assertEquals(WristStatus.ON_WRIST, WristStatus.fromSensorValue(1f))
        assertEquals(WristStatus.OFF_WRIST, WristStatus.fromSensorValue(0f))
        listOf(-1f, 2f, Float.NaN, Float.POSITIVE_INFINITY).forEach {
            assertEquals(WristStatus.UNKNOWN, WristStatus.fromSensorValue(it))
        }
    }

    @Test fun missingStaleAndFutureObservationsHaveNoData() {
        val now = 2_000_000L
        for (status in listOf(WristStatus.ON_WRIST, WristStatus.OFF_WRIST)) {
            assertEquals(status, status.at(now, now))
            assertEquals(status, status.at(now - GlucoseReading.STALE_AFTER_MILLIS + 1L, now))
            assertEquals(WristStatus.UNKNOWN, status.at(0L, now))
            assertEquals(WristStatus.UNKNOWN, status.at(now + 1L, now))
            assertEquals(WristStatus.UNKNOWN, status.at(now - GlucoseReading.STALE_AFTER_MILLIS, now))
        }
        assertEquals(WristStatus.UNKNOWN, WristStatus.UNKNOWN.at(now, now))
    }
}
