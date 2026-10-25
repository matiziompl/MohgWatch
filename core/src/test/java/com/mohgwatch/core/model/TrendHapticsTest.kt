package com.mohgwatch.core.model

import org.junit.Assert.*
import org.junit.Test

class TrendHapticsTest {
    private val now = 1_000_000L
    private fun reading(trend: TrendArrow, timestamp: Long = now, value: Float = 120f) = GlucoseReading(
        value = value, trendArrow = trend, measurementColor = MeasurementColor.IN_RANGE,
        timestamp = timestamp, isHigh = false, isLow = false
    )

    @Test fun arrowChangesInsideTheRangeDoNotVibrate() {
        assertNull(TrendHaptics.nextDirection(reading(TrendArrow.RISING_FAST), null, now))
        assertNull(TrendHaptics.nextDirection(reading(TrendArrow.FALLING), reading(TrendArrow.RISING, now - 1000), now))
        assertNull(TrendHaptics.nextDirection(reading(TrendArrow.STABLE), reading(TrendArrow.RISING, now - 1000), now))
    }

    @Test fun crossingThresholdUsesGlucoseEvenWhenTheArrowIsStableOrUnknown() {
        assertEquals(TrendDirection.FALLING, TrendHaptics.nextDirection(reading(TrendArrow.STABLE, value = 69f), null, now))
        assertEquals(TrendDirection.RISING, TrendHaptics.nextDirection(reading(TrendArrow.UNKNOWN, value = 181f), null, now))
        assertNull(TrendHaptics.nextDirection(reading(TrendArrow.FALLING, value = 68f), reading(TrendArrow.STABLE, now - 1000, 69f), now))
    }

    @Test fun directHighLowTransitionsAlertAndBothBoundariesRecover() {
        assertEquals(TrendDirection.FALLING, TrendHaptics.nextDirection(reading(TrendArrow.RISING, value = 69f), reading(TrendArrow.RISING, now - 1000, 181f), now))
        assertEquals(TrendDirection.RISING, TrendHaptics.nextDirection(reading(TrendArrow.FALLING, value = 181f), reading(TrendArrow.FALLING, now - 1000, 69f), now))
        assertEquals(TrendDirection.STABLE, TrendHaptics.nextDirection(reading(TrendArrow.FALLING, value = 70f), reading(TrendArrow.FALLING, now - 1000, 69f), now))
        assertEquals(TrendDirection.STABLE, TrendHaptics.nextDirection(reading(TrendArrow.RISING, value = 180f), reading(TrendArrow.RISING, now - 1000, 181f), now))
    }

    @Test fun ignoresDuplicateOlderFutureAndTenMinuteOldMeasurements() {
        assertNull(TrendHaptics.nextDirection(reading(TrendArrow.FALLING, value = 69f), reading(TrendArrow.RISING, value = 120f), now))
        assertNull(TrendHaptics.nextDirection(reading(TrendArrow.FALLING, now - 1000, 69f), reading(TrendArrow.RISING, value = 120f), now))
        assertNull(TrendHaptics.nextDirection(reading(TrendArrow.RISING, now + 1, 181f), null, now))
        assertNull(TrendHaptics.nextDirection(reading(TrendArrow.RISING, now - 600_000, 181f), null, now))
    }

    @Test fun thresholdAlertsUseTheConfiguredMeasurementAge() {
        val fiveMinuteOld = reading(TrendArrow.RISING, now - 5 * 60_000L, 181f)
        assertNull(TrendHaptics.nextDirection(fiveMinuteOld, null, now, UserSettings(staleDataMinutes = 3)))
        assertEquals(TrendDirection.RISING,
            TrendHaptics.nextDirection(fiveMinuteOld, null, now, UserSettings(staleDataMinutes = 15)))
    }

    @Test fun thresholdSwitchesRespectExplicitAlarmSettings() {
        val settings = UserSettings()
        assertTrue(TrendHaptics.isAlertEnabled(settings, "LOW"))
        assertTrue(TrendHaptics.isAlertEnabled(settings, "HIGH"))
        assertFalse(TrendHaptics.isAlertEnabled(settings.copy(alertLowHapticEnabled = false), "LOW"))
        assertFalse(TrendHaptics.isAlertEnabled(settings.copy(alertHighHapticEnabled = false), "HIGH"))
        assertFalse(TrendHaptics.isAlertEnabled(settings.copy(notifyOutOfRange = false), "LOW"))
        assertFalse(TrendHaptics.isAlertEnabled(settings.copy(notifyOutOfRange = false), "HIGH"))
        assertFalse(TrendHaptics.isAlertEnabled(settings, "STABLE"))
        assertTrue(TrendHaptics.isAlertEnabled(settings.copy(trendStableEnabled = true), "STABLE"))
    }

    @Test fun diabetesSettingsThresholdsAreIndependentOfWatchColors() {
        val settings = UserSettings(
            lowThreshold = 80f, highThreshold = 160f,
            watchLowThreshold = 40f, watchHighThreshold = 300f
        )
        assertEquals(TrendDirection.FALLING, TrendHaptics.nextDirection(reading(TrendArrow.RISING, value = 79f), null, now, settings))
        assertEquals(TrendDirection.RISING, TrendHaptics.nextDirection(reading(TrendArrow.FALLING, value = 161f), null, now, settings))
        assertNull(TrendHaptics.nextDirection(reading(TrendArrow.FALLING, value = 80f), null, now, settings))
        assertNull(TrendHaptics.nextDirection(reading(TrendArrow.RISING, value = 160f), null, now, settings))
    }

    @Test fun invalidGlucoseCannotGenerateARecoveryOrHighAlarm() {
        assertNull(TrendHaptics.nextDirection(reading(TrendArrow.RISING, value = Float.NaN), reading(TrendArrow.RISING, now - 1000, 181f), now))
        assertNull(TrendHaptics.nextDirection(reading(TrendArrow.RISING, value = Float.POSITIVE_INFINITY), null, now))
    }

    @Test fun strongerPatternsAreFiniteLongerAndUseFullAmplitude() {
        for (direction in TrendDirection.entries) {
            val short = TrendHaptics.waveform(direction, TrendVibrationPattern.SHORT)
            val strong = TrendHaptics.waveform(direction, TrendVibrationPattern.STRONG)
            val long = TrendHaptics.waveform(direction, TrendVibrationPattern.LONG)
            for (waveform in listOf(short, strong, long)) {
                assertEquals(waveform.timings.size, waveform.amplitudes.size)
                assertTrue(waveform.timings.all { it >= 0 })
                assertTrue(waveform.timings.sum() < 10_000)
                assertTrue(waveform.amplitudes.all { it in 0..255 })
            }
            assertTrue(strong.timings.sum() > short.timings.sum())
            assertTrue(long.timings.sum() > strong.timings.sum())
            assertTrue(strong.amplitudes.filter { it > 0 }.all { it == 255 })
            assertTrue(long.amplitudes.filter { it > 0 }.all { it == 255 })
        }
        assertEquals(3, TrendDirection.entries.map { TrendHaptics.waveform(it, TrendVibrationPattern.STRONG).timings.toList() }.distinct().size)
    }

    @Test fun criticalAlertsUseTheSelectedDirectionalPatterns() {
        for (pattern in TrendVibrationPattern.entries) {
            val settings = UserSettings(
                trendRisingPattern = pattern, trendFallingPattern = pattern
            )
            for ((type, direction) in listOf("LOW" to TrendDirection.FALLING, "HIGH" to TrendDirection.RISING)) {
                val expected = TrendHaptics.waveform(direction, pattern)
                val actual = requireNotNull(TrendHaptics.waveformForAlert(settings, type))
                assertArrayEquals(expected.timings, actual.timings)
                assertArrayEquals(expected.amplitudes, actual.amplitudes)
            }
        }
    }

    @Test fun lowAndHighUseIndependentPatternSelections() {
        val settings = UserSettings(
            trendRisingPattern = TrendVibrationPattern.LONG,
            trendFallingPattern = TrendVibrationPattern.SHORT
        )
        assertArrayEquals(
            TrendHaptics.waveform(TrendDirection.RISING, TrendVibrationPattern.LONG).timings,
            requireNotNull(TrendHaptics.waveformForAlert(settings, "HIGH")).timings
        )
        assertArrayEquals(
            TrendHaptics.waveform(TrendDirection.FALLING, TrendVibrationPattern.SHORT).timings,
            requireNotNull(TrendHaptics.waveformForAlert(settings, "LOW")).timings
        )
    }

    @Test fun recoveryUsesSelectedPatternAndUnknownAlertsAreIgnored() {
        val settings = UserSettings(trendStableEnabled = false, trendStablePattern = TrendVibrationPattern.LONG)
        val recovery = requireNotNull(TrendHaptics.waveformForAlert(settings, "STABLE"))
        assertArrayEquals(longArrayOf(0, 650, 350, 650, 500, 650, 350, 650), recovery.timings)
        assertArrayEquals(intArrayOf(0, 255, 0, 255, 0, 255, 0, 255), recovery.amplitudes)
        assertNull(TrendHaptics.waveformForAlert(settings, "UNKNOWN"))
    }
    @Test fun disabledHapticsDoNotSuppressRequestedVisualThresholdNotifications() {
        val settings = UserSettings(alertLowHapticEnabled = false, alertHighHapticEnabled = false)
        assertTrue(TrendHaptics.shouldSendAlert(settings, "LOW", showNotification = true))
        assertTrue(TrendHaptics.shouldSendAlert(settings, "HIGH", showNotification = true))
        assertFalse(TrendHaptics.shouldSendAlert(settings, "LOW", showNotification = false))
        assertFalse(TrendHaptics.shouldSendAlert(settings, "HIGH", showNotification = false))
    }

    @Test fun recoveryIsDeliveredToClearAnAlarmEvenWhenItsVibrationIsDisabled() {
        assertTrue(TrendHaptics.shouldSendAlert(UserSettings(trendStableEnabled = false), "STABLE"))
        assertTrue(TrendHaptics.shouldSendAlert(UserSettings(notifyOutOfRange = false, trendStableEnabled = false), "STABLE"))
    }

    @Test fun globalDisableAndUnknownTypesCannotSendThresholdAlerts() {
        val settings = UserSettings(notifyOutOfRange = false)
        assertFalse(TrendHaptics.shouldSendAlert(settings, "LOW", showNotification = true))
        assertFalse(TrendHaptics.shouldSendAlert(settings, "HIGH", showNotification = true))
        assertFalse(TrendHaptics.shouldSendAlert(UserSettings(), "UNKNOWN", showNotification = true))
        assertTrue(TrendHaptics.shouldSendAlert(UserSettings(), "LOW"))
        assertTrue(TrendHaptics.shouldSendAlert(UserSettings(), "HIGH"))
    }
}
