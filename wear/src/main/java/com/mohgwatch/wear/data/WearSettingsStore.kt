package com.mohgwatch.wear.data

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

private val Context.wearSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mohgwatch_wear_settings"
)

class WearSettingsStore(private val context: Context) {

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
    }

    val settingsFlow: Flow<UserSettings> = context.wearSettingsDataStore.data.map { prefs ->
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
            language = prefs[Keys.LANGUAGE] ?: "system"
        )
    }

    suspend fun getSettings(): UserSettings = settingsFlow.first()

    suspend fun saveSettings(settings: UserSettings) {
        context.wearSettingsDataStore.edit { prefs ->
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
        }
    }
}
