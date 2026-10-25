package com.mohgwatch.phone.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mohgwatch.core.model.AppTheme
import com.mohgwatch.core.model.GlucoseUnit
import com.mohgwatch.core.model.ThemeMode
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.core.model.TrendVibrationPattern
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mohgwatch_settings"
)

/**
 * Przechowywanie ustawień użytkownika (jednostki, progi, interwał, przycisk demo).
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val UNIT = stringPreferencesKey("unit")
        val LOW_THRESHOLD = floatPreferencesKey("low_threshold")
        val HIGH_THRESHOLD = floatPreferencesKey("high_threshold")
        val VERY_HIGH_THRESHOLD = floatPreferencesKey("very_high_threshold")
        val POLL_INTERVAL = intPreferencesKey("poll_interval")
        val SHOW_DEMO_BUTTON = booleanPreferencesKey("show_demo_button")
        val APP_THEME = stringPreferencesKey("app_theme")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFY_DISCONNECT = booleanPreferencesKey("notify_disconnect")
        val NOTIFY_OUT_OF_RANGE = booleanPreferencesKey("notify_out_of_range")
        val NOTIFY_STALE_DATA_WATCH = booleanPreferencesKey("notify_stale_data_watch")
        val STALE_DATA_MINUTES = intPreferencesKey("stale_data_minutes")
        val ALERT_LOW_HAPTIC_ENABLED = booleanPreferencesKey("alert_low_haptic_enabled")
        val ALERT_HIGH_HAPTIC_ENABLED = booleanPreferencesKey("alert_high_haptic_enabled")
        val TREND_STABLE_ENABLED = booleanPreferencesKey("trend_stable_enabled")
        val TREND_RISING_PATTERN = stringPreferencesKey("trend_rising_pattern")
        val TREND_FALLING_PATTERN = stringPreferencesKey("trend_falling_pattern")
        val TREND_STABLE_PATTERN = stringPreferencesKey("trend_stable_pattern")
        val DISCONNECT_SOUND_URI = stringPreferencesKey("disconnect_sound_uri")
        val LOW_GLUCOSE_SOUND_URI = stringPreferencesKey("low_glucose_sound_uri")
        val HIGH_GLUCOSE_SOUND_URI = stringPreferencesKey("high_glucose_sound_uri")
        val WATCH_LOW_THRESHOLD = floatPreferencesKey("watch_low_threshold")
        val WATCH_HIGH_THRESHOLD = floatPreferencesKey("watch_high_threshold")
        val WATCH_VERY_HIGH_THRESHOLD = floatPreferencesKey("watch_very_high_threshold")
        val NOTIFICATION_VOLUME = floatPreferencesKey("notification_volume")
        val NIGHT_MODE_ENABLED = booleanPreferencesKey("night_mode_enabled")
        val NIGHT_START_TIME = stringPreferencesKey("night_start_time")
        val NIGHT_END_TIME = stringPreferencesKey("night_end_time")
        val NIGHT_NOTIFICATION_VOLUME = floatPreferencesKey("night_notification_volume")
        val LOG_BACKGROUND_COLOR = stringPreferencesKey("log_background_color")
        val NOTIFICATION_SOUND_DELAY_SECONDS = intPreferencesKey("notification_sound_delay_seconds")
        val PERSISTENT_WAKELOCK_ENABLED = booleanPreferencesKey("persistent_wakelock_enabled")
        val SHOW_INJECTION_SITES = booleanPreferencesKey("show_injection_sites")
        val WIDGET_PRIMARY_THEME = stringPreferencesKey("widget_primary_theme")
        val WIDGET_BG_COLOR = stringPreferencesKey("widget_bg_color")
        val WIDGET_GLUCOSE_COLOR = stringPreferencesKey("widget_glucose_color")
        val COLOR_PICKER_STYLE = stringPreferencesKey("color_picker_style")
        val HIDE_STATUS_HEADER = booleanPreferencesKey("hide_status_header")
        val MUTE_PHONE_WHEN_WATCH_CONNECTED = booleanPreferencesKey("mute_phone_when_watch_connected")
        val IS_SYNC_ENABLED = booleanPreferencesKey("is_sync_enabled")
        val SHOW_WRIST_STATUS_IN_NOTIFICATION = booleanPreferencesKey("show_wrist_status_in_notification")
        val WATCH_WRIST_NOTIFY_MODE = stringPreferencesKey("watch_wrist_notify_mode")
        val DOSE_BREAKFAST = stringPreferencesKey("dose_breakfast")
        val DOSE_LUNCH = stringPreferencesKey("dose_lunch")
        val DOSE_DINNER = stringPreferencesKey("dose_dinner")
        val DOSE_NIGHT = stringPreferencesKey("dose_night")
        val WATCH_NOTIFY_OUT_OF_RANGE = booleanPreferencesKey("watch_notify_out_of_range")
        val NIGHT_PHONE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("night_phone_notifications_enabled")
    }

    val settingsFlow: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        UserSettings(
            unit = prefs[Keys.UNIT]?.let {
                try { GlucoseUnit.valueOf(it) } catch (e: Exception) { GlucoseUnit.MG_DL }
            } ?: GlucoseUnit.MG_DL,
            lowThreshold = prefs[Keys.LOW_THRESHOLD] ?: 70f,
            highThreshold = prefs[Keys.HIGH_THRESHOLD] ?: 180f,
            veryHighThreshold = prefs[Keys.VERY_HIGH_THRESHOLD] ?: 250f,
            pollIntervalMinutes = prefs[Keys.POLL_INTERVAL] ?: 5,
            showDemoButton = prefs[Keys.SHOW_DEMO_BUTTON] ?: true,
            appTheme = prefs[Keys.APP_THEME]?.let {
                try { AppTheme.valueOf(it) } catch (e: Exception) { AppTheme.DEFAULT }
            } ?: AppTheme.DEFAULT,
            themeMode = prefs[Keys.THEME_MODE]?.let {
                try { ThemeMode.valueOf(it) } catch (e: Exception) { ThemeMode.SYSTEM }
            } ?: ThemeMode.SYSTEM,
            language = prefs[Keys.LANGUAGE] ?: "system",
            notifyDisconnect = prefs[Keys.NOTIFY_DISCONNECT] ?: true,
            notifyOutOfRange = prefs[Keys.NOTIFY_OUT_OF_RANGE] ?: true,
            notifyStaleDataWatch = prefs[Keys.NOTIFY_STALE_DATA_WATCH] ?: true,
            staleDataMinutes = (prefs[Keys.STALE_DATA_MINUTES] ?: 10).coerceIn(1, 60),
            alertLowHapticEnabled = prefs[Keys.ALERT_LOW_HAPTIC_ENABLED] ?: true,
            alertHighHapticEnabled = prefs[Keys.ALERT_HIGH_HAPTIC_ENABLED] ?: true,
            trendStableEnabled = prefs[Keys.TREND_STABLE_ENABLED] ?: false,
            trendRisingPattern = TrendVibrationPattern.fromName(prefs[Keys.TREND_RISING_PATTERN]),
            trendFallingPattern = TrendVibrationPattern.fromName(prefs[Keys.TREND_FALLING_PATTERN]),
            trendStablePattern = TrendVibrationPattern.fromName(prefs[Keys.TREND_STABLE_PATTERN]),
            disconnectSoundUri = prefs[Keys.DISCONNECT_SOUND_URI],
            lowGlucoseSoundUri = prefs[Keys.LOW_GLUCOSE_SOUND_URI],
            highGlucoseSoundUri = prefs[Keys.HIGH_GLUCOSE_SOUND_URI],
            watchLowThreshold = prefs[Keys.WATCH_LOW_THRESHOLD] ?: 70f,
            watchHighThreshold = prefs[Keys.WATCH_HIGH_THRESHOLD] ?: 180f,
            watchVeryHighThreshold = prefs[Keys.WATCH_VERY_HIGH_THRESHOLD] ?: 250f,
            notificationVolume = prefs[Keys.NOTIFICATION_VOLUME] ?: 1.0f,
            nightModeEnabled = prefs[Keys.NIGHT_MODE_ENABLED] ?: true,
            nightStartTime = prefs[Keys.NIGHT_START_TIME] ?: "22:00",
            nightEndTime = prefs[Keys.NIGHT_END_TIME] ?: "07:00",
            nightNotificationVolume = prefs[Keys.NIGHT_NOTIFICATION_VOLUME] ?: 1.0f,
            logBackgroundColor = prefs[Keys.LOG_BACKGROUND_COLOR]?.let {
                try { com.mohgwatch.core.model.LogBgColor.valueOf(it) } catch (e: Exception) { com.mohgwatch.core.model.LogBgColor.WHITE }
            } ?: com.mohgwatch.core.model.LogBgColor.WHITE,
            notificationSoundDelaySeconds = prefs[Keys.NOTIFICATION_SOUND_DELAY_SECONDS] ?: 5,
            persistentWakeLockEnabled = prefs[Keys.PERSISTENT_WAKELOCK_ENABLED] ?: false,
            showInjectionSites = prefs[Keys.SHOW_INJECTION_SITES] ?: false,
            widgetPrimaryTheme = prefs[Keys.WIDGET_PRIMARY_THEME]?.let {
                try { AppTheme.valueOf(it) } catch (e: Exception) { AppTheme.DEFAULT }
            } ?: AppTheme.DEFAULT,
            widgetBgColor = prefs[Keys.WIDGET_BG_COLOR]?.let {
                try { com.mohgwatch.core.model.WidgetBgColor.valueOf(it) } catch (e: Exception) { com.mohgwatch.core.model.WidgetBgColor.SYSTEM }
            } ?: com.mohgwatch.core.model.WidgetBgColor.SYSTEM,
            widgetGlucoseColor = prefs[Keys.WIDGET_GLUCOSE_COLOR] ?: "by_range",
            colorPickerStyle = prefs[Keys.COLOR_PICKER_STYLE] ?: "list",
            hideStatusHeader = prefs[Keys.HIDE_STATUS_HEADER] ?: false,
            mutePhoneWhenWatchConnected = prefs[Keys.MUTE_PHONE_WHEN_WATCH_CONNECTED] ?: true,
            isSyncEnabled = prefs[Keys.IS_SYNC_ENABLED] ?: true,
            showWristStatusInNotification = prefs[Keys.SHOW_WRIST_STATUS_IN_NOTIFICATION] ?: true,
            watchWristNotifyMode = prefs[Keys.WATCH_WRIST_NOTIFY_MODE] ?: "both",
            doseBreakfast = prefs[Keys.DOSE_BREAKFAST] ?: "",
            doseLunch = prefs[Keys.DOSE_LUNCH] ?: "",
            doseDinner = prefs[Keys.DOSE_DINNER] ?: "",
            doseNight = prefs[Keys.DOSE_NIGHT] ?: "",
            watchNotifyOutOfRange = prefs[Keys.WATCH_NOTIFY_OUT_OF_RANGE] ?: false,
            nightPhoneNotificationsEnabled = prefs[Keys.NIGHT_PHONE_NOTIFICATIONS_ENABLED] ?: false
        )
    }

    suspend fun getSettings(): UserSettings = settingsFlow.first()

    suspend fun setSyncEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.IS_SYNC_ENABLED] = enabled
        }
    }

    suspend fun saveSettings(settings: UserSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.UNIT] = settings.unit.name
            prefs[Keys.LOW_THRESHOLD] = settings.lowThreshold
            prefs[Keys.HIGH_THRESHOLD] = settings.highThreshold
            prefs[Keys.VERY_HIGH_THRESHOLD] = settings.veryHighThreshold
            prefs[Keys.POLL_INTERVAL] = settings.pollIntervalMinutes
            prefs[Keys.SHOW_DEMO_BUTTON] = settings.showDemoButton
            prefs[Keys.APP_THEME] = settings.appTheme.name
            prefs[Keys.THEME_MODE] = settings.themeMode.name
            prefs[Keys.LANGUAGE] = settings.language
            prefs[Keys.NOTIFY_DISCONNECT] = settings.notifyDisconnect
            prefs[Keys.NOTIFY_OUT_OF_RANGE] = settings.notifyOutOfRange
            prefs[Keys.NOTIFY_STALE_DATA_WATCH] = settings.notifyStaleDataWatch
            prefs[Keys.STALE_DATA_MINUTES] = settings.staleDataMinutes.coerceIn(1, 60)
            prefs[Keys.ALERT_LOW_HAPTIC_ENABLED] = settings.alertLowHapticEnabled
            prefs[Keys.ALERT_HIGH_HAPTIC_ENABLED] = settings.alertHighHapticEnabled
            prefs[Keys.TREND_STABLE_ENABLED] = settings.trendStableEnabled
            prefs[Keys.TREND_RISING_PATTERN] = settings.trendRisingPattern.name
            prefs[Keys.TREND_FALLING_PATTERN] = settings.trendFallingPattern.name
            prefs[Keys.TREND_STABLE_PATTERN] = settings.trendStablePattern.name
            settings.disconnectSoundUri?.let { prefs[Keys.DISCONNECT_SOUND_URI] = it } ?: prefs.remove(Keys.DISCONNECT_SOUND_URI)
            settings.lowGlucoseSoundUri?.let { prefs[Keys.LOW_GLUCOSE_SOUND_URI] = it } ?: prefs.remove(Keys.LOW_GLUCOSE_SOUND_URI)
            settings.highGlucoseSoundUri?.let { prefs[Keys.HIGH_GLUCOSE_SOUND_URI] = it } ?: prefs.remove(Keys.HIGH_GLUCOSE_SOUND_URI)
            prefs[Keys.WATCH_LOW_THRESHOLD] = settings.watchLowThreshold
            prefs[Keys.WATCH_HIGH_THRESHOLD] = settings.watchHighThreshold
            prefs[Keys.WATCH_VERY_HIGH_THRESHOLD] = settings.watchVeryHighThreshold
            prefs[Keys.NOTIFICATION_VOLUME] = settings.notificationVolume
            prefs[Keys.NIGHT_MODE_ENABLED] = settings.nightModeEnabled
            prefs[Keys.NIGHT_START_TIME] = settings.nightStartTime
            prefs[Keys.NIGHT_END_TIME] = settings.nightEndTime
            prefs[Keys.NIGHT_NOTIFICATION_VOLUME] = settings.nightNotificationVolume
            prefs[Keys.LOG_BACKGROUND_COLOR] = settings.logBackgroundColor.name
            prefs[Keys.NOTIFICATION_SOUND_DELAY_SECONDS] = settings.notificationSoundDelaySeconds
            prefs[Keys.PERSISTENT_WAKELOCK_ENABLED] = settings.persistentWakeLockEnabled
            prefs[Keys.SHOW_INJECTION_SITES] = settings.showInjectionSites
            prefs[Keys.WIDGET_PRIMARY_THEME] = settings.widgetPrimaryTheme.name
            prefs[Keys.WIDGET_BG_COLOR] = settings.widgetBgColor.name
            prefs[Keys.WIDGET_GLUCOSE_COLOR] = settings.widgetGlucoseColor
            prefs[Keys.COLOR_PICKER_STYLE] = settings.colorPickerStyle
            prefs[Keys.HIDE_STATUS_HEADER] = settings.hideStatusHeader
            prefs[Keys.MUTE_PHONE_WHEN_WATCH_CONNECTED] = settings.mutePhoneWhenWatchConnected
            prefs[Keys.IS_SYNC_ENABLED] = settings.isSyncEnabled
            prefs[Keys.SHOW_WRIST_STATUS_IN_NOTIFICATION] = settings.showWristStatusInNotification
            prefs[Keys.WATCH_WRIST_NOTIFY_MODE] = settings.watchWristNotifyMode
            prefs[Keys.DOSE_BREAKFAST] = settings.doseBreakfast
            prefs[Keys.DOSE_LUNCH] = settings.doseLunch
            prefs[Keys.DOSE_DINNER] = settings.doseDinner
            prefs[Keys.DOSE_NIGHT] = settings.doseNight
            prefs[Keys.WATCH_NOTIFY_OUT_OF_RANGE] = settings.watchNotifyOutOfRange
            prefs[Keys.NIGHT_PHONE_NOTIFICATIONS_ENABLED] = settings.nightPhoneNotificationsEnabled
        }
    }
}
