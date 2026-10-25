package com.mohgwatch.core.model

import org.junit.Assert.*
import org.junit.Test

class GlucoseAlertPolicyTest {
    @Test fun crossingDirectlyFromHighToLowRequiresANewAlarm() {
        val high = GlucoseAlertPolicy.range(200f, 70f, 180f)
        val low = GlucoseAlertPolicy.range(65f, 70f, 180f)
        assertTrue(GlucoseAlertPolicy.shouldAlert(high, low))
        assertFalse(GlucoseAlertPolicy.shouldAlert(low, low))
    }

    @Test fun recoveryIsNotAnOutOfRangeAlarm() {
        assertFalse(GlucoseAlertPolicy.shouldAlert(GlucoseRange.LOW, GlucoseRange.NORMAL))
        assertEquals(GlucoseRange.NORMAL, GlucoseAlertPolicy.range(70f, 70f, 180f))
        assertEquals(GlucoseRange.NORMAL, GlucoseAlertPolicy.range(180f, 70f, 180f))
    }

    @Test fun oldUnknownAndFutureWristStatusCannotMutePhone() {
        assertFalse(GlucoseAlertPolicy.hasRecentWristStatus(0L, 900_000L))
        assertFalse(GlucoseAlertPolicy.hasRecentWristStatus(1L, 600_001L))
        assertFalse(GlucoseAlertPolicy.hasRecentWristStatus(1_001L, 1_000L))
        assertTrue(GlucoseAlertPolicy.hasRecentWristStatus(900_000L, 900_001L))
    }
}
