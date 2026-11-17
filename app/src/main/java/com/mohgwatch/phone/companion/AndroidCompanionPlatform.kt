package com.mohgwatch.phone.companion

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.companion.ObservingDevicePresenceRequest
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.PowerManager
import com.mohgwatch.phone.data.CredentialStore
import com.mohgwatch.phone.data.Diagnostics
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.service.GlucoseSyncState
import com.mohgwatch.phone.service.SyncScheduler

@SuppressLint("NewApi", "MissingPermission")
internal class AndroidCompanionPlatform(private val context: Context) : CompanionPlatform {
    override val unavailableReason: String?
        get() = when {
            Build.VERSION.SDK_INT < 36 -> "Ta opcja wymaga Androida 16 lub nowszego."
            !context.packageManager.hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP) ->
                "Telefon nie udostępnia konfiguracji urządzeń towarzyszących."
            context.getSystemService(BluetoothManager::class.java)?.adapter == null ->
                "Telefon nie udostępnia adaptera Bluetooth."
            context.getSystemService(CompanionDeviceManager::class.java) == null ->
                "System nie udostępnia usługi urządzeń towarzyszących."
            else -> null
        }

    private fun manager(): CompanionDeviceManager {
        check(unavailableReason == null)
        return requireNotNull(context.getSystemService(CompanionDeviceManager::class.java))
    }

    private fun ownBluetoothAssociations(): List<AssociationInfo> = manager().myAssociations
        .filter { !it.isSelfManaged && it.deviceMacAddress != null }

    override fun associations(): List<CompanionAssociation> = ownBluetoothAssociations()
        .map { CompanionAssociation(it.id, companionDisplayName(it.displayName?.toString())) }

    override fun bondedDevices(): List<BondedCompanionDevice> {
        check(unavailableReason == null)
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException()
        }
        val adapter = requireNotNull(context.getSystemService(BluetoothManager::class.java)?.adapter)
        check(adapter.isEnabled)
        return adapter.bondedDevices.map { device ->
            BondedCompanionDevice(device.address, companionDisplayName(device.name) ?: "Urządzenie Bluetooth")
        }.sortedBy { it.name.lowercase() }
    }

    override fun associate(device: BondedCompanionDevice, callback: CompanionAssociationCallback) {
        // Only an existing system bond can be chosen; CDM never creates or removes a Bluetooth bond here.
        require(bondedDevices().any { it.address.equals(device.address, ignoreCase = true) })
        val filter = BluetoothDeviceFilter.Builder().setAddress(device.address).build()
        val request = AssociationRequest.Builder().addDeviceFilter(filter).setSingleDevice(true).build()
        manager().associate(request, context.mainExecutor, object : CompanionDeviceManager.Callback() {
            override fun onAssociationPending(intentSender: IntentSender) = callback.pending(intentSender)
            override fun onAssociationCreated(associationInfo: AssociationInfo) = callback.created(associationInfo.id)
            override fun onFailure(error: CharSequence?) = callback.failed("System nie utworzył powiązania. Możesz spróbować ponownie.")
            override fun onFailure(errorCode: Int, error: CharSequence?) {
                callback.failed(if (errorCode == CompanionDeviceManager.RESULT_CANCELED ||
                    errorCode == CompanionDeviceManager.RESULT_USER_REJECTED) ""
                else "System nie utworzył powiązania. Możesz spróbować ponownie.")
            }
        })
    }

    private fun observationRequest(id: Int): ObservingDevicePresenceRequest {
        require(ownBluetoothAssociations().any { it.id == id })
        return ObservingDevicePresenceRequest.Builder().setAssociationId(id).build()
    }

    override fun startObservation(id: Int) = manager().startObservingDevicePresence(observationRequest(id))
    override fun stopObservation(id: Int) = manager().stopObservingDevicePresence(observationRequest(id))
    override fun removeAssociation(id: Int) {
        require(ownBluetoothAssociations().any { it.id == id })
        manager().disassociate(id)
    }

    override fun exemptions() = CompanionExemptions(
        batteryExempt = runCatching {
            context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName)
        }.getOrNull(),
        backgroundDataStatus = runCatching {
            when (context.getSystemService(ConnectivityManager::class.java)?.restrictBackgroundStatus) {
                ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED -> "DISABLED"
                ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED -> "WHITELISTED"
                ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED -> "ENABLED"
                else -> "UNKNOWN"
            }
        }.getOrNull()
    )

    override fun record(event: String, details: String) = Diagnostics.record(context, event, details)
}

private val bluetoothAddressPattern = Regex("(?i)(?:[0-9a-f]{2}[:-]){5}[0-9a-f]{2}")

internal fun companionDisplayName(name: String?): String? = name
    ?.replace(bluetoothAddressPattern, "…")?.trim()?.take(100)?.takeIf { it.isNotBlank() && it != "…" }

internal suspend fun requestCompanionSyncOpportunity(context: Context, isStillAllowed: () -> Boolean) {
    val settings = SettingsStore(context).getSettings()
    if (!settings.isSyncEnabled) return
    val hasCredentials = CredentialStore(context).hasCredentials()
    val due = System.currentTimeMillis() - GlucoseSyncState.lastSyncTimestamp.value >=
        settings.pollIntervalMinutes.coerceIn(1, 5) * 60_000L
    if (isStillAllowed() && CompanionSyncPolicy.shouldResume(settings.isSyncEnabled, hasCredentials, due, GlucoseSyncState.isSyncing.value)) {
        SyncScheduler.resumeService(context, trigger = "cdm_presence")
    }
}
