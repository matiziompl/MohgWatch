package com.mohgwatch.phone.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.phone.companion.BondedCompanionDevice
import com.mohgwatch.phone.companion.CompanionIntegrationManager
import com.mohgwatch.phone.companion.CompanionPresence
import com.mohgwatch.phone.data.NightTestManager
import com.mohgwatch.phone.data.NightTestMode
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.service.GlucoseSyncService
import com.mohgwatch.phone.service.GlucoseSyncState
import com.mohgwatch.phone.ui.components.SettingsPage
import com.mohgwatch.phone.util.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsCompanionScreen(
    onBack: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val manager = remember(context) { CompanionIntegrationManager.get(context) }
    val nightManager = remember(context) { NightTestManager.get(context) }
    val settingsStore = remember(context) { SettingsStore(context) }
    val status by manager.state.collectAsState()
    val nightState by nightManager.state.collectAsState()
    val settings by settingsStore.settingsFlow.collectAsState(initial = UserSettings())
    val latestReading by GlucoseSyncState.latestReading.collectAsState()
    val syncStatus by GlucoseSyncState.syncStatusText.collectAsState()
    val syncing by GlucoseSyncState.isSyncing.collectAsState()
    val syncError by GlucoseSyncState.lastError.collectAsState()
    val lastSuccess by GlucoseSyncState.lastSyncTimestamp.collectAsState()
    var feedback by remember { mutableStateOf<String?>(null) }
    var bondedPicker by remember { mutableStateOf<List<BondedCompanionDevice>?>(null) }
    var removingAssociationId by remember { mutableStateOf<Int?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val operationFailed = tr("Nie udało się wykonać operacji. Odśwież stan i spróbuj ponownie.",
        "The operation failed. Refresh the status and try again.")
    val permissionDenied = tr("Brak zgody na dostęp do sparowanych urządzeń. Ustawienia synchronizacji pozostają bez zmian.",
        "Access to paired devices was denied. Synchronization settings are unchanged.")
    val associationCancelled = tr("Anulowano wybór w oknie systemowym. Ustawienia synchronizacji pozostają bez zmian.",
        "The system dialog was cancelled. Synchronization settings are unchanged.")
    val noBondedDevices = tr("Brak sparowanych urządzeń Bluetooth. Najpierw skonfiguruj zegarek w jego aplikacji.",
        "No paired Bluetooth devices. Set up the watch in its own app first.")

    fun perform(clearFeedback: Boolean = true, action: suspend () -> Unit) {
        scope.launch {
            if (clearFeedback) feedback = null
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                feedback = operationFailed
            }
        }
    }

    fun showBondedDevices() {
        perform {
            val devices = manager.bondedDevices()
            if (devices.isEmpty()) feedback = noBondedDevices else bondedPicker = devices
        }
    }

    val associationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val cancelled = result.resultCode != Activity.RESULT_OK
        manager.associationUiResult(cancelled = cancelled)
        perform {
            manager.refresh()
            if (cancelled) feedback = associationCancelled
        }
    }
    val bluetoothPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showBondedDevices() else feedback = permissionDenied
    }

    DisposableEffect(lifecycleOwner, manager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) perform(clearFeedback = false) { manager.refresh() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        perform { manager.refresh() }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000L)
        }
    }

    val selectedAssociation = status.associations.firstOrNull { it.id == status.selectedAssociationId }
    val canAct = status.ready && status.supported && !status.busy
    val unknown = tr("nieznany", "unknown")
    val nightRun = nightState.activeRun
    val currentMode = nightRun?.mode ?: when {
        !status.ready || !status.supported -> NightTestMode.UNKNOWN
        status.associations.isEmpty() -> NightTestMode.BASELINE
        status.observationRequested -> NightTestMode.OBSERVING
        else -> NightTestMode.ASSOCIATED
    }
    val modeLabel = when (currentMode) {
        NightTestMode.BASELINE -> tr("bez asocjacji (pomiar bazowy)", "no association (baseline)")
        NightTestMode.ASSOCIATED -> tr("asocjacja bez obserwacji", "association without observation")
        NightTestMode.OBSERVING -> tr("obserwacja CDM", "CDM observation")
        NightTestMode.UNKNOWN -> unknown
    }
    val presenceLabel = when (status.presence) {
        CompanionPresence.UNKNOWN -> tr("nieznana", "unknown")
        CompanionPresence.PRESENT -> tr("obecny", "present")
        CompanionPresence.ABSENT -> tr("nieobecny", "absent")
    }
    val dataSaverLabel = when (status.backgroundDataStatus) {
        "ENABLED" -> tr("włączone, aplikacja ograniczana", "enabled, app restricted")
        "WHITELISTED" -> tr("włączone, aplikacja ma wyjątek", "enabled, app exempt")
        "DISABLED" -> tr("wyłączone", "disabled")
        else -> unknown
    }

    SettingsPage(title = tr("CDM TEST", "CDM TEST"), onBack = onBack) {
        Text(tr("Eksperymentalna obserwacja obecności zegarka przez CompanionDeviceManager. Domyślnie wyłączona. Nie gwarantuje odczytu co 5 minut ani dostępu do sieci w uśpieniu.",
            "Experimental watch presence observation through CompanionDeviceManager. Off by default. It does not guarantee a reading every 5 minutes or network access during sleep."),
            style = MaterialTheme.typography.bodyMedium)

        if (!status.ready || status.busy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        feedback?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        status.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }

        CompanionCard(tr("Obserwacja CDM", "CDM observation")) {
            Text(tr("Android API ${Build.VERSION.SDK_INT}. Ten test wymaga API 36 i dostępnej obsługi CDM.",
                "Android API ${Build.VERSION.SDK_INT}. This test requires API 36 and available CDM support."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(when {
                !status.ready -> tr("Sprawdzanie obsługi…", "Checking support…")
                status.supported -> tr("Obsługa testu: dostępna", "Test support: available")
                else -> tr("Obsługa testu: niedostępna", "Test support: unavailable")
            }, style = MaterialTheme.typography.bodyMedium)
            status.unavailableReason?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                    value = status.observationRequested,
                    enabled = canAct && selectedAssociation != null,
                    role = Role.Switch,
                    onValueChange = { requested ->
                        perform { if (requested) manager.startObservation() else manager.stopObservation() }
                    }
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr("Żądanie obserwacji", "Observation request"),
                    style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Switch(checked = status.observationRequested, onCheckedChange = null,
                    enabled = canAct && selectedAssociation != null)
            }
            Text(if (status.observationRequested) tr("Żądanie aplikacji: WŁĄCZONE", "App request: ON")
                else tr("Żądanie aplikacji: WYŁĄCZONE", "App request: OFF"),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            Text(tr("Włącznik pokazuje żądanie aplikacji, nie potwierdza przyłączenia usługi przez Androida. Ten stan nie jest dostępny przez publiczne API. Obecność zegarka i utworzenie usługi są pokazane oddzielnie poniżej.",
                "The switch shows the app request, not confirmation that Android has bound the service. That state is not exposed by the public API. Watch presence and service creation are shown separately below."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (selectedAssociation == null) {
                Text(tr("Aby włączyć obserwację, ręcznie dodaj i wybierz powiązanie aplikacji.",
                    "To enable observation, manually add and select an app association."),
                    style = MaterialTheme.typography.bodySmall)
            }
            Text(tr("Wyłączenie obserwacji zachowuje asocjację i może pozostawić przywileje aplikacji. Nie przywraca automatycznie poprzednich wyjątków baterii i danych w tle.",
                "Stopping observation keeps the association and may retain app privileges. It does not automatically restore previous battery and background data exemptions."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        CompanionCard(tr("Powiązania tej aplikacji", "This app’s associations")) {
            if (status.associations.isEmpty()) {
                Text(tr("Brak asocjacji CDM dla MohgWatch.", "No CDM associations for MohgWatch."),
                    style = MaterialTheme.typography.bodyMedium)
            }
            status.associations.forEach { association ->
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                        selected = association.id == status.selectedAssociationId,
                        enabled = canAct,
                        role = Role.RadioButton,
                        onClick = { perform { manager.selectAssociation(association.id) } }
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = association.id == status.selectedAssociationId,
                        onClick = null, enabled = canAct)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(association.displayName ?: tr("Zegarek", "Watch"),
                            style = MaterialTheme.typography.bodyMedium)
                        Text(tr("Asocjacja #${association.id}", "Association #${association.id}"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            OutlinedButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
                        PackageManager.PERMISSION_GRANTED) {
                        bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    } else showBondedDevices()
                },
                enabled = canAct,
                modifier = Modifier.fillMaxWidth()
            ) { Text(tr("Dodaj już sparowany zegarek", "Add an already paired watch")) }
            Text(tr("Lista obejmuje urządzenia już sparowane w Androidzie. Wybierz zegarek i potwierdź powiązanie z MohgWatch w oknie systemowym. Odmowa zgody lub anulowanie pozostawia synchronizację bez CDM.",
                "The list contains devices already paired in Android. Choose your watch and confirm its association with MohgWatch in the system dialog. Denial or cancellation leaves synchronization working without CDM."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { removingAssociationId = selectedAssociation?.id },
                enabled = canAct && selectedAssociation != null, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Usuń wybrane powiązanie aplikacji", "Remove selected app association"))
            }
        }

        CompanionCard(tr("Stan systemu", "System status")) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                Text(tr("Obecność zegarka: $presenceLabel", "Watch presence: $presenceLabel"))
                Text(if (status.serviceRunning) tr("Usługa Companion: utworzona", "Companion service: created")
                    else tr("Usługa Companion: nieutworzona", "Companion service: not created"))
                val lastEvent = status.lastEventAtMillis?.let(::companionTime) ?: unknown
                Text(tr("Ostatnie zdarzenie: $lastEvent", "Last event: $lastEvent"))
                val battery = when (status.batteryExempt) {
                    true -> tr("tak", "yes")
                    false -> tr("nie", "no")
                    null -> unknown
                }
                Text(tr("Wyjątek optymalizacji baterii: $battery", "Battery optimization exemption: $battery"))
                Text(tr("Oszczędzanie danych: $dataSaverLabel", "Data Saver: $dataSaverLabel"))
            }
            OutlinedButton(onClick = { perform { manager.refresh() } },
                enabled = status.ready && !status.busy, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Odśwież stan", "Refresh status"))
            }
            if (status.exemptionChanged) {
                Text(tr("Zmienił się wyjątek baterii lub dostęp do danych w tle. Wyłączenie testu nie cofa przywilejów. Sprawdź ustawienia Androida i porównuj wyniki przy tych samych ustawieniach.",
                    "Battery exemption or background data access has changed. Stopping the test does not revoke privileges. Check Android settings and compare results using the same settings."),
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            TextButton(onClick = onOpenBatterySettings, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Bateria i praca w tle", "Battery and background"))
            }
            TextButton(onClick = {
                perform {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:${context.packageName}")))
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Ustawienia aplikacji w Androidzie", "Android app settings"))
            }
        }

        CompanionCard(tr("Synchronizacja", "Synchronization")) {
            Text(if (settings.isSyncEnabled) tr("Synchronizacja: włączona", "Synchronization: enabled")
                else tr("Synchronizacja: wyłączona", "Synchronization: disabled"))
            Text(if (syncing) tr("Usługa synchronizacji: aktywna", "Sync service: active")
                else tr("Usługa synchronizacji: nieaktywna", "Sync service: inactive"))
            Text(syncStatus, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            val age = latestReading?.timestamp?.takeIf { it in 1L..now }?.let { (now - it) / 60_000L }
            Text(if (age != null) tr("Wiek ostatniego odczytu: $age min", "Latest reading age: $age min")
                else tr("Wiek ostatniego odczytu: nieznany", "Latest reading age: unknown"))
            val successTime = lastSuccess.takeIf { it > 0 }?.let(::companionTime) ?: unknown
            Text(tr("Ostatni sukces: $successTime", "Last success: $successTime"))
            Text(syncError?.let { tr("Ostatni błąd: $it", "Last error: $it") }
                ?: tr("Ostatni błąd: brak", "Last error: none"),
                style = MaterialTheme.typography.bodySmall,
                color = if (syncError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            OutlinedButton(onClick = { perform { GlucoseSyncService.forceSync(context) } },
                enabled = settings.isSyncEnabled, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Synchronizuj teraz", "Sync now"))
            }
        }

        CompanionCard(tr("Pomiar nocny", "Night measurement")) {
            Text(tr("Najpierw zbierz pomiar bazowy bez żadnej asocjacji. Samo powiązanie może zmienić przywileje aplikacji, nawet przy wyłączonej obserwacji. Porównuj tę samą wersję APK i ustawienia w podobnych warunkach.",
                "Collect a baseline with no association first. Association alone may change app privileges, even with observation off. Compare the same APK version and settings under similar conditions."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(tr("Tryb: $modeLabel", "Mode: $modeLabel"))
            if (nightRun != null) {
                SelectionContainer {
                    Text(tr("Identyfikator pomiaru: ${nightRun.id}", "Run ID: ${nightRun.id}"),
                        style = MaterialTheme.typography.bodySmall)
                }
                Text(tr("Pomiar trwa od: ${companionTime(nightRun.startedAtMillis)}",
                    "Measurement started: ${companionTime(nightRun.startedAtMillis)}"),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Button(onClick = { perform { nightManager.stop() } }, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Zakończ pomiar nocny", "Stop night measurement"))
                }
            } else {
                Button(onClick = { perform { nightManager.start() } },
                    enabled = nightState.ready && settings.isSyncEnabled,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Rozpocznij pomiar nocny", "Start night measurement"))
                }
            }
            Text(tr("Zmiana asocjacji, obserwacji CDM, synchronizacji, interwału, wybudzenia lub progu świeżości przerywa pomiar. Wynik opisuje obserwacje i nie stanowi gwarancji interwału synchronizacji.",
                "Changing the association, CDM observation, synchronization, polling interval, wake lock, or freshness threshold interrupts the measurement. Results describe observations and do not guarantee a synchronization interval."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            nightState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            nightState.lastReport?.let {
                Text(tr("Ostatnie podsumowanie", "Last summary"), style = MaterialTheme.typography.titleSmall)
                SelectionContainer {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
            OutlinedButton(onClick = onOpenLogs, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Otwórz logi i diagnostykę", "Open logs and diagnostics"))
            }
        }
    }

    bondedPicker?.let { devices ->
        AlertDialog(
            onDismissRequest = { bondedPicker = null },
            title = { Text(tr("Wybierz sparowany zegarek", "Choose a paired watch")) },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    Text(tr("Zostanie dodane tylko powiązanie z MohgWatch.", "Only an association with MohgWatch will be added."),
                        style = MaterialTheme.typography.bodySmall)
                    devices.forEachIndexed { index, device ->
                        TextButton(onClick = {
                            bondedPicker = null
                            feedback = null
                            try {
                                manager.associate(device) { sender ->
                                    try {
                                        associationLauncher.launch(IntentSenderRequest.Builder(sender).build())
                                    } catch (_: Exception) {
                                        manager.associationUiResult(cancelled = true)
                                        feedback = operationFailed
                                    }
                                }
                            } catch (_: Exception) {
                                feedback = operationFailed
                            }
                        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(if (devices.count { it.name == device.name } > 1) "${device.name} (${index + 1})" else device.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { bondedPicker = null }) { Text(tr("Anuluj", "Cancel")) }
            }
        )
    }
    removingAssociationId?.let { id ->
        AlertDialog(
            onDismissRequest = { removingAssociationId = null },
            title = { Text(tr("Usunąć powiązanie aplikacji?", "Remove app association?")) },
            text = { Text(tr("Usunięte zostanie tylko powiązanie #$id między MohgWatch a urządzeniem oraz jego obserwacja. Systemowe sparowanie Bluetooth zegarka pozostanie. Wyjątki baterii i danych w tle sprawdź osobno w ustawieniach Androida.",
                "Only association #$id between MohgWatch and the device and its observation will be removed. The watch’s system Bluetooth pairing will remain. Check battery and background data exemptions separately in Android settings.")) },
            confirmButton = {
                TextButton(onClick = {
                    removingAssociationId = null
                    perform { manager.removeAssociation(id) }
                }) { Text(tr("Usuń powiązanie", "Remove association")) }
            },
            dismissButton = {
                TextButton(onClick = { removingAssociationId = null }) { Text(tr("Anuluj", "Cancel")) }
            }
        )
    }
}

@Composable
private fun CompanionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

private fun companionTime(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(timestamp))
