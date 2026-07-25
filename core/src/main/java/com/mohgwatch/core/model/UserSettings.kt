package com.mohgwatch.core.model

enum class AppTheme(val label: String) {
    DEFAULT("Domyślny"),
    TEAL("Morski (Teal)"),
    BLUE("Niebieski"),
    RED("Czerwony"),
    GREEN("Zielony"),
    PURPLE("Fioletowy"),
    ORANGE("Pomarańczowy"),
    PINK("Różowy"),
    AMBER("Bursztynowy"),
    CYAN("Cyjanowy"),
    MONOCHROME("Monochromatyczny"),
    GRAY("Szary"),
    LIGHT_GRAY("Jasno Szary"),
    BLACK("Czarny")
}

enum class ThemeMode(val label: String) {
    SYSTEM("Systemowy"),
    LIGHT("Jasny"),
    DARK("Ciemny")
}

/**
 * Ustawienia użytkownika synchronizowane między telefonem a zegarkiem.
 */
data class UserSettings(
    val unit: GlucoseUnit = GlucoseUnit.MG_DL,
    val lowThreshold: Float = 70f,
    val highThreshold: Float = 180f,
    val preset: WatchFacePreset = WatchFacePreset.D1_CLASSIC,
    val pollIntervalMinutes: Int = 5,
    val showDemoButton: Boolean = true,
    val appTheme: AppTheme = AppTheme.DEFAULT,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val syncThemeWithWatch: Boolean = true,
    val watchAppTheme: AppTheme = AppTheme.DEFAULT,
    val language: String = "system",
    val notifyDisconnect: Boolean = true,
    val notifyOutOfRange: Boolean = true
) {
    fun isInRange(value: Float): Boolean = value in lowThreshold..highThreshold
    fun isLow(value: Float): Boolean = value < lowThreshold
    fun isHigh(value: Float): Boolean = value > highThreshold
}
