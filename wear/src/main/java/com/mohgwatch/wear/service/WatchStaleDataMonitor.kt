package com.mohgwatch.wear.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.mohgwatch.wear.data.GlucoseRepository
import com.mohgwatch.wear.data.WearSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A local alarm still runs when the phone stops sending data. */
object WatchStaleDataMonitor {
    internal const val ACTION_CHECK = "com.mohgwatch.wear.CHECK_STALE_DATA"
    private const val CHANNEL = "mohgwatch_stale_data"
    private const val NOTIFICATION_ID = 2001
    private val mutex = Mutex()

    suspend fun refresh(context: Context) = mutex.withLock {
        val app = context.applicationContext
        val timestamp = GlucoseRepository(app).getLatest()?.timestamp
        val settings = WearSettingsStore(app).getSettings()
        val timeoutMillis = settings.staleAfterMillis
        val now = System.currentTimeMillis()
        val fresh = WatchStaleDataPolicy.isFresh(timestamp, now, timeoutMillis)
        val enabled = settings.notifyStaleDataWatch
        val state = app.getSharedPreferences("watch_stale_monitor", Context.MODE_PRIVATE)
        val alreadyAlerted = state.getBoolean("alerted", false)
        val notifications = app.getSystemService(NotificationManager::class.java)
        val alarms = app.getSystemService(AlarmManager::class.java)
        val intent = Intent(app, WatchStaleDataReceiver::class.java).setAction(ACTION_CHECK)
        val pending = PendingIntent.getBroadcast(
            app, 2001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarms.cancel(pending)
        if (fresh) {
            val expiresAt = timestamp!! + timeoutMillis
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, expiresAt, pending)
            } else {
                // Wear may defer this alarm in Doze; complication validity expires independently.
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, expiresAt, pending)
            }
        }

        if (timestamp == null || fresh || !enabled) {
            notifications.cancel(NOTIFICATION_ID)
            state.edit().putBoolean("alerted", false).commit()
        } else if (WatchStaleDataPolicy.shouldAlert(timestamp, now, enabled, alreadyAlerted, timeoutMillis)) {
            val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (permissionGranted && NotificationManagerCompat.from(app).areNotificationsEnabled()) {
                notifications.createNotificationChannel(
                    NotificationChannel(CHANNEL, "Stare dane glukozy", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Brak nowych odczytów na zegarku"
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 600, 200, 600, 200, 600)
                    }
                )
                val minutes = ((now - timestamp) / 60_000).coerceAtLeast(0)
                val notification = NotificationCompat.Builder(app, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("Brak nowych danych!")
                    .setContentText("Ostatni pomiar glukozy był $minutes minut temu.")
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                val token = watchHapticPriority.optionalToken()
                val posted = watchHapticPriority.playOptional(token, android.os.SystemClock.elapsedRealtime()) {
                    notifications.notify(NOTIFICATION_ID, notification.build())
                }
                if (!posted) notifications.notify(NOTIFICATION_ID, notification.setSilent(true).build())
                state.edit().putBoolean("alerted", true).commit()
            }
        }

        if ((timestamp != null && !fresh) || (alreadyAlerted && fresh)) {
            listOf(
                GlucoseTrendWhiteComplicationService::class.java,
                GlucoseAndTrendComplicationService::class.java,
                GlucoseWhiteComplicationService::class.java,
                TrendWhiteComplicationService::class.java
            ).forEach {
                ComplicationDataSourceUpdateRequester.create(app, ComponentName(app, it)).requestUpdateAll()
            }
            TileService.getUpdater(app).requestUpdate(GlucoseHistoryTileService::class.java)
        }
    }
}

class WatchStaleDataReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(
                WatchStaleDataMonitor.ACTION_CHECK,
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_MY_PACKAGE_REPLACED
            )) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                WatchStaleDataMonitor.refresh(context)
            } catch (e: Exception) {
                Log.e("WatchStaleData", "Nie udało się sprawdzić świeżości pomiaru", e)
            } finally {
                pending.finish()
            }
        }
    }
}
