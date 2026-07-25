package com.mohgwatch.phone.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mohgwatch.phone.data.CredentialStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Restartuje GlucoseSyncService po rebocie telefonu.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val store = CredentialStore(context)
                val creds = store.getCredentials()
                if (creds != null) {
                    GlucoseSyncService.start(context)
                }
            }
        }
    }
}
