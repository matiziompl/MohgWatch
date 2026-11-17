package com.mohgwatch.phone.companion

import android.companion.CompanionDeviceService
import android.companion.DevicePresenceEvent
import androidx.annotation.RequiresApi

@RequiresApi(36)
class CompanionPresenceService : CompanionDeviceService() {
    override fun onCreate() {
        super.onCreate()
        CompanionIntegrationManager.get(this).onServiceLifecycle(true)
    }

    override fun onDestroy() {
        CompanionIntegrationManager.get(this).onServiceLifecycle(false)
        super.onDestroy()
    }

    override fun onDevicePresenceEvent(event: DevicePresenceEvent) {
        val transport = when (event.event) {
            DevicePresenceEvent.EVENT_BLE_APPEARED -> CompanionTransportEvent.BLE_APPEARED
            DevicePresenceEvent.EVENT_BLE_DISAPPEARED -> CompanionTransportEvent.BLE_DISAPPEARED
            DevicePresenceEvent.EVENT_BT_CONNECTED -> CompanionTransportEvent.BT_CONNECTED
            DevicePresenceEvent.EVENT_BT_DISCONNECTED -> CompanionTransportEvent.BT_DISCONNECTED
            else -> return
        }
        CompanionIntegrationManager.get(this).onPresenceEvent(event.associationId, transport)
    }
}
