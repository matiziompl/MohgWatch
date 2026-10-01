package com.mohgwatch.phone.widget

import com.mohgwatch.core.model.AppTheme
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.model.TrendArrow
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.core.model.WidgetBgColor
import org.junit.Assert.assertNotNull
import org.junit.Test

class WidgetThemeHelperTest {

    @Test
    fun testColorProvidersNotNull() {
        for (bg in WidgetBgColor.entries) {
            assertNotNull(WidgetThemeHelper.surface(bg))
            assertNotNull(WidgetThemeHelper.onSurface(bg))
            assertNotNull(WidgetThemeHelper.onSurfaceVariant(bg))
            assertNotNull(WidgetThemeHelper.inactiveSlot(bg))
        }

        for (theme in AppTheme.entries) {
            assertNotNull(WidgetThemeHelper.primary(theme))
            assertNotNull(WidgetThemeHelper.activeSlot(theme))
        }
    }

    @Test
    fun testGlucoseColorThresholds() {
        val settings = UserSettings(
            lowThreshold = 70f,
            highThreshold = 180f,
            veryHighThreshold = 250f
        )

        val lowReading = GlucoseReading(
            value = 55f,
            trendArrow = TrendArrow.FALLING,
            measurementColor = com.mohgwatch.core.model.MeasurementColor.LOW,
            timestamp = System.currentTimeMillis(),
            isHigh = false,
            isLow = true
        )
        val normalReading = GlucoseReading(
            value = 110f,
            trendArrow = TrendArrow.STABLE,
            measurementColor = com.mohgwatch.core.model.MeasurementColor.IN_RANGE,
            timestamp = System.currentTimeMillis(),
            isHigh = false,
            isLow = false
        )
        val highReading = GlucoseReading(
            value = 190f,
            trendArrow = TrendArrow.RISING,
            measurementColor = com.mohgwatch.core.model.MeasurementColor.HIGH,
            timestamp = System.currentTimeMillis(),
            isHigh = true,
            isLow = false
        )
        val veryHighReading = GlucoseReading(
            value = 280f,
            trendArrow = TrendArrow.RISING_FAST,
            measurementColor = com.mohgwatch.core.model.MeasurementColor.VERY_HIGH,
            timestamp = System.currentTimeMillis(),
            isHigh = true,
            isLow = false
        )

        assertNotNull(WidgetThemeHelper.glucoseColor(null, settings))
        assertNotNull(WidgetThemeHelper.glucoseColor(lowReading, settings))
        assertNotNull(WidgetThemeHelper.glucoseColor(normalReading, settings))
        assertNotNull(WidgetThemeHelper.glucoseColor(highReading, settings))
        assertNotNull(WidgetThemeHelper.glucoseColor(veryHighReading, settings))

        for (mode in listOf("light_gray", "gray", "dark_gray")) {
            val customSettings = settings.copy(widgetGlucoseColor = mode)
            assertNotNull(WidgetThemeHelper.glucoseColor(normalReading, customSettings))
        }

        val matchSettings = settings.copy(widgetPrimaryTheme = AppTheme.MATCH_GLUCOSE)
        assertNotNull(WidgetThemeHelper.primary(AppTheme.MATCH_GLUCOSE, normalReading, matchSettings))
    }
}
