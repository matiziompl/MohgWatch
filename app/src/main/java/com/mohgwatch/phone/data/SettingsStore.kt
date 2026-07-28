package com.mohgwatch.phone.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mohgwatch.core.model.AppTheme
import com.mohgwatch.core.model.GlucoseUnit
import com.mohgwatch.core.model.ThemeMode
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.core.model.WatchFacePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mohgwatch_settings"
)

/**
 * Przechowywanie ustawień użytkownika (jednostki, progi, preset, interwał, przycisk demo).
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val UNIT = stringPreferencesKey("unit")
        val LOW_THRESHOLD = floatPreferencesKey("low_threshold")
        val HIGH_THRESHOLD = floatPreferencesKey("high_threshold")
        val PRESET = stringPreferencesKey("preset")
        val POLL_INTERVAL = intPreferencesKey("poll_interval")
        val SHOW_DEMO_BUTTON = booleanPreferencesKey("show_demo_button")
        val APP_THEME = stringPreferencesKey("app_theme")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SYNC_THEME_WITH_WATCH = booleanPreferencesKey("sync_theme_with_watch")
        val WATCH_APP_THEME = stringPreferencesKey("watch_app_theme")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFY_DISCONNECT = booleanPreferencesKey("notify_disconnect")
        val NOTIFY_OUT_OF_RANGE = booleanPreferencesKey("notify_out_of_range")
        val ALERT_LOW_THRESHOLD = floatPreferencesKey("alert_low_threshold")
        val ALERT_HIGH_THRESHOLD = floatPreferencesKey("alert_high_threshold")
        val ALERT_VERY_HIGH_THRESHOLD = floatPreferencesKey("alert_very_high_threshold")
        val DISCONNECT_SOUND_URI = stringPreferencesKey("disconnect_sound_uri")
        val LOW_GLUCOSE_SOUND_URI = stringPreferencesKey("low_glucose_sound_uri")
        val HIGH_GLUCOSE_SOUND_URI = stringPreferencesKey("high_glucose_sound_uri")
        val WATCH_LOW_THRESHOLD = floatPreferencesKey("watch_low_threshold")
        val WATCH_HIGH_THRESHOLD = floatPreferencesKey("watch_high_threshold")
        val WATCH_VERY_HIGH_THRESHOLD = floatPreferencesKey("watch_very_high_threshold")
        val WATCH_TREND_ARROW_STYLE = stringPreferencesKey("watch_trend_arrow_style")
        val WATCH_GLUCOSE_FONT = stringPreferencesKey("watch_glucose_font")
        val NOTIFICATION_VOLUME = floatPreferencesKey("notification_volume")
        val NIGHT_MODE_ENABLED = booleanPreferencesKey("night_mode_enabled")
        val NIGHT_START_TIME = stringPreferencesKey("night_start_time")
        val NIGHT_END_TIME = stringPreferencesKey("night_end_time")
        val NIGHT_NOTIFICATION_VOLUME = floatPreferencesKey("night_notification_volume")
        val LOG_BACKGROUND_COLOR = stringPreferencesKey("log_background_color")
        val NOTIFICATION_SOUND_DELAY_SECONDS = intPreferencesKey("notification_sound_delay_seconds")
    }

    val settingsFlow: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        UserSettings(
            unit = prefs[Keys.UNIT]?.let {
                try { GlucoseUnit.valueOf(it) } catch (e: Exception) { GlucoseUnit.MG_DL }
            } ?: GlucoseUnit.MG_DL,
            lowThreshold = prefs[Keys.LOW_THRESHOLD] ?: 70f,
            highThreshold = prefs[Keys.HIGH_THRESHOLD] ?: 180f,
            preset = prefs[Keys.PRESET]?.let {
                try { WatchFacePreset.valueOf(it) } catch (e: Exception) { WatchFacePreset.D1_CLASSIC }
            } ?: WatchFacePreset.D1_CLASSIC,
            pollIntervalMinutes = prefs[Keys.POLL_INTERVAL] ?: 5,
            showDemoButton = prefs[Keys.SHOW_DEMO_BUTTON] ?: true,
            appTheme = prefs[Keys.APP_THEME]?.let {
                try { AppTheme.valueOf(it) } catch (e: Exception) { AppTheme.DEFAULT }
            } ?: AppTheme.DEFAULT,
            themeMode = prefs[Keys.THEME_MODE]?.let {
                try { ThemeMode.valueOf(it) } catch (e: Exception) { ThemeMode.SYSTEM }
            } ?: ThemeMode.SYSTEM,
            syncThemeWithWatch = prefs[Keys.SYNC_THEME_WITH_WATCH] ?: true,
            watchAppTheme = prefs[Keys.WATCH_APP_THEME]?.let {
                try { AppTheme.valueOf(it) } catch (e: Exception) { AppTheme.DEFAULT }
            } ?: AppTheme.DEFAULT,
            language = prefs[Keys.LANGUAGE] ?: "system",
            notifyDisconnect = prefs[Keys.NOTIFY_DISCONNECT] ?: true,
            notifyOutOfRange = prefs[Keys.NOTIFY_OUT_OF_RANGE] ?: true,
            alertLowThreshold = prefs[Keys.ALERT_LOW_THRESHOLD] ?: 70f,
            alertHighThreshold = prefs[Keys.ALERT_HIGH_THRESHOLD] ?: 180f,
            alertVeryHighThreshold = prefs[Keys.ALERT_VERY_HIGH_THRESHOLD] ?: 250f,
            disconnectSoundUri = prefs[Keys.DISCONNECT_SOUND_URI],
            lowGlucoseSoundUri = prefs[Keys.LOW_GLUCOSE_SOUND_URI],
            highGlucoseSoundUri = prefs[Keys.HIGH_GLUCOSE_SOUND_URI],
            watchLowThreshold = prefs[Keys.WATCH_LOW_THRESHOLD] ?: 70f,
            watchHighThreshold = prefs[Keys.WATCH_HIGH_THRESHOLD] ?: 180f,
            watchVeryHighThreshold = prefs[Keys.WATCH_VERY_HIGH_THRESHOLD] ?: 250f,
            watchTrendArrowStyle = prefs[Keys.WATCH_TREND_ARROW_STYLE] ?: "next_to_value",
            watchGlucoseFont = prefs[Keys.WATCH_GLUCOSE_FONT] ?: "default",
            notificationVolume = prefs[Keys.NOTIFICATION_VOLUME] ?: 1.0f,
            nightModeEnabled = prefs[Keys.NIGHT_MODE_ENABLED] ?: true,
            nightStartTime = prefs[Keys.NIGHT_START_TIME] ?: "22:00",
            nightEndTime = prefs[Keys.NIGHT_END_TIME] ?: "07:00",
            nightNotificationVolume = prefs[Keys.NIGHT_NOTIFICATION_VOLUME] ?: 1.0f,
            logBackgroundColor = prefs[Keys.LOG_BACKGROUND_COLOR]?.let {
                try { com.mohgwatch.core.model.LogBgColor.valueOf(it) } catch (e: Exception) { com.mohgwatch.core.model.LogBgColor.WHITE }
            } ?: com.mohgwatch.core.model.LogBgColor.WHITE,
            notificationSoundDelaySeconds = prefs[Keys.NOTIFICATION_SOUND_DELAY_SECONDS] ?: 5
        )
    }

    suspend fun getSettings(): UserSettings = settingsFlow.first()

    suspend fun saveSettings(settings: UserSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.UNIT] = settings.unit.name
            prefs[Keys.LOW_THRESHOLD] = settings.lowThreshold
            prefs[Keys.HIGH_THRESHOLD] = settings.highThreshold
            prefs[Keys.PRESET] = settings.preset.name
            prefs[Keys.POLL_INTERVAL] = settings.pollIntervalMinutes
            prefs[Keys.SHOW_DEMO_BUTTON] = settings.showDemoButton
            prefs[Keys.APP_THEME] = settings.appTheme.name
            prefs[Keys.THEME_MODE] = settings.themeMode.name
            prefs[Keys.SYNC_THEME_WITH_WATCH] = settings.syncThemeWithWatch
            prefs[Keys.WATCH_APP_THEME] = settings.watchAppTheme.name
            prefs[Keys.LANGUAGE] = settings.language
            prefs[Keys.NOTIFY_DISCONNECT] = settings.notifyDisconnect
            prefs[Keys.NOTIFY_OUT_OF_RANGE] = settings.notifyOutOfRange
            prefs[Keys.ALERT_LOW_THRESHOLD] = settings.alertLowThreshold
            prefs[Keys.ALERT_HIGH_THRESHOLD] = settings.alertHighThreshold
            prefs[Keys.ALERT_VERY_HIGH_THRESHOLD] = settings.alertVeryHighThreshold
            settings.disconnectSoundUri?.let { prefs[Keys.DISCONNECT_SOUND_URI] = it } ?: prefs.remove(Keys.DISCONNECT_SOUND_URI)
            settings.lowGlucoseSoundUri?.let { prefs[Keys.LOW_GLUCOSE_SOUND_URI] = it } ?: prefs.remove(Keys.LOW_GLUCOSE_SOUND_URI)
            settings.highGlucoseSoundUri?.let { prefs[Keys.HIGH_GLUCOSE_SOUND_URI] = it } ?: prefs.remove(Keys.HIGH_GLUCOSE_SOUND_URI)
            prefs[Keys.WATCH_LOW_THRESHOLD] = settings.watchLowThreshold
            prefs[Keys.WATCH_HIGH_THRESHOLD] = settings.watchHighThreshold
            prefs[Keys.WATCH_VERY_HIGH_THRESHOLD] = settings.watchVeryHighThreshold
            prefs[Keys.WATCH_TREND_ARROW_STYLE] = settings.watchTrendArrowStyle
            prefs[Keys.WATCH_GLUCOSE_FONT] = settings.watchGlucoseFont
            prefs[Keys.NOTIFICATION_VOLUME] = settings.notificationVolume
            prefs[Keys.NIGHT_MODE_ENABLED] = settings.nightModeEnabled
            prefs[Keys.NIGHT_START_TIME] = settings.nightStartTime
            prefs[Keys.NIGHT_END_TIME] = settings.nightEndTime
            prefs[Keys.NIGHT_NOTIFICATION_VOLUME] = settings.nightNotificationVolume
            prefs[Keys.LOG_BACKGROUND_COLOR] = settings.logBackgroundColor.name
            prefs[Keys.NOTIFICATION_SOUND_DELAY_SECONDS] = settings.notificationSoundDelaySeconds
        }
    }
}
