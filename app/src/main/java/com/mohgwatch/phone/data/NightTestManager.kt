package com.mohgwatch.phone.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mohgwatch.core.model.UserSettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.ZoneId
import java.util.UUID

data class NightTestRun(
    val id: String,
    val startedAtMillis: Long,
    val startedElapsedMillis: Long = 0L,
    val bootCount: Int? = null,
    val configurationFingerprint: String = "",
    val batteryStart: Int? = null,
    val pluggedAtStart: Int? = null,
    val batteryExemptAtStart: Boolean? = null,
    val backgroundDataAtStart: String? = null,
    val initialDiscardedEvents: Long = 0L,
    val initialUnreadableLines: Int = 0,
    val initialDroppedEvents: Long = 0L,
    val initialWriteFailures: Long = 0L,
    val initialQueueFlushed: Boolean = true,
    val resumed: Boolean = false,
    val observedChargingChanges: Int = 0,
    val lastPlugged: Int? = pluggedAtStart,
    val startedZoneId: String = ZoneId.systemDefault().id,
    val powerAtStart: String? = null
)
data class NightTestState(val ready: Boolean = false, val activeRun: NightTestRun? = null,
                          val lastReport: String? = null, val error: String? = null)

internal data class NightTestConfiguration(val syncEnabled: Boolean, val fingerprint: String,
    val batteryExempt: Boolean? = null, val backgroundDataStatus: String? = null)
internal data class NightTestClocks(val wallMillis: Long, val elapsedMillis: Long, val bootCount: Int?)
internal data class NightTestBattery(val percent: Int? = null, val plugged: Int? = null)
internal data class NightTestStoredState(val activeRun: NightTestRun? = null, val lastReport: String? = null)
internal interface NightTestPreferences {
    suspend fun load(): NightTestStoredState
    suspend fun save(value: NightTestStoredState)
}
internal interface NightTestEnvironment {
    val configurations: Flow<NightTestConfiguration>
    val batteries: Flow<NightTestBattery>
    suspend fun configuration(): NightTestConfiguration
    fun clocks(): NightTestClocks
    fun battery(): NightTestBattery
    fun powerSnapshot(): String
    suspend fun window(runId: String? = null): DiagnosticWindow
    fun setActiveRun(id: String?)
    fun record(event: String, details: String)
}

class NightTestManager internal constructor(
    private val environment: NightTestEnvironment,
    private val preferences: NightTestPreferences,
    private val scope: CoroutineScope
) {
    private val mutableState = MutableStateFlow(NightTestState())
    val state: StateFlow<NightTestState> = mutableState
    private val mutex = Mutex()
    private var initialized = false

    init {
        scope.launch {
            action { initializeLocked() }
            launch {
                try {
                    environment.configurations.collect {
                        action {
                            val run = state.value.activeRun ?: return@action
                            // Read again after acquiring the lock; a queued emission can predate Start.
                            val current = environment.configuration()
                            val clocks = environment.clocks()
                            NightTestPolicy.interruptionReason(run.bootCount, clocks.bootCount,
                                run.configurationFingerprint, current.fingerprint)?.let { reason ->
                                finishLocked(reason, current, clocks)
                            }
                        }
                    }
                } catch (failure: Exception) {
                    reportFailure(failure)
                }
            }
            launch {
                try {
                    environment.batteries.collect { battery ->
                        action {
                            val run = state.value.activeRun ?: return@action
                            val plugged = battery.plugged ?: return@action
                            if (plugged == run.lastPlugged) return@action
                            val updated = run.copy(lastPlugged = plugged,
                                observedChargingChanges = run.observedChargingChanges + if (run.lastPlugged == null) 0 else 1)
                            preferences.save(NightTestStoredState(updated, state.value.lastReport))
                            mutableState.value = state.value.copy(activeRun = updated)
                            environment.record("NIGHT_TEST_POWER_CHANGED", "plugged=$plugged batteryPercent=${battery.percent}")
                        }
                    }
                } catch (failure: Exception) {
                    reportFailure(failure)
                }
            }
        }
    }

    suspend fun start() = action {
        initializeLocked()
        if (state.value.activeRun != null) return@action
        val configuration = environment.configuration()
        if (!configuration.syncEnabled) {
            mutableState.value = state.value.copy(error = "Najpierw włącz synchronizację, aby rozpocząć pomiar nocny.")
            return@action
        }
        val before = environment.window()
        val clocks = environment.clocks()
        if (clocks.bootCount == null) {
            mutableState.value = state.value.copy(error = "Nie można odczytać numeru uruchomienia telefonu. Pomiar nie został rozpoczęty.")
            return@action
        }
        val battery = environment.battery()
        val run = NightTestRun(UUID.randomUUID().toString(), clocks.wallMillis,
            startedElapsedMillis = clocks.elapsedMillis, bootCount = clocks.bootCount,
            configurationFingerprint = configuration.fingerprint, batteryStart = battery.percent,
            pluggedAtStart = battery.plugged, batteryExemptAtStart = configuration.batteryExempt,
            backgroundDataAtStart = configuration.backgroundDataStatus,
            initialDiscardedEvents = before.discardedEvents, initialUnreadableLines = before.unreadableLines,
            initialDroppedEvents = before.droppedEvents, initialWriteFailures = before.writeFailures,
            initialQueueFlushed = before.queueFlushed, powerAtStart = environment.powerSnapshot())
        preferences.save(NightTestStoredState(run, state.value.lastReport))
        mutableState.value = state.value.copy(ready = true, activeRun = run, error = null)
        environment.setActiveRun(run.id)
        environment.record("NIGHT_TEST_START", "boot=${run.bootCount} config=${run.configurationFingerprint}; " +
            "batteryPercent=${run.batteryStart} plugged=${run.pluggedAtStart} batteryExempt=${run.batteryExemptAtStart} data=${run.backgroundDataAtStart}")
    }

    suspend fun stop() = action {
        initializeLocked()
        val run = state.value.activeRun ?: return@action
        val current = environment.configuration()
        val clocks = environment.clocks()
        val reason = NightTestPolicy.interruptionReason(run.bootCount, clocks.bootCount,
            run.configurationFingerprint, current.fingerprint) ?: "COMPLETED"
        finishLocked(reason, current, clocks)
    }

    private suspend fun initializeLocked() {
        if (initialized) return
        val stored = preferences.load()
        mutableState.value = state.value.copy(lastReport = stored.lastReport, activeRun = stored.activeRun)
        val run = stored.activeRun
        if (run != null) {
            environment.setActiveRun(run.id)
            val configuration = environment.configuration()
            val clocks = environment.clocks()
            val reason = NightTestPolicy.interruptionReason(run.bootCount, clocks.bootCount,
                run.configurationFingerprint, configuration.fingerprint)
            if (reason != null) {
                finishLocked(reason, configuration, clocks)
            } else {
                val resumed = run.copy(resumed = true)
                preferences.save(NightTestStoredState(resumed, stored.lastReport))
                mutableState.value = state.value.copy(activeRun = resumed)
                environment.record("NIGHT_TEST_RESUMED", "boot=${clocks.bootCount}; previousProcessQueueUnknown=true")
            }
        }
        initialized = true
        mutableState.value = state.value.copy(ready = true, error = null)
    }

    private suspend fun finishLocked(reason: String, configuration: NightTestConfiguration, clocks: NightTestClocks) {
        val run = state.value.activeRun ?: return
        val battery = environment.battery()
        val power = environment.powerSnapshot()
        environment.record("NIGHT_TEST_END", "reason=$reason batteryPercent=${battery.percent} plugged=${battery.plugged}; $power")
        val window = environment.window(run.id)
        val comparable = run.bootCount != null && run.bootCount == clocks.bootCount && clocks.elapsedMillis >= run.startedElapsedMillis
        val incomplete = !comparable || reason != "COMPLETED" || run.resumed || !run.initialQueueFlushed || !window.queueFlushed ||
            window.discardedEvents > run.initialDiscardedEvents || window.unreadableLines > run.initialUnreadableLines ||
            window.droppedEvents > run.initialDroppedEvents || window.writeFailures > run.initialWriteFailures ||
            window.events.any { it.name == "DIAGNOSTICS_LOSS" } || window.events.none { it.name == "NIGHT_TEST_START" }
        val report = buildString {
            appendLine("MohgWatch — pomiar nocny")
            appendLine("Id pomiaru: ${run.id}")
            appendLine("Zakończenie: $reason")
            appendLine("Początek: ${DiagnosticJournal.formatTime(run.startedAtMillis, run.startedZoneId)}")
            appendLine("Koniec: ${DiagnosticJournal.formatTime(clocks.wallMillis)}")
            appendLine("Uruchomienie telefonu: ${run.bootCount ?: "nieznane"} → ${clocks.bootCount ?: "nieznane"}; wznowiony proces: ${run.resumed}")
            appendLine("Konfiguracja początkowa: ${run.configurationFingerprint}")
            appendLine("Konfiguracja końcowa: ${configuration.fingerprint}")
            appendLine("Poziom baterii: ${run.batteryStart?.let { "$it%" } ?: "nieznany"} → ${battery.percent?.let { "$it%" } ?: "nieznany"}")
            val delta = run.batteryStart?.let { start -> battery.percent?.minus(start) }
            appendLine("Zmiana poziomu baterii (przybliżona): ${delta?.let { "$it p.p." } ?: "nieznana"}; obejmuje cały telefon.")
            appendLine("Zasilanie (0=odłączone): ${run.pluggedAtStart ?: "nieznane"} → ${battery.plugged ?: "nieznane"}; zarejestrowane zmiany: ${run.observedChargingChanges}")
            appendLine("Zmiany zasilania opisują odebrane zdarzenia; restart procesu może pozostawić luki.")
            appendLine("Wyjątki początkowe: bateria=${run.batteryExemptAtStart}; dane=${run.backgroundDataAtStart}")
            appendLine("Wyjątki końcowe: bateria=${configuration.batteryExempt}; dane=${configuration.backgroundDataStatus}")
            appendLine("Stan zasilania na początku: ${run.powerAtStart ?: "nieznany"}")
            appendLine("Stan zasilania na końcu: $power")
            appendLine("Źródło: kolejka opróżniona=${window.queueFlushed}; usunięte=${run.initialDiscardedEvents}→${window.discardedEvents}; " +
                "nieczytelne=${run.initialUnreadableLines}→${window.unreadableLines}; utracone w kolejce=${run.initialDroppedEvents}→${window.droppedEvents}; " +
                "błędy zapisu=${run.initialWriteFailures}→${window.writeFailures}")
            appendLine()
            if (comparable) {
                append(NightTestReport.summarize(NightTestSample(run.startedElapsedMillis,
                    clocks.elapsedMillis, window.events, incomplete)))
            } else {
                appendLine("Kompletność: niepełna")
                append("Czas i przerwy są nieporównywalne: brak potwierdzenia tego samego uruchomienia i ciągłości zegara monotonicznego.")
            }
        }
        // One transaction keeps the finished report when the active run is removed.
        preferences.save(NightTestStoredState(lastReport = report))
        mutableState.value = state.value.copy(ready = true, activeRun = null, lastReport = report, error = null)
        environment.setActiveRun(null)
    }

    private suspend fun action(block: suspend () -> Unit) = scope.async {
        mutex.withLock {
            try { block() } catch (failure: Exception) { reportFailure(failure) }
        }
    }.await()

    private fun reportFailure(failure: Exception) {
        if (failure is CancellationException) throw failure
        mutableState.value = state.value.copy(ready = true, error = "Nie udało się odczytać lub zapisać pomiaru nocnego. Spróbuj ponownie.")
        environment.record("NIGHT_TEST_ERROR", "type=${failure.javaClass.simpleName}")
    }

    companion object {
        @Volatile private var instance: NightTestManager? = null

        fun get(context: Context): NightTestManager = instance ?: synchronized(this) {
            instance ?: run {
                val app = context.applicationContext
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                NightTestManager(AndroidNightTestEnvironment(app), NightTestPreferenceStore(app, scope), scope)
                    .also { instance = it }
            }
        }
    }
}

internal fun nightTestConfiguration(settings: UserSettings, batteryExempt: Boolean?, backgroundDataStatus: String?): NightTestConfiguration {
    val fingerprint = "sync=${settings.isSyncEnabled}; intervalMinutes=${settings.pollIntervalMinutes}; " +
        "persistentWakeLock=${settings.persistentWakeLockEnabled}; staleMinutes=${settings.staleDataMinutes}"
    return NightTestConfiguration(settings.isSyncEnabled, fingerprint, batteryExempt, backgroundDataStatus)
}

private class NightTestPreferenceStore(context: Context, scope: CoroutineScope) : NightTestPreferences {
    private val store = PreferenceDataStoreFactory.create(scope = scope,
        produceFile = { File(context.noBackupFilesDir, "mohgwatch_night_test.preferences_pb") })
    private val active = stringPreferencesKey("active_run_json")
    private val report = stringPreferencesKey("last_report")
    private val adapter by lazy { Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build().adapter(NightTestRun::class.java) }

    override suspend fun load(): NightTestStoredState {
        val values = store.data.first()
        return NightTestStoredState(values[active]?.let { requireNotNull(adapter.fromJson(it)) }, values[report])
    }

    override suspend fun save(value: NightTestStoredState) {
        val json = value.activeRun?.let(adapter::toJson)
        store.edit { values ->
            json?.let { values[active] = it } ?: values.remove(active)
            value.lastReport?.let { values[report] = it } ?: values.remove(report)
        }
    }
}

private class AndroidNightTestEnvironment(private val context: Context) : NightTestEnvironment {
    private val settings = SettingsStore(context)

    override val configurations = settings.settingsFlow.map(::configurationOf).distinctUntilChangedBy { it.fingerprint }

    override val batteries = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null) trySend(readBattery(intent))
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val sticky = if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            else context.registerReceiver(receiver, filter)
        sticky?.let { trySend(readBattery(it)) }
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    override suspend fun configuration(): NightTestConfiguration = configurationOf(settings.getSettings())

    private fun configurationOf(settings: UserSettings) = nightTestConfiguration(settings,
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
        }.getOrNull())

    override fun clocks() = NightTestClocks(System.currentTimeMillis(), SystemClock.elapsedRealtime(), runCatching {
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
    }.getOrNull())

    override fun battery(): NightTestBattery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        ?.let(::readBattery) ?: NightTestBattery()

    override fun powerSnapshot() = Diagnostics.snapshot(context)
    override suspend fun window(runId: String?) = Diagnostics.window(context, DiagnosticFilter(runId = runId))
    override fun setActiveRun(id: String?) = Diagnostics.setActiveRun(id)
    override fun record(event: String, details: String) = Diagnostics.record(context, event, details)

    private fun readBattery(intent: Intent): NightTestBattery {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return NightTestBattery(
            percent = if (level >= 0 && scale > 0) (100L * level / scale).toInt().coerceIn(0, 100) else null,
            plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1).takeIf { it >= 0 })
    }
}
