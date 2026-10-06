package com.mohgwatch.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingsNotificationSwitchesTest {

    @Test
    fun testDefaultNotificationSwitches() {
        val settings = UserSettings()
        assertFalse(settings.watchNotifyOutOfRange)
        assertFalse(settings.nightPhoneNotificationsEnabled)
    }

    @Test
    fun testCustomNotificationSwitches() {
        val settings = UserSettings(
            watchNotifyOutOfRange = true,
            nightPhoneNotificationsEnabled = true
        )
        assertTrue(settings.watchNotifyOutOfRange)
        assertTrue(settings.nightPhoneNotificationsEnabled)
    }

    @Test
    fun testIsNightTime() {
        val settings = UserSettings(
            nightModeEnabled = true,
            nightStartTime = "22:00", // 1320 min
            nightEndTime = "07:00"    // 420 min
        )
        // 23:30 = 1410 min -> night
        assertTrue(settings.isNightTime(1410))
        // 03:00 = 180 min -> night
        assertTrue(settings.isNightTime(180))
        // 12:00 = 720 min -> day
        assertFalse(settings.isNightTime(720))
        // 21:59 = 1319 min -> day
        assertFalse(settings.isNightTime(1319))
        // 22:00 = 1320 min -> night
        assertTrue(settings.isNightTime(1320))
        // 06:59 = 419 min -> night
        assertTrue(settings.isNightTime(419))
        // 07:00 = 420 min -> day
        assertFalse(settings.isNightTime(420))
    }

    @Test
    fun testShouldMutePhoneAlert() {
        val baseSettings = UserSettings(
            mutePhoneWhenWatchConnected = true,
            nightModeEnabled = true,
            nightStartTime = "22:00",
            nightEndTime = "07:00"
        )

        // Day time (14:00 = 840 min)
        // Watch connected and worn -> muted
        assertTrue(baseSettings.shouldMutePhoneAlert(isWatchConnected = true, isWatchWorn = true, currentMinutes = 840))
        // Watch disconnected -> not muted
        assertFalse(baseSettings.shouldMutePhoneAlert(isWatchConnected = false, isWatchWorn = true, currentMinutes = 840))
        // Watch not worn -> not muted
        assertFalse(baseSettings.shouldMutePhoneAlert(isWatchConnected = true, isWatchWorn = false, currentMinutes = 840))

        // Night time (23:00 = 1380 min) with nightPhoneNotificationsEnabled = false -> muted when watch is worn
        val nightDisabled = baseSettings.copy(nightPhoneNotificationsEnabled = false)
        assertTrue(nightDisabled.shouldMutePhoneAlert(isWatchConnected = true, isWatchWorn = true, currentMinutes = 1380))

        // Night time (23:00 = 1380 min) with nightPhoneNotificationsEnabled = true -> NOT muted! (Option A)
        val nightEnabled = baseSettings.copy(nightPhoneNotificationsEnabled = true)
        assertFalse(nightEnabled.shouldMutePhoneAlert(isWatchConnected = true, isWatchWorn = true, currentMinutes = 1380))

        // Day time with nightPhoneNotificationsEnabled = true -> still muted during day when watch is worn
        assertTrue(nightEnabled.shouldMutePhoneAlert(isWatchConnected = true, isWatchWorn = true, currentMinutes = 840))
    }
}
