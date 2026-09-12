package com.mohgwatch.phone.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import com.mohgwatch.phone.widget.MohgWatchWidget
import com.mohgwatch.core.api.LibreLinkUpClient
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.util.GlucoseFormatter
import com.mohgwatch.phone.MainActivity
import com.mohgwatch.phone.R
import com.mohgwatch.phone.data.CredentialStore
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.service.DataLayerSender
import com.mohgwatch.phone.util.trStr
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

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
        const val ACTION_START = "com.mohgwatch.START_SYNC"
        const val ACTION_STOP = "com.mohgwatch.STOP_SYNC"
        const val ACTION_TEST_ALERT_LOW = "com.mohgwatch.TEST_ALERT_LOW"
        const val ACTION_TEST_ALERT_HIGH = "com.mohgwatch.TEST_ALERT_HIGH"
        const val ACTION_TEST_ALERT_DISCONNECT = "com.mohgwatch.TEST_ALERT_DISCONNECT"
        const val ACTION_TEST_SYNC = "com.mohgwatch.TEST_SYNC"
        const val ACTION_FORCE_SYNC = "com.mohgwatch.FORCE_SYNC"
        const val ACTION_ALARM_WAKEUP = "com.mohgwatch.ALARM_WAKEUP"
        const val ACTION_MUTE_ALERT = "com.mohgwatch.MUTE_ALERT"
        
        const val ACTION_CLEAR_NOTIFICATIONS = "com.mohgwatch.CLEAR_NOTIFICATIONS"

        fun start(context: Context) {
            val intent = Intent(context, GlucoseSyncService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GlucoseSyncService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
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
        fun testAlertStaleDataWatch(context: Context) {
            context.startService(Intent(context, GlucoseSyncService::class.java).apply { action = "com.mohgwatch.TEST_ALERT_STALE_WATCH" })
        }
        
        fun clearNotifications(context: Context) {
            context.startService(Intent(context, GlucoseSyncService::class.java).apply { action = ACTION_CLEAR_NOTIFICATIONS })
        }

        fun testSyncStatus(context: Context) {
            val intent = Intent(context, GlucoseSyncService::class.java).apply {
                action = ACTION_TEST_SYNC
            }
            context.startService(intent)
        }

        fun forceSync(context: Context) {
            val intent = Intent(context, GlucoseSyncService::class.java).apply {
                action = ACTION_FORCE_SYNC
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val apiClient = LibreLinkUpClient()
    private lateinit var dataLayerSender: DataLayerSender
    private lateinit var credentialStore: CredentialStore
    private lateinit var settingsStore: SettingsStore
    private lateinit var logsStore: com.mohgwatch.phone.data.LogsStore

    private var syncJob: Job? = null
    
    // State tracking for alerts
    private var isCurrentlyOutOfRange = false
    private var isCurrentlyDisconnected = false
    private val mutedAlerts = java.util.concurrent.CopyOnWriteArraySet<Int>()
    private val activeMediaPlayers = java.util.concurrent.CopyOnWriteArrayList<android.media.MediaPlayer>()
    private var lastClearedTimestamp = 0L

    private val alertActionReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_MUTE_ALERT) {
                val alertId = intent.getIntExtra("alert_id", -1)
                if (alertId != -1) {
                    mutedAlerts.add(alertId)
                    activeMediaPlayers.forEach { 
                        try {
                            if (it.isPlaying) it.stop()
                            it.release()
                        } catch(e:Exception){} 
                    }
                    activeMediaPlayers.clear()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        dataLayerSender = DataLayerSender(this)
        credentialStore = CredentialStore(this)
        settingsStore = SettingsStore(this)
        logsStore = com.mohgwatch.phone.data.LogsStore(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSync()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_MUTE_ALERT -> {
                val alertId = intent.getIntExtra("alert_id", -1)
                if (alertId != -1) {
                    mutedAlerts.add(alertId)
                    activeMediaPlayers.forEach { 
                        try {
                            if (it.isPlaying) it.stop()
                            it.release()
                        } catch(e:Exception){} 
                    }
                    activeMediaPlayers.clear()
                }
                return START_STICKY
            }
            ACTION_CLEAR_NOTIFICATIONS -> {
                val manager = getSystemService(NotificationManager::class.java)
                manager.cancelAll()
                activeMediaPlayers.forEach { 
                    try {
                        if (it.isPlaying) it.stop()
                        it.release()
                    } catch(e:Exception){} 
                }
                activeMediaPlayers.clear()
                lastClearedTimestamp = System.currentTimeMillis()
                return START_STICKY
            }
            ACTION_TEST_ALERT_LOW -> {
                val lang = runBlocking { settingsStore.getSettings().language }
                val forceMode = intent?.getStringExtra("force_mode")
                serviceScope.launch {
                    val settings = settingsStore.getSettings()
                    sendAlertNotification(
                        trStr(lang, "Testowy Niski", "Test Low"),
                        trStr(lang, "To jest alert testowy dla niskiego stężenia glukozy.", "This is a test low glucose alert."),
                        settings.lowGlucoseSoundUri,
                        ALERT_LOW_CHANNEL_ID,
                        forceMode
                    )
                }
                return START_STICKY
            }
            ACTION_TEST_ALERT_HIGH -> {
                val lang = runBlocking { settingsStore.getSettings().language }
                val forceMode = intent?.getStringExtra("force_mode")
                serviceScope.launch {
                    val settings = settingsStore.getSettings()
                    sendAlertNotification(
                        trStr(lang, "Testowy Wysoki", "Test High"),
                        trStr(lang, "To jest alert testowy dla wysokiego stężenia glukozy.", "This is a test high glucose alert."),
                        settings.highGlucoseSoundUri,
                        ALERT_HIGH_CHANNEL_ID,
                        forceMode
                    )
                }
                return START_STICKY
            }
            ACTION_TEST_ALERT_DISCONNECT -> {
                val lang = runBlocking { settingsStore.getSettings().language }
                val forceMode = intent?.getStringExtra("force_mode")
                serviceScope.launch {
                    val settings = settingsStore.getSettings()
                    sendAlertNotification(
                        trStr(lang, "Testowe Rozłączenie", "Test Disconnect"),
                        trStr(lang, "To jest alert testowy o błędzie połączenia.", "This is a test disconnect alert."),
                        settings.disconnectSoundUri,
                        ALERT_DISCONNECT_CHANNEL_ID,
                        forceMode
                    )
                }
                return START_STICKY
            }
            "com.mohgwatch.TEST_ALERT_STALE_WATCH" -> {
                serviceScope.launch {
                    dataLayerSender.sendStaleDataAlert(6)
                }
                return START_STICKY
            }
            ACTION_TEST_SYNC -> {
                val lang = runBlocking { settingsStore.getSettings().language }
                updateNotification(
                    trStr(lang, "Testowa Synchronizacja", "Test Sync"),
                    trStr(lang, "To jest przykładowe powiadomienie o statusie.", "This is a sample status notification.")
                )
                return START_STICKY
            }
            ACTION_FORCE_SYNC -> {
                startForegroundWithNotification()
                doSyncAndScheduleNext()
                return START_STICKY
            }
            ACTION_ALARM_WAKEUP -> {
                startForegroundWithNotification()
                if (GlucoseSyncState.isSyncing.value) {
                    doSyncAndScheduleNext()
                }
                return START_STICKY
            }
            else -> {
                startForegroundWithNotification()
                GlucoseSyncState.isSyncing.value = true
                doSyncAndScheduleNext()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterReceiver(alertActionReceiver)
        serviceScope.cancel()
        GlucoseSyncState.isSyncing.value = false
        super.onDestroy()
    }

    private fun startForegroundWithNotification() {
        val lang = runBlocking { settingsStore.getSettings().language }
        val notification = buildNotification(trStr(lang, "Uruchamianie synchronizacji…", "Starting sync..."), null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun scheduleNextSync(intervalMs: Long) {
        val alarmManager = getSystemService(AlarmManager::class.java)
        val intent = Intent(this, GlucoseSyncService::class.java).apply {
            action = ACTION_ALARM_WAKEUP
        }
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val now = System.currentTimeMillis()
        val triggerTime = now - (now % intervalMs) + intervalMs

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Brak uprawnień do dokładnych alarmów", e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        }
    }

    private fun doSyncAndScheduleNext() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "MohgWatch::SyncWakeLock")
            wakeLock.acquire(3 * 60 * 1000L) // 3 minuty max timeout
            
            var intervalMs = 5 * 60_000L // Domyślny interwał awaryjny
            
            try {
                val settings = settingsStore.getSettings()
                intervalMs = settings.pollIntervalMinutes * 60_000L
                val lang = settings.language
                
                val creds = credentialStore.getCredentials()
                if (creds == null) {
                    val errorMsg = trStr(lang, "Brak danych logowania. Zaloguj się w zakładce Logowanie!", "No login data. Please sign in in the Login tab!")
                    GlucoseSyncState.syncStatusText.value = errorMsg
                    GlucoseSyncState.lastError.value = errorMsg
                    updateNotification(trStr(lang, "Brak danych logowania — otwórz aplikację", "No login data — open app"), null)
                    return@launch
                }
    
                if (!apiClient.isAuthenticated()) {
                    apiClient.restoreSession(
                        token = creds.token,
                        userId = creds.userId,
                        region = creds.region,
                        expires = creds.expires,
                        patientId = creds.patientId
                    )
                }
    
                try {
                    syncOnce(settings.alertLowThreshold, settings.alertHighThreshold)
                } catch (e: Exception) {
                    Log.e(TAG, "Wyjątek w synchronizacji", e)
                    GlucoseSyncState.lastError.value = e.localizedMessage
                    GlucoseSyncState.syncStatusText.value = "Błąd: ${e.localizedMessage}"
                    updateNotification(trStr(lang, "Błąd synchronizacji: ${e.message}", "Sync error: ${e.message}"), null)
                    
                    if (settings.notifyDisconnect && !isCurrentlyDisconnected) {
                        isCurrentlyDisconnected = true
                        sendAlertNotification(trStr(lang, "Rozłączono", "Disconnected"), trStr(lang, "Aplikacja natrafiła na błąd: ${e.message}", "App encountered an error: ${e.message}"), settings.disconnectSoundUri, ALERT_DISCONNECT_CHANNEL_ID)
                    }
                }
    
                val latestReading = GlucoseSyncState.latestReading.value
                if (latestReading != null && settings.notifyStaleDataWatch) {
                    val stalenessMinutes = (System.currentTimeMillis() - latestReading.timestamp) / 60000L
                    if (stalenessMinutes >= 5) {
                        dataLayerSender.sendStaleDataAlert(stalenessMinutes)
                    }
                }
    
                dataLayerSender.sendConnectionStatus(
                    connected = true,
                    lastSync = System.currentTimeMillis()
                )
            } finally {
                if (GlucoseSyncState.isSyncing.value) {
                    scheduleNextSync(intervalMs)
                }
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }
    }

    private suspend fun syncOnce(alertLowThreshold: Float, alertHighThreshold: Float) {
        val lang = settingsStore.getSettings().language
        val settings = settingsStore.getSettings()
        when (val result = apiClient.getLatestReading()) {
            is LibreLinkUpClient.ApiResult.Success -> {
                var reading = result.data
                
                val currentHistory = GlucoseSyncState.history.value.toMutableList()
                when (val graphResult = apiClient.getGraphData()) {
                    is LibreLinkUpClient.ApiResult.Success -> {
                        if (graphResult.data.isNotEmpty()) {
                            currentHistory.clear()
                            currentHistory.addAll(graphResult.data.map { r ->
                                r.copy(
                                    measurementColor = when {
                                        r.value < settings.watchLowThreshold -> com.mohgwatch.core.model.MeasurementColor.LOW
                                        r.value > settings.watchVeryHighThreshold -> com.mohgwatch.core.model.MeasurementColor.VERY_HIGH
                                        r.value > settings.watchHighThreshold -> com.mohgwatch.core.model.MeasurementColor.HIGH
                                        else -> com.mohgwatch.core.model.MeasurementColor.IN_RANGE
                                    }
                                )
                            })
                        }
                    }
                    else -> {}
                }
                
                val now = System.currentTimeMillis()
                currentHistory.removeAll { it.timestamp < now - 24 * 60 * 60 * 1000L }
                
                if (currentHistory.none { it.timestamp == reading.timestamp }) {
                    currentHistory.add(reading)
                    currentHistory.sortBy { it.timestamp }
                }
                
                GlucoseSyncState.history.value = currentHistory
                dataLayerSender.sendGlucoseHistory(currentHistory)
                
                val nowTimestamp = reading.timestamp
                val fifteenMinsAgo = nowTimestamp - (15 * 60 * 1000)
                
                val recentReadings = currentHistory.filter { it.timestamp >= fifteenMinsAgo }
                
                if (recentReadings.size >= 3) {
                    val startT = recentReadings.first().timestamp
                    var sumX = 0f
                    var sumY = 0f
                    var sumXY = 0f
                    var sumX2 = 0f
                    val n = recentReadings.size.toFloat()
                    
                    for (r in recentReadings) {
                        val x = (r.timestamp - startT) / 60000f
                        val y = r.value
                        sumX += x
                        sumY += y
                        sumXY += x * y
                        sumX2 += x * x
                    }
                    
                    val divisor = n * sumX2 - sumX * sumX
                    if (divisor != 0f) {
                        val rateOfChange = (n * sumXY - sumX * sumY) / divisor
                        val customTrend = when {
                            rateOfChange > 2f -> com.mohgwatch.core.model.TrendArrow.RISING_FAST
                            rateOfChange >= 1f -> com.mohgwatch.core.model.TrendArrow.RISING
                            rateOfChange > -1f -> com.mohgwatch.core.model.TrendArrow.STABLE
                            rateOfChange >= -2f -> com.mohgwatch.core.model.TrendArrow.FALLING
                            else -> com.mohgwatch.core.model.TrendArrow.FALLING_FAST
                        }
                        reading = reading.copy(trendArrow = customTrend)
                    }
                } else {
                    val pastReading = currentHistory.minByOrNull { Math.abs(it.timestamp - fifteenMinsAgo) }
                    if (pastReading != null && Math.abs(pastReading.timestamp - fifteenMinsAgo) <= 7 * 60 * 1000) {
                        val delta = reading.value - pastReading.value
                        val minutes = (reading.timestamp - pastReading.timestamp) / 60000f
                        if (minutes > 0) {
                            val rateOfChange = delta / minutes
                            val customTrend = when {
                                rateOfChange > 2f -> com.mohgwatch.core.model.TrendArrow.RISING_FAST
                                rateOfChange >= 1f -> com.mohgwatch.core.model.TrendArrow.RISING
                                rateOfChange > -1f -> com.mohgwatch.core.model.TrendArrow.STABLE
                                rateOfChange >= -2f -> com.mohgwatch.core.model.TrendArrow.FALLING
                                else -> com.mohgwatch.core.model.TrendArrow.FALLING_FAST
                            }
                            reading = reading.copy(trendArrow = customTrend)
                        }
                    }
                }
                
                reading = reading.copy(
                    measurementColor = when {
                        reading.value < settings.watchLowThreshold -> com.mohgwatch.core.model.MeasurementColor.LOW
                        reading.value > settings.watchVeryHighThreshold -> com.mohgwatch.core.model.MeasurementColor.VERY_HIGH
                        reading.value > settings.watchHighThreshold -> com.mohgwatch.core.model.MeasurementColor.HIGH
                        else -> com.mohgwatch.core.model.MeasurementColor.IN_RANGE
                    }
                )

                GlucoseSyncState.latestReading.value = reading
                GlucoseSyncState.syncStatusText.value = trStr(lang, "Pobrano z LibreLinkUp", "Fetched from LibreLinkUp")
                GlucoseSyncState.lastError.value = null
                GlucoseSyncState.lastSyncTimestamp.value = System.currentTimeMillis()
                isCurrentlyDisconnected = false 
                handleConnect()

                try {
                    MohgWatchWidget().updateAll(this)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update widget", e)
                }

                dataLayerSender.sendGlucoseReading(reading)

                val settings = settingsStore.getSettings()
                val rangeLabel = when {
                    reading.value < settings.lowThreshold -> trStr(lang, "⬇ Niski", "⬇ Low")
                    reading.value > settings.highThreshold -> trStr(lang, "⬆ Wysoki", "⬆ High")
                    else -> trStr(lang, "✓ W normie", "✓ In Range")
                }
                val minutesAgo = reading.getMinutesAgo()
                val timeText = GlucoseFormatter.formatMinutesAgo(minutesAgo)
                val title = "${trStr(lang, "Glukoza:", "Glucose:")} ${reading.value.toInt()} mg/dL ${reading.trendArrow.symbol}  •  $rangeLabel"
                updateNotification(title, timeText)

                if (reading.value < alertLowThreshold || reading.value > alertHighThreshold) {
                    if (settings.notifyOutOfRange && !isCurrentlyOutOfRange) {
                        isCurrentlyOutOfRange = true
                        val channel = if (reading.value < alertLowThreshold) ALERT_LOW_CHANNEL_ID else ALERT_HIGH_CHANNEL_ID
                        val soundUri = if (reading.value < alertLowThreshold) settings.lowGlucoseSoundUri else settings.highGlucoseSoundUri
                        sendAlertNotification("Uwaga na Glukozę!", "Ostatni odczyt to ${reading.value.toInt()} mg/dL ($rangeLabel)", soundUri, channel)
                    }
                } else {
                    isCurrentlyOutOfRange = false
                }
            }

            is LibreLinkUpClient.ApiResult.LoginRequired -> {
                handleDisconnect(com.mohgwatch.core.model.DisconnectReason.AUTH_ERROR)
                val msg = "Sesja wygasła — ponowne logowanie..."
                GlucoseSyncState.syncStatusText.value = msg
                updateNotification(msg, null)
                
                val settings = settingsStore.getSettings()
                if (settings.notifyDisconnect && !isCurrentlyDisconnected) {
                    isCurrentlyDisconnected = true
                    sendAlertNotification("Błąd autoryzacji", "Sesja wygasła, wymagane działanie.", settings.disconnectSoundUri, ALERT_DISCONNECT_CHANNEL_ID)
                }

                val creds = credentialStore.getCredentials()
                if (!creds?.email.isNullOrBlank() && !creds?.password.isNullOrBlank()) {
                    val loginResult = apiClient.login(creds!!.email!!, creds.password!!, creds.region)
                    if (loginResult is LibreLinkUpClient.ApiResult.Success) {
                        credentialStore.saveCredentials(
                            token = loginResult.data.token,
                            userId = loginResult.data.userId,
                            region = loginResult.data.region,
                            expires = loginResult.data.expires,
                            patientId = loginResult.data.patientId,
                            email = creds.email,
                            password = creds.password
                        )
                        syncOnce(alertLowThreshold, alertHighThreshold)
                        return
                    }
                }
                val failMsg = "Błąd autoryzacji — zaloguj się ponownie w aplikacji"
                GlucoseSyncState.syncStatusText.value = failMsg
                GlucoseSyncState.lastError.value = failMsg
                updateNotification(failMsg, null)
            }

            is LibreLinkUpClient.ApiResult.Error -> {
                handleDisconnect(com.mohgwatch.core.model.DisconnectReason.API_ERROR)
                GlucoseSyncState.syncStatusText.value = result.message
                GlucoseSyncState.lastError.value = result.message
                updateNotification("Błąd API: ${result.message}", null)
                
                val settings = settingsStore.getSettings()
                if (settings.notifyDisconnect && !isCurrentlyDisconnected) {
                    isCurrentlyDisconnected = true
                    sendAlertNotification("Błąd API LibreLinkUp", result.message, settings.disconnectSoundUri, ALERT_DISCONNECT_CHANNEL_ID)
                }
            }

            is LibreLinkUpClient.ApiResult.NetworkError -> {
                handleDisconnect(com.mohgwatch.core.model.DisconnectReason.NETWORK_ERROR)
                val netMsg = "Brak połączenia z serwerami LibreLinkUp"
                GlucoseSyncState.syncStatusText.value = netMsg
                GlucoseSyncState.lastError.value = netMsg
                updateNotification(netMsg, null)
                
                val settings = settingsStore.getSettings()
                if (settings.notifyDisconnect && !isCurrentlyDisconnected) {
                    isCurrentlyDisconnected = true
                    sendAlertNotification("Brak połączenia sieciowego", "Zegarek może nie wyświetlać nowych odczytów.", settings.disconnectSoundUri, ALERT_DISCONNECT_CHANNEL_ID)
                }
            }
        }
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
        syncJob?.cancel()
        syncJob = null
        val alarmManager = getSystemService(AlarmManager::class.java)
        val pendingIntent = PendingIntent.getService(this, 0, Intent(this, GlucoseSyncService::class.java).apply { action = ACTION_ALARM_WAKEUP }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
        GlucoseSyncState.isSyncing.value = false
        GlucoseSyncState.syncStatusText.value = "Synchronizacja zatrzymana"
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.sync_notification_title))
            .setContentText(title)
            .apply { if (subtitle != null) setSubText(subtitle) }
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(title: String, subtitle: String?) {
        val notification = buildNotification(title, subtitle)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun sendAlertNotification(title: String, content: String, soundUriString: String?, channelId: String, forceMode: String? = null) {
        val alertTime = System.currentTimeMillis()
        val alertId = System.currentTimeMillis().toInt()
        mutedAlerts.remove(alertId)
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            alertId,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val muteIntent = Intent(this, GlucoseSyncService::class.java).apply {
            action = ACTION_MUTE_ALERT
            putExtra("alert_id", alertId)
        }
        val mutePendingIntent = PendingIntent.getService(
            this,
            alertId,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Najpierw wysyłamy powiadomienie bez dźwięku z przyciskiem Pomiń
        val silentNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(if (forceMode != null) "$title (Test)" else title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Pomiń (Wycisz)", mutePendingIntent)
            .setDeleteIntent(mutePendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(alertId, silentNotification)
        
        // Czekamy konfigurawalny czas (lub natychmiast dla testu) i podmieniamy na głośne powiadomienie
        serviceScope.launch {
            val settingsFirst = settingsStore.getSettings()
            val delayMs = if (forceMode != null) 0L else settingsFirst.notificationSoundDelaySeconds * 1000L
            delay(delayMs)
            if (!mutedAlerts.contains(alertId) && alertTime >= lastClearedTimestamp) {
                val soundUri = if (soundUriString != null) android.net.Uri.parse(soundUriString) else android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                
                try {
                    val settings = settingsStore.getSettings()
                    
                    var volume = settings.notificationVolume
                    if (forceMode == "day") {
                        volume = settings.notificationVolume
                    } else if (forceMode == "night") {
                        volume = settings.nightNotificationVolume
                    } else if (settings.nightModeEnabled) {
                        val cal = java.util.Calendar.getInstance()
                        val currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                        val currentMin = cal.get(java.util.Calendar.MINUTE)
                        val currentMinutes = currentHour * 60 + currentMin
                        
                        val startParts = settings.nightStartTime.split(":")
                        val startMinutes = (startParts.getOrNull(0)?.toIntOrNull() ?: 22) * 60 + (startParts.getOrNull(1)?.toIntOrNull() ?: 0)
                        
                        val endParts = settings.nightEndTime.split(":")
                        val endMinutes = (endParts.getOrNull(0)?.toIntOrNull() ?: 7) * 60 + (endParts.getOrNull(1)?.toIntOrNull() ?: 0)
                        
                        val isNight = if (startMinutes > endMinutes) {
                            currentMinutes >= startMinutes || currentMinutes < endMinutes
                        } else {
                            currentMinutes in startMinutes..<endMinutes
                        }
                        
                        if (isNight) {
                            volume = settings.nightNotificationVolume
                        }
                    }

                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                    val originalVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM)
                    val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, maxVolume, 0)
                    
                    val mp = android.media.MediaPlayer().apply {
                        setDataSource(this@GlucoseSyncService, soundUri)
                        setAudioAttributes(
                            android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        setVolume(volume, volume)
                        setOnCompletionListener { 
                            audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, originalVolume, 0)
                            activeMediaPlayers.remove(this)
                            it.release() 
                        }
                        setOnErrorListener { _, _, _ ->
                            audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, originalVolume, 0)
                            activeMediaPlayers.remove(this)
                            false
                        }
                        prepare()
                        start()
                    }
                    activeMediaPlayers.add(mp)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to play custom notification sound", e)
                }

                val loudNotification = NotificationCompat.Builder(this@GlucoseSyncService, channelId)
                    .setContentTitle(if (forceMode != null) "$title (Test)" else title)
                    .setContentText(content)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Pomiń (Wycisz)", mutePendingIntent)
                    .setDeleteIntent(mutePendingIntent)
                    .build()
                manager.notify(alertId, loudNotification)
            }
        }
    }
}
