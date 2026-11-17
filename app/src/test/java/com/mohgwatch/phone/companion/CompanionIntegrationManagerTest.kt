package com.mohgwatch.phone.companion

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CompanionIntegrationManagerTest {
    @Test fun unsupportedDeviceDoesNotAttemptAssociationOrObservation() = runTest {
        val platform = FakePlatform().apply { reason = "Android 16 wymagany" }
        val manager = CompanionIntegrationManager(platform, MemoryPreferences(), {}, backgroundScope) { 123L }
        manager.refresh()
        manager.startObservation()
        manager.associate(BondedCompanionDevice("AA:BB:CC:DD:EE:FF", "Watch")) {}
        runCurrent()
        assertTrue(manager.state.value.ready)
        assertFalse(manager.state.value.supported)
        assertTrue(platform.started.isEmpty())
        assertNull(platform.callback)
    }

    @Test fun multipleAssociationsRequireExplicitSelection() = runTest {
        val platform = FakePlatform().apply { own = listOf(association(1), association(2)) }
        val manager = CompanionIntegrationManager(platform, MemoryPreferences(), {}, backgroundScope) { 123L }
        manager.refresh()
        manager.startObservation()
        assertNull(manager.state.value.selectedAssociationId)
        assertTrue(platform.started.isEmpty())
        manager.selectAssociation(2)
        manager.startObservation()
        assertEquals(listOf(2), platform.started)
    }

    @Test fun restartRestoresOnlyAStillOwnedSelectedAssociationAndStartsUnknown() = runTest {
        val platform = FakePlatform().apply { own = listOf(association(4)) }
        val prefs = MemoryPreferences(CompanionPreferencesSnapshot(observationRequested = true, selectedAssociationId = 4))
        val manager = CompanionIntegrationManager(platform, prefs, {}, backgroundScope) { 123L }
        manager.refresh()
        manager.refresh()
        assertEquals(listOf(4), platform.started)
        assertTrue(manager.state.value.observationRequested)
        assertEquals(CompanionPresence.UNKNOWN, manager.state.value.presence)
    }

    @Test fun removedSavedAssociationCannotRestoreObservationOrChooseAnother() = runTest {
        val platform = FakePlatform().apply { own = listOf(association(5)) }
        val prefs = MemoryPreferences(CompanionPreferencesSnapshot(observationRequested = true, selectedAssociationId = 4))
        val manager = CompanionIntegrationManager(platform, prefs, {}, backgroundScope) { 123L }
        manager.refresh()
        assertFalse(manager.state.value.observationRequested)
        assertNull(manager.state.value.selectedAssociationId)
        assertTrue(platform.started.isEmpty())
        assertFalse(prefs.value.observationRequested)
    }

    @Test fun bothNegativeTransportsAreRequiredForAbsenceAndRepeatedPresenceDoesNotWakeSync() = runTest {
        val platform = FakePlatform().apply { own = listOf(association(1)) }
        var wakes = 0
        val manager = CompanionIntegrationManager(platform, MemoryPreferences(), { wakes++ }, backgroundScope) { 123L }
        manager.refresh()
        manager.selectAssociation(1)
        manager.startObservation()
        manager.onPresenceEvent(1, CompanionTransportEvent.BT_DISCONNECTED)
        runCurrent()
        assertEquals(CompanionPresence.UNKNOWN, manager.state.value.presence)
        manager.onPresenceEvent(1, CompanionTransportEvent.BLE_DISAPPEARED)
        runCurrent()
        assertEquals(CompanionPresence.ABSENT, manager.state.value.presence)
        assertEquals(0, wakes)
        manager.onPresenceEvent(1, CompanionTransportEvent.BT_CONNECTED)
        manager.onPresenceEvent(1, CompanionTransportEvent.BLE_APPEARED)
        runCurrent()
        assertEquals(CompanionPresence.PRESENT, manager.state.value.presence)
        assertEquals(1, wakes)
        assertEquals(123L, manager.state.value.lastEventAtMillis)
    }

    @Test fun stopMakesLatePresenceAndAssociationCallbacksUnableToRestartObservationOrSync() = runTest {
        val platform = FakePlatform().apply { own = listOf(association(1)) }
        var wakes = 0
        val manager = CompanionIntegrationManager(platform, MemoryPreferences(), { wakes++ }, backgroundScope) { 123L }
        manager.refresh()
        manager.selectAssociation(1)
        manager.startObservation()
        manager.associate(BondedCompanionDevice("AA:BB:CC:DD:EE:FF", "Watch")) {}
        runCurrent()
        val late = platform.callback!!
        manager.stopObservation()
        late.created(1)
        manager.onPresenceEvent(1, CompanionTransportEvent.BT_CONNECTED)
        runCurrent()
        assertFalse(manager.state.value.observationRequested)
        assertEquals(CompanionPresence.UNKNOWN, manager.state.value.presence)
        assertEquals(listOf(1), platform.started)
        assertEquals(listOf(1), platform.stopped)
        assertEquals(0, wakes)
    }

    @Test fun cancelledAssociationIsNotReportedAsAnIncompatibleDevice() = runTest {
        val platform = FakePlatform()
        val manager = CompanionIntegrationManager(platform, MemoryPreferences(), {}, backgroundScope) { 123L }
        manager.refresh()
        manager.associate(BondedCompanionDevice("AA:BB:CC:DD:EE:FF", "Watch")) {}
        runCurrent()
        val late = platform.callback!!
        manager.associationUiResult(cancelled = true)
        late.failed("Nie udało się utworzyć powiązania")
        runCurrent()
        assertTrue(manager.state.value.supported)
        assertFalse(manager.state.value.busy)
        assertNull(manager.state.value.error)
        assertTrue(platform.started.isEmpty())
    }

    @Test fun removalDisablesObservationAndReportsLostExemptionWithoutRemovingBluetoothBond() = runTest {
        val platform = FakePlatform().apply { own = listOf(association(1)) }
        val manager = CompanionIntegrationManager(platform, MemoryPreferences(), {}, backgroundScope) { 123L }
        manager.refresh()
        manager.selectAssociation(1)
        manager.startObservation()
        platform.afterRemoval = CompanionExemptions(false, "ENABLED")
        manager.removeAssociation(1)
        assertEquals(listOf(1), platform.removed)
        assertFalse(manager.state.value.observationRequested)
        assertNull(manager.state.value.selectedAssociationId)
        assertTrue(manager.state.value.exemptionChanged)
        assertEquals(false, manager.state.value.batteryExempt)
    }

    @Test fun unrelatedCallbacksNeverChangeSelectedPresence() = runTest {
        val platform = FakePlatform().apply { own = listOf(association(1), association(2)) }
        var wakes = 0
        val manager = CompanionIntegrationManager(platform, MemoryPreferences(), { wakes++ }, backgroundScope) { 123L }
        manager.refresh()
        manager.selectAssociation(1)
        manager.startObservation()
        manager.onPresenceEvent(2, CompanionTransportEvent.BT_CONNECTED)
        runCurrent()
        assertEquals(CompanionPresence.UNKNOWN, manager.state.value.presence)
        assertEquals(0, wakes)
    }

    @Test fun createdAssociationIsSelectedOnlyAfterTheSystemListConfirmsOwnership() = runTest {
        val platform = FakePlatform()
        val manager = CompanionIntegrationManager(platform, MemoryPreferences(), {}, backgroundScope) { 123L }
        manager.refresh()
        manager.associate(BondedCompanionDevice("AA:BB:CC:DD:EE:FF", "Watch")) {}
        runCurrent()
        platform.callback!!.created(17)
        runCurrent()
        assertNull(manager.state.value.selectedAssociationId)
        manager.associate(BondedCompanionDevice("AA:BB:CC:DD:EE:FF", "Watch")) {}
        runCurrent()
        platform.own = listOf(association(17))
        platform.callback!!.created(17)
        runCurrent()
        assertEquals(17, manager.state.value.selectedAssociationId)
        assertFalse(manager.state.value.observationRequested)
        assertTrue(platform.started.isEmpty())
    }

    @Test fun switchingAssociationRequiresNewExplicitObservationRequest() = runTest {
        val platform = FakePlatform().apply { own = listOf(association(1), association(2)) }
        val manager = CompanionIntegrationManager(platform, MemoryPreferences(), {}, backgroundScope) { 123L }
        manager.refresh()
        manager.selectAssociation(1)
        manager.startObservation()
        manager.selectAssociation(2)
        assertEquals(listOf(1), platform.stopped)
        assertEquals(listOf(1), platform.started)
        assertEquals(2, manager.state.value.selectedAssociationId)
        assertFalse(manager.state.value.observationRequested)
    }

    @Test fun exemptionLostWhileProcessWasGoneIsStillReportedAfterRestart() = runTest {
        val platform = FakePlatform().apply { currentExemptions = CompanionExemptions(false, "ENABLED") }
        val prefs = MemoryPreferences(CompanionPreferencesSnapshot(previousBatteryExempt = true,
            previousBackgroundDataStatus = "WHITELISTED"))
        val manager = CompanionIntegrationManager(platform, prefs, {}, backgroundScope) { 123L }
        manager.refresh()
        assertTrue(manager.state.value.exemptionChanged)
        assertTrue(prefs.value.exemptionChanged)
    }

    @Test fun removingAnAssociationNotOwnedByTheAppDoesNothing() = runTest {
        val platform = FakePlatform().apply { own = listOf(association(1)) }
        val manager = CompanionIntegrationManager(platform, MemoryPreferences(), {}, backgroundScope) { 123L }
        manager.refresh()
        manager.removeAssociation(99)
        assertTrue(platform.removed.isEmpty())
        assertEquals(listOf(association(1)), manager.state.value.associations)
    }

    @Test fun stoppingWhileCredentialsAreBeingReadPreventsThePendingSyncOpportunity() = runTest {
        val platform = FakePlatform().apply { own = listOf(association(1)) }
        val credentialRead = CompletableDeferred<Unit>()
        var wakes = 0
        val manager = CompanionIntegrationManager(platform, MemoryPreferences(), { allowed ->
            credentialRead.await()
            if (allowed()) wakes++
        }, backgroundScope) { 123L }
        manager.refresh()
        manager.selectAssociation(1)
        manager.startObservation()
        manager.onPresenceEvent(1, CompanionTransportEvent.BT_CONNECTED)
        runCurrent()
        val stop = launch { manager.stopObservation() }
        runCurrent()
        credentialRead.complete(Unit)
        runCurrent()
        stop.join()
        assertEquals(0, wakes)
        assertFalse(manager.state.value.observationRequested)
    }

    @Test fun bluetoothDevicePermissionRevocationDisablesObservationAndIgnoresLatePresence() = runTest {
        val platform = FakePlatform().apply { own = listOf(association(1)) }
        val prefs = MemoryPreferences()
        var wakes = 0
        val manager = CompanionIntegrationManager(platform, prefs, { wakes++ }, backgroundScope) { 123L }
        manager.refresh()
        manager.selectAssociation(1)
        manager.startObservation()
        platform.denyBondedDevices = true

        assertTrue(manager.bondedDevices().isEmpty())

        assertFalse(manager.state.value.observationRequested)
        assertFalse(prefs.value.observationRequested)
        assertFalse(manager.state.value.error.isNullOrBlank())
        platform.denyBondedDevices = false
        manager.refresh()
        manager.onPresenceEvent(1, CompanionTransportEvent.BT_CONNECTED)
        runCurrent()
        assertEquals(0, wakes)
        assertEquals(listOf(1), platform.started)
        assertEquals(CompanionPresence.UNKNOWN, manager.state.value.presence)
        assertNull(platform.callback)
    }

    @Test fun deniedRestorationDoesNotKeepAnObservationRequestForAnAutomaticRetry() = runTest {
        val platform = FakePlatform().apply {
            own = listOf(association(1))
            denyStartObservation = true
        }
        val prefs = MemoryPreferences(CompanionPreferencesSnapshot(observationRequested = true, selectedAssociationId = 1))
        var wakes = 0
        val manager = CompanionIntegrationManager(platform, prefs, { wakes++ }, backgroundScope) { 123L }

        manager.refresh()

        assertFalse(manager.state.value.observationRequested)
        assertFalse(prefs.value.observationRequested)
        assertFalse(manager.state.value.error.isNullOrBlank())
        platform.denyStartObservation = false
        manager.refresh()
        manager.onPresenceEvent(1, CompanionTransportEvent.BT_CONNECTED)
        runCurrent()
        assertTrue(platform.started.isEmpty())
        assertEquals(0, wakes)
        assertNull(platform.callback)
    }

    @Test fun associationPermissionRevocationInvalidatesThePersistedObservationRequest() = runTest {
        val platform = FakePlatform().apply { own = listOf(association(1)) }
        val prefs = MemoryPreferences()
        var wakes = 0
        val manager = CompanionIntegrationManager(platform, prefs, { wakes++ }, backgroundScope) { 123L }
        manager.refresh()
        manager.selectAssociation(1)
        manager.startObservation()
        platform.denyAssociations = true

        manager.refresh()

        assertFalse(manager.state.value.observationRequested)
        assertFalse(prefs.value.observationRequested)
        assertFalse(manager.state.value.error.isNullOrBlank())
        platform.denyAssociations = false
        manager.refresh()
        manager.onPresenceEvent(1, CompanionTransportEvent.BT_CONNECTED)
        runCurrent()
        assertEquals(listOf(1), platform.started)
        assertEquals(0, wakes)
        assertEquals(CompanionPresence.UNKNOWN, manager.state.value.presence)
        assertNull(platform.callback)
    }

    @Test fun uiCallerCancellationDuringSaveDoesNotAbandonStoppingSystemObservation() = runTest {
        val platform = FakePlatform().apply { own = listOf(association(1)) }
        val preferences = MemoryPreferences()
        val applicationScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        try {
            val manager = CompanionIntegrationManager(platform, preferences, {}, applicationScope) { 123L }
            manager.refresh()
            manager.selectAssociation(1)
            manager.startObservation()
            assertTrue(manager.state.value.observationRequested)
            assertTrue(preferences.value.observationRequested)
            assertEquals(listOf(1), platform.started)
            val saveEntered = CompletableDeferred<Unit>()
            val releaseSave = CompletableDeferred<Unit>()
            preferences.beforeSave = { value ->
                if (!value.observationRequested) {
                    saveEntered.complete(Unit)
                    releaseSave.await()
                }
            }
            val uiCaller = launch { manager.stopObservation() }
            runCurrent()
            assertTrue("Stop must reach the persisted OFF state", saveEntered.isCompleted)
            saveEntered.await()

            uiCaller.cancel()
            releaseSave.complete(Unit)
            advanceUntilIdle()

            assertTrue(uiCaller.isCancelled)
            assertEquals(listOf(1), platform.stopped)
            assertFalse(preferences.value.observationRequested)
            assertFalse(manager.state.value.observationRequested)
        } finally {
            applicationScope.cancel()
        }
    }

    @Test fun failedStopPersistenceStillStopsSystemObservationAndReportsTheError() = runTest {
        val platform = FakePlatform().apply { own = listOf(association(1)) }
        val preferences = MemoryPreferences()
        val manager = CompanionIntegrationManager(platform, preferences, {}, backgroundScope) { 123L }
        manager.refresh()
        manager.selectAssociation(1)
        manager.startObservation()
        assertTrue(manager.state.value.observationRequested)
        preferences.beforeSave = { value ->
            if (!value.observationRequested) throw IOException("Preferences storage unavailable")
        }

        manager.stopObservation()

        assertEquals(listOf(1), platform.stopped)
        assertFalse(manager.state.value.observationRequested)
        assertNotNull(manager.state.value.error)
    }

    private fun association(id: Int) = CompanionAssociation(id, "Watch $id")

    private class MemoryPreferences(var value: CompanionPreferencesSnapshot = CompanionPreferencesSnapshot()) : CompanionPreferences {
        var beforeSave: suspend (CompanionPreferencesSnapshot) -> Unit = {}
        override suspend fun load() = value
        override suspend fun save(value: CompanionPreferencesSnapshot) {
            beforeSave(value)
            this.value = value
        }
    }

    private class FakePlatform : CompanionPlatform {
        var reason: String? = null
        override val unavailableReason get() = reason
        var own = emptyList<CompanionAssociation>()
        var callback: CompanionAssociationCallback? = null
        val started = mutableListOf<Int>()
        val stopped = mutableListOf<Int>()
        val removed = mutableListOf<Int>()
        var currentExemptions = CompanionExemptions(true, "WHITELISTED")
        var afterRemoval = currentExemptions
        var denyAssociations = false
        var denyBondedDevices = false
        var denyStartObservation = false
        override fun associations(): List<CompanionAssociation> {
            if (denyAssociations) throw SecurityException("Companion access revoked")
            return own
        }
        override fun bondedDevices(): List<BondedCompanionDevice> {
            if (denyBondedDevices) throw SecurityException("Bluetooth access revoked")
            return emptyList()
        }
        override fun associate(device: BondedCompanionDevice, callback: CompanionAssociationCallback) { this.callback = callback }
        override fun startObservation(id: Int) {
            if (denyStartObservation) throw SecurityException("Observation access denied")
            started += id
        }
        override fun stopObservation(id: Int) { stopped += id }
        override fun removeAssociation(id: Int) {
            removed += id
            own = own.filterNot { it.id == id }
            currentExemptions = afterRemoval
        }
        override fun exemptions() = currentExemptions
        override fun record(event: String, details: String) = Unit
    }
}
