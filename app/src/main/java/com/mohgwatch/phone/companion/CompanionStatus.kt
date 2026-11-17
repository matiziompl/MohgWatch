package com.mohgwatch.phone.companion

import android.content.IntentSender

enum class CompanionPresence { UNKNOWN, PRESENT, ABSENT }

data class CompanionAssociation(val id: Int, val displayName: String?)
data class BondedCompanionDevice(val address: String, val name: String)

data class CompanionStatus(
    val ready: Boolean = false,
    val supported: Boolean = false,
    val unavailableReason: String? = null,
    val observationRequested: Boolean = false,
    val associations: List<CompanionAssociation> = emptyList(),
    val selectedAssociationId: Int? = null,
    val presence: CompanionPresence = CompanionPresence.UNKNOWN,
    val serviceRunning: Boolean = false,
    val lastEventAtMillis: Long? = null,
    val error: String? = null,
    val busy: Boolean = false,
    val batteryExempt: Boolean? = null,
    val backgroundDataStatus: String? = null,
    val exemptionChanged: Boolean = false
)

internal enum class CompanionTransportEvent { BLE_APPEARED, BLE_DISAPPEARED, BT_CONNECTED, BT_DISCONNECTED }
internal data class CompanionExemptions(val batteryExempt: Boolean?, val backgroundDataStatus: String?)
internal data class CompanionPreferencesSnapshot(
    val observationRequested: Boolean = false,
    val selectedAssociationId: Int? = null,
    val previousBatteryExempt: Boolean? = null,
    val previousBackgroundDataStatus: String? = null,
    val exemptionChanged: Boolean = false
)

internal interface CompanionPreferences {
    suspend fun load(): CompanionPreferencesSnapshot
    suspend fun save(value: CompanionPreferencesSnapshot)
}

internal interface CompanionAssociationCallback {
    fun pending(sender: IntentSender)
    fun created(id: Int)
    fun failed(message: String)
}

internal interface CompanionPlatform {
    val unavailableReason: String?
    fun associations(): List<CompanionAssociation>
    fun bondedDevices(): List<BondedCompanionDevice>
    fun associate(device: BondedCompanionDevice, callback: CompanionAssociationCallback)
    fun startObservation(id: Int)
    fun stopObservation(id: Int)
    fun removeAssociation(id: Int)
    fun exemptions(): CompanionExemptions
    fun record(event: String, details: String = "")
}
