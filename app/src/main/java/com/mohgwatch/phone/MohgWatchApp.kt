package com.mohgwatch.phone

import android.app.Application
import com.mohgwatch.phone.companion.CompanionIntegrationManager
import com.mohgwatch.phone.data.NightTestManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MohgWatchApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.mohgwatch.phone.service.SyncWatchdogWorker.schedule(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            CompanionIntegrationManager.get(this@MohgWatchApp).refresh()
            NightTestManager.get(this@MohgWatchApp)
        }
    }
}
