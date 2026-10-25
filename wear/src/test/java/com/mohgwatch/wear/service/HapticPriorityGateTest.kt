package com.mohgwatch.wear.service

import org.junit.Assert.*
import org.junit.Test

class HapticPriorityGateTest {
    @Test fun suspendedTrendCannotReplaceAnInterveningCriticalAlertEvenAfterItFinishes() {
        val gate = HapticPriorityGate()
        val suspendedTrend = gate.optionalToken()
        var played = ""
        gate.playCritical(1000L, 720L) { played = "LOW" }
        assertFalse(gate.playOptional(suspendedTrend, 1800L) { played = "TREND" })
        assertEquals("LOW", played)
    }

    @Test fun previewAndRecoveryCannotTruncateCriticalWaveformButNewEventsCanPlayAfterItsEnd() {
        val gate = HapticPriorityGate()
        gate.playCritical(1000L, 720L) { }
        val preview = gate.optionalToken()
        assertFalse(gate.playOptional(preview, 1719L) { fail("Critical waveform is still playing") })
        var played = false
        assertTrue(gate.playOptional(preview, 1720L) { played = true })
        assertTrue(played)
    }

    @Test fun latestCriticalAlertReplacesPreviousCriticalAndExtendsProtection() {
        val gate = HapticPriorityGate()
        var played = ""
        gate.playCritical(1000L, 720L) { played = "LOW" }
        gate.playCritical(1500L, 550L) { played = "HIGH" }
        assertEquals("HIGH", played)
        assertFalse(gate.playOptional(gate.optionalToken(), 2049L) { fail("Second alarm still playing") })
        assertTrue(gate.playOptional(gate.optionalToken(), 2050L) { })
    }
}
