package com.mohgwatch.phone.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.mohgwatch.phone.data.Diagnostics
import java.time.Instant
import java.time.ZoneId

object SyncScheduler {
    private const val PREFS = "mohgwatch_sync_recovery"
    private const val EXTRA_EPOCH = "sync_epoch"
    private const val EXTRA_SEQUENCE = "sync_alarm_sequence"
    private const val EXTRA_DUE = "sync_due_elapsed"
    private const val FALLBACK_DELAY = 5 * 60_000L
    private val lock = Any()
    private var installed: Triple<Long, Long, Long>? = null

    private fun preferences(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun state(context: Context): RecoveryAlarmState {
        val prefs = preferences(context)
        return RecoveryAlarmState(
            enabled = prefs.getBoolean("enabled", true),
            dueElapsedMillis = prefs.getLong("due_elapsed", -1L).takeIf { it >= 0L },
            armedAtElapsedMillis = prefs.getLong("armed_at_elapsed", -1L).takeIf { it >= 0L },
            bootCount = prefs.getInt("boot_count", -1).takeIf { it >= 0 },
            epoch = prefs.getLong("epoch", 0L), sequence = prefs.getLong("sequence", 0L))
    }

    private fun save(context: Context, state: RecoveryAlarmState) {
        val saved = preferences(context).edit().putBoolean("enabled", state.enabled)
            .putLong("due_elapsed", state.dueElapsedMillis ?: -1L)
            .putLong("armed_at_elapsed", state.armedAtElapsedMillis ?: -1L)
            .putInt("boot_count", state.bootCount ?: -1)
            .putLong("epoch", state.epoch).putLong("sequence", state.sequence).commit()
        if (!saved) Diagnostics.record(context, "SCHEDULER_PERSIST_ERROR", "Nie udało się utrwalić stanu alarmu")
    }

    private fun bootCount(context: Context): Int? = try {
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT).takeIf { it >= 0 }
    } catch (_: Exception) { null }

    private fun alarmIntent(context: Context, state: RecoveryAlarmState? = null,
                            flags: Int = PendingIntent.FLAG_UPDATE_CURRENT): PendingIntent? =
        PendingIntent.getBroadcast(context, 0,
            Intent(context, SyncAlarmReceiver::class.java).setAction(GlucoseSyncService.ACTION_ALARM_WAKEUP).apply {
                state?.let {
                    putExtra(EXTRA_EPOCH, it.epoch)
                    putExtra(EXTRA_SEQUENCE, it.sequence)
                    putExtra(EXTRA_DUE, it.dueElapsedMillis ?: -1L)
                }
            }, flags or PendingIntent.FLAG_IMMUTABLE)

    fun enable(context: Context) = synchronized(lock) {
        val enabled = RecoveryAlarmPolicy.enable(state(context))
        save(context, enabled)
        installed = null
        Diagnostics.record(context, "SCHEDULER_ENABLED", "epoch=${enabled.epoch}")
    }

    fun schedule(context: Context, delayMs: Long = FALLBACK_DELAY, expectedEpoch: Long? = null) = synchronized(lock) {
        scheduleLocked(context, delayMs, expectedEpoch)
    }

    private fun scheduleLocked(context: Context, delayMs: Long, expectedEpoch: Long?): Boolean {
        val now = SystemClock.elapsedRealtime()
        val planned = RecoveryAlarmPolicy.schedule(state(context), now, bootCount(context), delayMs, expectedEpoch)
            ?: return false
        val due = planned.dueElapsedMillis ?: return false
        val identity = Triple(planned.epoch, planned.sequence, due)
        save(context, planned)
        if (installed == identity) return true
        val manager = context.getSystemService(AlarmManager::class.java) ?: return false
        val alarm = alarmIntent(context, planned) ?: return false
        var exact = Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()
        try {
            if (exact) manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, due, alarm)
            else manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, due, alarm)
        } catch (error: SecurityException) {
            exact = false
            Diagnostics.record(context, "SYNC_ALARM_EXACT_DENIED", Diagnostics.error(error))
            try {
                manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, due, alarm)
            } catch (fallbackError: Exception) {
                Diagnostics.record(context, "SYNC_ALARM_SCHEDULE_ERROR", Diagnostics.error(fallbackError))
                return false
            }
        } catch (error: Exception) {
            Diagnostics.record(context, "SYNC_ALARM_SCHEDULE_ERROR", Diagnostics.error(error))
            return false
        }
        installed = identity
        val plannedWall = Instant.ofEpochMilli(System.currentTimeMillis() + due - now).atZone(ZoneId.systemDefault())
        Diagnostics.record(context, "SYNC_ALARM_PLANNED",
            "epoch=${planned.epoch} sequence=${planned.sequence} dueElapsedMs=$due due=$plannedWall exact=$exact boot=${planned.bootCount ?: "nieznane"}")
        return true
    }

    /** Consume this delivery and arm recovery before any asynchronous settings or service work. */
    fun onAlarmDelivered(context: Context, intent: Intent): Long? = synchronized(lock) {
        val current = state(context)
        val epoch = intent.getLongExtra(EXTRA_EPOCH, -1L).takeIf { it >= 0L }
        val sequence = intent.getLongExtra(EXTRA_SEQUENCE, -1L).takeIf { it >= 0L }
        val due = intent.getLongExtra(EXTRA_DUE, -1L).takeIf { it >= 0L }
        val consumed = RecoveryAlarmPolicy.consume(current, epoch, sequence)
        Diagnostics.record(context, "SYNC_ALARM_DELIVERED",
            "epoch=$epoch sequence=$sequence plannedElapsedMs=${due ?: "nieznane"} actualElapsedMs=${SystemClock.elapsedRealtime()} latenessMs=${due?.let { SystemClock.elapsedRealtime() - it } ?: "nieznane"} accepted=${consumed != null}")
        if (consumed == null) return@synchronized null
        save(context, consumed)
        installed = null
        scheduleLocked(context, FALLBACK_DELAY, consumed.epoch)
        consumed.epoch
    }

    fun onBoot(context: Context): Long? = synchronized(lock) {
        val restarted = RecoveryAlarmPolicy.afterBoot(state(context), bootCount(context))
        save(context, restarted)
        installed = null
        Diagnostics.record(context, "SYSTEM_BOOT", "schedulerEnabled=${restarted.enabled} boot=${restarted.bootCount ?: "nieznane"}")
        if (!restarted.enabled) return@synchronized null
        scheduleLocked(context, FALLBACK_DELAY, restarted.epoch)
        restarted.epoch
    }

    fun cancel(context: Context, expectedEpoch: Long? = null): Boolean = synchronized(lock) {
        val cancelled = RecoveryAlarmPolicy.cancel(state(context), expectedEpoch) ?: return@synchronized false
        save(context, cancelled)
        installed = null
        val manager = context.getSystemService(AlarmManager::class.java)
        alarmIntent(context, flags = PendingIntent.FLAG_NO_CREATE)?.let { manager?.cancel(it); it.cancel() }
        val legacy = Intent(context, GlucoseSyncService::class.java).setAction(GlucoseSyncService.ACTION_ALARM_WAKEUP)
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        listOf(PendingIntent.getService(context, 0, legacy, flags),
            PendingIntent.getForegroundService(context, 0, legacy, flags)).filterNotNull().forEach {
            manager?.cancel(it)
            it.cancel()
        }
        Diagnostics.record(context, "SCHEDULER_DISABLED", "epoch=${cancelled.epoch}")
        true
    }

    fun resumeService(context: Context, expectedEpoch: Long? = null, trigger: String = "recovery") = synchronized(lock) {
        val current = state(context)
        if (!RecoveryAlarmPolicy.isCurrent(current, expectedEpoch)) {
            Diagnostics.record(context, "SYNC_WAKE_SKIPPED", "enabled=${current.enabled} epoch=${current.epoch} expectedEpoch=$expectedEpoch")
            return@synchronized
        }
        Diagnostics.record(context, "SYNC_WAKE_REQUESTED", "epoch=${current.epoch} trigger=$trigger")
        try {
            context.startForegroundService(Intent(context, GlucoseSyncService::class.java)
                .setAction(GlucoseSyncService.ACTION_ALARM_WAKEUP).putExtra("sync_trigger", trigger))
        } catch (error: IllegalStateException) {
            Diagnostics.record(context, "SYNC_WAKE_REJECTED", Diagnostics.error(error))
        } catch (error: SecurityException) {
            Diagnostics.record(context, "SYNC_WAKE_REJECTED", Diagnostics.error(error))
        }
    }
}
