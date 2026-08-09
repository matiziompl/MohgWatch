package com.mohgwatch.phone.service

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.mohgwatch.core.data.DataLayerPaths
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.model.UserSettings
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Wysyła dane na zegarek przez Wearable Data Layer API (Bluetooth).
 * Każda operacja używa setUrgent() dla natychmiastowej dostawy.
 */
class DataLayerSender(private val context: Context) {

    private val dataClient by lazy { Wearable.getDataClient(context) }

    companion object {
        private const val TAG = "DataLayerSender"
    }

    /**
     * Wysyła najnowszy odczyt glukozy na zegarek.
     */
    suspend fun sendGlucoseReading(reading: GlucoseReading) {
        try {
            val request = PutDataMapRequest.create(DataLayerPaths.GLUCOSE_DATA).apply {
                dataMap.putFloat(DataLayerPaths.Keys.GLUCOSE_VALUE, reading.value)
                dataMap.putInt(DataLayerPaths.Keys.TREND_ARROW, reading.trendArrow.apiValue)
                dataMap.putInt(DataLayerPaths.Keys.MEASUREMENT_COLOR, reading.measurementColor.apiValue)
                dataMap.putLong(DataLayerPaths.Keys.TIMESTAMP, reading.timestamp)
                dataMap.putBoolean(DataLayerPaths.Keys.IS_HIGH, reading.isHigh)
                dataMap.putBoolean(DataLayerPaths.Keys.IS_LOW, reading.isLow)
                // Unikatowy ID wymusza propagację nawet przy tych samych wartościach
                dataMap.putString(DataLayerPaths.Keys.UPDATE_ID, UUID.randomUUID().toString())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            Log.d(TAG, "Wysłano odczyt glukozy: ${reading.value} ${reading.trendArrow.symbol}")
        } catch (e: Exception) {
            Log.e(TAG, "Błąd wysyłania odczytu glukozy", e)
        }
    }

    /**
     * Wysyła historię 12h odczytów glukozy na zegarek.
     */
    suspend fun sendGlucoseHistory(readings: List<GlucoseReading>) {
        try {
            val request = PutDataMapRequest.create(DataLayerPaths.GLUCOSE_HISTORY).apply {
                dataMap.putFloatArray(
                    DataLayerPaths.Keys.HISTORY_VALUES,
                    readings.map { it.value }.toFloatArray()
                )
                dataMap.putLongArray(
                    DataLayerPaths.Keys.HISTORY_TIMESTAMPS,
                    readings.map { it.timestamp }.toLongArray()
                )
                dataMap.putIntegerArrayList(
                    DataLayerPaths.Keys.HISTORY_TRENDS,
                    ArrayList(readings.map { it.trendArrow.apiValue })
                )
                dataMap.putIntegerArrayList(
                    DataLayerPaths.Keys.HISTORY_COLORS,
                    ArrayList(readings.map { it.measurementColor.apiValue })
                )
                dataMap.putString(DataLayerPaths.Keys.UPDATE_ID, UUID.randomUUID().toString())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            Log.d(TAG, "Wysłano historię: ${readings.size} odczytów")
        } catch (e: Exception) {
            Log.e(TAG, "Błąd wysyłania historii", e)
        }
    }

    /**
     * Wysyła ustawienia użytkownika na zegarek.
     */
    suspend fun sendSettings(settings: UserSettings) {
        try {
            val request = PutDataMapRequest.create(DataLayerPaths.SETTINGS).apply {
                dataMap.putString(DataLayerPaths.Keys.UNIT, settings.unit.name)
                dataMap.putFloat(DataLayerPaths.Keys.LOW_THRESHOLD, settings.lowThreshold)
                dataMap.putFloat(DataLayerPaths.Keys.HIGH_THRESHOLD, settings.highThreshold)
                dataMap.putString(DataLayerPaths.Keys.PRESET, settings.preset.name)
                dataMap.putInt(DataLayerPaths.Keys.POLL_INTERVAL, settings.pollIntervalMinutes)
                dataMap.putBoolean(DataLayerPaths.Keys.SHOW_DEMO_BUTTON, settings.showDemoButton)
                dataMap.putString(DataLayerPaths.Keys.APP_THEME, settings.appTheme.name)
                dataMap.putString(DataLayerPaths.Keys.THEME_MODE, settings.themeMode.name)
                dataMap.putBoolean(DataLayerPaths.Keys.SYNC_THEME_WITH_WATCH, settings.syncThemeWithWatch)
                dataMap.putString(DataLayerPaths.Keys.WATCH_APP_THEME, settings.watchAppTheme.name)
                dataMap.putString(DataLayerPaths.Keys.LANGUAGE, settings.language)
                dataMap.putString(DataLayerPaths.Keys.UPDATE_ID, UUID.randomUUID().toString())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            Log.d(TAG, "Wysłano ustawienia na zegarek")
        } catch (e: Exception) {
            Log.e(TAG, "Błąd wysyłania ustawień", e)
        }
    }

    /**
     * Wysyła status połączenia telefon→zegarek.
     */
    suspend fun sendConnectionStatus(connected: Boolean, lastSync: Long) {
        try {
            val request = PutDataMapRequest.create(DataLayerPaths.CONNECTION_STATUS).apply {
                dataMap.putBoolean(DataLayerPaths.Keys.IS_CONNECTED, connected)
                dataMap.putLong(DataLayerPaths.Keys.LAST_SYNC, lastSync)
                dataMap.putString(DataLayerPaths.Keys.UPDATE_ID, UUID.randomUUID().toString())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
        } catch (e: Exception) {
            Log.e(TAG, "Błąd wysyłania statusu", e)
        }
    }

    /**
     * Wysyła alert o starych danych na zegarek (brak odczytów).
     */
    suspend fun sendStaleDataAlert(minutes: Long) {
        try {
            val request = PutDataMapRequest.create(DataLayerPaths.STALE_DATA_ALERT).apply {
                dataMap.putLong(DataLayerPaths.Keys.STALE_MINUTES, minutes)
                dataMap.putString(DataLayerPaths.Keys.UPDATE_ID, UUID.randomUUID().toString())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            Log.d(TAG, "Wysłano alert o starych danych: $minutes min")
        } catch (e: Exception) {
            Log.e(TAG, "Błąd wysyłania alertu o starych danych", e)
        }
    }
}
