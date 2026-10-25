package com.mohgwatch.core.model

enum class TrendDirection(val label: String) {
    RISING("Wysoki cukier"), FALLING("Niski cukier"), STABLE("Powrót do zakresu");

    val hapticType: String get() = when (this) {
        RISING -> "HIGH"
        FALLING -> "LOW"
        STABLE -> "STABLE"
    }
}

enum class TrendVibrationPattern(val label: String) {
    SHORT("Krótkie"), STRONG("Mocne"), LONG("Długie mocne");

    companion object {
        fun fromName(name: String?): TrendVibrationPattern =
            entries.firstOrNull { it.name == name } ?: STRONG
    }
}

data class HapticWaveform(val timings: LongArray, val amplitudes: IntArray)

object TrendHaptics {
    fun nextDirection(
        reading: GlucoseReading,
        previousReading: GlucoseReading?,
        nowMillis: Long,
        settings: UserSettings = UserSettings()
    ): TrendDirection? {
        if (!reading.isFreshAt(nowMillis, settings.staleAfterMillis) || !reading.value.isFinite()) return null
        if (previousReading != null && reading.timestamp <= previousReading.timestamp) return null
        val current = GlucoseAlertPolicy.range(reading.value, settings.lowThreshold, settings.highThreshold)
        val previous = previousReading?.let {
            GlucoseAlertPolicy.range(it.value, settings.lowThreshold, settings.highThreshold)
        } ?: GlucoseRange.NORMAL
        if (current == previous) return null
        return when (current) {
            GlucoseRange.LOW -> TrendDirection.FALLING
            GlucoseRange.HIGH -> TrendDirection.RISING
            GlucoseRange.NORMAL -> TrendDirection.STABLE
        }
    }

    fun isEnabled(settings: UserSettings, direction: TrendDirection): Boolean = when (direction) {
        TrendDirection.RISING -> settings.notifyOutOfRange && settings.alertHighHapticEnabled
        TrendDirection.FALLING -> settings.notifyOutOfRange && settings.alertLowHapticEnabled
        TrendDirection.STABLE -> settings.trendStableEnabled
    }

    fun isAlertEnabled(settings: UserSettings, type: String): Boolean =
        TrendDirection.entries.firstOrNull { it.hapticType == type }?.let { isEnabled(settings, it) } ?: false

    fun shouldSendAlert(settings: UserSettings, type: String, showNotification: Boolean = false): Boolean = when (type) {
        "STABLE" -> true
        "LOW", "HIGH" -> settings.notifyOutOfRange && (isAlertEnabled(settings, type) || showNotification)
        else -> false
    }

    fun patternFor(settings: UserSettings, direction: TrendDirection): TrendVibrationPattern = when (direction) {
        TrendDirection.RISING -> settings.trendRisingPattern
        TrendDirection.FALLING -> settings.trendFallingPattern
        TrendDirection.STABLE -> settings.trendStablePattern
    }

    fun waveformForAlert(settings: UserSettings, type: String, pattern: TrendVibrationPattern? = null): HapticWaveform? {
        val direction = TrendDirection.entries.firstOrNull { it.hapticType == type } ?: return null
        return waveform(direction, pattern ?: patternFor(settings, direction))
    }

    fun waveform(direction: TrendDirection, pattern: TrendVibrationPattern): HapticWaveform {
        val timings = when (pattern) {
            TrendVibrationPattern.SHORT -> when (direction) {
                TrendDirection.RISING -> longArrayOf(0, 350)
                TrendDirection.FALLING -> longArrayOf(0, 120, 100, 120, 100, 160)
                TrendDirection.STABLE -> longArrayOf(0, 100, 150, 100)
            }
            TrendVibrationPattern.STRONG -> when (direction) {
                TrendDirection.RISING -> longArrayOf(0, 650, 200, 850)
                TrendDirection.FALLING -> longArrayOf(0, 300, 130, 300, 130, 300, 250, 600)
                TrendDirection.STABLE -> longArrayOf(0, 450, 300, 450, 300, 450)
            }
            TrendVibrationPattern.LONG -> when (direction) {
                TrendDirection.RISING -> longArrayOf(0, 800, 250, 1000, 300, 1200)
                TrendDirection.FALLING -> longArrayOf(0, 400, 150, 400, 150, 400, 350, 400, 150, 400, 150, 800)
                TrendDirection.STABLE -> longArrayOf(0, 650, 350, 650, 500, 650, 350, 650)
            }
        }
        val amplitude = if (pattern == TrendVibrationPattern.SHORT) 200 else 255
        return HapticWaveform(timings, IntArray(timings.size) { if (it % 2 == 0) 0 else amplitude })
    }
}
