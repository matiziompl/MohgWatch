package com.mohgwatch.phone.service

import com.mohgwatch.core.model.*
import org.junit.Assert.*
import org.junit.Test

class PhoneGlucoseAlertPolicyTest {
    private val now = 2_000_000L
    private fun reading(value: Float = 60f, timestamp: Long = now - 1_000L) =
        GlucoseReading(value, TrendArrow.STABLE, timestamp, false, false)

    @Test fun `fresh high to low selects a different hazard`() {
        assertEquals(GlucoseRange.HIGH, PhoneGlucoseAlertPolicy.hazard(reading(220f), now, 70f, 180f))
        assertEquals(GlucoseRange.LOW, PhoneGlucoseAlertPolicy.hazard(reading(60f), now, 70f, 180f))
    }

    @Test fun `old future normal and absent readings never play a glucose alarm`() {
        assertNull(PhoneGlucoseAlertPolicy.hazard(reading(timestamp = now - 10 * 60_000L), now, 70f, 180f))
        assertNull(PhoneGlucoseAlertPolicy.hazard(reading(timestamp = now + 1L), now, 70f, 180f))
        assertNull(PhoneGlucoseAlertPolicy.hazard(reading(100f), now, 70f, 180f))
        assertNull(PhoneGlucoseAlertPolicy.hazard(null, now, 70f, 180f))
    }

    @Test fun `delayed sound checks the selected freshness limit`() {
        val measurement = reading(timestamp = now - 3 * 60_000L)
        assertNull(PhoneGlucoseAlertPolicy.hazard(measurement, now, 70f, 180f, 3 * 60_000L))
        assertEquals(GlucoseRange.LOW,
            PhoneGlucoseAlertPolicy.hazard(measurement, now, 70f, 180f, 15 * 60_000L))
    }

    @Test fun `connected watch without recent wrist evidence cannot silence phone`() {
        val settings = UserSettings(mutePhoneWhenWatchConnected = true, nightModeEnabled = false)
        assertFalse(PhoneGlucoseAlertPolicy.shouldMute(settings, true, true, 0L, now))
        assertFalse(PhoneGlucoseAlertPolicy.shouldMute(settings, true, true, now - 10 * 60_000L, now))
        assertFalse(PhoneGlucoseAlertPolicy.shouldMute(settings, true, true, now + 1L, now))
        assertTrue(PhoneGlucoseAlertPolicy.shouldMute(settings, true, true, now - 1L, now))
        assertFalse(PhoneGlucoseAlertPolicy.shouldMute(settings, false, true, now - 1L, now))
        assertFalse(PhoneGlucoseAlertPolicy.shouldMute(settings, true, false, now - 1L, now))
    }

    @Test fun `connection failure respects watch muting even without a glucose reading`() {
        val settings = UserSettings(nightModeEnabled = false)
        assertFalse(PhoneGlucoseAlertPolicy.shouldPlaySound(settings, null, null, true, true, now - 1L, now))
        assertTrue(PhoneGlucoseAlertPolicy.shouldPlaySound(settings, null, null, false, true, now - 1L, now))
        assertTrue(PhoneGlucoseAlertPolicy.shouldPlaySound(settings, null, null, true, false, now - 1L, now))
        assertTrue(PhoneGlucoseAlertPolicy.shouldPlaySound(settings, null, null, true, true, 0L, now))
    }

    @Test fun `connection failure obeys the explicit phone sound preference`() {
        val settings = UserSettings(mutePhoneWhenWatchConnected = false)
        assertTrue(PhoneGlucoseAlertPolicy.shouldPlaySound(settings, null, null, true, true, now - 1L, now))
    }

    @Test fun `sound requires the matching current glucose hazard`() {
        val settings = UserSettings(nightModeEnabled = false)
        assertTrue(PhoneGlucoseAlertPolicy.shouldPlaySound(settings, reading(), GlucoseRange.LOW, false, false, 0L, now))
        assertFalse(PhoneGlucoseAlertPolicy.shouldPlaySound(settings, reading(), GlucoseRange.HIGH, false, false, 0L, now))
        assertFalse(PhoneGlucoseAlertPolicy.shouldPlaySound(settings, null, GlucoseRange.LOW, false, false, 0L, now))
        assertFalse(PhoneGlucoseAlertPolicy.shouldPlaySound(settings, reading(timestamp = now - settings.staleAfterMillis),
            GlucoseRange.LOW, false, false, 0L, now))
    }
}
