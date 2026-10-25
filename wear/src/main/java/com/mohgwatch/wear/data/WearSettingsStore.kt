package com.mohgwatch.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mohgwatch.core.model.GlucoseUnit
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.core.model.TrendVibrationPattern
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.wearSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mohgwatch_wear_settings"
)

class WearSettingsStore(private val context: Context) {

    private object Keys {
        val UNIT = stringPreferencesKey("unit")
        val LOW_THRESHOLD = floatPreferencesKey("low_threshold")
        val HIGH_THRESHOLD = floatPreferencesKey("high_threshold")
        val POLL_INTERVAL = intPreferencesKey("poll_interval")
        val LANGUAGE = stringPreferencesKey("language")
        val WATCH_WRIST_NOTIFY_MODE = stringPreferencesKey("watch_wrist_notify_mode")
        val WATCH_NOTIFY_OUT_OF_RANGE = booleanPreferencesKey("watch_notify_out_of_range")
        val NOTIFY_OUT_OF_RANGE = booleanPreferencesKey("notify_out_of_range")
        val NOTIFY_STALE_DATA_WATCH = booleanPreferencesKey("notify_stale_data_watch")
        val STALE_DATA_MINUTES = intPreferencesKey("stale_data_minutes")
        val ALERT_LOW_HAPTIC_ENABLED = booleanPreferencesKey("alert_low_haptic_enabled")
        val ALERT_HIGH_HAPTIC_ENABLED = booleanPreferencesKey("alert_high_haptic_enabled")
        val TREND_STABLE_ENABLED = booleanPreferencesKey("trend_stable_enabled")
        val TREND_RISING_PATTERN = stringPreferencesKey("trend_rising_pattern")
        val TREND_FALLING_PATTERN = stringPreferencesKey("trend_falling_pattern")
        val TREND_STABLE_PATTERN = stringPreferencesKey("trend_stable_pattern")
    }

    val settingsFlow: Flow<UserSettings> = context.wearSettingsDataStore.data.map { prefs ->
        UserSettings(
            unit = prefs[Keys.UNIT]?.let {
                try { GlucoseUnit.valueOf(it) } catch (e: Exception) { GlucoseUnit.MG_DL }
            } ?: GlucoseUnit.MG_DL,
            lowThreshold = prefs[Keys.LOW_THRESHOLD] ?: 70f,
            highThreshold = prefs[Keys.HIGH_THRESHOLD] ?: 180f,
            pollIntervalMinutes = prefs[Keys.POLL_INTERVAL] ?: 5,
            language = prefs[Keys.LANGUAGE] ?: "system",
            watchWristNotifyMode = prefs[Keys.WATCH_WRIST_NOTIFY_MODE] ?: "both",
            watchNotifyOutOfRange = prefs[Keys.WATCH_NOTIFY_OUT_OF_RANGE] ?: false,
            notifyOutOfRange = prefs[Keys.NOTIFY_OUT_OF_RANGE] ?: true,
            notifyStaleDataWatch = prefs[Keys.NOTIFY_STALE_DATA_WATCH] ?: true,
            staleDataMinutes = (prefs[Keys.STALE_DATA_MINUTES] ?: 10).coerceIn(1, 60),
            alertLowHapticEnabled = prefs[Keys.ALERT_LOW_HAPTIC_ENABLED] ?: true,
            alertHighHapticEnabled = prefs[Keys.ALERT_HIGH_HAPTIC_ENABLED] ?: true,
            trendStableEnabled = prefs[Keys.TREND_STABLE_ENABLED] ?: false,
            trendRisingPattern = TrendVibrationPattern.fromName(prefs[Keys.TREND_RISING_PATTERN]),
            trendFallingPattern = TrendVibrationPattern.fromName(prefs[Keys.TREND_FALLING_PATTERN]),
            trendStablePattern = TrendVibrationPattern.fromName(prefs[Keys.TREND_STABLE_PATTERN])
        )
    }

    suspend fun getSettings(): UserSettings = settingsFlow.first()

    suspend fun saveSettings(settings: UserSettings) {
        context.wearSettingsDataStore.edit { prefs ->
            prefs[Keys.UNIT] = settings.unit.name
            prefs[Keys.LOW_THRESHOLD] = settings.lowThreshold
            prefs[Keys.HIGH_THRESHOLD] = settings.highThreshold
            prefs[Keys.POLL_INTERVAL] = settings.pollIntervalMinutes
            prefs[Keys.LANGUAGE] = settings.language
            prefs[Keys.WATCH_WRIST_NOTIFY_MODE] = settings.watchWristNotifyMode
            prefs[Keys.WATCH_NOTIFY_OUT_OF_RANGE] = settings.watchNotifyOutOfRange
            prefs[Keys.NOTIFY_OUT_OF_RANGE] = settings.notifyOutOfRange
            prefs[Keys.NOTIFY_STALE_DATA_WATCH] = settings.notifyStaleDataWatch
            prefs[Keys.STALE_DATA_MINUTES] = settings.staleDataMinutes.coerceIn(1, 60)
            prefs[Keys.ALERT_LOW_HAPTIC_ENABLED] = settings.alertLowHapticEnabled
            prefs[Keys.ALERT_HIGH_HAPTIC_ENABLED] = settings.alertHighHapticEnabled
            prefs[Keys.TREND_STABLE_ENABLED] = settings.trendStableEnabled
            prefs[Keys.TREND_RISING_PATTERN] = settings.trendRisingPattern.name
            prefs[Keys.TREND_FALLING_PATTERN] = settings.trendFallingPattern.name
            prefs[Keys.TREND_STABLE_PATTERN] = settings.trendStablePattern.name
        }
    }
}
