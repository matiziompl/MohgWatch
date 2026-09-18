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
    BLACK("Czarny"),
    MATCH_BACKGROUND("Uzależnij od koloru tła")
}

enum class ThemeMode(val label: String) {
    SYSTEM("Systemowy"),
    LIGHT("Jasny"),
    DARK("Ciemny")
}
enum class LogBgColor(val label: String) {
    DEFAULT("Domyślny"),
    WHITE("Biały"),
    GRAY("Szary"),
    ORANGE("Pomarańczowy"),
    LIGHT_BLUE("Jasno-niebieski"),
    MONOCHROME("Monochromatyczny"),
    PURPLE("Fioletowy"),
    DARK_GREEN("Ciemno-zielony"),
    CRIMSON("Crimson"),
    VAPOR("Vapor"),
    TIDE("Tide"),
    PULSE("Pulse"),
    ACID("Acid")
}

/**
 * Ustawienia użytkownika synchronizowane między telefonem a zegarkiem.
 */
data class UserSettings(
    val unit: GlucoseUnit = GlucoseUnit.MG_DL,
    val lowThreshold: Float = 70f,
    val highThreshold: Float = 180f,
    val veryHighThreshold: Float = 250f,
    val preset: WatchFacePreset = WatchFacePreset.D1_CLASSIC,
    val pollIntervalMinutes: Int = 5,
    val showDemoButton: Boolean = true,
    val appTheme: AppTheme = AppTheme.DEFAULT,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val syncThemeWithWatch: Boolean = true,
    val watchAppTheme: AppTheme = AppTheme.DEFAULT,
    val language: String = "system",
    val notifyDisconnect: Boolean = true,
    val notifyOutOfRange: Boolean = true,
    val alertLowThreshold: Float = 70f,
    val alertHighThreshold: Float = 180f,
    val alertVeryHighThreshold: Float = 250f,
    val disconnectSoundUri: String? = null,
    val lowGlucoseSoundUri: String? = null,
    val highGlucoseSoundUri: String? = null,
    val watchLowThreshold: Float = 70f,
    val watchHighThreshold: Float = 180f,
    val watchVeryHighThreshold: Float = 250f,
    val watchTrendArrowStyle: String = "next_to_value", // "next_to_value", "under_value"
    val watchGlucoseFont: String = "default",
    val notificationVolume: Float = 1.0f,
    val nightModeEnabled: Boolean = true,
    val nightStartTime: String = "22:00",
    val nightEndTime: String = "07:00",
    val nightNotificationVolume: Float = 1.0f,
    val logBackgroundColor: LogBgColor = LogBgColor.WHITE,
    val notificationSoundDelaySeconds: Int = 5,
    val notifyStaleDataWatch: Boolean = true,
    val trendThresholdFastFalling: Float = -5f,
    val trendThresholdFalling: Float = -2f,
    val trendThresholdRising: Float = 2f,
    val trendThresholdFastRising: Float = 5f,
    val persistentWakeLockEnabled: Boolean = false,
    val showInjectionSites: Boolean = false
) {
    fun isInRange(value: Float): Boolean = value in lowThreshold..highThreshold
    fun isLow(value: Float): Boolean = value < lowThreshold
    fun isHigh(value: Float): Boolean = value > highThreshold
    fun isVeryHigh(value: Float): Boolean = value > veryHighThreshold
    
    fun isAlertLow(value: Float): Boolean = value < alertLowThreshold
    fun isAlertHigh(value: Float): Boolean = value > alertHighThreshold
}
