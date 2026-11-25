package com.mohgwatch.phone

import android.app.Application
import android.companion.CompanionDeviceManager
import android.os.Build
import com.mohgwatch.phone.data.Diagnostics
import com.mohgwatch.phone.data.NightTestManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class MohgWatchApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.mohgwatch.phone.service.SyncWatchdogWorker.schedule(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            removeCompanionExperiment()
            NightTestManager.get(this@MohgWatchApp)
        }
    }

    /** v1.9 dropped the CDM experiment; an association left by v1.7/v1.8 could still revoke the battery exemption. */
    private fun removeCompanionExperiment() {
        val prefs = getSharedPreferences("mohgwatch_migrations", MODE_PRIVATE)
        if (prefs.getBoolean("cdm_removed", false)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) runCatching {
            val manager = getSystemService(CompanionDeviceManager::class.java)
            manager?.myAssociations?.forEach { association ->
                runCatching { manager.disassociate(association.id) }
                    .onSuccess { Diagnostics.record(this, "CDM_ASSOCIATION_REMOVED", "id=${association.id}") }
            }
        }
        File(noBackupFilesDir, "mohgwatch_companion.preferences_pb").delete()
        prefs.edit().putBoolean("cdm_removed", true).apply()
    }
}
