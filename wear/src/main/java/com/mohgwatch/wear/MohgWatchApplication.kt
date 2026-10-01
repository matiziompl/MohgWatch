package com.mohgwatch.wear

import android.app.Application
import com.mohgwatch.wear.service.WristDetectionManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MohgWatchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WristDetectionManager.start(this)
    }
}
