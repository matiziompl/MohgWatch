package com.mohgwatch.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingsDoseAndWristTest {

    @Test
    fun testDefaultWristAndDoseSettings() {
        val settings = UserSettings()
        assertTrue(settings.showWristStatusInNotification)
        assertEquals("both", settings.watchWristNotifyMode)
        assertEquals("", settings.doseBreakfast)
        assertEquals("", settings.doseLunch)
        assertEquals("", settings.doseDinner)
        assertEquals("", settings.doseNight)
    }

    @Test
    fun testCustomDoseAndWristSettings() {
        val settings = UserSettings(
            showWristStatusInNotification = false,
            watchWristNotifyMode = "off_only",
            doseBreakfast = "1,1j",
            doseLunch = "1,2j",
            doseDinner = "1,3j",
            doseNight = "3j"
        )
        assertEquals(false, settings.showWristStatusInNotification)
        assertEquals("off_only", settings.watchWristNotifyMode)
        assertEquals("1,1j", settings.doseBreakfast)
        assertEquals("1,2j", settings.doseLunch)
        assertEquals("1,3j", settings.doseDinner)
        assertEquals("3j", settings.doseNight)
    }
}
