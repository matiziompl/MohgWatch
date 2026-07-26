package com.mohgwatch.phone.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

import com.mohgwatch.core.model.AppTheme
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

@Composable
fun MohgWatchTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when (appTheme) {
        AppTheme.DEFAULT, AppTheme.TEAL -> if (darkTheme) createDarkColorScheme(Teal500, Teal700, Teal400) else createLightColorScheme(Teal600, Teal400, Teal700)
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = colorScheme.background.toArgb()
                androidx.core.view.WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MohgWatchTypography,
        content = content
    )
}
