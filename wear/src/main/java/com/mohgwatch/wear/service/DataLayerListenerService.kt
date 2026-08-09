package com.mohgwatch.wear.service

import android.content.ComponentName
import android.util.Log
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.mohgwatch.core.data.DataLayerPaths
import com.mohgwatch.core.model.*
import com.mohgwatch.wear.data.GlucoseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Odbiera dane glukozy z telefonu przez Wearable Data Layer API (Bluetooth).
 * Zapisuje do Room DB i aktualizuje komplikacje oraz Kafelki (Tiles).
 */
class DataLayerListenerService : WearableListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val repository by lazy { GlucoseRepository(this) }

    companion object {
        private const val TAG = "DataLayerListener"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue

            val path = event.dataItem.uri.path ?: continue

            try {
                when (path) {
                    DataLayerPaths.GLUCOSE_DATA -> handleGlucoseData(event)
                    DataLayerPaths.GLUCOSE_HISTORY -> handleGlucoseHistory(event)
                    DataLayerPaths.SETTINGS -> handleSettings(event)
                    DataLayerPaths.CONNECTION_STATUS -> handleConnectionStatus(event)
                    DataLayerPaths.STALE_DATA_ALERT -> handleStaleDataAlert(event)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Błąd przetwarzania zdarzenia Data Layer: $path", e)
            }
        }
    }

    private fun handleGlucoseData(event: DataEvent) {
        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

        val reading = GlucoseReading(
            value = dataMap.getFloat(DataLayerPaths.Keys.GLUCOSE_VALUE),
            trendArrow = TrendArrow.fromApiValue(dataMap.getInt(DataLayerPaths.Keys.TREND_ARROW)),
            measurementColor = MeasurementColor.fromApiValue(
                dataMap.getInt(DataLayerPaths.Keys.MEASUREMENT_COLOR)
            ),
            timestamp = dataMap.getLong(DataLayerPaths.Keys.TIMESTAMP),
            isHigh = dataMap.getBoolean(DataLayerPaths.Keys.IS_HIGH),
            isLow = dataMap.getBoolean(DataLayerPaths.Keys.IS_LOW)
        )

        Log.d(TAG, "Odebrano odczyt: ${reading.value} ${reading.trendArrow.symbol}")

        scope.launch {
            repository.insertReading(reading)
            repository.cleanup()

            // Aktualizuj komplikacje i Tile
            requestComplicationUpdate()
            requestTileUpdate()
        }
    }

    private fun handleGlucoseHistory(event: DataEvent) {
        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

        val values = dataMap.getFloatArray(DataLayerPaths.Keys.HISTORY_VALUES) ?: return
        val timestamps = dataMap.getLongArray(DataLayerPaths.Keys.HISTORY_TIMESTAMPS) ?: return
        val trends = dataMap.getIntegerArrayList(DataLayerPaths.Keys.HISTORY_TRENDS) ?: return
        val colors = dataMap.getIntegerArrayList(DataLayerPaths.Keys.HISTORY_COLORS) ?: return

        val readings = values.indices.map { i ->
            GlucoseReading(
                value = values[i],
                trendArrow = TrendArrow.fromApiValue(trends.getOrElse(i) { 0 }),
                measurementColor = MeasurementColor.fromApiValue(colors.getOrElse(i) { 0 }),
                timestamp = timestamps[i],
                isHigh = values[i] > 180f,
                isLow = values[i] < 70f
            )
        }

        Log.d(TAG, "Odebrano historię: ${readings.size} odczytów")

        scope.launch {
            repository.insertReadings(readings)
            repository.cleanup()

            requestTileUpdate()
        }
    }

    private fun handleSettings(event: DataEvent) {
        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
        
        val newSettings = UserSettings(
            unit = GlucoseUnit.valueOf(dataMap.getString(DataLayerPaths.Keys.UNIT) ?: GlucoseUnit.MG_DL.name),
            lowThreshold = dataMap.getFloat(DataLayerPaths.Keys.LOW_THRESHOLD),
            highThreshold = dataMap.getFloat(DataLayerPaths.Keys.HIGH_THRESHOLD),
            preset = WatchFacePreset.valueOf(dataMap.getString(DataLayerPaths.Keys.PRESET) ?: WatchFacePreset.D1_CLASSIC.name),
            pollIntervalMinutes = dataMap.getInt(DataLayerPaths.Keys.POLL_INTERVAL),
            showDemoButton = dataMap.getBoolean(DataLayerPaths.Keys.SHOW_DEMO_BUTTON),
            appTheme = try { AppTheme.valueOf(dataMap.getString(DataLayerPaths.Keys.APP_THEME) ?: AppTheme.DEFAULT.name) } catch(e: Exception) { AppTheme.DEFAULT },
            themeMode = try { ThemeMode.valueOf(dataMap.getString(DataLayerPaths.Keys.THEME_MODE) ?: ThemeMode.SYSTEM.name) } catch(e: Exception) { ThemeMode.SYSTEM },
            syncThemeWithWatch = dataMap.getBoolean(DataLayerPaths.Keys.SYNC_THEME_WITH_WATCH, true),
            watchAppTheme = try { AppTheme.valueOf(dataMap.getString(DataLayerPaths.Keys.WATCH_APP_THEME) ?: AppTheme.DEFAULT.name) } catch(e: Exception) { AppTheme.DEFAULT },
            language = dataMap.getString(DataLayerPaths.Keys.LANGUAGE) ?: "system"
        )

        Log.d(TAG, "Odebrano ustawienia: unit=${newSettings.unit}, theme=${newSettings.appTheme}")
        
        val settingsStore = com.mohgwatch.wear.data.WearSettingsStore(this)
        scope.launch {
            settingsStore.saveSettings(newSettings)
        }
    }

    private fun handleConnectionStatus(event: DataEvent) {
        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
        val connected = dataMap.getBoolean(DataLayerPaths.Keys.IS_CONNECTED)
        val lastSync = dataMap.getLong(DataLayerPaths.Keys.LAST_SYNC)
        Log.d(TAG, "Status połączenia: connected=$connected, lastSync=$lastSync")
    }

    private fun requestComplicationUpdate() {
        try {
            val services = listOf(
                GlucoseTrendWhiteComplicationService::class.java,
                GlucoseAndTrendComplicationService::class.java,
                GlucoseWhiteComplicationService::class.java,
                TrendWhiteComplicationService::class.java
            )
            
            services.forEach { serviceClass ->
                val requester = ComplicationDataSourceUpdateRequester.create(
                    this,
                    ComponentName(this, serviceClass)
                )
                requester.requestUpdateAll()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Błąd aktualizacji komplikacji", e)
        }
    }

    private fun requestTileUpdate() {
        try {
            TileService.getUpdater(this).requestUpdate(GlucoseHistoryTileService::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Błąd aktualizacji kafelka (Tile)", e)
        }
    }

    private fun handleStaleDataAlert(event: DataEvent) {
        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
        val minutes = dataMap.getLong(DataLayerPaths.Keys.STALE_MINUTES)
        
        Log.d(TAG, "Odebrano alert o starych danych: $minutes minut")
        
        val manager = getSystemService(android.app.NotificationManager::class.java)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "mohgwatch_stale_data",
                "Stare Dane Glukozy",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarmy o braku nowych odczytów"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
        
        val notification = androidx.core.app.NotificationCompat.Builder(this, "mohgwatch_stale_data")
            .setContentTitle("Brak nowych danych!")
            .setContentText("Ostatni odczyt glukozy był $minutes minut temu.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        manager.notify(2001, notification)
    }
}
