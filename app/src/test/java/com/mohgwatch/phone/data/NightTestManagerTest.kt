package com.mohgwatch.phone.data

import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.phone.companion.CompanionAssociation
import com.mohgwatch.phone.companion.CompanionPresence
import com.mohgwatch.phone.companion.CompanionStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NightTestManagerTest {
    @Test fun testCannotStartWithoutMonitoringOrAKnownBoot() = runTest {
        val environment = FakeEnvironment()
        val manager = NightTestManager(environment, MemoryPreferences(), backgroundScope)
        environment.currentConfiguration = environment.currentConfiguration.copy(syncEnabled = false)
        manager.start()
        assertNull(manager.state.value.activeRun)
        assertNotNull(manager.state.value.error)
        environment.currentConfiguration = environment.currentConfiguration.copy(syncEnabled = true)
        environment.currentClocks = environment.currentClocks.copy(bootCount = null)
        manager.start()
        assertNull(manager.state.value.activeRun)
        assertNotNull(manager.state.value.error)
        assertFalse(environment.events.any { it.name == "NIGHT_TEST_START" })
    }

    @Test fun startAndStopAreIdempotentAndPersistBeforeTaggingDiagnostics() = runTest {
        val environment = FakeEnvironment()
        val preferences = MemoryPreferences()
        environment.onSetActive = { id -> if (id != null) assertEquals(id, preferences.value.activeRun?.id) }
        val manager = NightTestManager(environment, preferences, backgroundScope)
        manager.start()
        manager.start()
        assertNotNull(manager.state.value.activeRun)
        val run = manager.state.value.activeRun!!
        assertEquals(run.id, environment.currentRunId)
        assertEquals(1, environment.events.count { it.name == "NIGHT_TEST_START" })
        environment.currentClocks = environment.currentClocks.copy(wallMillis = 30_000L, elapsedMillis = 22_000L)
        manager.stop()
        manager.stop()
        assertNull(manager.state.value.activeRun)
        assertNull(environment.currentRunId)
        assertEquals(1, environment.events.count { it.name == "NIGHT_TEST_END" })
        assertTrue(manager.state.value.lastReport!!.contains("COMPLETED"))
        assertTrue(manager.state.value.lastReport!!.contains("Czas pomiaru: 20 s"))
        assertNull(preferences.value.activeRun)
        assertEquals(manager.state.value.lastReport, preferences.value.lastReport)
    }

    @Test fun processRestartOnSameBootResumesButCannotCertifyLostQueueData() = runTest {
        val environment = FakeEnvironment()
        val preferences = MemoryPreferences()
        val first = NightTestManager(environment, preferences, backgroundScope)
        first.start()
        assertNotNull(first.state.value.activeRun)
        val second = NightTestManager(environment, preferences, backgroundScope)
        runCurrent()
        assertNotNull(second.state.value.activeRun)
        assertEquals(first.state.value.activeRun!!.id, second.state.value.activeRun!!.id)
        assertTrue(second.state.value.activeRun!!.resumed)
        environment.currentClocks = environment.currentClocks.copy(elapsedMillis = 22_000L)
        second.stop()
        assertTrue(second.state.value.lastReport!!.contains("Kompletność: niepełna"))
        assertTrue(environment.events.any { it.name == "NIGHT_TEST_RESUMED" })
    }

    @Test fun rebootEndsRestoredRunWithoutComparingElapsedClocks() = runTest {
        val environment = FakeEnvironment()
        val preferences = MemoryPreferences()
        val first = NightTestManager(environment, preferences, backgroundScope)
        first.start()
        environment.currentClocks = environment.currentClocks.copy(bootCount = 3, elapsedMillis = 500L)
        val restored = NightTestManager(environment, preferences, backgroundScope)
        runCurrent()
        assertNull(restored.state.value.activeRun)
        assertNotNull(restored.state.value.lastReport)
        assertTrue(restored.state.value.lastReport!!.contains("INTERRUPTED_REBOOT"))
        assertFalse(restored.state.value.lastReport!!.contains("Czas pomiaru:"))
    }

    @Test fun configurationChangeFinishesRunAndDiagnosticLossMarksItsReportIncomplete() = runTest {
        val environment = FakeEnvironment()
        val manager = NightTestManager(environment, MemoryPreferences(), backgroundScope)
        manager.start()
        runCurrent()
        environment.currentClocks = environment.currentClocks.copy(elapsedMillis = 22_000L)
        environment.currentConfiguration = environment.currentConfiguration.copy(fingerprint = "changed")
        environment.configurations.emit(environment.currentConfiguration)
        runCurrent()
        assertNull(manager.state.value.activeRun)
        assertNotNull(manager.state.value.lastReport)
        assertTrue(manager.state.value.lastReport!!.contains("INTERRUPTED_CONFIGURATION_CHANGE"))
        assertTrue(manager.state.value.lastReport!!.contains("Kompletność: niepełna"))
    }

    @Test fun missingStartOrNewJournalLossCannotProduceACompleteReport() = runTest {
        val environment = FakeEnvironment()
        val manager = NightTestManager(environment, MemoryPreferences(), backgroundScope)
        manager.start()
        environment.events.clear()
        environment.discarded = 1L
        manager.stop()
        assertNotNull(manager.state.value.lastReport)
        assertTrue(manager.state.value.lastReport!!.contains("Kompletność: niepełna"))
    }

    @Test fun changingPresenceAndServiceLifecycleDoesNotChangeTheFingerprint() {
        val settings = UserSettings(isSyncEnabled = true)
        val base = CompanionStatus(ready = true, supported = true,
            associations = listOf(CompanionAssociation(2, "Watch"), CompanionAssociation(1, "Other")),
            selectedAssociationId = 2, observationRequested = true)
        val changed = base.copy(presence = CompanionPresence.PRESENT, serviceRunning = true,
            busy = true, error = "ignored", associations = base.associations.reversed())
        assertEquals(nightTestConfiguration(settings, base).fingerprint, nightTestConfiguration(settings, changed).fingerprint)
        assertNotEquals(nightTestConfiguration(settings, base).fingerprint,
            nightTestConfiguration(settings, base.copy(observationRequested = false)).fingerprint)
        assertEquals(NightTestMode.OBSERVING, nightTestConfiguration(settings, base).mode)
        assertEquals(NightTestMode.ASSOCIATED, nightTestConfiguration(settings, base.copy(observationRequested = false)).mode)
        assertEquals(NightTestMode.BASELINE, nightTestConfiguration(settings, base.copy(associations = emptyList(), observationRequested = false)).mode)
        assertEquals(NightTestMode.UNKNOWN, nightTestConfiguration(settings, base.copy(supported = false)).mode)
    }

    @Test fun uiCallerCancellationDuringSaveDoesNotAbandonApplicationOwnedStart() = runTest {
        val environment = FakeEnvironment()
        val preferences = MemoryPreferences()
        val applicationScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        try {
            val manager = NightTestManager(environment, preferences, applicationScope)
            runCurrent()
            val saveEntered = CompletableDeferred<Unit>()
            val releaseSave = CompletableDeferred<Unit>()
            preferences.beforeSave = {
                saveEntered.complete(Unit)
                releaseSave.await()
            }
            val uiCaller = launch { manager.start() }
            runCurrent()
            saveEntered.await()

            uiCaller.cancel()
            releaseSave.complete(Unit)
            advanceUntilIdle()

            assertTrue(uiCaller.isCancelled)
            assertNotNull(preferences.value.activeRun)
            val run = preferences.value.activeRun!!
            assertEquals(run.id, manager.state.value.activeRun?.id)
            assertEquals(run.id, environment.currentRunId)
            assertEquals(1, environment.events.count { it.name == "NIGHT_TEST_START" })
        } finally {
            applicationScope.cancel()
        }
    }

    private class MemoryPreferences(var value: NightTestStoredState = NightTestStoredState()) : NightTestPreferences {
        var beforeSave: suspend () -> Unit = {}
        override suspend fun load() = value
        override suspend fun save(value: NightTestStoredState) {
            beforeSave()
            this.value = value
        }
    }

    private class FakeEnvironment : NightTestEnvironment {
        override val configurations = MutableSharedFlow<NightTestConfiguration>(extraBufferCapacity = 4)
        override val batteries = MutableSharedFlow<NightTestBattery>(extraBufferCapacity = 4)
        var currentConfiguration = NightTestConfiguration(true, "original", NightTestMode.BASELINE, true, "WHITELISTED")
        var currentClocks = NightTestClocks(10_000L, 2_000L, 2)
        var currentRunId: String? = null
        var onSetActive: (String?) -> Unit = {}
        var discarded = 0L
        val events = mutableListOf<DiagnosticEvent>()
        override suspend fun configuration() = currentConfiguration
        override fun clocks() = currentClocks
        override fun battery() = NightTestBattery(90, 0)
        override fun powerSnapshot() = "powerSave=false"
        override suspend fun window(runId: String?) = DiagnosticWindow(
            events.filter { runId == null || it.runId() == runId }, discardedEvents = discarded)
        override fun setActiveRun(id: String?) { onSetActive(id); currentRunId = id }
        override fun record(event: String, details: String) {
            events += DiagnosticEvent(currentClocks.wallMillis, currentClocks.elapsedMillis, "session", 1, null, event,
                currentRunId?.let { "runId=$it; $details" } ?: details)
        }
    }
}
