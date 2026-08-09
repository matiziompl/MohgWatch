package com.mohgwatch.wear.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import com.mohgwatch.core.model.AppTheme
import com.mohgwatch.core.model.ThemeMode
import androidx.compose.ui.graphics.Color

// Zegarki Wear OS preferują ciemny motyw jako domyślny, aby oszczędzać OLED
@Composable
fun WearMohgWatchTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    // Zegarki Wear OS preferują zawsze ciemny motyw, aby oszczędzać OLED
    val darkTheme = true

    // Prosty wybór koloru podstawowego w zależności od motywu
    val primaryColor = when (appTheme) {
        AppTheme.DEFAULT, AppTheme.TEAL -> if (darkTheme) Teal400 else Teal500
        AppTheme.BLUE -> if (darkTheme) Blue400 else Blue500
        AppTheme.RED -> if (darkTheme) Red400 else Red500
        AppTheme.GREEN -> if (darkTheme) Green400 else Green500
        AppTheme.PURPLE -> if (darkTheme) Purple400 else Purple500
        AppTheme.ORANGE -> if (darkTheme) Orange400 else Orange500
        AppTheme.PINK -> if (darkTheme) Pink400 else Pink500
        AppTheme.AMBER -> if (darkTheme) Amber400 else Amber500
        AppTheme.CYAN -> if (darkTheme) Cyan400 else Cyan500
        AppTheme.MONOCHROME -> if (darkTheme) Mono400 else Mono500
        AppTheme.GRAY -> if (darkTheme) Gray400 else Gray500
        AppTheme.LIGHT_GRAY -> if (darkTheme) LightGray400 else LightGray500
        AppTheme.BLACK -> if (darkTheme) Black400 else Black500
        AppTheme.MATCH_BACKGROUND -> if (darkTheme) Teal400 else Teal500
    }

    val scheme = if (darkTheme) {
        ColorScheme(
            primary = primaryColor,
            background = DarkBg,
            onBackground = TextPrimaryDark
        )
    } else {
        // Wear OS zazwyczaj działa na czarnym tle, ale wspieramy jasne tło jeśli użytkownik zażąda
        ColorScheme(
            primary = primaryColor,
            background = LightBg,
            onBackground = TextPrimaryLight
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}
