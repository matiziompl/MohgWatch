package com.mohgwatch.wear.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.core.util.GlucoseFormatter
import com.mohgwatch.wear.data.GlucoseRepository
import com.mohgwatch.wear.data.WearSettingsStore
import com.mohgwatch.wear.ui.WearActivity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal const val GLUCOSE_ALERT_CHANNEL_ID = "mohgwatch_glucose_alert"

internal object WatchGlucoseNotifications {
    private const val NOTIFICATION_ID = 2002
    private val mutex = Mutex()
    private val observationLock = Any()
    private var newestObservedMeasurement = 0L

    fun observeReading(reading: GlucoseReading) {
        if (!reading.value.isFinite() || reading.timestamp <= 0 || reading.timestamp > System.currentTimeMillis()) return
        synchronized(observationLock) {
            newestObservedMeasurement = maxOf(newestObservedMeasurement, reading.timestamp)
        }
    }

    suspend fun postAlert(
        context: Context,
        type: String,
        glucoseValue: Float,
        measurementTimestamp: Long?,
        showNotificationExplicit: Boolean,
        gate: LatestGlucoseAlertGate,
        token: Long
    ) = mutex.withLock {
        val settings = settings(context)
        val latestTimestamp = GlucoseRepository(context).getLatest()?.timestamp
        synchronized(observationLock) {
            val now = System.currentTimeMillis()
            val update = WatchGlucoseNotificationPolicy.alert(settings, type, showNotificationExplicit,
                measurementTimestamp, maxOf(latestTimestamp ?: 0L, newestObservedMeasurement), now)
            gate.applyIfCurrent(token) {
                apply(context, update, glucoseValue, measurementTimestamp, settings, now)
            }
        }
    }

    suspend fun refresh(context: Context) = mutex.withLock {
        val settings = settings(context)
        val reading = GlucoseRepository(context).getLatest()
        val manager = context.getSystemService(NotificationManager::class.java)
        synchronized(observationLock) {
            // A newer Data Layer reading may already be waiting for its database write.
            if (reading != null && reading.timestamp < newestObservedMeasurement &&
                settings.notifyOutOfRange && settings.watchNotifyOutOfRange) return@synchronized
            val visible = manager.activeNotifications.firstOrNull { it.id == NOTIFICATION_ID }
            val update = WatchGlucoseNotificationPolicy.refresh(
                visible != null, reading, settings, System.currentTimeMillis(), visible?.notification?.`when` ?: 0L)
            apply(context, update, reading?.value ?: 0f, reading?.timestamp, settings, System.currentTimeMillis())
        }
    }

    private suspend fun settings(context: Context): UserSettings = try {
        WearSettingsStore(context).getSettings()
    } catch (error: java.io.IOException) {
        Log.e("WatchGlucoseNotifications", "Nie można odczytać ustawień powiadomień", error)
        UserSettings()
    }

    private fun apply(
        context: Context,
        update: GlucoseNotificationUpdate,
        value: Float,
        measurementTimestamp: Long?,
        settings: UserSettings,
        now: Long
    ) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (update == GlucoseNotificationUpdate.IGNORE) return
        if (update == GlucoseNotificationUpdate.CANCEL) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.w("WatchGlucoseNotifications", "System zablokował powiadomienia aplikacji na zegarku")
            return
        }
        manager.createNotificationChannel(
            NotificationChannel(GLUCOSE_ALERT_CHANNEL_ID, "Alerty Glukozy", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Standardowe powiadomienia o stężeniu glukozy poza normą"
                enableVibration(false)
                setSound(null, null)
            }
        )
        val low = update == GlucoseNotificationUpdate.LOW
        val subtext = if (low) "Poniżej normy" else "Powyżej normy"
        val content = if (value.isFinite() && value > 0f) "${GlucoseFormatter.formatWithUnit(value, settings.unit)} ($subtext)" else subtext
        val open = PendingIntent.getActivity(context, NOTIFICATION_ID, Intent(context, WearActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, GLUCOSE_ALERT_CHANNEL_ID)
            .setContentTitle(if (low) "Niski cukier!" else "Wysoki cukier!")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setLocalOnly(true)
            .setWhen(measurementTimestamp ?: now)
            .setTimeoutAfter(((measurementTimestamp ?: now) + settings.staleAfterMillis - now).coerceAtLeast(1L))
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}
