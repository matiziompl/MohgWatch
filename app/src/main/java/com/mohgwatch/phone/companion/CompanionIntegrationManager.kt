package com.mohgwatch.phone.companion

import android.content.Context
import android.content.IntentSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

class CompanionIntegrationManager internal constructor(
    private val platform: CompanionPlatform,
    private val preferences: CompanionPreferences,
    private val requestSyncOpportunity: suspend (isStillAllowed: () -> Boolean) -> Unit,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val mutableState = MutableStateFlow(CompanionStatus())
    val state: StateFlow<CompanionStatus> = mutableState
    private val mutex = Mutex()
    private val associationGeneration = AtomicLong()
    private var saved: CompanionPreferencesSnapshot? = null
    private var observedId: Int? = null
    private var bluetoothConnected: Boolean? = null
    private var bleNearby: Boolean? = null

    suspend fun refresh() = action { refreshLocked() }

    suspend fun bondedDevices(): List<BondedCompanionDevice> = scope.async {
        mutex.withLock {
            try {
                refreshLocked()
                if (state.value.supported) platform.bondedDevices() else emptyList()
            } catch (failure: Exception) {
                reportFailure(failure)
                emptyList()
            }
        }
    }.await()

    fun associate(device: BondedCompanionDevice, onPending: (IntentSender) -> Unit) {
        val generation = associationGeneration.incrementAndGet()
        scope.launch {
            action {
                if (generation != associationGeneration.get()) return@action
                refreshLocked()
                if (!state.value.supported) return@action
                captureExemptions("before_associate")
                mutableState.value = state.value.copy(busy = true, error = null)
                platform.record("CDM_ASSOCIATION_REQUESTED")
                platform.associate(device, object : CompanionAssociationCallback {
                    override fun pending(sender: IntentSender) {
                        scope.launch {
                            action {
                                if (generation == associationGeneration.get() && state.value.busy) onPending(sender)
                            }
                        }
                    }

                    override fun created(id: Int) {
                        scope.launch {
                            action {
                                val currentRequest = generation == associationGeneration.get() && state.value.busy
                                if (currentRequest) mutableState.value = state.value.copy(busy = false)
                                refreshLocked(restoreObservation = false)
                                if (currentRequest && state.value.associations.any { it.id == id }) selectLocked(id)
                                captureExemptions("after_associate")
                                platform.record("CDM_ASSOCIATION_CREATED", "id=$id accepted=$currentRequest")
                            }
                        }
                    }

                    override fun failed(message: String) {
                        scope.launch {
                            action {
                                if (generation != associationGeneration.get()) return@action
                                mutableState.value = state.value.copy(busy = false, error = message.ifBlank { null })
                                captureExemptions("after_associate_failure")
                                platform.record("CDM_ASSOCIATION_FINISHED", "cancelled=${message.isBlank()}")
                            }
                        }
                    }
                })
            }
        }
    }

    fun associationUiResult(cancelled: Boolean) {
        if (!cancelled) return
        associationGeneration.incrementAndGet()
        scope.launch {
            action {
                mutableState.value = state.value.copy(busy = false, error = null)
                platform.record("CDM_ASSOCIATION_CANCELLED")
                refreshLocked(restoreObservation = false)
            }
        }
    }

    suspend fun selectAssociation(id: Int) {
        associationGeneration.incrementAndGet()
        action {
            refreshLocked()
            selectLocked(id)
        }
    }

    private suspend fun selectLocked(id: Int) {
        if (state.value.associations.none { it.id == id }) {
            mutableState.value = state.value.copy(error = "Powiązanie nie jest już dostępne dla tej aplikacji.")
            return
        }
        if (state.value.selectedAssociationId == id) return
        stopLocked()
        mutableState.value = state.value.copy(selectedAssociationId = id, error = null)
        persist()
        platform.record("CDM_ASSOCIATION_SELECTED", "id=$id")
    }

    suspend fun startObservation() {
        val generation = associationGeneration.get()
        action {
            refreshLocked()
            if (!state.value.supported || generation != associationGeneration.get()) return@action
            val id = state.value.selectedAssociationId
            if (id == null || state.value.associations.none { it.id == id }) {
                mutableState.value = state.value.copy(error = "Najpierw wybierz powiązanie zegarka.")
                return@action
            }
            if (observedId == id && state.value.observationRequested) return@action
            captureExemptions("before_start")
            if (generation != associationGeneration.get()) return@action
            platform.startObservation(id)
            observedId = id
            resetPresence()
            mutableState.value = state.value.copy(observationRequested = true, error = null)
            persist()
            captureExemptions("after_start")
            platform.record("CDM_OBSERVATION_REQUESTED", "id=$id")
        }
    }

    suspend fun stopObservation() {
        associationGeneration.incrementAndGet()
        action {
            loadPreferences()
            stopLocked()
        }
    }

    private suspend fun stopLocked() {
        val id = observedId ?: state.value.selectedAssociationId.takeIf { state.value.observationRequested }
        observedId = null
        resetPresence()
        mutableState.value = state.value.copy(observationRequested = false, busy = false, error = null)
        try {
            captureExemptions("before_stop")
            persist()
        } finally {
            try {
                if (id != null && platform.unavailableReason == null) platform.stopObservation(id)
                platform.record("CDM_OBSERVATION_STOPPED", "id=${id ?: "none"}; associationRetained=true")
            } finally {
                captureExemptions("after_stop")
            }
        }
    }

    suspend fun removeAssociation(id: Int) {
        associationGeneration.incrementAndGet()
        action {
            refreshLocked()
            if (!state.value.supported || state.value.associations.none { it.id == id }) return@action
            captureExemptions("before_remove")
            if (state.value.selectedAssociationId == id) stopLocked()
            platform.removeAssociation(id)
            platform.record("CDM_ASSOCIATION_REMOVED", "id=$id")
            refreshLocked()
            captureExemptions("after_remove")
        }
    }

    internal fun onPresenceEvent(id: Int, event: CompanionTransportEvent) {
        val generation = associationGeneration.get()
        scope.launch {
            action {
                platform.record("CDM_PRESENCE_CALLBACK", "id=$id event=$event requested=${state.value.observationRequested}")
                mutableState.value = state.value.copy(lastEventAtMillis = now())
                refreshLocked(restoreObservation = false)
                if (!state.value.observationRequested || observedId != id || state.value.selectedAssociationId != id) return@action
                val before = state.value.presence
                when (event) {
                    CompanionTransportEvent.BT_CONNECTED -> bluetoothConnected = true
                    CompanionTransportEvent.BT_DISCONNECTED -> bluetoothConnected = false
                    CompanionTransportEvent.BLE_APPEARED -> bleNearby = true
                    CompanionTransportEvent.BLE_DISAPPEARED -> bleNearby = false
                }
                val presence = when {
                    bluetoothConnected == true || bleNearby == true -> CompanionPresence.PRESENT
                    bluetoothConnected == false && bleNearby == false -> CompanionPresence.ABSENT
                    else -> CompanionPresence.UNKNOWN
                }
                mutableState.value = state.value.copy(presence = presence)
                if (presence == CompanionPresence.PRESENT && before != CompanionPresence.PRESENT) {
                    requestSyncOpportunity {
                        generation == associationGeneration.get() && state.value.observationRequested && observedId == id
                    }
                }
            }
        }
    }

    internal fun onServiceLifecycle(running: Boolean) {
        scope.launch {
            action {
                mutableState.value = state.value.copy(serviceRunning = running)
                platform.record(if (running) "CDM_SERVICE_CREATED" else "CDM_SERVICE_DESTROYED")
            }
        }
    }

    private suspend fun refreshLocked(restoreObservation: Boolean = true) {
        val generation = associationGeneration.get()
        loadPreferences()
        val reason = platform.unavailableReason
        if (reason != null) {
            observedId = null
            resetPresence()
            mutableState.value = state.value.copy(ready = true, supported = false, unavailableReason = reason,
                observationRequested = false, associations = emptyList(), selectedAssociationId = null)
            captureExemptions("refresh")
            return
        }
        val associations = platform.associations()
        val selected = state.value.selectedAssociationId?.takeIf { selectedId -> associations.any { it.id == selectedId } }
        val requested = state.value.observationRequested && selected != null
        if (selected != state.value.selectedAssociationId || !requested) {
            observedId = null
            resetPresence()
        }
        mutableState.value = state.value.copy(ready = true, supported = true, unavailableReason = null,
            associations = associations, selectedAssociationId = selected, observationRequested = requested)
        persist()
        captureExemptions("refresh")
        if (restoreObservation && requested && observedId != selected && generation == associationGeneration.get()) {
            platform.startObservation(selected!!)
            observedId = selected
            platform.record("CDM_OBSERVATION_RESTORED", "id=$selected")
        }
    }

    private suspend fun loadPreferences() {
        if (saved != null) return
        val prefs = preferences.load()
        saved = prefs
        mutableState.value = state.value.copy(observationRequested = prefs.observationRequested,
            selectedAssociationId = prefs.selectedAssociationId, exemptionChanged = prefs.exemptionChanged,
            batteryExempt = prefs.previousBatteryExempt, backgroundDataStatus = prefs.previousBackgroundDataStatus)
    }

    private suspend fun captureExemptions(stage: String) {
        val current = platform.exemptions()
        val previous = saved ?: CompanionPreferencesSnapshot()
        val lost = (previous.previousBatteryExempt == true && current.batteryExempt == false) ||
            (previous.previousBackgroundDataStatus == "WHITELISTED" && current.backgroundDataStatus in setOf("ENABLED", "DISABLED"))
        mutableState.value = state.value.copy(batteryExempt = current.batteryExempt,
            backgroundDataStatus = current.backgroundDataStatus,
            exemptionChanged = state.value.exemptionChanged || lost)
        persist()
        if (stage != "refresh" || lost) platform.record("CDM_EXEMPTIONS", "stage=$stage battery=${current.batteryExempt} data=${current.backgroundDataStatus} dropped=$lost")
    }

    private suspend fun persist() {
        val value = CompanionPreferencesSnapshot(state.value.observationRequested, state.value.selectedAssociationId,
            state.value.batteryExempt, state.value.backgroundDataStatus, state.value.exemptionChanged)
        if (value != saved) {
            preferences.save(value)
            saved = value
        }
    }

    private fun resetPresence() {
        bluetoothConnected = null
        bleNearby = null
        mutableState.value = state.value.copy(presence = CompanionPresence.UNKNOWN)
    }

    // Leaving the settings screen must not abandon a system operation after its local save.
    private suspend fun action(block: suspend () -> Unit) = scope.async {
        mutex.withLock {
            try { block() } catch (failure: Exception) { reportFailure(failure) }
        }
    }.await()

    private suspend fun reportFailure(failure: Exception) {
        if (failure is CancellationException) throw failure
        mutableState.value = state.value.copy(ready = true, busy = false, error = when (failure) {
            is SecurityException -> "Brak zgody systemu. Sprawdź uprawnienie Bluetooth i spróbuj ponownie."
            else -> "Operacja CDM nie powiodła się. Odśwież stan i spróbuj ponownie."
        })
        if (failure is SecurityException) {
            associationGeneration.incrementAndGet()
            val id = observedId ?: state.value.selectedAssociationId.takeIf { state.value.observationRequested }
            observedId = null
            resetPresence()
            mutableState.value = state.value.copy(observationRequested = false)
            try {
                persist()
            } finally {
                try {
                    if (id != null && platform.unavailableReason == null) platform.stopObservation(id)
                } catch (stopFailure: Exception) {
                    if (stopFailure is CancellationException) throw stopFailure
                    platform.record("CDM_OBSERVATION_STOP_REJECTED", "type=${stopFailure.javaClass.simpleName}")
                }
            }
        }
        platform.record("CDM_OPERATION_FAILED", "type=${failure.javaClass.simpleName}")
    }

    companion object {
        @Volatile private var instance: CompanionIntegrationManager? = null

        fun get(context: Context): CompanionIntegrationManager = instance ?: synchronized(this) {
            instance ?: run {
                val app = context.applicationContext
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
                CompanionIntegrationManager(AndroidCompanionPlatform(app), CompanionPreferenceStore(app),
                    { isStillAllowed -> requestCompanionSyncOpportunity(app, isStillAllowed) }, scope).also {
                    instance = it
                    scope.launch { it.refresh() }
                }
            }
        }
    }
}

internal object CompanionSyncPolicy {
    fun shouldResume(syncEnabled: Boolean, hasCredentials: Boolean, due: Boolean, syncing: Boolean): Boolean =
        syncEnabled && hasCredentials && (due || !syncing)
}
