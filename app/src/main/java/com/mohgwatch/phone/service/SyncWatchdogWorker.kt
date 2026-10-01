package com.mohgwatch.phone.service

import android.content.Context
import android.util.Log
import androidx.work.*
import com.mohgwatch.phone.data.SettingsStore
import java.util.concurrent.TimeUnit

class SyncWatchdogWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settingsStore = SettingsStore(applicationContext)
        val settings = settingsStore.getSettings()
        if (settings.isSyncEnabled) {
            val lastSync = GlucoseSyncState.lastSyncTimestamp.value
            val maxAllowedGapMs = (settings.pollIntervalMinutes + 5) * 60_000L
            if (System.currentTimeMillis() - lastSync > maxAllowedGapMs) {
                Log.w(TAG, "Watchdog wykrył przestój synchronizacji. Wznawianie usługi...")
                GlucoseSyncService.start(applicationContext)
            }
        }
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
