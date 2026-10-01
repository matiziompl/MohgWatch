package com.mohgwatch.phone.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import com.mohgwatch.core.model.AppTheme
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.core.model.WidgetBgColor

object WidgetThemeHelper {

    fun surface(mode: WidgetBgColor): ColorProvider = when (mode) {
        WidgetBgColor.SYSTEM -> ColorProvider(day = Color(0xFFF9F8FE), night = Color(0xFF121318))
        WidgetBgColor.LIGHT -> ColorProvider(Color(0xFFF9F8FE))
        WidgetBgColor.DARK -> ColorProvider(Color(0xFF121318))
        WidgetBgColor.LIGHT_GRAY -> ColorProvider(day = Color(0xFFF1F5F9), night = Color(0xFF475569))
        WidgetBgColor.GRAY -> ColorProvider(day = Color(0xFFE5E7EB), night = Color(0xFF242424))
        WidgetBgColor.DARK_GRAY -> ColorProvider(day = Color(0xFF334155), night = Color(0xFF1E293B))
        WidgetBgColor.ORANGE -> ColorProvider(day = Color(0xFFFFEDD5), night = Color(0xFF431407))
        WidgetBgColor.LIGHT_BLUE -> ColorProvider(day = Color(0xFFE0F2FE), night = Color(0xFF082F49))
        WidgetBgColor.MONOCHROME -> ColorProvider(day = Color(0xFFF3F4F6), night = Color(0xFF111827))
        WidgetBgColor.PURPLE -> ColorProvider(day = Color(0xFFF3E8FF), night = Color(0xFF3B0764))
        WidgetBgColor.DARK_GREEN -> ColorProvider(day = Color(0xFFDCFCE7), night = Color(0xFF052E16))
        WidgetBgColor.CRIMSON -> ColorProvider(day = Color(0xFFFFE4E6), night = Color(0xFF4C0519))
        WidgetBgColor.VAPOR -> ColorProvider(day = Color(0xFFFAE8FF), night = Color(0xFF350035))
        WidgetBgColor.TIDE -> ColorProvider(day = Color(0xFFCCFBF1), night = Color(0xFF042F2E))
        WidgetBgColor.PULSE -> ColorProvider(day = Color(0xFFEDE9FE), night = Color(0xFF2E1065))
        WidgetBgColor.ACID -> ColorProvider(day = Color(0xFFFEF9C3), night = Color(0xFF1F2405))
    }

    fun onSurface(mode: WidgetBgColor): ColorProvider = when (mode) {
        WidgetBgColor.LIGHT -> ColorProvider(Color(0xFF111827))
        WidgetBgColor.DARK -> ColorProvider(Color(0xFFF9FAFB))
        WidgetBgColor.SYSTEM -> ColorProvider(day = Color(0xFF111827), night = Color(0xFFF9FAFB))
        WidgetBgColor.LIGHT_GRAY -> ColorProvider(day = Color(0xFF0F172A), night = Color(0xFFF8FAFC))
        WidgetBgColor.GRAY -> ColorProvider(day = Color(0xFF111827), night = Color(0xFFF9FAFB))
        WidgetBgColor.DARK_GRAY -> ColorProvider(day = Color(0xFFF8FAFC), night = Color(0xFFF1F5F9))
        WidgetBgColor.ORANGE -> ColorProvider(day = Color(0xFF431407), night = Color(0xFFFFEDD5))
        WidgetBgColor.LIGHT_BLUE -> ColorProvider(day = Color(0xFF082F49), night = Color(0xFFE0F2FE))
        WidgetBgColor.MONOCHROME -> ColorProvider(day = Color(0xFF111827), night = Color(0xFFF9FAFB))
        WidgetBgColor.PURPLE -> ColorProvider(day = Color(0xFF3B0764), night = Color(0xFFF3E8FF))
        WidgetBgColor.DARK_GREEN -> ColorProvider(day = Color(0xFF052E16), night = Color(0xFFDCFCE7))
        WidgetBgColor.CRIMSON -> ColorProvider(day = Color(0xFF4C0519), night = Color(0xFFFFE4E6))
        WidgetBgColor.VAPOR -> ColorProvider(day = Color(0xFF350035), night = Color(0xFFFAE8FF))
        WidgetBgColor.TIDE -> ColorProvider(day = Color(0xFF042F2E), night = Color(0xFFCCFBF1))
        WidgetBgColor.PULSE -> ColorProvider(day = Color(0xFF2E1065), night = Color(0xFFEDE9FE))
        WidgetBgColor.ACID -> ColorProvider(day = Color(0xFF1F2405), night = Color(0xFFFEF9C3))
    }

    fun onSurfaceVariant(mode: WidgetBgColor): ColorProvider = when (mode) {
        WidgetBgColor.LIGHT -> ColorProvider(Color(0xFF6B7280))
        WidgetBgColor.DARK -> ColorProvider(Color(0xFF9CA3AF))
        WidgetBgColor.SYSTEM -> ColorProvider(day = Color(0xFF6B7280), night = Color(0xFF9CA3AF))
        WidgetBgColor.LIGHT_GRAY -> ColorProvider(day = Color(0xFF475569), night = Color(0xFF94A3B8))
        WidgetBgColor.GRAY -> ColorProvider(day = Color(0xFF4B5563), night = Color(0xFF9CA3AF))
        WidgetBgColor.DARK_GRAY -> ColorProvider(day = Color(0xFF94A3B8), night = Color(0xFF64748B))
        WidgetBgColor.ORANGE -> ColorProvider(day = Color(0xFF7C2D12), night = Color(0xFFFDBA74))
        WidgetBgColor.LIGHT_BLUE -> ColorProvider(day = Color(0xFF0369A1), night = Color(0xFF7DD3FC))
        WidgetBgColor.MONOCHROME -> ColorProvider(day = Color(0xFF4B5563), night = Color(0xFF9CA3AF))
        WidgetBgColor.PURPLE -> ColorProvider(day = Color(0xFF6B21A8), night = Color(0xFFD8B4FE))
        WidgetBgColor.DARK_GREEN -> ColorProvider(day = Color(0xFF15803D), night = Color(0xFF86EFAC))
        WidgetBgColor.CRIMSON -> ColorProvider(day = Color(0xFF9F1239), night = Color(0xFFFDA4AF))
        WidgetBgColor.VAPOR -> ColorProvider(day = Color(0xFF86198F), night = Color(0xFFF0ABFC))
        WidgetBgColor.TIDE -> ColorProvider(day = Color(0xFF0F766E), night = Color(0xFF5EEAD4))
        WidgetBgColor.PULSE -> ColorProvider(day = Color(0xFF5B21B6), night = Color(0xFFC4B5FD))
        WidgetBgColor.ACID -> ColorProvider(day = Color(0xFF4D7C0F), night = Color(0xFFBEF264))
    }

    fun primary(theme: AppTheme): ColorProvider = when (theme) {
        AppTheme.DEFAULT, AppTheme.TEAL, AppTheme.MATCH_BACKGROUND, AppTheme.MATCH_GLUCOSE -> ColorProvider(day = Color(0xFF0F766E), night = Color(0xFF2DD4BF))
        AppTheme.BLUE -> ColorProvider(day = Color(0xFF2563EB), night = Color(0xFF60A5FA))
        AppTheme.RED -> ColorProvider(day = Color(0xFFDC2626), night = Color(0xFFF87171))
        AppTheme.GREEN -> ColorProvider(day = Color(0xFF16A34A), night = Color(0xFF4ADE80))
        AppTheme.PURPLE -> ColorProvider(day = Color(0xFF9333EA), night = Color(0xFFC084FC))
        AppTheme.ORANGE -> ColorProvider(day = Color(0xFFEA580C), night = Color(0xFFFB923C))
        AppTheme.PINK -> ColorProvider(day = Color(0xFFDB2777), night = Color(0xFFF472B6))
        AppTheme.AMBER -> ColorProvider(day = Color(0xFFD97706), night = Color(0xFFFBBF24))
        AppTheme.CYAN -> ColorProvider(day = Color(0xFF0891B2), night = Color(0xFF22D3EE))
        AppTheme.MONOCHROME -> ColorProvider(day = Color(0xFF404040), night = Color(0xFFA3A3A3))
        AppTheme.GRAY -> ColorProvider(day = Color(0xFF4B5563), night = Color(0xFF9CA3AF))
        AppTheme.LIGHT_GRAY -> ColorProvider(day = Color(0xFF475569), night = Color(0xFFCBD5E1))
        AppTheme.BLACK -> ColorProvider(day = Color(0xFF000000), night = Color(0xFFE5E7EB))
    }

    fun primary(theme: AppTheme, reading: GlucoseReading?, settings: UserSettings): ColorProvider {
        return if (theme == AppTheme.MATCH_GLUCOSE) {
            glucoseColor(reading, settings)
        } else {
            primary(theme)
        }
    }

    fun activeSlot(theme: AppTheme): ColorProvider = primary(theme)
    fun activeSlot(theme: AppTheme, reading: GlucoseReading?, settings: UserSettings): ColorProvider = primary(theme, reading, settings)

    fun inactiveSlot(mode: WidgetBgColor): ColorProvider = when (mode) {
        WidgetBgColor.LIGHT -> ColorProvider(Color(0xFFE5E7EB))
        WidgetBgColor.DARK -> ColorProvider(Color(0xFF2E333D))
        WidgetBgColor.SYSTEM -> ColorProvider(day = Color(0xFFE5E7EB), night = Color(0xFF2E333D))
        WidgetBgColor.LIGHT_GRAY -> ColorProvider(day = Color(0xFFCBD5E1), night = Color(0xFF334155))
        WidgetBgColor.DARK_GRAY -> ColorProvider(day = Color(0xFF475569), night = Color(0xFF0F172A))
        else -> ColorProvider(day = Color(0x33000000), night = Color(0x33FFFFFF))
    }

    fun glucoseColor(reading: GlucoseReading?, settings: UserSettings): ColorProvider {
        if (reading == null) return ColorProvider(Color(0xFF9CA3AF))
        return when (settings.widgetGlucoseColor) {
            "theme" -> onSurface(settings.widgetBgColor)
            "light" -> ColorProvider(Color(0xFFFFFFFF))
            "dark" -> ColorProvider(Color(0xFF111827))
            "light_gray" -> ColorProvider(day = Color(0xFF64748B), night = Color(0xFFCBD5E1))
            "gray" -> ColorProvider(Color(0xFF9CA3AF))
            "dark_gray" -> ColorProvider(day = Color(0xFF334155), night = Color(0xFF64748B))
            "primary" -> primary(settings.widgetPrimaryTheme)
            "blue" -> ColorProvider(day = Color(0xFF2563EB), night = Color(0xFF60A5FA))
            "green" -> ColorProvider(day = Color(0xFF16A34A), night = Color(0xFF4ADE80))
            "amber" -> ColorProvider(day = Color(0xFFD97706), night = Color(0xFFFBBF24))
            "purple" -> ColorProvider(day = Color(0xFF9333EA), night = Color(0xFFC084FC))
            "red" -> ColorProvider(day = Color(0xFFDC2626), night = Color(0xFFF87171))
            else -> {
                val v = reading.value
                when {
                    v < settings.lowThreshold -> ColorProvider(Color(0xFFEF4444)) // Red
                    v > settings.veryHighThreshold -> ColorProvider(Color(0xFFDC2626)) // Dark Red
                    v > settings.highThreshold -> ColorProvider(Color(0xFFF59E0B)) // Amber/Yellow
                    else -> ColorProvider(Color(0xFF10B981)) // Green
                }
            }
        }
    }
}
