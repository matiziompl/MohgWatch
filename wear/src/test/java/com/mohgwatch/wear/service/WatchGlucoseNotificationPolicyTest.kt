package com.mohgwatch.wear.service

import com.mohgwatch.core.model.*
import org.junit.Assert.*
import org.junit.Test

class WatchGlucoseNotificationPolicyTest {
    private val now = 2_000_000L
    private val settings = UserSettings(watchNotifyOutOfRange = true)
    private fun reading(value: Float = 240f, timestamp: Long = now - 1_000L) =
        GlucoseReading(value, TrendArrow.STABLE, MeasurementColor.IN_RANGE, timestamp, false, false)

    @Test fun cardsRemainEnabledWhenBothDirectionalHapticsAreDisabled() {
        val quiet = settings.copy(alertLowHapticEnabled = false, alertHighHapticEnabled = false)
        assertEquals(GlucoseNotificationUpdate.LOW,
            WatchGlucoseNotificationPolicy.alert(quiet, "LOW", false, now - 1_000L, now - 1_000L, now))
        assertEquals(GlucoseNotificationUpdate.HIGH,
            WatchGlucoseNotificationPolicy.alert(quiet, "HIGH", false, now - 1_000L, now - 1_000L, now))
    }

    @Test fun visualAndMasterSwitchesAreRespectedButRecoveryAlwaysClears() {
        assertEquals(GlucoseNotificationUpdate.CANCEL,
            WatchGlucoseNotificationPolicy.alert(settings.copy(watchNotifyOutOfRange = false), "HIGH", false, null, null, now))
        assertEquals(GlucoseNotificationUpdate.HIGH,
            WatchGlucoseNotificationPolicy.alert(settings.copy(watchNotifyOutOfRange = false), "HIGH", true, null, null, now))
        assertEquals(GlucoseNotificationUpdate.CANCEL,
            WatchGlucoseNotificationPolicy.alert(settings.copy(notifyOutOfRange = false), "HIGH", true, null, null, now))
        assertEquals(GlucoseNotificationUpdate.CANCEL,
            WatchGlucoseNotificationPolicy.alert(settings.copy(notifyOutOfRange = false), "STABLE", false, null, null, now))
    }

    @Test fun delayedOrExpiredAlertCannotReplaceANewerMeasurement() {
        assertEquals(GlucoseNotificationUpdate.IGNORE,
            WatchGlucoseNotificationPolicy.alert(settings, "LOW", false, now - 2_000L, now - 1_000L, now))
        assertEquals(GlucoseNotificationUpdate.IGNORE,
            WatchGlucoseNotificationPolicy.alert(settings, "HIGH", false, now - settings.staleAfterMillis, null, now))
        assertEquals(GlucoseNotificationUpdate.IGNORE,
            WatchGlucoseNotificationPolicy.alert(settings, "HIGH", false, now + 1L, null, now))
        assertEquals(GlucoseNotificationUpdate.HIGH,
            WatchGlucoseNotificationPolicy.alert(settings, "HIGH", false, now - 1_000L, now - 1_000L, now))
    }

    @Test fun newerReadingUpdatesVisibleCardWithoutRecreatingDismissedCard() {
        assertEquals(GlucoseNotificationUpdate.HIGH,
            WatchGlucoseNotificationPolicy.refresh(true, reading(), settings, now))
        assertEquals(GlucoseNotificationUpdate.LOW,
            WatchGlucoseNotificationPolicy.refresh(true, reading(55f), settings, now))
        assertEquals(GlucoseNotificationUpdate.IGNORE,
            WatchGlucoseNotificationPolicy.refresh(false, reading(), settings, now))
    }

    @Test fun alertArrivingBeforeItsReadingIsNotClearedByOlderNormalCache() {
        assertEquals(GlucoseNotificationUpdate.IGNORE,
            WatchGlucoseNotificationPolicy.refresh(true, reading(120f, now - 2_000L), settings, now,
                visibleMeasurementTimestamp = now - 1_000L))
        assertEquals(GlucoseNotificationUpdate.LOW,
            WatchGlucoseNotificationPolicy.refresh(true, reading(55f, now - 1_000L), settings, now,
                visibleMeasurementTimestamp = now - 1_000L))
        assertEquals(GlucoseNotificationUpdate.CANCEL,
            WatchGlucoseNotificationPolicy.refresh(true, reading(120f), settings, now,
                visibleMeasurementTimestamp = now - settings.staleAfterMillis))
    }

    @Test fun normalStaleAndDisabledSettingsClearVisibleCard() {
        for ((measurement, preferences) in listOf(
            reading(120f) to settings,
            reading(timestamp = now - settings.staleAfterMillis) to settings,
            reading(timestamp = now + 1L) to settings,
            reading(Float.NaN) to settings,
            reading() to settings.copy(watchNotifyOutOfRange = false),
            reading() to settings.copy(notifyOutOfRange = false),
            null to settings
        )) {
            assertEquals(GlucoseNotificationUpdate.CANCEL,
                WatchGlucoseNotificationPolicy.refresh(true, measurement, preferences, now))
        }
    }
}
