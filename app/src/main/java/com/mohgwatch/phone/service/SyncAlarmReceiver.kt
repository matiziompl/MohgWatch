package com.mohgwatch.phone.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mohgwatch.phone.data.CredentialStore
import com.mohgwatch.phone.data.Diagnostics
import com.mohgwatch.phone.data.SettingsStore
import kotlinx.coroutines.*

class SyncAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != GlucoseSyncService.ACTION_ALARM_WAKEUP) return
        val epoch = SyncScheduler.onAlarmDelivered(context, intent) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(8_000L) {
                    val settings = SettingsStore(context).getSettings()
                    if (!settings.isSyncEnabled) {
                        SyncScheduler.cancel(context, epoch)
                        return@withTimeout
                    }
                    SyncScheduler.schedule(context, settings.pollIntervalMinutes.coerceIn(1, 5) * 60_000L, epoch)
                    if (CredentialStore(context).hasCredentials()) SyncScheduler.resumeService(context, epoch, trigger = "alarm")
                }
            } catch (error: TimeoutCancellationException) {
                Diagnostics.record(context, "SYNC_ALARM_HANDLER_TIMEOUT", "limitMs=8000 epoch=$epoch; alarm awaryjny zaplanowany przed odczytem ustawień")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Diagnostics.record(context, "SYNC_ALARM_HANDLER_ERROR", Diagnostics.error(error))
            } finally {
                pending.finish()
            }
        }
    }
}
