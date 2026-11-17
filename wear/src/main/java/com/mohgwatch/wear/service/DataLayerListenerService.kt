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
import kotlinx.coroutines.cancel

/**
 * Odbiera dane glukozy z telefonu przez Wearable Data Layer API (Bluetooth).
 * Zapisuje do Room DB i aktualizuje komplikacje oraz Kafelki (Tiles).
 */
class DataLayerListenerService : WearableListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val repository by lazy { GlucoseRepository(this) }

    companion object {
        private const val TAG = "DataLayerListener"
        private val glucoseAlertNotifications = LatestGlucoseAlertGate()
    }

    override fun onCreate() {
        super.onCreate()
        glucoseAlertNotifications.restoreLastEventTimestamp(
            getSharedPreferences("mohgwatch_alert_delivery", MODE_PRIVATE).getLong("last_event_timestamp", 0L))
        glucoseAlertNotifications.restoreLastMeasurementTimestamp(
            getSharedPreferences("mohgwatch_alert_delivery", MODE_PRIVATE).getLong("last_measurement_timestamp", 0L))
        WristDetectionManager.start(this)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onMessageReceived(messageEvent: com.google.android.gms.wearable.MessageEvent) {
        if (messageEvent.path == DataLayerPaths.REQUEST_WRIST_CHECK) {
            WristDetectionManager.requestStatusUpdate(notify = true)
        }
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
                    DataLayerPaths.STALE_DATA_ALERT -> handleStaleDataAlert()
                    DataLayerPaths.HAPTIC_ALERT -> handleHapticAlert(event)
                    DataLayerPaths.REQUEST_WRIST_CHECK -> WristDetectionManager.requestStatusUpdate(notify = true)
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
        WatchGlucoseNotifications.observeReading(reading)

        scope.launch {
            repository.insertReading(reading)
            WatchStaleDataMonitor.refresh(this@DataLayerListenerService)

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
        if (values.size != timestamps.size) {
            Log.w(TAG, "Pomijam historię z różną liczbą wartości i znaczników czasu")
            return
        }

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
            WatchStaleDataMonitor.refresh(this@DataLayerListenerService)

            requestTileUpdate()
        }
    }

    private fun handleSettings(event: DataEvent) {
        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
        
        val newSettings = UserSettings(
            unit = GlucoseUnit.valueOf(dataMap.getString(DataLayerPaths.Keys.UNIT) ?: GlucoseUnit.MG_DL.name),
            lowThreshold = dataMap.getFloat(DataLayerPaths.Keys.LOW_THRESHOLD),
            highThreshold = dataMap.getFloat(DataLayerPaths.Keys.HIGH_THRESHOLD),
            pollIntervalMinutes = dataMap.getInt(DataLayerPaths.Keys.POLL_INTERVAL),
            language = dataMap.getString(DataLayerPaths.Keys.LANGUAGE) ?: "system",
            watchWristNotifyMode = dataMap.getString(DataLayerPaths.Keys.WATCH_WRIST_NOTIFY_MODE) ?: "both",
            watchNotifyOutOfRange = dataMap.getBoolean(DataLayerPaths.Keys.WATCH_NOTIFY_OUT_OF_RANGE, false),
            notifyOutOfRange = dataMap.getBoolean(DataLayerPaths.Keys.NOTIFY_OUT_OF_RANGE, true),
            alertLowHapticEnabled = dataMap.getBoolean(DataLayerPaths.Keys.ALERT_LOW_HAPTIC_ENABLED, true),
            alertHighHapticEnabled = dataMap.getBoolean(DataLayerPaths.Keys.ALERT_HIGH_HAPTIC_ENABLED, true),
            notifyStaleDataWatch = dataMap.getBoolean(DataLayerPaths.Keys.NOTIFY_STALE_DATA_WATCH, true),
            staleDataMinutes = dataMap.getInt(DataLayerPaths.Keys.STALE_DATA_MINUTES, 10).coerceIn(1, 60),
            trendStableEnabled = dataMap.getBoolean(DataLayerPaths.Keys.TREND_STABLE_ENABLED, false),
            trendRisingPattern = TrendVibrationPattern.fromName(dataMap.getString(DataLayerPaths.Keys.TREND_RISING_PATTERN)),
            trendFallingPattern = TrendVibrationPattern.fromName(dataMap.getString(DataLayerPaths.Keys.TREND_FALLING_PATTERN)),
            trendStablePattern = TrendVibrationPattern.fromName(dataMap.getString(DataLayerPaths.Keys.TREND_STABLE_PATTERN))
        )

        Log.d(TAG, "Odebrano ustawienia: unit=${newSettings.unit}")
        
        val settingsStore = com.mohgwatch.wear.data.WearSettingsStore(this)
        scope.launch {
            settingsStore.saveSettings(newSettings)
            requestComplicationUpdate()
            WatchStaleDataMonitor.refresh(this@DataLayerListenerService)
            requestTileUpdate()
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
            val updater = TileService.getUpdater(this)
            updater.requestUpdate(GlucoseHistoryTileService::class.java)
            updater.requestUpdate(InjectionSiteTileService::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Błąd aktualizacji kafelków (Tile)", e)
        }
    }

    private fun handleStaleDataAlert() {
        scope.launch { WatchStaleDataMonitor.refresh(this@DataLayerListenerService) }
    }

    private fun handleHapticAlert(event: DataEvent) {
        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
        val type = dataMap.getString(DataLayerPaths.Keys.HAPTIC_TYPE) ?: return
        Log.d(TAG, "Odebrano alert haptyczny: $type")

        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vm = getSystemService(android.os.VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(android.os.Vibrator::class.java)
        }

        val preview = dataMap.getBoolean(DataLayerPaths.Keys.HAPTIC_PREVIEW, false)
        val requestedPattern = dataMap.getString(DataLayerPaths.Keys.HAPTIC_PATTERN)
        val direction = TrendDirection.entries.firstOrNull {
            it.hapticType == type || (preview && "TREND_${it.name}" == type)
        } ?: return
        if (preview) {
            if (vibrator == null || !vibrator.hasVibrator()) return
            val token = watchHapticPriority.optionalToken()
            scope.launch {
                val settings = com.mohgwatch.wear.data.WearSettingsStore(this@DataLayerListenerService).getSettings()
                val selected = requestedPattern?.let { TrendVibrationPattern.fromName(it) }
                    ?: TrendHaptics.patternFor(settings, direction)
                val waveform = TrendHaptics.waveformForAlert(settings, direction.hapticType, selected) ?: return@launch
                watchHapticPriority.playOptional(token, android.os.SystemClock.elapsedRealtime()) {
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(waveform.timings, waveform.amplitudes, -1))
                }
            }
            return
        }

        val measurementTimestamp = if (dataMap.containsKey(DataLayerPaths.Keys.MEASUREMENT_TIMESTAMP)) {
            dataMap.getLong(DataLayerPaths.Keys.MEASUREMENT_TIMESTAMP)
        } else null
        val notificationToken = synchronized(glucoseAlertNotifications) {
            val token = glucoseAlertNotifications.begin(type,
                eventTimestamp = dataMap.getLong(DataLayerPaths.Keys.TIMESTAMP),
                measurementTimestamp = measurementTimestamp,
                // Preserve arrival order before settings IO; check the configured limit before playback.
                nowMillis = System.currentTimeMillis(), measurementTimeoutMillis = 60 * 60_000L) ?: return
            getSharedPreferences("mohgwatch_alert_delivery", MODE_PRIVATE).edit()
                .putLong("last_event_timestamp", glucoseAlertNotifications.lastEventTimestamp)
                .putLong("last_measurement_timestamp", glucoseAlertNotifications.lastMeasurementTimestamp).apply()
            token
        }
        val optionalHapticToken = watchHapticPriority.optionalToken()
        val showNotificationExplicit = dataMap.getBoolean(DataLayerPaths.Keys.SHOW_NOTIFICATION, false)
        val glucoseVal = dataMap.getFloat(DataLayerPaths.Keys.GLUCOSE_VALUE, 0f)

        scope.launch {
            WatchGlucoseNotifications.postAlert(this@DataLayerListenerService, type, glucoseVal,
                measurementTimestamp, showNotificationExplicit, glucoseAlertNotifications, notificationToken)
        }
        if (vibrator == null || !vibrator.hasVibrator()) return

        scope.launch {
            val wearSettings = try {
                com.mohgwatch.wear.data.WearSettingsStore(this@DataLayerListenerService).getSettings()
            } catch (e: java.io.IOException) {
                Log.e(TAG, "Błąd odczytu ustawień; alert użyje domyślnych wibracji", e)
                UserSettings()
            }
            if (measurementTimestamp != null &&
                !WatchStaleDataPolicy.isFresh(measurementTimestamp, System.currentTimeMillis(), wearSettings.staleAfterMillis)) return@launch
            val waveform = TrendHaptics.waveformForAlert(wearSettings, type,
                requestedPattern?.let { TrendVibrationPattern.fromName(it) }) ?: return@launch
            if (TrendHaptics.isAlertEnabled(wearSettings, type)) {
                val effect = android.os.VibrationEffect.createWaveform(waveform.timings, waveform.amplitudes, -1)
                glucoseAlertNotifications.applyIfCurrent(notificationToken) {
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (type == "LOW" || type == "HIGH") {
                        watchHapticPriority.playCritical(now, waveform.timings.sum() + 100L) { vibrator.vibrate(effect) }
                    } else {
                        watchHapticPriority.playOptional(optionalHapticToken, now) { vibrator.vibrate(effect) }
                    }
                }
            }
        }
    }
}
