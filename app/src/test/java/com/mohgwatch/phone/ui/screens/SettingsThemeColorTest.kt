package com.mohgwatch.phone.ui.screens

import androidx.compose.ui.graphics.Color
import com.mohgwatch.core.model.AppTheme
import com.mohgwatch.core.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsThemeColorTest {
    @Test fun circularWidgetColorSelectionsUseTheDefaultInsteadOfCrashing() {
        val settings = UserSettings(
            widgetPrimaryTheme = AppTheme.MATCH_GLUCOSE,
            widgetGlucoseColor = "primary"
        )
        assertEquals(Color(0xFF0F766E), resolveAppThemeColor(AppTheme.MATCH_GLUCOSE, settings, false))
        assertEquals(Color(0xFF2DD4BF), resolveGlucoseColor("primary", settings, true))
    }

    @Test fun independentWidgetColorsStillResolveTheirSelectedColor() {
        val linked = UserSettings(widgetPrimaryTheme = AppTheme.MATCH_GLUCOSE, widgetGlucoseColor = "blue")
        assertEquals(Color(0xFF2563EB), resolveAppThemeColor(AppTheme.MATCH_GLUCOSE, linked, false))
        val primary = UserSettings(widgetPrimaryTheme = AppTheme.RED, widgetGlucoseColor = "primary")
        assertEquals(Color(0xFFF87171), resolveGlucoseColor("primary", primary, true))
    }
}
