package com.mohgwatch.phone

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MohgWatchApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.mohgwatch.phone.service.SyncWatchdogWorker.schedule(this)
    }
}
