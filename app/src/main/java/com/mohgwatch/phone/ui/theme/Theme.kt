package com.mohgwatch.phone.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

import com.mohgwatch.core.model.AppTheme
import com.mohgwatch.core.model.LogBgColor
import com.mohgwatch.core.model.ThemeMode

private fun createDarkColorScheme(primary: Color, primaryContainer: Color, onPrimaryContainer: Color): ColorScheme {
    return darkColorScheme(
        primary = primary,
        onPrimary = TextPrimaryDark,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = Indigo500,
        onSecondary = TextPrimaryDark,
        secondaryContainer = Indigo600,
        onSecondaryContainer = Indigo400,
        background = DarkBg,
        onBackground = TextPrimaryDark,
        surface = DarkSurface,
        onSurface = TextPrimaryDark,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = TextSecondaryDark,
        error = ErrorRed,
        onError = TextPrimaryDark,
        outline = TextTertiaryDark,
        outlineVariant = DarkSurfaceVariant
    )
}

private fun createLightColorScheme(primary: Color, primaryContainer: Color, onPrimaryContainer: Color): ColorScheme {
    return lightColorScheme(
        primary = primary,
        onPrimary = TextPrimaryLight,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = Indigo500,
        onSecondary = TextPrimaryLight,
        secondaryContainer = Indigo400,
        onSecondaryContainer = Indigo600,
        background = LightBg,
        onBackground = TextPrimaryLight,
        surface = LightSurface,
        onSurface = TextPrimaryLight,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = TextSecondaryLight,
        error = ErrorRed,
        onError = TextPrimaryLight,
        outline = TextTertiaryLight,
        outlineVariant = LightSurfaceVariant
    )
}

val LocalAppBackgroundBrush = compositionLocalOf<Brush?> { null }

@Composable
fun MohgWatchTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    logBackgroundColor: LogBgColor = LogBgColor.DEFAULT,
    content: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val actualAppTheme = if (appTheme == AppTheme.MATCH_BACKGROUND) {
        when (logBackgroundColor) {
            LogBgColor.ORANGE -> AppTheme.ORANGE
            LogBgColor.LIGHT_BLUE, LogBgColor.TIDE -> AppTheme.CYAN
            LogBgColor.PURPLE, LogBgColor.VAPOR, LogBgColor.PULSE -> AppTheme.PURPLE
            LogBgColor.DARK_GREEN, LogBgColor.ACID -> AppTheme.GREEN
            LogBgColor.CRIMSON -> AppTheme.RED
            LogBgColor.MONOCHROME, LogBgColor.GRAY, LogBgColor.WHITE -> AppTheme.MONOCHROME
            else -> AppTheme.DEFAULT
        }
    } else appTheme

    val colorScheme = when (actualAppTheme) {
        AppTheme.DEFAULT, AppTheme.TEAL, AppTheme.MATCH_BACKGROUND -> if (darkTheme) createDarkColorScheme(Teal500, Teal700, Teal400) else createLightColorScheme(Teal600, Teal400, Teal700)
        AppTheme.BLUE -> if (darkTheme) createDarkColorScheme(Blue500, Blue700, Blue400) else createLightColorScheme(Blue600, Blue400, Blue700)
        AppTheme.RED -> if (darkTheme) createDarkColorScheme(Red500, Red700, Red400) else createLightColorScheme(Red600, Red400, Red700)
        AppTheme.GREEN -> if (darkTheme) createDarkColorScheme(Green500, Green700, Green400) else createLightColorScheme(Green600, Green400, Green700)
        AppTheme.PURPLE -> if (darkTheme) createDarkColorScheme(Purple500, Purple700, Purple400) else createLightColorScheme(Purple600, Purple400, Purple700)
        AppTheme.ORANGE -> if (darkTheme) createDarkColorScheme(Orange500, Orange700, Orange400) else createLightColorScheme(Orange600, Orange400, Orange700)
        AppTheme.PINK -> if (darkTheme) createDarkColorScheme(Pink500, Pink700, Pink400) else createLightColorScheme(Pink600, Pink400, Pink700)
        AppTheme.AMBER -> if (darkTheme) createDarkColorScheme(Amber500, Amber700, Amber400) else createLightColorScheme(Amber600, Amber400, Amber700)
        AppTheme.CYAN -> if (darkTheme) createDarkColorScheme(Cyan500, Cyan700, Cyan400) else createLightColorScheme(Cyan600, Cyan400, Cyan700)
        AppTheme.MONOCHROME -> if (darkTheme) createDarkColorScheme(Mono500, Mono700, Mono400) else createLightColorScheme(Mono600, Mono400, Mono700)
        AppTheme.GRAY -> if (darkTheme) createDarkColorScheme(Gray500, Gray700, Gray400) else createLightColorScheme(Gray600, Gray400, Gray700)
        AppTheme.LIGHT_GRAY -> if (darkTheme) createDarkColorScheme(LightGray500, LightGray700, LightGray400) else createLightColorScheme(LightGray600, LightGray400, LightGray700)
        AppTheme.BLACK -> if (darkTheme) createDarkColorScheme(Black500, Black700, Black400) else createLightColorScheme(Black600, Black400, Black700)
    }

    val backgroundBrush = when (logBackgroundColor) {
        LogBgColor.DEFAULT -> null
        LogBgColor.WHITE -> if (darkTheme) Brush.linearGradient(listOf(Color(0xFF1E1E1E), Color(0xFF121212))) else Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF5F5F5)))
        LogBgColor.GRAY -> if (darkTheme) Brush.linearGradient(listOf(Color(0xFF2D2D2D), Color(0xFF1E1E1E))) else Brush.linearGradient(listOf(Color(0xFFF0F0F0), Color(0xFFE0E0E0)))
        LogBgColor.ORANGE -> if (darkTheme) Brush.linearGradient(listOf(Color(0xFF4A2B0F), Color(0xFF2D1908))) else Brush.linearGradient(listOf(Color(0xFFFFB74D), Color(0xFFF57C00)))
        LogBgColor.LIGHT_BLUE -> if (darkTheme) Brush.linearGradient(listOf(Color(0xFF0D253F), Color(0xFF061320))) else Brush.linearGradient(listOf(Color(0xFF81D4FA), Color(0xFF0288D1)))
        LogBgColor.MONOCHROME -> if (darkTheme) Brush.linearGradient(listOf(Color(0xFF212121), Color(0xFF000000))) else Brush.linearGradient(listOf(Color(0xFFE0E0E0), Color(0xFF9E9E9E)))
        LogBgColor.PURPLE -> if (darkTheme) Brush.linearGradient(listOf(Color(0xFF311B42), Color(0xFF190C23))) else Brush.linearGradient(listOf(Color(0xFFCE93D8), Color(0xFF8E24AA)))
        LogBgColor.DARK_GREEN -> if (darkTheme) Brush.linearGradient(listOf(Color(0xFF1B3A22), Color(0xFF0D1C10))) else Brush.linearGradient(listOf(Color(0xFFA5D6A7), Color(0xFF388E3C)))
        LogBgColor.CRIMSON -> Brush.linearGradient(listOf(Color(0xFFFF0054), Color(0xFF130008)))
        LogBgColor.VAPOR -> Brush.linearGradient(listOf(Color(0xFFFF10F0), Color(0xFF130013)))
        LogBgColor.TIDE -> Brush.linearGradient(listOf(Color(0xFF00D4FF), Color(0xFF001419)))
        LogBgColor.PULSE -> Brush.linearGradient(listOf(Color(0xFF7000FF), Color(0xFF07001A)))
        LogBgColor.ACID -> Brush.linearGradient(listOf(Color(0xFFCBFF00), Color(0xFF0C1100)))
    }

    val finalColorScheme = if (logBackgroundColor != LogBgColor.DEFAULT) {
        colorScheme.copy(
            background = Color.Transparent,
            surface = if (darkTheme) Color(0x80000000) else Color(0xCCFFFFFF)
        )
    } else colorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = finalColorScheme.background.toArgb()
                androidx.core.view.WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalAppBackgroundBrush provides backgroundBrush) {
        MaterialTheme(
            colorScheme = finalColorScheme,
            typography = MohgWatchTypography,
            content = content
        )
    }
}
