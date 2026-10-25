package com.mohgwatch.wear

import android.app.Application
import com.mohgwatch.wear.service.WristDetectionManager
import com.mohgwatch.wear.service.WatchStaleDataMonitor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MohgWatchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WristDetectionManager.start(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                WatchStaleDataMonitor.refresh(this@MohgWatchApplication)
            } catch (e: Exception) {
                Log.e("WatchStaleData", "Nie udało się odtworzyć kontroli świeżości", e)
            }
        }
    }
}
