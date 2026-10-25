package com.mohgwatch.phone

import android.app.Application

class MohgWatchApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.mohgwatch.phone.service.SyncWatchdogWorker.schedule(this)
    }
}
