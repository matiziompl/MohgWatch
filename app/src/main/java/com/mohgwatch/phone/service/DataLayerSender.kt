package com.mohgwatch.phone.service

import android.content.Context
import android.util.Log
import android.os.SystemClock
import com.mohgwatch.phone.data.Diagnostics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
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
class DataLayerSender(private val context: Context, private val cycleId: () -> String? = { null }) {

    private val dataClient by lazy { Wearable.getDataClient(context) }

    companion object {
        private const val TAG = "DataLayerSender"
    }

    /**
     * Wysyła najnowszy odczyt glukozy na zegarek.
     */
    suspend fun sendGlucoseReading(reading: GlucoseReading) {
        operation("glucose_reading") {
            val request = PutDataMapRequest.create(DataLayerPaths.GLUCOSE_DATA).apply {
                dataMap.putFloat(DataLayerPaths.Keys.GLUCOSE_VALUE, reading.value)
                dataMap.putInt(DataLayerPaths.Keys.TREND_ARROW, reading.trendArrow.apiValue)
                dataMap.putLong(DataLayerPaths.Keys.TIMESTAMP, reading.timestamp)
                dataMap.putBoolean(DataLayerPaths.Keys.IS_HIGH, reading.isHigh)
                dataMap.putBoolean(DataLayerPaths.Keys.IS_LOW, reading.isLow)
                // Unikatowy ID wymusza propagację nawet przy tych samych wartościach
                dataMap.putString(DataLayerPaths.Keys.UPDATE_ID, UUID.randomUUID().toString())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            Log.d(TAG, "Wysłano odczyt glukozy: ${reading.value} ${reading.trendArrow.symbol}")
        }
    }

    /**
     * Wysyła historię 12h odczytów glukozy na zegarek.
     */
    suspend fun sendGlucoseHistory(readings: List<GlucoseReading>) {
        operation("glucose_history") {
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
                dataMap.putString(DataLayerPaths.Keys.UPDATE_ID, UUID.randomUUID().toString())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            Log.d(TAG, "Wysłano historię: ${readings.size} odczytów")
        }
    }

    /**
     * Wysyła ustawienia użytkownika na zegarek.
     */
    suspend fun sendSettings(settings: UserSettings) {
        operation("settings") {
            val request = PutDataMapRequest.create(DataLayerPaths.SETTINGS).apply {
                dataMap.putString(DataLayerPaths.Keys.UNIT, settings.unit.name)
                dataMap.putFloat(DataLayerPaths.Keys.LOW_THRESHOLD, settings.lowThreshold)
                dataMap.putFloat(DataLayerPaths.Keys.HIGH_THRESHOLD, settings.highThreshold)
                dataMap.putInt(DataLayerPaths.Keys.POLL_INTERVAL, settings.pollIntervalMinutes)
                dataMap.putString(DataLayerPaths.Keys.LANGUAGE, settings.language)
                dataMap.putString(DataLayerPaths.Keys.WATCH_WRIST_NOTIFY_MODE, settings.watchWristNotifyMode)
                dataMap.putBoolean(DataLayerPaths.Keys.WATCH_NOTIFY_OUT_OF_RANGE, settings.watchNotifyOutOfRange)
                dataMap.putBoolean(DataLayerPaths.Keys.NOTIFY_STALE_DATA_WATCH, settings.notifyStaleDataWatch)
                dataMap.putInt(DataLayerPaths.Keys.STALE_DATA_MINUTES, settings.staleDataMinutes.coerceIn(1, 60))
                dataMap.putBoolean(DataLayerPaths.Keys.NOTIFY_OUT_OF_RANGE, settings.notifyOutOfRange)
                dataMap.putBoolean(DataLayerPaths.Keys.ALERT_LOW_HAPTIC_ENABLED, settings.alertLowHapticEnabled)
                dataMap.putBoolean(DataLayerPaths.Keys.ALERT_HIGH_HAPTIC_ENABLED, settings.alertHighHapticEnabled)
                dataMap.putBoolean(DataLayerPaths.Keys.TREND_STABLE_ENABLED, settings.trendStableEnabled)
                dataMap.putString(DataLayerPaths.Keys.TREND_RISING_PATTERN, settings.trendRisingPattern.name)
                dataMap.putString(DataLayerPaths.Keys.TREND_FALLING_PATTERN, settings.trendFallingPattern.name)
                dataMap.putString(DataLayerPaths.Keys.TREND_STABLE_PATTERN, settings.trendStablePattern.name)
                dataMap.putString(DataLayerPaths.Keys.UPDATE_ID, UUID.randomUUID().toString())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            Log.d(TAG, "Wysłano ustawienia na zegarek")
        }
    }

    /**
     * Wysyła status połączenia telefon→zegarek.
     */
    suspend fun sendConnectionStatus(connected: Boolean, lastSync: Long) {
        operation("connection_status") {
            val request = PutDataMapRequest.create(DataLayerPaths.CONNECTION_STATUS).apply {
                dataMap.putBoolean(DataLayerPaths.Keys.IS_CONNECTED, connected)
                dataMap.putLong(DataLayerPaths.Keys.LAST_SYNC, lastSync)
                dataMap.putString(DataLayerPaths.Keys.UPDATE_ID, UUID.randomUUID().toString())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
        }
    }

    /**
     * Wysyła komendę haptyczną (O-Haptics) na zegarek: LOW (spadek/hipo), HIGH (wzrost/hiper), STABLE (norma).
     */
    suspend fun sendHapticAlert(
        type: String,
        glucoseValue: Float? = null,
        showNotification: Boolean = false,
        pattern: com.mohgwatch.core.model.TrendVibrationPattern? = null,
        preview: Boolean = false,
        measurementTimestamp: Long? = null
    ) {
        operation("haptic_alert") {
            val request = PutDataMapRequest.create(DataLayerPaths.HAPTIC_ALERT).apply {
                dataMap.putString(DataLayerPaths.Keys.HAPTIC_TYPE, type)
                dataMap.putLong(DataLayerPaths.Keys.TIMESTAMP, System.currentTimeMillis())
                measurementTimestamp?.let { dataMap.putLong(DataLayerPaths.Keys.MEASUREMENT_TIMESTAMP, it) }
                pattern?.let { dataMap.putString(DataLayerPaths.Keys.HAPTIC_PATTERN, it.name) }
                dataMap.putBoolean(DataLayerPaths.Keys.HAPTIC_PREVIEW, preview)
                if (glucoseValue != null) {
                    dataMap.putFloat(DataLayerPaths.Keys.GLUCOSE_VALUE, glucoseValue)
                }
                dataMap.putBoolean(DataLayerPaths.Keys.SHOW_NOTIFICATION, showNotification)
                dataMap.putString(DataLayerPaths.Keys.UPDATE_ID, UUID.randomUUID().toString())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            Log.d(TAG, "Wysłano alert haptyczny na zegarek: $type (notyfikacja: $showNotification, glukoza: $glucoseValue)")
        }
    }

    /**
     * Sprawdza, czy zegarek jest aktualnie połączony przez Bluetooth.
     */
    suspend fun isWatchConnected(): Boolean {
        return operation("connected_nodes") {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            nodes.isNotEmpty()
        } ?: false
    }

    /**
     * Wysyła żądanie natychmiastowego sprawdzenia stanu czujnika nadgarstka do zegarka.
     */
    suspend fun requestWristCheck() {
        operation("wrist_check") {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            for (node in nodes) {
                Wearable.getMessageClient(context).sendMessage(
                    node.id,
                    DataLayerPaths.REQUEST_WRIST_CHECK,
                    ByteArray(0)
                ).await()
            }
            Log.d(TAG, "Wysłano żądanie sprawdzenia nadgarstka do zegarka")
        }
    }
    private suspend fun <T> operation(name: String, block: suspend () -> T): T? {
        val started = SystemClock.elapsedRealtime()
        Diagnostics.record(context, "WATCH_START", "operation=$name", cycleId())
        var outcome = "success"
        try {
            return boundedWatchOperation(onFailure = { error ->
                outcome = if (error is TimeoutCancellationException) "timeout" else "error"
                Diagnostics.record(context, "WATCH_${outcome.uppercase()}",
                    "operation=$name causes=${Diagnostics.error(error)}", cycleId())
            }, operation = block)
        } catch (cancelled: CancellationException) {
            outcome = "cancelled"
            throw cancelled
        } finally {
            Diagnostics.record(context, "WATCH_END", "operation=$name result=$outcome durationMs=${SystemClock.elapsedRealtime() - started}", cycleId())
        }
    }

}
