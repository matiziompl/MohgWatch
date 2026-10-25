package com.mohgwatch.wear.service

import org.junit.Assert.*
import org.junit.Test

class LatestGlucoseAlertGateTest {
    @Test fun configuredMeasurementLimitPreservesTheSeparateDeliveryDeadline() {
        val now = 5_000_000L
        val measurement = now - 14 * 60_000L
        assertNotNull(LatestGlucoseAlertGate().begin("LOW", now, measurement, now, 15 * 60_000L))
        assertNull(LatestGlucoseAlertGate().begin("LOW", now, measurement, now, 3 * 60_000L))
        assertNull(LatestGlucoseAlertGate().begin("LOW", now - 10 * 60_000L, measurement, now, 15 * 60_000L))
        assertNull(LatestGlucoseAlertGate().begin("LOW", now, now - 15 * 60_000L, now, 15 * 60_000L))
    }
    @Test fun suspendedLowCannotReappearAfterRangeRecovery() {
        val gate = LatestGlucoseAlertGate()
        val low = gate.begin("LOW")!!
        val stable = gate.begin("STABLE")!!
        var notification: String? = "LOW"
        assertTrue(gate.applyIfCurrent(stable) { notification = null })
        assertFalse(gate.applyIfCurrent(low) { notification = "LOW" })
        assertNull(notification)
    }

    @Test fun suspendedRecoveryCannotCancelNewerHigh() {
        val gate = LatestGlucoseAlertGate()
        val stable = gate.begin("STABLE")!!
        val high = gate.begin("HIGH")!!
        var notification: String? = null
        assertTrue(gate.applyIfCurrent(high) { notification = "HIGH" })
        assertFalse(gate.applyIfCurrent(stable) { notification = null })
        assertEquals("HIGH", notification)
    }

    @Test fun optionalTrendsDoNotInvalidateGlucoseNotification() {
        val gate = LatestGlucoseAlertGate()
        val low = gate.begin("LOW")!!
        assertNull(gate.begin("TREND_STABLE"))
        assertNull(gate.begin("TREND_RISING"))
        assertNull(gate.begin("TREND_FALLING"))
        assertTrue(gate.applyIfCurrent(low) { })
    }
    @Test fun repeatedAndOlderDeliveryCannotReplaceTheNewestAlarm() {
        val gate = LatestGlucoseAlertGate()
        val low = gate.begin("LOW", 999_000L, 998_000L, 1_000_000L)!!
        assertNull(gate.begin("HIGH", 999_000L, 998_000L, 1_000_000L))
        assertNull(gate.begin("HIGH", 998_999L, 997_000L, 1_000_000L))
        assertTrue(gate.applyIfCurrent(low) {})
    }

    @Test fun expiredOrFutureMeasurementAndExpiredDeliveryAreIgnored() {
        val gate = LatestGlucoseAlertGate()
        assertNull(gate.begin("LOW", 999_000L, 400_000L, 1_000_000L))
        assertNull(gate.begin("HIGH", 999_000L, 1_000_001L, 1_000_000L))
        assertNull(gate.begin("LOW", 400_000L, 999_000L, 1_000_000L))
        assertNotNull(gate.begin("HIGH", 999_999L, 999_000L, 1_000_000L))
    }

    @Test fun slightlyAheadPhoneEnvelopeStillDeliversFreshMeasurement() {
        val gate = LatestGlucoseAlertGate()
        assertNotNull(gate.begin("LOW", 1_000_500L, 999_000L, 1_000_000L))
    }

    @Test fun envelopeClockSkewIsBoundedAndDoesNotRelaxMeasurementFreshness() {
        val gate = LatestGlucoseAlertGate()
        val low = gate.begin("LOW", 999_000L, 998_000L, 1_000_000L)!!
        assertNull(gate.begin("HIGH", 1_030_001L, 999_000L, 1_000_000L))
        assertTrue(gate.applyIfCurrent(low) {})
        assertNull(gate.begin("HIGH", 1_030_000L, 1_000_001L, 1_000_000L))
        assertNull(gate.begin("HIGH", 1_030_000L, 400_000L, 1_000_000L))
        assertNotNull(gate.begin("HIGH", 1_030_000L, 999_000L, 1_000_000L))
        assertFalse(gate.applyIfCurrent(low) {})
    }

    @Test fun repeatedMeasurementWithNewerEnvelopeCannotRetriggerAlarm() {
        val gate = LatestGlucoseAlertGate()
        val low = gate.begin("LOW", 999_000L, 998_000L, 1_000_000L)!!
        assertNull(gate.begin("LOW", 999_001L, 998_000L, 1_000_000L))
        assertTrue(gate.applyIfCurrent(low) {})
    }

    @Test fun olderMeasurementWithNewerEnvelopeCannotReplaceCurrentAlarm() {
        val gate = LatestGlucoseAlertGate()
        val high = gate.begin("HIGH", 999_000L, 998_000L, 1_000_000L)!!
        assertNull(gate.begin("LOW", 999_001L, 997_999L, 1_000_000L))
        assertTrue(gate.applyIfCurrent(high) {})
    }

    @Test fun freshHighLowAndLowHighTransitionsRemainAllowed() {
        val gate = LatestGlucoseAlertGate()
        assertNotNull(gate.begin("HIGH", 999_000L, 997_000L, 1_000_000L))
        assertNotNull(gate.begin("LOW", 999_001L, 998_000L, 1_000_000L))
        assertNotNull(gate.begin("HIGH", 999_002L, 999_000L, 1_000_000L))
    }

    @Test fun manualTestWithoutMeasurementDoesNotForgetLastRealMeasurement() {
        val gate = LatestGlucoseAlertGate()
        assertNotNull(gate.begin("LOW", 999_000L, 998_000L, 1_000_000L))
        assertNotNull(gate.begin("HIGH", 999_001L, null, 1_000_000L))
        assertNull(gate.begin("LOW", 999_002L, 998_000L, 1_000_000L))
        assertNotNull(gate.begin("HIGH", 999_003L, 998_001L, 1_000_000L))
    }
    @Test fun persistedDeliveryWatermarkRejectsReplayAfterServiceRestart() {
        val first = LatestGlucoseAlertGate()
        assertNotNull(first.begin("LOW", 999_000L, 998_000L, 1_000_000L))
        val restarted = LatestGlucoseAlertGate()
        restarted.restoreLastEventTimestamp(first.lastEventTimestamp)
        restarted.restoreLastMeasurementTimestamp(first.lastMeasurementTimestamp)
        assertNull(restarted.begin("LOW", 999_000L, 998_000L, 1_000_001L))
        assertNull(restarted.begin("LOW", 999_001L, 998_000L, 1_000_001L))
        assertNull(restarted.begin("LOW", 999_002L, 997_999L, 1_000_001L))
        assertNotNull(restarted.begin("HIGH", 1_000_001L, 999_999L, 1_000_001L))
    }
}
