package com.mohgwatch.wear.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.mohgwatch.core.data.DataLayerPaths
import kotlinx.coroutines.*
import java.util.UUID

/**
 * Zarządza sprzętowym czujnikiem kontaktu ze skórą (LOW_LATENCY_OFFBODY_DETECT) na Wear OS.
 * Przekazuje stan noszenia zegarka (isWorn) do telefonu przez DataLayer API.
 */
object WristDetectionManager {

    private const val TAG = "WristDetectionManager"
    private const val CHANNEL_ID = "mohgwatch_wrist_channel"
    private const val NOTIFICATION_ID = 3001

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isListening = false
    private var lastWornState: Boolean = true
    private var lastSensorValue: Float = 1.0f

    val isWornState = kotlinx.coroutines.flow.MutableStateFlow(true)

    private var sensorManager: SensorManager? = null
    private var offBodySensor: Sensor? = null

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT) {
                val value = event.values.getOrNull(0) ?: return
                val isWorn = (value == 1.0f)
                val stateChanged = (isWorn != lastWornState)
                lastWornState = isWorn
                lastSensorValue = value
                isWornState.value = isWorn

                Log.d(TAG, "Wykryto zmianę czujnika nadgarstka: value=$value, isWorn=$isWorn")
                sendWristStatus(isWorn, value)

                if (stateChanged) {
                    showWatchNotification(isWorn, value)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private var appContext: Context? = null

    fun start(context: Context) {
        if (isListening) return
        appContext = context.applicationContext
        createNotificationChannel(context)

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        offBodySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT)

        if (offBodySensor != null) {
            sensorManager?.registerListener(sensorListener, offBodySensor, SensorManager.SENSOR_DELAY_FASTEST)
            isListening = true
            Log.d(TAG, "Zarejestrowano czujnik LOW_LATENCY_OFFBODY_DETECT")
        } else {
            Log.w(TAG, "Brak sprzętowego sensora LOW_LATENCY_OFFBODY_DETECT na tym urządzeniu")
        }

        // Wyślij stan początkowy
        isWornState.value = lastWornState
        sendWristStatus(lastWornState, lastSensorValue)
    }

    fun stop() {
        if (!isListening) return
        sensorManager?.unregisterListener(sensorListener)
        isListening = false
    }

    fun requestStatusUpdate(notify: Boolean = true) {
        sendWristStatus(lastWornState, lastSensorValue)
        if (notify) {
            showWatchNotification(lastWornState, lastSensorValue)
        }
    }

    private fun sendWristStatus(isWorn: Boolean, sensorValue: Float) {
        val context = appContext ?: return
        scope.launch {
            try {
                val request = PutDataMapRequest.create(DataLayerPaths.WRIST_STATUS).apply {
                    dataMap.putBoolean(DataLayerPaths.Keys.IS_WORN, isWorn)
                    dataMap.putFloat(DataLayerPaths.Keys.SENSOR_VALUE, sensorValue)
                    dataMap.putString(
                        DataLayerPaths.Keys.SENSOR_STATUS_TEXT,
                        if (isWorn) "Na ręku (kontakt ze skórą)" else "Zdjęty z ręki (brak kontaktu)"
                    )
                    dataMap.putLong(DataLayerPaths.Keys.TIMESTAMP, System.currentTimeMillis())
                    dataMap.putString(DataLayerPaths.Keys.UPDATE_ID, UUID.randomUUID().toString())
                }.asPutDataRequest().setUrgent()

                Wearable.getDataClient(context).putDataItem(request)
                Log.d(TAG, "Wysłano stan nadgarstka do telefonu: isWorn=$isWorn, val=$sensorValue")
            } catch (e: Exception) {
                Log.e(TAG, "Błąd wysyłania stanu nadgarstka", e)
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Status nadgarstka",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Powiadomienia o wykryciu obecności zegarka na ręku"
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun showWatchNotification(isWorn: Boolean, sensorValue: Float) {
        val context = appContext ?: return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val statusText = if (isWorn) "Zegarek jest NA RĘKU" else "Zegarek jest ZDJĘTY Z RĘKI"
        val detail = "Wartość sensora: $sensorValue (1.0 = kontakt, 0.0 = brak)"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Czujnik nadgarstka")
            .setContentText(statusText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$statusText\n$detail"))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }
}
