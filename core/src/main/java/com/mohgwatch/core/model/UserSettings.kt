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
    MATCH_BACKGROUND("Uzależnij od koloru tła"),
    MATCH_GLUCOSE("Taki sam jak kolor glukozy")
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

enum class WidgetBgColor(val label: String) {
    SYSTEM("Systemowy"),
    LIGHT("Jasny"),
    DARK("Ciemny"),
    LIGHT_GRAY("Jasny szary"),
    GRAY("Szary"),
    DARK_GRAY("Ciemny szary"),
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
    val showInjectionSites: Boolean = false,
    val widgetPrimaryTheme: AppTheme = AppTheme.DEFAULT,
    val widgetBgColor: WidgetBgColor = WidgetBgColor.SYSTEM,
    val widgetGlucoseColor: String = "by_range",
    val colorPickerStyle: String = "list",
    val hideStatusHeader: Boolean = false,
    val mutePhoneWhenWatchConnected: Boolean = true,
    val isSyncEnabled: Boolean = true,
    val showWristStatusInNotification: Boolean = true,
    val watchWristNotifyMode: String = "both", // "both", "off_only"
    val staleDataTimeoutMinutes: Int = 5,
    val doseBreakfast: String = "",
    val doseLunch: String = "",
    val doseDinner: String = "",
    val doseNight: String = "",
    val watchNotifyOutOfRange: Boolean = false,
    val nightPhoneNotificationsEnabled: Boolean = false
) {
    fun isInRange(value: Float): Boolean = value in lowThreshold..highThreshold
    fun isLow(value: Float): Boolean = value < lowThreshold
    fun isHigh(value: Float): Boolean = value > highThreshold
    fun isVeryHigh(value: Float): Boolean = value > veryHighThreshold
    
    fun isAlertLow(value: Float): Boolean = value < alertLowThreshold
    fun isAlertHigh(value: Float): Boolean = value > alertHighThreshold

    fun isNightTime(currentMinutes: Int? = null): Boolean {
        if (!nightModeEnabled) return false
        val minutes = currentMinutes ?: run {
            val cal = java.util.Calendar.getInstance()
            cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        }
        val startParts = nightStartTime.split(":")
        val startMinutes = (startParts.getOrNull(0)?.toIntOrNull() ?: 22) * 60 + (startParts.getOrNull(1)?.toIntOrNull() ?: 0)
        val endParts = nightEndTime.split(":")
        val endMinutes = (endParts.getOrNull(0)?.toIntOrNull() ?: 7) * 60 + (endParts.getOrNull(1)?.toIntOrNull() ?: 0)
        return if (startMinutes > endMinutes) {
            minutes >= startMinutes || minutes < endMinutes
        } else {
            minutes in startMinutes..<endMinutes
        }
    }

    fun shouldMutePhoneAlert(isWatchConnected: Boolean, isWatchWorn: Boolean, currentMinutes: Int? = null): Boolean {
        if (!mutePhoneWhenWatchConnected) return false
        if (!isWatchConnected || !isWatchWorn) return false
        if (isNightTime(currentMinutes) && nightPhoneNotificationsEnabled) {
            return false
        }
        return true
    }
}
