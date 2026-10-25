package com.mohgwatch.phone.data

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.util.Log
import com.mohgwatch.phone.service.GlucoseSyncState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/** Bounded local diagnostics. Sync and alarm callers never wait for disk writes. */
object Diagnostics {
    private val sessionId = UUID.randomUUID().toString()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var writer: Writer? = null
    private val droppedEvents = AtomicLong()
    private val writeFailures = AtomicLong()

    private sealed interface Command {
        data class Event(val event: DiagnosticEvent) : Command
        data class Flush(val completion: CompletableDeferred<Unit>) : Command
    }

    private class Writer(val context: Context) {
        val journal = DiagnosticJournal(File(context.noBackupFilesDir, "diagnostics"))
        val commands = Channel<Command>(256)

        init {
            scope.launch {
                write(event("PROCESS_SESSION_START", snapshot(context)))
                write(event("PROCESS_PREVIOUS_EXIT", previousExit(context)))
                var reportedDrops = 0L
                var reportedFailures = 0L
                for (command in commands) {
                    val drops = droppedEvents.get()
                    val failures = writeFailures.get()
                    if (drops != reportedDrops || failures != reportedFailures) {
                        write(event("DIAGNOSTICS_LOSS", "dropped=$drops writeFailures=$failures (bieżący proces)"))
                        if (writeFailures.get() == failures) {
                            reportedDrops = drops
                            reportedFailures = failures
                        }
                    }
                    when (command) {
                        is Command.Event -> write(command.event)
                        is Command.Flush -> command.completion.complete(Unit)
                    }
                }
            }
        }

        private fun write(event: DiagnosticEvent) {
            try {
                journal.append(event)
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                writeFailures.incrementAndGet()
                Log.w("Diagnostics", "Nie udało się zapisać diagnostyki: ${error(failure)}")
            }
        }

        suspend fun flush(): Boolean = withTimeoutOrNull(2_000L) {
            val completion = CompletableDeferred<Unit>()
            commands.send(Command.Flush(completion))
            completion.await()
            true
        } ?: false
    }

    private fun writer(context: Context): Writer = writer ?: synchronized(this) {
        writer ?: Writer(context.applicationContext).also { writer = it }
    }

    private fun event(name: String, details: String, cycleId: String? = null) = DiagnosticEvent(
        System.currentTimeMillis(), SystemClock.elapsedRealtime(), sessionId, Process.myPid(), cycleId,
        name, DiagnosticSanitizer.clean(details)
    )

    fun record(context: Context, event: String, details: String = "", cycleId: String? = null) {
        try {
            if (!writer(context).commands.trySend(Command.Event(event(event, details, cycleId))).isSuccess) {
                droppedEvents.incrementAndGet()
            }
        } catch (failure: Exception) {
            if (failure is CancellationException) throw failure
            writeFailures.incrementAndGet()
        }
    }

    /** Safe exception cause chain; response bodies, credentials and exception messages stay out. */
    fun error(error: Throwable): String = generateSequence(error) { it.cause }.take(6).joinToString(" <- ") {
        it.javaClass.simpleName + if (it is retrofit2.HttpException) "(HTTP ${it.code()})" else ""
    }

    fun snapshot(context: Context): String = buildString {
        val power = context.getSystemService(PowerManager::class.java)
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        append("app=${observed { context.packageManager.getPackageInfo(context.packageName, 0).versionName }}")
        append("; model=${Build.MANUFACTURER} ${Build.MODEL}; Android=${Build.VERSION.RELEASE}; API=${Build.VERSION.SDK_INT}")
        append("; screenInteractive=${observed { power?.isInteractive }}")
        append("; doze=${observed { power?.isDeviceIdleMode }}")
        append("; lightDoze=${if (Build.VERSION.SDK_INT >= 33) observed { power?.isDeviceLightIdleMode } else "nieznane (API<33)"}")
        append("; powerSave=${observed { power?.isPowerSaveMode }}")
        append("; lowPowerStandbyEnabled=${if (Build.VERSION.SDK_INT >= 33) observed { power?.isLowPowerStandbyEnabled } else "nieznane (API<33)"}")
        append("; lowPowerStandbyActive=nieznane (brak bezpośredniej obserwacji)")
        append("; lowPowerStandbyExempt=${if (Build.VERSION.SDK_INT >= 34) observed { power?.isExemptFromLowPowerStandby } else "nieznane (API<34)"}")
        append("; batteryExempt=${observed { power?.isIgnoringBatteryOptimizations(context.packageName) }}")
        append("; exactAlarm=${if (Build.VERSION.SDK_INT >= 31) observed { context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() } else "true (API<31)"}")
        append("; network=${observed {
            val network = connectivity?.activeNetwork ?: return@observed "brak aktywnej sieci"
            val capabilities = connectivity.getNetworkCapabilities(network) ?: return@observed "nieznane (brak capabilities)"
            listOf(
                NetworkCapabilities.TRANSPORT_WIFI to "Wi-Fi", NetworkCapabilities.TRANSPORT_CELLULAR to "komórkowa",
                NetworkCapabilities.TRANSPORT_ETHERNET to "Ethernet", NetworkCapabilities.TRANSPORT_VPN to "VPN",
                NetworkCapabilities.TRANSPORT_BLUETOOTH to "Bluetooth"
            ).filter { capabilities.hasTransport(it.first) }.joinToString("+") { it.second }.ifEmpty { "inny" } +
                ",validated=${capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}" +
                ",internetCapability=${capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}" +
                ",notRestricted=${capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)}" +
                ",metered=${connectivity.isActiveNetworkMetered}"
        }}")
        append("; backgroundData=${observed {
            when (connectivity?.restrictBackgroundStatus) {
                ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED -> "bez ograniczenia Data Saver"
                ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED -> "wyjątek Data Saver"
                ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED -> "ograniczone Data Saver"
                else -> "nieznane"
            }
        }}")
        append("; networkBlocked=nieznane (odczyt blokady wyłącznie w zdarzeniach callback Androida)")
    }

    suspend fun report(context: Context, hours: Int = 24): String = withContext(Dispatchers.IO) {
        val writer = writer(context)
        val flushed = writer.flush()
        val now = System.currentTimeMillis()
        writer.journal.report(now - hours.coerceIn(1, 168) * 3_600_000L, now, configuration(context, flushed))
    }

    suspend fun export(context: Context, hours: Int = 24): File = withContext(Dispatchers.IO) {
        val writer = writer(context)
        val flushed = writer.flush()
        val now = System.currentTimeMillis()
        val directory = File(context.cacheDir, "diagnostics").apply { mkdirs() }
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(now))
        val file = File(directory, "mohgwatch-logi-$stamp.txt")
        file.writeText(writer.journal.report(
            now - hours.coerceIn(1, 168) * 3_600_000L, now, configuration(context, flushed), Int.MAX_VALUE
        ), Charsets.UTF_8)
        directory.listFiles()?.filter { it.name.startsWith("mohgwatch-logi-") && it != file }
            ?.sortedByDescending { it.lastModified() }?.drop(2)?.forEach { it.delete() }
        file
    }

    private suspend fun configuration(context: Context, flushed: Boolean): String = buildString {
        appendLine(snapshot(context))
        val settings = readConfiguration { SettingsStore(context).getSettings() }
        if (settings == null) appendLine("Ustawienia: nieznane (timeout)") else {
            appendLine("sync=${settings.isSyncEnabled}; intervalMinutes=${settings.pollIntervalMinutes}; persistentWakeLockSetting=${settings.persistentWakeLockEnabled}")
            appendLine("alarmLow=${settings.lowThreshold}; alarmHigh=${settings.highThreshold}; phoneAlarm=${settings.notifyOutOfRange}; watchAlarm=${settings.watchNotifyOutOfRange}; watchStale=${settings.notifyStaleDataWatch}; staleMinutes=${settings.staleDataMinutes}")
            appendLine("watchColorLow=${settings.watchLowThreshold}; watchColorHigh=${settings.watchHighThreshold}")
            appendLine("hapticLowEnabled=${settings.alertLowHapticEnabled}; hapticHighEnabled=${settings.alertHighHapticEnabled}")
            appendLine("patternLow=${settings.trendFallingPattern}; patternHigh=${settings.trendRisingPattern}; patternReturn=${settings.trendStablePattern}; returnEnabled=${settings.trendStableEnabled}")
        }
        val lastSuccess = readConfiguration { GlucoseHistoryStore(context).lastSuccessfulSync() }
        appendLine("lastSuccessfulSync=${lastSuccess?.takeIf { it > 0 }?.let { DiagnosticJournal.formatTime(it) } ?: "nieznane"}")
        val reading = GlucoseSyncState.latestReading.value
        appendLine("measurementTime=${reading?.let { DiagnosticJournal.formatTime(it.timestamp) } ?: "nieznane"}; measurementAgeMs=${reading?.let { System.currentTimeMillis() - it.timestamp } ?: "nieznane"}")
        append("Kolejka opróżniona=$flushed; utracone w kolejce (bieżący proces)=${droppedEvents.get()}; błędy zapisu (bieżący proces)=${writeFailures.get()}")
    }

    private suspend fun <T> readConfiguration(read: suspend () -> T): T? = try {
        withTimeoutOrNull(2_000L) { read() }
    } catch (failure: Exception) {
        if (failure is CancellationException) throw failure
        null
    }

    private fun observed(read: () -> Any?): String = try {
        read()?.toString() ?: "nieznane"
    } catch (failure: Exception) {
        "nieznane (${failure.javaClass.simpleName})"
    }

    private fun previousExit(context: Context): String {
        if (Build.VERSION.SDK_INT < 30) return "nieznane (API<30)"
        return observed {
            val exits = context.getSystemService(ActivityManager::class.java)
                ?.getHistoricalProcessExitReasons(context.packageName, 0, 3)
            if (exits.isNullOrEmpty()) return@observed "nieznane (Android nie udostępnił poprzedniego zakończenia)"
            exits.joinToString("; ") {
                val reason = when (it.reason) {
                    ApplicationExitInfo.REASON_ANR -> "ANR"
                    ApplicationExitInfo.REASON_CRASH -> "CRASH"
                    ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
                    ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
                    ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
                    ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
                    ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
                    else -> "code_${it.reason}"
                }
                "Android observation: timestamp=${DiagnosticJournal.formatTime(it.timestamp)} pid=${it.pid} reason=$reason importance=${it.importance} pssKb=${it.pss} rssKb=${it.rss}"
            }
        }
    }
}
