package com.mohgwatch.phone.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mohgwatch.core.api.LibreLinkUpClient
import com.mohgwatch.core.api.fetchWithSessionRecovery
import com.mohgwatch.core.model.*
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.util.GlucoseFormatter
import com.mohgwatch.phone.MainActivity
import com.mohgwatch.phone.R
import com.mohgwatch.phone.data.CredentialStore
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.data.GlucoseHistoryStore
import com.mohgwatch.phone.data.WatchState
import com.mohgwatch.phone.data.Diagnostics
import com.mohgwatch.phone.data.NetworkObservationPolicy
import com.mohgwatch.phone.companion.CompanionIntegrationManager
import android.os.SystemClock
import com.mohgwatch.phone.service.DataLayerSender
import com.mohgwatch.phone.util.trStr
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Foreground Service odpytujący LibreLinkUp API na telefonie
 * i pushujący dane glukozy na zegarek przez Data Layer API.
 *
 * Działa cyklicznie co 1-5 minut (konfigurowane).
 * Zegarek NIE łączy się z internetem — wszystkie dane przechodzą przez ten serwis.
 */
class GlucoseSyncService : Service() {

    companion object {
        private const val TAG = "GlucoseSyncService"
        const val CHANNEL_ID = "mohgwatch_sync"
        const val ALERT_LOW_CHANNEL_ID = "mohgwatch_alert_low"
        const val ALERT_HIGH_CHANNEL_ID = "mohgwatch_alert_high"
        const val ALERT_DISCONNECT_CHANNEL_ID = "mohgwatch_alert_disconnect"
        const val NOTIFICATION_ID = 1001
        const val ACTION_LOGOUT = "com.mohgwatch.LOGOUT"
        const val ACTION_TEST_ALERT_LOW = "com.mohgwatch.TEST_ALERT_LOW"
        const val ACTION_TEST_ALERT_HIGH = "com.mohgwatch.TEST_ALERT_HIGH"
        const val ACTION_TEST_ALERT_DISCONNECT = "com.mohgwatch.TEST_ALERT_DISCONNECT"
        const val ACTION_FORCE_SYNC = "com.mohgwatch.FORCE_SYNC"
        const val ACTION_ALARM_WAKEUP = "com.mohgwatch.ALARM_WAKEUP"
        const val ACTION_MUTE_ALERT = "com.mohgwatch.MUTE_ALERT"
        const val ACTION_UPDATE_WAKELOCK = "com.mohgwatch.UPDATE_WAKELOCK"

        const val ACTION_CLEAR_NOTIFICATIONS = "com.mohgwatch.CLEAR_NOTIFICATIONS"
        private val controlScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val controlMutex = Mutex()

        fun updateWakeLock(context: Context) {
            val intent = Intent(context, GlucoseSyncService::class.java).apply {
                action = ACTION_UPDATE_WAKELOCK
            }
            context.startService(intent)
        }

        fun start(context: Context) {
            val app = context.applicationContext
            controlScope.launch {
                controlMutex.withLock {
                    val store = SettingsStore(app)
                    store.setSyncEnabled(true)
                    SyncScheduler.enable(app)
                    SyncScheduler.schedule(app, store.getSettings().pollIntervalMinutes.coerceIn(1, 5) * 60_000L)
                    SyncWatchdogWorker.schedule(app)
                    SyncScheduler.resumeService(app)
                }
            }
        }

        fun stop(context: Context) {
            val app = context.applicationContext
            controlScope.launch {
                controlMutex.withLock {
                    SettingsStore(app).setSyncEnabled(false)
                    SyncScheduler.cancel(app)
                    SyncWatchdogWorker.cancel(app)
                    app.stopService(Intent(app, GlucoseSyncService::class.java))
                }
            }
        }

        fun logout(context: Context) {
            context.startForegroundService(Intent(context, GlucoseSyncService::class.java).setAction(ACTION_LOGOUT))
        }

        fun testAlertLow(context: Context, forceMode: String? = null) {
            context.startService(Intent(context, GlucoseSyncService::class.java).apply { action = ACTION_TEST_ALERT_LOW; forceMode?.let { putExtra("force_mode", it) } })
        }
        fun testAlertHigh(context: Context, forceMode: String? = null) {
            context.startService(Intent(context, GlucoseSyncService::class.java).apply { action = ACTION_TEST_ALERT_HIGH; forceMode?.let { putExtra("force_mode", it) } })
        }
        fun testAlertDisconnect(context: Context, forceMode: String? = null) {
            context.startService(Intent(context, GlucoseSyncService::class.java).apply { action = ACTION_TEST_ALERT_DISCONNECT; forceMode?.let { putExtra("force_mode", it) } })
        }
        const val ACTION_INJECT_READING = "com.mohgwatch.INJECT_READING"

        fun injectReading(context: Context, value: Float, trendArrow: TrendArrow = TrendArrow.STABLE) {
            val intent = Intent(context, GlucoseSyncService::class.java).apply {
                action = ACTION_INJECT_READING
                putExtra("glucose_value", value)
                putExtra("trend_arrow", trendArrow.apiValue)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }


        fun clearNotifications(context: Context) {
            context.startService(Intent(context, GlucoseSyncService::class.java).apply { action = ACTION_CLEAR_NOTIFICATIONS })
        }

        fun forceSync(context: Context) {
            start(context)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Volatile private var activeApiStage = "latest"
    private val apiClient = LibreLinkUpClient { event, details ->
        Diagnostics.record(this, event, "stage=$activeApiStage $details", activeCycleId)
    }
    private lateinit var dataLayerSender: DataLayerSender
    private lateinit var credentialStore: CredentialStore
    private lateinit var settingsStore: SettingsStore
    private lateinit var logsStore: com.mohgwatch.phone.data.LogsStore
    private lateinit var historyStore: GlucoseHistoryStore

    @Volatile private var activeCycleId: String? = null
    private val readingMutex = Mutex()
    private var syncJob: Job? = null
    private var pollTimerJob: Job? = null
    private var persistentWakeLock: android.os.PowerManager.WakeLock? = null

    // State tracking for alerts
    private var glucoseRange = GlucoseRange.NORMAL
    @Volatile private var isCurrentlyDisconnected = false
    private var phoneAlertContext: String? = null
    private var activeAccountId: String? = null
    private var lastGraphFetch = 0L
    @Volatile private var destroyed = false
    @Volatile private var stopping = false
    @Volatile private var watchConnected = false
    @Volatile private var networkBlocked: Boolean? = null
    @Volatile private var blockedNetwork: android.net.Network? = null
    private val soundMutex = Mutex()
    @Volatile private var soundJob: Job? = null
    @Volatile private var soundAlertId: Int? = null
    @Volatile private var soundChannel: String? = null
    private val alertIds = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private val nextAlertId = java.util.concurrent.atomic.AtomicInteger(10_000)

    @Synchronized private fun updatePersistentWakeLock(enabled: Boolean) {
        if (enabled && GlucoseSyncState.isSyncing.value) {
            if (persistentWakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                persistentWakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "MohgWatch::PersistentWakeLock")
                persistentWakeLock?.setReferenceCounted(false)
            }
            persistentWakeLock?.let {
                val wasHeld = it.isHeld
                if (wasHeld) it.release()
                it.acquire(15 * 60_000L)
                Diagnostics.record(this, "WAKELOCK_ACQUIRED", "type=persistent renewed=$wasHeld held=${it.isHeld} timeoutMs=900000", activeCycleId)
            }
        } else {
            persistentWakeLock?.let {
                val held = it.isHeld
                if (held) it.release()
                Diagnostics.record(this, "WAKELOCK_RELEASED", "type=persistent wasHeld=$held held=${it.isHeld}", activeCycleId)
            }
            persistentWakeLock = null
        }
    }

    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null
    private val networkObservations = NetworkObservationPolicy()

    override fun onCreate() {
        super.onCreate()
        dataLayerSender = DataLayerSender(this) { activeCycleId }
        Diagnostics.record(this, "SERVICE_CREATE", Diagnostics.snapshot(this))
        registerPowerEvents()
        credentialStore = CredentialStore(this)
        settingsStore = SettingsStore(this)
        logsStore = com.mohgwatch.phone.data.LogsStore(this)
        historyStore = GlucoseHistoryStore(this)
        createNotificationChannel()
        registerNetworkCallback()
        observeWristState()
    }

    private fun observeWristState() {
        serviceScope.launch {
            val clock = flow {
                while (currentCoroutineContext().isActive) {
                    emit(System.currentTimeMillis())
                    delay(30_000L)
                }
            }
            combine(WatchState.wristStatus, WatchState.lastUpdateTimestamp, clock) { status, timestamp, _ ->
                status.at(timestamp, System.currentTimeMillis())
            }.distinctUntilChanged().collect { status ->
                val reading = GlucoseSyncState.latestReading.value
                if (reading != null && !isCurrentlyDisconnected) {
                    val settings = settingsStore.getSettings()
                    val lang = settings.language
                    val wristSuffix = if (settings.showWristStatusInNotification) {
                        val wristLabel = trStr(lang, status.polishLabel, status.englishLabel)
                        "  •  $wristLabel"
                    } else ""
                    val minutesAgo = reading.getMinutesAgo()
                    val timeText = GlucoseFormatter.formatMinutesAgo(minutesAgo)
                    val title = "${trStr(lang, "Glukoza:", "Glucose:")} ${GlucoseFormatter.formatWithUnit(reading.value, settings.unit)} ${reading.trendArrow.symbol}$wristSuffix"
                    updateNotification(title, timeText)
                }
            }
        }
    }

    private var powerEventsRegistered = false
    private val powerEvents = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Diagnostics.record(context, "POWER_STATE", "action=${intent.action}; ${Diagnostics.snapshot(context)}", activeCycleId)
        }
    }

    private fun registerPowerEvents() {
        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(android.os.PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
            addAction(android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            if (Build.VERSION.SDK_INT >= 33) addAction(android.os.PowerManager.ACTION_LOW_POWER_STANDBY_ENABLED_CHANGED)
        }
        androidx.core.content.ContextCompat.registerReceiver(this, powerEvents, filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        powerEventsRegistered = true
    }

    private fun registerNetworkCallback() {
        val cm = getSystemService(android.net.ConnectivityManager::class.java) ?: return
        networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onBlockedStatusChanged(network: android.net.Network, blocked: Boolean) {
                blockedNetwork = network
                networkBlocked = blocked
                Diagnostics.record(this@GlucoseSyncService, if (blocked) "NETWORK_BLOCKED" else "NETWORK_UNBLOCKED",
                    "network=$network blocked=$blocked", activeCycleId)
                val pm = getSystemService(android.os.PowerManager::class.java)
                Log.w(TAG, "Android network blocked=$blocked, idle=${pm.isDeviceIdleMode}, batteryExempt=${pm.isIgnoringBatteryOptimizations(packageName)}")
                if (blocked) {
                    GlucoseSyncState.lastError.value = "Android zgłosił blokadę sieci aplikacji (przyczyna nieznana)"
                } else if (GlucoseSyncState.isSyncing.value) {
                    doSyncAndScheduleNext("network_unblocked")
                }
            }

            override fun onLost(network: android.net.Network) {
                if (network == blockedNetwork) { networkBlocked = null; blockedNetwork = null }
                networkObservations.lost(network.toString())
                Diagnostics.record(this@GlucoseSyncService, "NETWORK_LOST", "network=$network", activeCycleId)
            }

            override fun onCapabilitiesChanged(network: android.net.Network, capabilities: android.net.NetworkCapabilities) {
                val transports = listOf(
                    android.net.NetworkCapabilities.TRANSPORT_WIFI to "wifi",
                    android.net.NetworkCapabilities.TRANSPORT_CELLULAR to "cellular",
                    android.net.NetworkCapabilities.TRANSPORT_ETHERNET to "ethernet",
                    android.net.NetworkCapabilities.TRANSPORT_VPN to "vpn",
                    android.net.NetworkCapabilities.TRANSPORT_BLUETOOTH to "bluetooth"
                ).filter { capabilities.hasTransport(it.first) }.joinToString("+") { it.second }.ifEmpty { "other" }
                val observed = "transport=$transports validated=${capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)} " +
                    "internet=${capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)} " +
                    "notRestricted=${capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)} " +
                    "notMetered=${capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED)} " +
                    "notSuspended=${capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)}"
                if (networkObservations.shouldRecord(network.toString(), observed)) {
                    Diagnostics.record(this@GlucoseSyncService, "NETWORK_CAPABILITIES", "network=$network $observed", activeCycleId)
                }
            }

            override fun onAvailable(network: android.net.Network) {
                networkBlocked = null
                blockedNetwork = network
                Diagnostics.record(this@GlucoseSyncService, "NETWORK_AVAILABLE", "network=$network", activeCycleId)
                serviceScope.launch {
                    val settings = settingsStore.getSettings()
                    if (settings.isSyncEnabled) {
                        val lastSync = GlucoseSyncState.lastSyncTimestamp.value
                        val intervalMs = settings.pollIntervalMinutes * 60_000L
                        if (System.currentTimeMillis() - lastSync >= intervalMs || isCurrentlyDisconnected) {
                            Log.d(TAG, "Połączenie sieciowe przywrócone — wznawiam synchronizację")
                            doSyncAndScheduleNext("network_available")
                        }
                    }
                }
            }
        }
        try {
            cm.registerDefaultNetworkCallback(networkCallback!!)
        } catch (e: Exception) {
            Log.e(TAG, "Nie udało się zarejestrować network callback", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Diagnostics.record(this, "SERVICE_START", "action=${intent?.action ?: "sticky_restart"} flags=$flags startId=$startId", activeCycleId)
        when (intent?.action) {
            ACTION_LOGOUT -> {
                startForegroundWithNotification()
                stopping = true
                SyncScheduler.cancel(this)
                controlScope.launch {
                    controlMutex.withLock {
                        settingsStore.setSyncEnabled(false)
                        GlucoseSyncState.isSyncing.value = false
                        pollTimerJob?.cancelAndJoin()
                        syncJob?.cancelAndJoin()
                        soundJob?.cancelAndJoin()
                        credentialStore.clear()
                        apiClient.clearSession()
                        stopSync()
                        stopSelf()
                    }
                }
                return START_NOT_STICKY
            }
            ACTION_MUTE_ALERT -> {
                val alertId = intent.getIntExtra("alert_id", -1)
                if (alertId == soundAlertId) cancelSound()
                return START_STICKY
            }
            ACTION_CLEAR_NOTIFICATIONS -> {
                val manager = getSystemService(NotificationManager::class.java)
                manager.cancelAll()
                cancelSound()
                alertIds.clear()
                return START_STICKY
            }
            ACTION_TEST_ALERT_LOW -> {
                val forceMode = intent?.getStringExtra("force_mode")
                serviceScope.launch {
                    val settings = settingsStore.getSettings()
                    val lang = settings.language
                    sendAlertNotification(
                        trStr(lang, "Testowy Niski", "Test Low"),
                        trStr(lang, "To jest alert testowy dla niskiego stężenia glukozy.", "This is a test low glucose alert."),
                        settings.lowGlucoseSoundUri,
                        ALERT_LOW_CHANNEL_ID,
                        forceMode ?: "test"
                    )
                }
                return START_STICKY
            }
            ACTION_TEST_ALERT_HIGH -> {
                val forceMode = intent?.getStringExtra("force_mode")
                serviceScope.launch {
                    val settings = settingsStore.getSettings()
                    val lang = settings.language
                    sendAlertNotification(
                        trStr(lang, "Testowy Wysoki", "Test High"),
                        trStr(lang, "To jest alert testowy dla wysokiego stężenia glukozy.", "This is a test high glucose alert."),
                        settings.highGlucoseSoundUri,
                        ALERT_HIGH_CHANNEL_ID,
                        forceMode ?: "test"
                    )
                }
                return START_STICKY
            }
            ACTION_TEST_ALERT_DISCONNECT -> {
                val forceMode = intent?.getStringExtra("force_mode")
                serviceScope.launch {
                    val settings = settingsStore.getSettings()
                    val lang = settings.language
                    sendAlertNotification(
                        trStr(lang, "Testowe Rozłączenie", "Test Disconnect"),
                        trStr(lang, "To jest alert testowy o błędzie połączenia.", "This is a test disconnect alert."),
                        settings.disconnectSoundUri,
                        ALERT_DISCONNECT_CHANNEL_ID,
                        forceMode ?: "test"
                    )
                }
                return START_STICKY
            }
            ACTION_INJECT_READING -> {
                val value = intent?.getFloatExtra("glucose_value", 100f) ?: 100f
                val trend = TrendArrow.fromApiValue(intent?.getIntExtra("trend_arrow", TrendArrow.STABLE.apiValue) ?: TrendArrow.STABLE.apiValue)
                startForegroundWithNotification()
                serviceScope.launch {
                    val settings = settingsStore.getSettings()
                    val reading = com.mohgwatch.core.model.GlucoseReading(
                        value = value,
                        trendArrow = trend,
                        measurementColor = when {
                            value < settings.watchLowThreshold -> com.mohgwatch.core.model.MeasurementColor.LOW
                            value > settings.watchVeryHighThreshold -> com.mohgwatch.core.model.MeasurementColor.VERY_HIGH
                            value > settings.watchHighThreshold -> com.mohgwatch.core.model.MeasurementColor.HIGH
                            else -> com.mohgwatch.core.model.MeasurementColor.IN_RANGE
                        },
                        timestamp = System.currentTimeMillis(),
                        isHigh = value > settings.highThreshold,
                        isLow = value < settings.lowThreshold
                    )
                    processReading(reading, statusMsg = "Testowy pomiar: ${value.toInt()} mg/dL")
                }
                return START_STICKY
            }
            ACTION_UPDATE_WAKELOCK -> {
                serviceScope.launch {
                    val settings = settingsStore.getSettings()
                    updatePersistentWakeLock(settings.persistentWakeLockEnabled)
                }
                return START_STICKY
            }
            ACTION_FORCE_SYNC -> {
                startForegroundWithNotification()
                resumeIfEnabled("user")
                return START_STICKY
            }
            ACTION_ALARM_WAKEUP -> {
                startForegroundWithNotification()
                resumeIfEnabled(intent.getStringExtra("sync_trigger") ?: "alarm")
                return START_STICKY
            }
            else -> {
                startForegroundWithNotification()
                resumeIfEnabled("sticky_restart")
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun resumeIfEnabled(trigger: String) {
        serviceScope.launch {
            if (stopping || !settingsStore.getSettings().isSyncEnabled) {
                stopSync()
                stopSelf()
                return@launch
            }
            GlucoseSyncState.isSyncing.value = true
            doSyncAndScheduleNext(trigger)
        }
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        Log.e(TAG, "Android zakończył limit czasu usługi: $fgsType")
        Diagnostics.record(this, "SERVICE_TIMEOUT", "fgsType=$fgsType startId=$startId")
        SyncScheduler.schedule(this, 60_000L)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        destroyed = true
        Diagnostics.record(this, "SERVICE_DESTROY", "explicitStop=$stopping; ${Diagnostics.snapshot(this)}", activeCycleId)
        if (powerEventsRegistered) runCatching { unregisterReceiver(powerEvents) }
        networkCallback?.let {
            val cm = getSystemService(android.net.ConnectivityManager::class.java)
            try { cm?.unregisterNetworkCallback(it) } catch (e: Exception) {}
        }
        cancelSound()
        serviceScope.cancel()
        GlucoseSyncState.isSyncing.value = false
        updatePersistentWakeLock(false)
        super.onDestroy()
    }

    private fun startForegroundWithNotification() {
        // Foreground promotion has a system deadline; do not wait for disk on the main thread.
        val lang = "system"
        val notification = buildNotification(trStr(lang, "Uruchamianie synchronizacji…", "Starting sync..."), null)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @Synchronized private fun doSyncAndScheduleNext(trigger: String = "unknown") {
        if (stopping || destroyed || !GlucoseSyncState.isSyncing.value) return
        if (syncJob?.isActive == true) {
            Diagnostics.record(this, "SYNC_SKIPPED_ACTIVE", "trigger=$trigger cycle still active", activeCycleId)
            return
        }
        pollTimerJob?.cancel()
        syncJob = serviceScope.launch(start = CoroutineStart.LAZY) {
            val cycle = java.util.UUID.randomUUID().toString()
            activeCycleId = cycle
            val started = SystemClock.elapsedRealtime()
            val pm = getSystemService(android.os.PowerManager::class.java)
            val wakeLock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "MohgWatch::SyncWakeLock")
            var nextPoll = started + 5 * 60_000L
            var outcome = "cancelled"
            Diagnostics.record(this@GlucoseSyncService, "SYNC_START", "trigger=$trigger uid=${android.os.Process.myUid()} " +
                "fgsType=${if (Build.VERSION.SDK_INT >= 29) foregroundServiceType else 0} ${Diagnostics.snapshot(this@GlucoseSyncService)}", cycle)
            try {
                wakeLock.acquire(3 * 60_000L)
                Diagnostics.record(this@GlucoseSyncService, "WAKELOCK_ACQUIRED", "type=sync timeoutMs=180000 held=${wakeLock.isHeld}", cycle)
                withTimeout(150_000L) {
                    val settings = settingsStore.getSettings()
                    if (!settings.isSyncEnabled || stopping) { outcome = "disabled"; return@withTimeout }
                    CompanionIntegrationManager.get(this@GlucoseSyncService).refresh()
                    val interval = settings.pollIntervalMinutes.coerceIn(1, 5) * 60_000L
                    nextPoll = started + interval
                    SyncScheduler.schedule(this@GlucoseSyncService, interval)
                    updatePersistentWakeLock(settings.persistentWakeLockEnabled)
                    launch {
                        withTimeoutOrNull(5_000L) {
                            watchConnected = dataLayerSender.isWatchConnected()
                            dataLayerSender.requestWristCheck()
                        }
                    }
                    Diagnostics.record(this@GlucoseSyncService, "SYNC_STAGE", "credentials_and_cache", cycle)
                    val creds = credentialStore.getCredentials()
                    if (creds == null) {
                        reportFailure(DisconnectReason.AUTH_ERROR, "Brak danych logowania — otwórz aplikację")
                        outcome = "auth_error"
                        dataLayerSender.sendConnectionStatus(false, GlucoseSyncState.lastSyncTimestamp.value)
                        return@withTimeout
                    }
                    if (activeAccountId != creds.userId) {
                        activeAccountId = creds.userId
                        apiClient.clearSession()
                        glucoseRange = GlucoseRange.NORMAL
                        phoneAlertContext = null
                        lastGraphFetch = 0L
                        cancelGlucoseAlerts()
                        GlucoseSyncState.history.value = historyStore.load(creds.userId)
                        GlucoseSyncState.latestReading.value = GlucoseSyncState.history.value.lastOrNull()
                        GlucoseSyncState.lastSyncTimestamp.value = historyStore.lastSuccessfulSync()
                    }
                    if (!apiClient.isAuthenticated()) {
                        apiClient.restoreSession(creds.token, creds.userId, creds.region, creds.expires, creds.patientId)
                    }
                    val success = syncOnce()
                    outcome = if (success) "success" else "failed"
                    dataLayerSender.sendConnectionStatus(success, GlucoseSyncState.lastSyncTimestamp.value)
                }
            } catch (error: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                outcome = "timeout"
                Diagnostics.record(this@GlucoseSyncService, "SYNC_TIMEOUT", "limitMs=150000", cycle)
                reportCycleFailure(networkFailureReason(), "Przekroczono czas synchronizacji. ${networkDiagnostics()}")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                outcome = "error"
                Diagnostics.record(this@GlucoseSyncService, "SYNC_ERROR", Diagnostics.error(error), cycle)
                reportCycleFailure(DisconnectReason.API_ERROR, "Błąd synchronizacji: ${error.javaClass.simpleName}")
            } finally {
                val held = wakeLock.isHeld
                if (held) wakeLock.release()
                Diagnostics.record(this@GlucoseSyncService, "WAKELOCK_RELEASED", "type=sync wasHeld=$held held=${wakeLock.isHeld}", cycle)
                Diagnostics.record(this@GlucoseSyncService, "SYNC_END", "result=$outcome durationMs=${SystemClock.elapsedRealtime() - started} lastSuccess=${GlucoseSyncState.lastSyncTimestamp.value} measurementAgeMs=${GlucoseSyncState.latestReading.value?.let { System.currentTimeMillis() - it.timestamp } ?: "nieznane"}", cycle)
                activeCycleId = null
                activeApiStage = "latest"
                if (!destroyed && !stopping && GlucoseSyncState.isSyncing.value) {
                    Diagnostics.record(this@GlucoseSyncService, "TIMER_PLANNED", "dueElapsed=$nextPoll", cycle)
                    pollTimerJob = serviceScope.launch {
                        delay((nextPoll - SystemClock.elapsedRealtime()).coerceAtLeast(1_000L))
                        Diagnostics.record(this@GlucoseSyncService, "TIMER_DELIVERED", "dueElapsed=$nextPoll lateMs=${SystemClock.elapsedRealtime() - nextPoll}")
                        doSyncAndScheduleNext("timer")
                    }
                }
            }
        }.also { it.start() }
    }

    private suspend fun reportCycleFailure(reason: DisconnectReason, message: String) {
        // Error reporting has a separate bounded budget, including its Data Layer status.
        withTimeoutOrNull(15_000L) {
            try {
                reportFailure(reason, message)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Diagnostics.record(this@GlucoseSyncService, "FAILURE_REPORT_ERROR", Diagnostics.error(failure), activeCycleId)
            }
            dataLayerSender.sendConnectionStatus(false, GlucoseSyncState.lastSyncTimestamp.value)
        } ?: Diagnostics.record(this, "FAILURE_REPORT_TIMEOUT", "limitMs=15000", activeCycleId)
    }

    private suspend fun syncOnce(): Boolean {
        val settings = settingsStore.getSettings()
        Diagnostics.record(this, "SYNC_STAGE", "latest_reading", activeCycleId)
        val result = fetchWithSessionRecovery(
            fetch = { activeApiStage = "latest"; apiClient.getLatestReading() },
            recoverSession = {
                Diagnostics.record(this, "SYNC_STAGE", "automatic_session_recovery", activeCycleId)
                val creds = credentialStore.getCredentials()
                if (creds?.email.isNullOrBlank() || creds?.password.isNullOrBlank()) {
                    LibreLinkUpClient.ApiResult.LoginRequired
                } else {
                    activeApiStage = "login"
                    val login = apiClient.login(creds!!.email!!, creds.password!!, creds.region)
                    if (login is LibreLinkUpClient.ApiResult.Success) {
                        val session = login.data
                        credentialStore.saveCredentials(session.token, session.userId, session.region,
                            session.expires, session.patientId, creds.email, creds.password)
                    }
                    login
                }
            }
        )
        when (result) {
            is LibreLinkUpClient.ApiResult.Success -> {
                val now = System.currentTimeMillis()
                val previousSync = GlucoseSyncState.lastSyncTimestamp.value
                // A graph download must never delay the alarm for the latest measurement.
                processReading(result.data, persistReading = true)
                val fresh = result.data.isFreshAt(now, settings.staleAfterMillis)
                if (!fresh) {
                    reportFailure(DisconnectReason.API_ERROR,
                        "Brak świeżych pomiarów LibreLinkUp — ostatni pomiar ma ${result.data.getMinutesAgo()} min")
                    dataLayerSender.sendGlucoseHistory(GlucoseSyncState.history.value)
                    return false
                }
                val needsGraph = lastGraphFetch == 0L || now - lastGraphFetch >= 60 * 60_000L ||
                    now - previousSync > settings.pollIntervalMinutes.coerceIn(1, 5) * 2 * 60_000L
                if (needsGraph) {
                    Diagnostics.record(this, "SYNC_STAGE", "history_download", activeCycleId)
                    activeApiStage = "history"
                    when (val graph = withTimeoutOrNull(30_000L) { apiClient.getGraphData() }) {
                        is LibreLinkUpClient.ApiResult.Success -> {
                            GlucoseSyncState.history.value = GlucoseHistoryPolicy.merge(
                                GlucoseSyncState.history.value, graph.data, System.currentTimeMillis())
                            lastGraphFetch = System.currentTimeMillis()
                            activeAccountId?.let { historyStore.save(it, GlucoseSyncState.history.value) }
                        }
                        else -> {
                            Diagnostics.record(this, "HISTORY_ERROR", "stage=history outcome=${if (graph == null) "timeout" else "unavailable"}", activeCycleId)
                            Log.w(TAG, "Historia niedostępna; bieżący odczyt i alarm zostały już obsłużone")
                        }
                    }
                    activeApiStage = "latest"
                }
                dataLayerSender.sendGlucoseHistory(GlucoseSyncState.history.value)
                dataLayerSender.sendSettings(settings)
                return true
            }
            is LibreLinkUpClient.ApiResult.LoginRequired ->
                reportFailure(DisconnectReason.AUTH_ERROR, "LibreLinkUp odrzucił sesję — sprawdź zapisane dane logowania")
            is LibreLinkUpClient.ApiResult.Error -> {
                Diagnostics.record(this, "API_ERROR", "HTTP=${result.code}", activeCycleId)
                reportFailure(DisconnectReason.API_ERROR, result.message)
            }
            is LibreLinkUpClient.ApiResult.NetworkError ->
                reportFailure(networkFailureReason(), "${result.message} ${networkDiagnostics()}")
        }
        return false
    }

    private fun networkDiagnostics(): String =
        "Android: networkBlocked=${networkBlocked ?: "nieznane"}; ${Diagnostics.snapshot(this)}"

    private fun networkFailureReason(): DisconnectReason =
        if (networkBlocked == true) DisconnectReason.ANDROID_NETWORK_BLOCKED else DisconnectReason.NETWORK_ERROR

    private suspend fun reportFailure(reason: DisconnectReason, message: String) {
        Log.w(TAG, message)
        Diagnostics.record(this, "SYNC_FAILURE", "reason=$reason; $message", activeCycleId)
        val shouldNotify = !isCurrentlyDisconnected
        isCurrentlyDisconnected = true
        handleDisconnect(reason)
        val notificationText = when (reason) {
            DisconnectReason.NETWORK_ERROR, DisconnectReason.ANDROID_NETWORK_BLOCKED ->
                "Brak połączenia z internetem. Aplikacja ponawia pobieranie."
            DisconnectReason.AUTH_ERROR -> "Nie można odnowić połączenia z kontem. Sprawdź konto w ustawieniach."
            else -> "Brak nowych danych glukozy. Aplikacja ponawia pobieranie."
        }
        GlucoseSyncState.syncStatusText.value = notificationText
        GlucoseSyncState.lastError.value = message
        updateNotification(notificationText, null)
        val settings = settingsStore.getSettings()
        if (shouldNotify && settings.notifyDisconnect) {
            sendAlertNotification("Brak nowych danych glukozy", notificationText,
                settings.disconnectSoundUri, ALERT_DISCONNECT_CHANNEL_ID)
        }
    }

    private suspend fun processReading(
        rawReading: GlucoseReading,
        statusMsg: String? = null,
        fromCache: Boolean = false,
        persistReading: Boolean = false
    ) = readingMutex.withLock {
        val settings = settingsStore.getSettings()
        val lang = settings.language
        val now = System.currentTimeMillis()
        val previousReading = GlucoseSyncState.latestReading.value
        GlucoseSyncState.history.value = GlucoseHistoryPolicy.merge(
            GlucoseSyncState.history.value, listOf(rawReading), now)
        if (previousReading != null && rawReading.timestamp <= previousReading.timestamp) {
            if (rawReading.timestamp == previousReading.timestamp && rawReading.isFreshAt(now, settings.staleAfterMillis) && !fromCache) {
                GlucoseSyncState.lastSyncTimestamp.value = now
                GlucoseSyncState.lastError.value = null
                isCurrentlyDisconnected = false
                cancelAlert(ALERT_DISCONNECT_CHANNEL_ID)
                if (persistReading) activeAccountId?.let {
                    historyStore.save(it, GlucoseSyncState.history.value)
                    Diagnostics.record(this, "MEASUREMENT_SAVED", "duplicate=true timestamp=${rawReading.timestamp}", activeCycleId)
                }
                handleConnect()
            }
            Diagnostics.record(this, "MEASUREMENT_IGNORED", "duplicateOrOlder=true timestamp=${rawReading.timestamp} ageMs=${now - rawReading.timestamp}", activeCycleId)
            return@withLock
        }

        val reading = rawReading.copy(measurementColor = when {
            rawReading.value < settings.watchLowThreshold -> MeasurementColor.LOW
            rawReading.value > settings.watchVeryHighThreshold -> MeasurementColor.VERY_HIGH
            rawReading.value > settings.watchHighThreshold -> MeasurementColor.HIGH
            else -> MeasurementColor.IN_RANGE
        })
        val fresh = reading.isFreshAt(now, settings.staleAfterMillis)
        GlucoseSyncState.latestReading.value = reading
        if (persistReading) {
            activeAccountId?.let {
                historyStore.save(it, GlucoseSyncState.history.value, recordSuccessfulSync = fresh)
                Diagnostics.record(this, "MEASUREMENT_SAVED", "duplicate=false timestamp=${reading.timestamp} fresh=$fresh", activeCycleId)
            }
        }
        GlucoseSyncState.syncStatusText.value = statusMsg ?: trStr(lang, "Pobrano z LibreLinkUp", "Fetched from LibreLinkUp")
        if (!fromCache && fresh) {
            GlucoseSyncState.lastError.value = null
            GlucoseSyncState.lastSyncTimestamp.value = now
            isCurrentlyDisconnected = false
            cancelAlert(ALERT_DISCONNECT_CHANNEL_ID)
        }

        Diagnostics.record(this, "MEASUREMENT", "timestamp=${reading.timestamp} ageMs=${now - reading.timestamp} fresh=$fresh cache=$fromCache", activeCycleId)
        val range = GlucoseAlertPolicy.range(reading.value, settings.lowThreshold, settings.highThreshold)
        if (!fresh || range == GlucoseRange.NORMAL || !settings.notifyOutOfRange) {
            cancelGlucoseAlerts()
            phoneAlertContext = null
        }
        if (fresh && range != GlucoseRange.NORMAL && settings.notifyOutOfRange) {
            val newHazard = GlucoseAlertPolicy.shouldAlert(glucoseRange, range)
            val muted = PhoneGlucoseAlertPolicy.shouldMute(settings, watchConnected,
                WatchState.isWornOnWrist.value, WatchState.lastUpdateTimestamp.value, now)
            val context = "${range.name}:${settings.isNightTime()}:$muted"
            if (newHazard || context != phoneAlertContext) {
                cancelGlucoseAlerts()
                val low = range == GlucoseRange.LOW
                val channel = if (low) ALERT_LOW_CHANNEL_ID else ALERT_HIGH_CHANNEL_ID
                val sound = if (low) settings.lowGlucoseSoundUri else settings.highGlucoseSoundUri
                sendAlertNotification("Uwaga na glukozę!",
                    "Ostatni odczyt to ${GlucoseFormatter.formatWithUnit(reading.value, settings.unit)} (${if (low) "Niski" else "Wysoki"})",
                    sound, channel)
                phoneAlertContext = context
            }
        }
        val transition = TrendHaptics.nextDirection(reading, previousReading, now, settings)
        if (transition != null && TrendHaptics.shouldSendAlert(settings, transition.hapticType, settings.watchNotifyOutOfRange)) {
            // STABLE also clears a notification when its optional vibration is disabled.
            dataLayerSender.sendHapticAlert(transition.hapticType, reading.value,
                showNotification = settings.watchNotifyOutOfRange,
                pattern = TrendHaptics.patternFor(settings, transition),
                measurementTimestamp = reading.timestamp)
        }
        glucoseRange = if (fresh) range else GlucoseRange.NORMAL

        // Persisted history makes the next service instance re-evaluate a pending fresh hazard.
        if (!fromCache && fresh) handleConnect()
        dataLayerSender.sendGlucoseReading(reading)
        try {
            com.mohgwatch.phone.widget.WidgetUpdateHelper.updateAllWidgets(this)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "Failed to update widgets", error)
        }
        val wristStatus = WatchState.statusAt(now)
        val wristSuffix = if (settings.showWristStatusInNotification) {
            "  •  " + trStr(lang, wristStatus.polishLabel, wristStatus.englishLabel)
        } else ""
        updateNotification("${trStr(lang, "Glukoza:", "Glucose:")} ${GlucoseFormatter.formatWithUnit(reading.value, settings.unit)} ${reading.trendArrow.symbol}$wristSuffix",
            GlucoseFormatter.formatMinutesAgo(reading.getMinutesAgo()))
    }

    private suspend fun handleDisconnect(reason: com.mohgwatch.core.model.DisconnectReason) {
        val logs = logsStore.logsFlow.first().toMutableList()
        val activeLog = logs.find { it.reconnectTime == null }
        if (activeLog == null) {
            logs.add(com.mohgwatch.core.model.DisconnectLog(disconnectTime = System.currentTimeMillis(), reason = reason))
            logsStore.saveLogs(logs)
        }
    }

    private suspend fun handleConnect() {
        val logs = logsStore.logsFlow.first().toMutableList()
        val activeLogIndex = logs.indexOfLast { it.reconnectTime == null }
        if (activeLogIndex != -1) {
            val activeLog = logs[activeLogIndex]
            val reconnectTime = System.currentTimeMillis()
            val duration = reconnectTime - activeLog.disconnectTime
            if (duration >= 5 * 60 * 1000) {
                logs[activeLogIndex] = activeLog.copy(reconnectTime = reconnectTime)
                val filtered = logs.filter { it.reconnectTime != null }.takeLast(100)
                logsStore.saveLogs(filtered)
            } else {
                logs.removeAt(activeLogIndex)
                logsStore.saveLogs(logs)
            }
        }
    }

    private fun stopSync() {
        stopping = true
        Diagnostics.record(this, "SYNC_STOP", "explicit request", activeCycleId)
        GlucoseSyncState.isSyncing.value = false
        pollTimerJob?.cancel()
        pollTimerJob = null
        syncJob?.cancel()
        syncJob = null
        SyncScheduler.cancel(this)
        SyncWatchdogWorker.cancel(this)
        cancelSound()
        GlucoseSyncState.syncStatusText.value = "Synchronizacja zatrzymana"
        updatePersistentWakeLock(false)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)

            val alertLowChannel = NotificationChannel(
                ALERT_LOW_CHANNEL_ID,
                "Alert Niskiego Stężenia Glukozy",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarmy ostrzegające o spadku stężenia glukozy"
                setSound(null, null)
                enableVibration(true)
            }
            manager.createNotificationChannel(alertLowChannel)

            val alertHighChannel = NotificationChannel(
                ALERT_HIGH_CHANNEL_ID,
                "Alert Wysokiego Stężenia Glukozy",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarmy ostrzegające o wzroście stężenia glukozy"
                setSound(null, null)
                enableVibration(true)
            }
            manager.createNotificationChannel(alertHighChannel)

            val alertDisconnectChannel = NotificationChannel(
                ALERT_DISCONNECT_CHANNEL_ID,
                "Alert Połączenia",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarmy dotyczące rozłączenia z serwerami"
                setSound(null, null)
                enableVibration(true)
            }
            manager.createNotificationChannel(alertDisconnectChannel)

            // Delete old channel
            manager.deleteNotificationChannel("mohgwatch_alerts")
            manager.deleteNotificationChannel("mohgwatch_alerts_silent")
        }
    }

    private fun buildNotification(title: String, subtitle: String?): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.sync_notification_title))
            .setContentText(title)
            .apply { if (subtitle != null) setSubText(subtitle) }
            .setSmallIcon(R.drawable.ic_glucose)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)

        return builder.build()
    }

    private fun updateNotification(title: String, subtitle: String?) {
        val notification = buildNotification(title, subtitle)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    @Synchronized private fun cancelSound() {
        soundJob?.cancel()
        soundJob = null
        soundAlertId = null
        soundChannel = null
    }

    private fun cancelAlert(channel: String) {
        alertIds.remove(channel)?.let { getSystemService(NotificationManager::class.java).cancel(it) }
        if (soundChannel == channel) cancelSound()
    }

    private fun cancelGlucoseAlerts() {
        cancelAlert(ALERT_LOW_CHANNEL_ID)
        cancelAlert(ALERT_HIGH_CHANNEL_ID)
    }

    @Synchronized private fun sendAlertNotification(
        title: String, content: String, soundUriString: String?, channelId: String, forceMode: String? = null
    ) {
        if (destroyed || !serviceScope.isActive) return
        cancelAlert(channelId)
        cancelSound()
        val alertId = nextAlertId.incrementAndGet()
        alertIds[channelId] = alertId
        val pendingIntent = PendingIntent.getActivity(this, alertId, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val muteIntent = Intent(this, GlucoseSyncService::class.java).setAction(ACTION_MUTE_ALERT)
            .putExtra("alert_id", alertId)
        val mutePendingIntent = PendingIntent.getService(this, alertId, muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(if (forceMode != null) "$title (Test)" else title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_glucose)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSilent(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Pomiń (Wycisz)", mutePendingIntent)
            .setDeleteIntent(mutePendingIntent)
            .build()
        getSystemService(NotificationManager::class.java).notify(alertId, notification)
        val pm = getSystemService(android.os.PowerManager::class.java)
        val wakeLock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "MohgWatch::AlertWakeLock")
        wakeLock.setReferenceCounted(false)
        wakeLock.acquire(2 * 60_000L)
        Diagnostics.record(this, "WAKELOCK_ACQUIRED", "type=alert timeoutMs=120000 held=${wakeLock.isHeld}")
        soundAlertId = alertId
        soundChannel = channelId
        val job = serviceScope.launch(Dispatchers.Main.immediate, start = CoroutineStart.LAZY) {
            try {
                // Serialize cleanup and playback so cancelling an old alert cannot restore
                // its volume while a new alert is already playing.
                soundMutex.withLock {
                    val initial = settingsStore.getSettings()
                    if (forceMode == null) delay(initial.notificationSoundDelaySeconds.coerceIn(0, 30) * 1000L)
                    val settings = settingsStore.getSettings()
                    if (forceMode == null) {
                        val expected = when (channelId) {
                            ALERT_LOW_CHANNEL_ID -> GlucoseRange.LOW
                            ALERT_HIGH_CHANNEL_ID -> GlucoseRange.HIGH
                            else -> null
                        }
                        val connected = withTimeoutOrNull(2_000L) { dataLayerSender.isWatchConnected() } ?: false
                        val now = System.currentTimeMillis()
                        val reading = GlucoseSyncState.latestReading.value
                        if (!PhoneGlucoseAlertPolicy.shouldPlaySound(settings, reading, expected, connected, WatchState.isWornOnWrist.value,
                                WatchState.lastUpdateTimestamp.value, now)) {
                            Diagnostics.record(this@GlucoseSyncService, "PHONE_ALERT_SOUND_SKIPPED", "channel=$channelId")
                            return@withLock
                        }
                    }
                    val volume = when (forceMode) {
                        "day" -> settings.notificationVolume
                        "night" -> settings.nightNotificationVolume
                        else -> if (settings.isNightTime()) settings.nightNotificationVolume else settings.notificationVolume
                    }.coerceIn(0f, 1f)
                    if (volume <= 0f) return@withLock
                    val audio = getSystemService(android.media.AudioManager::class.java)
                    val originalVolume = audio.getStreamVolume(android.media.AudioManager.STREAM_ALARM)
                    val player = android.media.MediaPlayer()
                    try {
                        player.setAudioAttributes(android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                        val uri = soundUriString?.let { android.net.Uri.parse(it) }
                            ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                        player.setDataSource(this@GlucoseSyncService, uri)
                        player.setVolume(volume, volume)
                        audio.setStreamVolume(android.media.AudioManager.STREAM_ALARM,
                            audio.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM), 0)
                        withTimeoutOrNull(60_000L) {
                            suspendCancellableCoroutine<Unit> { continuation ->
                                player.setOnCompletionListener {
                                    if (continuation.isActive) continuation.resume(Unit)
                                }
                                player.setOnErrorListener { _, what, extra ->
                                    Log.e(TAG, "Błąd odtwarzania alarmu: $what/$extra")
                                    if (continuation.isActive) continuation.resume(Unit)
                                    true
                                }
                                player.setOnPreparedListener {
                                    if (continuation.isActive) {
                                        try { it.start() } catch (error: Exception) {
                                            if (continuation.isActive) continuation.resumeWithException(error)
                                        }
                                    }
                                }
                                player.prepareAsync()
                            }
                        }
                    } finally {
                        runCatching { player.setOnCompletionListener(null) }
                        runCatching { player.setOnPreparedListener(null) }
                        runCatching { player.setOnErrorListener(null) }
                        runCatching { player.release() }
                        runCatching { audio.setStreamVolume(android.media.AudioManager.STREAM_ALARM, originalVolume, 0) }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e(TAG, "Nie udało się odtworzyć dźwięku alarmu", error)
            } finally {
                val held = wakeLock.isHeld
                if (held) wakeLock.release()
                Diagnostics.record(this@GlucoseSyncService, "WAKELOCK_RELEASED", "type=alert wasHeld=$held held=${wakeLock.isHeld}")
            }
        }
        soundJob = job
        job.invokeOnCompletion {
            if (wakeLock.isHeld) runCatching { wakeLock.release() }
        }
        job.start()
    }
}
