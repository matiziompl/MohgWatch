package com.mohgwatch.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

// Zegarki Wear OS preferują ciemny motyw jako domyślny, aby oszczędzać OLED
@Composable
fun WearMohgWatchTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ColorScheme(
            primary = Teal400,
            background = DarkBg,
            onBackground = TextPrimaryDark
        ),
        content = content
    )
}
