package com.mohgwatch.phone.service

import android.content.Context
import android.util.Log
import androidx.work.*
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.data.Diagnostics
import com.mohgwatch.phone.data.GlucoseHistoryStore
import java.util.concurrent.TimeUnit

class SyncWatchdogWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Diagnostics.record(applicationContext, "WATCHDOG_START", Diagnostics.snapshot(applicationContext))
        val settingsStore = SettingsStore(applicationContext)
        val settings = settingsStore.getSettings()
        if (settings.isSyncEnabled) {
            val lastSync = GlucoseHistoryStore(applicationContext).lastSuccessfulSync()
            val maxAllowedGapMs = (settings.pollIntervalMinutes + 5) * 60_000L
            if (System.currentTimeMillis() - lastSync > maxAllowedGapMs) {
                Log.w(TAG, "Watchdog wykrył przestój synchronizacji. Wznawianie usługi...")
                Diagnostics.record(applicationContext, "WATCHDOG_ACTION", "action=arm_recovery lastSuccessfulSync=$lastSync gapMs=${System.currentTimeMillis() - lastSync} allowedGapMs=$maxAllowedGapMs")
                SyncScheduler.schedule(applicationContext, 1_000L)
            }
        }
        Diagnostics.record(applicationContext, "WATCHDOG_END", "enabled=${settings.isSyncEnabled}")
        return Result.success()
    }

    companion object {
        private const val TAG = "SyncWatchdogWorker"
        private const val WORK_NAME = "MohgWatchSyncWatchdog"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWatchdogWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
