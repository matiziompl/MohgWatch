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
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.util.GlucoseFormatter
import com.mohgwatch.phone.MainActivity
import com.mohgwatch.phone.R
import com.mohgwatch.phone.data.CredentialStore
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.service.DataLayerSender
import com.mohgwatch.phone.util.trStr
import kotlinx.coroutines.*

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
        const val ALERT_CHANNEL_ID = "mohgwatch_alerts"
        const val NOTIFICATION_ID = 1001
        const val ALERT_NOTIFICATION_ID = 1002
        const val ACTION_START = "com.mohgwatch.START_SYNC"
        const val ACTION_STOP = "com.mohgwatch.STOP_SYNC"
        const val ACTION_TEST_ALERT = "com.mohgwatch.TEST_ALERT"
        const val ACTION_TEST_SYNC = "com.mohgwatch.TEST_SYNC"
        const val ACTION_FORCE_SYNC = "com.mohgwatch.FORCE_SYNC"

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

        fun testAlert(context: Context) {
            val intent = Intent(context, GlucoseSyncService::class.java).apply {
                action = ACTION_TEST_ALERT
            }
            context.startService(intent)
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

    private var syncJob: Job? = null
    
    // State tracking for alerts
    private var isCurrentlyOutOfRange = false
    private var isCurrentlyDisconnected = false

    override fun onCreate() {
        super.onCreate()
        dataLayerSender = DataLayerSender(this)
        credentialStore = CredentialStore(this)
        settingsStore = SettingsStore(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSync()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TEST_ALERT -> {
                val lang = runBlocking { settingsStore.getSettings().language }
                sendAlertNotification(
                    trStr(lang, "Testowy Alert", "Test Alert"),
                    trStr(lang, "To jest przykładowy alert wywołany z ustawień.", "This is a sample alert from settings.")
                )
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
                if (syncJob == null || syncJob?.isActive == false) {
                    startSync()
                } else {
                    serviceScope.launch {
                        val settings = settingsStore.getSettings()
                        syncOnce(settings.lowThreshold, settings.highThreshold)
                    }
                }
                return START_STICKY
            }
            else -> {
                startForegroundWithNotification()
                startSync()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
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

    private fun startSync() {
        syncJob?.cancel()
        GlucoseSyncState.isSyncing.value = true

        syncJob = serviceScope.launch {
            // Przywróć sesję z zapisanych credentials
            val creds = credentialStore.getCredentials()
            val lang = settingsStore.getSettings().language
            if (creds == null) {
                val errorMsg = trStr(lang, "Brak danych logowania. Zaloguj się w zakładce Logowanie!", "No login data. Please sign in in the Login tab!")
                GlucoseSyncState.syncStatusText.value = errorMsg
                GlucoseSyncState.lastError.value = errorMsg
                updateNotification(trStr(lang, "Brak danych logowania — otwórz aplikację", "No login data — open app"), null)
                return@launch
            }

            apiClient.restoreSession(
                token = creds.token,
                userId = creds.userId,
                region = creds.region,
                expires = creds.expires,
                patientId = creds.patientId
            )

            // Główna pętla synchronizacji
            while (isActive) {
                val settings = settingsStore.getSettings()
                val intervalMs = settings.pollIntervalMinutes * 60_000L

                try {
                    syncOnce(settings.lowThreshold, settings.highThreshold)
                } catch (e: Exception) {
                    Log.e(TAG, "Wyjątek w pętli synchronizacji", e)
                    GlucoseSyncState.lastError.value = e.localizedMessage
                    GlucoseSyncState.syncStatusText.value = "Błąd: ${e.localizedMessage}"
                    updateNotification(trStr(lang, "Błąd synchronizacji: ${e.message}", "Sync error: ${e.message}"), null)
                    
                    if (settings.notifyDisconnect && !isCurrentlyDisconnected) {
                        isCurrentlyDisconnected = true
                        sendAlertNotification(trStr(lang, "Rozłączono", "Disconnected"), trStr(lang, "Aplikacja natrafiła na błąd: ${e.message}", "App encountered an error: ${e.message}"))
                    }
                }

                // Wyślij status połączenia na zegarek
                dataLayerSender.sendConnectionStatus(
                    connected = true,
                    lastSync = System.currentTimeMillis()
                )

                delay(intervalMs)
            }
        }
    }

    private suspend fun syncOnce(lowThreshold: Float, highThreshold: Float) {
        // Pobierz najnowszy odczyt
        val lang = settingsStore.getSettings().language
        when (val result = apiClient.getLatestReading()) {
            is LibreLinkUpClient.ApiResult.Success -> {
                var reading = result.data
                
                // Zbuduj historię na bieżąco (bez getGraphData) by unikać HTTP 429
                val currentHistory = GlucoseSyncState.history.value.toMutableList()
                
                // Dodaj nowy odczyt jeśli nie istnieje
                if (currentHistory.none { it.timestamp == reading.timestamp }) {
                    currentHistory.add(reading)
                }
                
                // Usuń starsze niż 24h
                val now = System.currentTimeMillis()
                currentHistory.removeAll { it.timestamp < now - 24 * 60 * 60 * 1000L }
                
                GlucoseSyncState.history.value = currentHistory
                dataLayerSender.sendGlucoseHistory(currentHistory)
                
                // Custom Trend Logic (15 min)
                val fifteenMinsAgo = reading.timestamp - (15 * 60 * 1000)
                val pastReading = currentHistory.minByOrNull { Math.abs(it.timestamp - fifteenMinsAgo) }
                
                if (pastReading != null && Math.abs(pastReading.timestamp - fifteenMinsAgo) <= 7 * 60 * 1000) {
                    val delta = reading.value - pastReading.value
                    val customTrend = when {
                        delta >= 30 -> com.mohgwatch.core.model.TrendArrow.RISING_FAST
                        delta >= 15 -> com.mohgwatch.core.model.TrendArrow.RISING
                        delta <= -30 -> com.mohgwatch.core.model.TrendArrow.FALLING_FAST
                        delta <= -15 -> com.mohgwatch.core.model.TrendArrow.FALLING
                        else -> com.mohgwatch.core.model.TrendArrow.STABLE
                    }
                    reading = reading.copy(trendArrow = customTrend)
                }

                GlucoseSyncState.latestReading.value = reading
                GlucoseSyncState.syncStatusText.value = trStr(lang, "Pobrano z LibreLinkUp", "Fetched from LibreLinkUp")
                GlucoseSyncState.lastError.value = null
                GlucoseSyncState.lastSyncTimestamp.value = System.currentTimeMillis()
                isCurrentlyDisconnected = false // Reset disconnect flag na sukces

                // Push na zegarek
                dataLayerSender.sendGlucoseReading(reading)

                // Aktualizuj notyfikację
                val rangeLabel = when {
                    reading.value < lowThreshold -> trStr(lang, "⬇ Niski", "⬇ Low")
                    reading.value > highThreshold -> trStr(lang, "⬆ Wysoki", "⬆ High")
                    else -> trStr(lang, "✓ W normie", "✓ In Range")
                }
                val minutesAgo = reading.getMinutesAgo()
                val timeText = GlucoseFormatter.formatMinutesAgo(minutesAgo)
                val title = "${trStr(lang, "Glukoza:", "Glucose:")} ${reading.value.toInt()} mg/dL ${reading.trendArrow.symbol}  •  $rangeLabel"
                updateNotification(title, timeText)

                // Alert Check
                val settings = settingsStore.getSettings()
                if (reading.value < lowThreshold || reading.value > highThreshold) {
                    if (settings.notifyOutOfRange && !isCurrentlyOutOfRange) {
                        isCurrentlyOutOfRange = true
                        sendAlertNotification("Uwaga na Glukozę!", "Ostatni odczyt to ${reading.value.toInt()} mg/dL ($rangeLabel)")
                    }
                } else {
                    isCurrentlyOutOfRange = false
                }
            }

            is LibreLinkUpClient.ApiResult.LoginRequired -> {
                val msg = "Sesja wygasła — ponowne logowanie..."
                GlucoseSyncState.syncStatusText.value = msg
                updateNotification(msg, null)
                
                val settings = settingsStore.getSettings()
                if (settings.notifyDisconnect && !isCurrentlyDisconnected) {
                    isCurrentlyDisconnected = true
                    sendAlertNotification("Błąd autoryzacji", "Sesja wygasła, wymagane działanie.")
                }

                // Spróbuj ponownie zalogować z zapisanych danych
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
                        // Retry sync
                        syncOnce(lowThreshold, highThreshold)
                        return
                    }
                }
                val failMsg = "Błąd autoryzacji — zaloguj się ponownie w aplikacji"
                GlucoseSyncState.syncStatusText.value = failMsg
                GlucoseSyncState.lastError.value = failMsg
                updateNotification(failMsg, null)
            }

            is LibreLinkUpClient.ApiResult.Error -> {
                GlucoseSyncState.syncStatusText.value = result.message
                GlucoseSyncState.lastError.value = result.message
                updateNotification("Błąd API: ${result.message}", null)
                
                val settings = settingsStore.getSettings()
                if (settings.notifyDisconnect && !isCurrentlyDisconnected) {
                    isCurrentlyDisconnected = true
                    sendAlertNotification("Błąd API LibreLinkUp", result.message)
                }
            }

            is LibreLinkUpClient.ApiResult.NetworkError -> {
                val netMsg = "Brak połączenia z serwerami LibreLinkUp"
                GlucoseSyncState.syncStatusText.value = netMsg
                GlucoseSyncState.lastError.value = netMsg
                updateNotification(netMsg, null)
                
                val settings = settingsStore.getSettings()
                if (settings.notifyDisconnect && !isCurrentlyDisconnected) {
                    isCurrentlyDisconnected = true
                    sendAlertNotification("Brak połączenia sieciowego", "Zegarek może nie wyświetlać nowych odczytów.")
                }
            }
        }
    }

    private fun stopSync() {
        syncJob?.cancel()
        syncJob = null
        GlucoseSyncState.isSyncing.value = false
        GlucoseSyncState.syncStatusText.value = "Synchronizacja zatrzymana"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Alerty Glukozy i Połączenia",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Ważne alarmy (poza zakresem, utrata połączenia)"
                enableVibration(true)
            }
            manager.createNotificationChannel(alertChannel)
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
    
    private fun sendAlertNotification(title: String, content: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(ALERT_NOTIFICATION_ID, notification)
    }
}
