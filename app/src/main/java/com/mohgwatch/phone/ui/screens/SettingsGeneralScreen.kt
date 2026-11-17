package com.mohgwatch.phone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.service.DataLayerSender
import com.mohgwatch.phone.service.GlucoseSyncService
import com.mohgwatch.phone.util.tr
import kotlinx.coroutines.launch

enum class GeneralSettingsSection { SYNC, BACKGROUND }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsGeneralScreen(
    onBack: () -> Unit,
    section: GeneralSettingsSection = GeneralSettingsSection.BACKGROUND,
    onNavigateToCompanion: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val dataLayerSender = remember { DataLayerSender(context) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = UserSettings())
    val lifecycleOwner = LocalLifecycleOwner.current
    val powerManager = remember(context) { context.getSystemService(android.os.PowerManager::class.java) }
    val alarmManager = remember(context) { context.getSystemService(android.app.AlarmManager::class.java) }
    var isIgnoring by remember { mutableStateOf(false) }
    var exactAlarmsAllowed by remember { mutableStateOf(false) }
    var notificationsAllowed by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, context) {
        fun refreshReadiness() {
            isIgnoring = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
            exactAlarmsAllowed = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ||
                alarmManager?.canScheduleExactAlarms() == true
            notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshReadiness()
        }
        refreshReadiness()
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    com.mohgwatch.phone.ui.components.SettingsPage(
        title = if (section == GeneralSettingsSection.SYNC) tr("Synchronizacja", "Synchronization")
            else tr("Bateria i praca w tle", "Battery and background"),
        onBack = onBack
    ) {
        if (section == GeneralSettingsSection.SYNC) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(tr("Interwał odpytywania", "Poll interval"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        tr("Jak często odpytywać LibreLinkUp", "How often to poll LibreLinkUp"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3, 5).forEach { interval ->
                            FilterChip(
                                selected = settings.pollIntervalMinutes == interval,
                                onClick = {
                                    scope.launch {
                                        val newSettings = settings.copy(pollIntervalMinutes = interval)
                                        settingsStore.saveSettings(newSettings)
                                        dataLayerSender.sendSettings(newSettings)
                                        if (newSettings.isSyncEnabled && com.mohgwatch.phone.data.CredentialStore(context).hasCredentials()) {
                                            GlucoseSyncService.forceSync(context)
                                        }
                                    }
                                },
                                label = { Text("${interval}m") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }


                }
            }


            if (!settings.isSyncEnabled) {
                Button(onClick = { GlucoseSyncService.start(context) }, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Włącz synchronizację", "Start synchronization"))
                }
            }
            OutlinedButton(onClick = { GlucoseSyncService.forceSync(context) },
                enabled = settings.isSyncEnabled, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Synchronizuj teraz", "Sync now"))
            }
            OutlinedButton(onClick = {
                GlucoseSyncService.stop(context)
                (context as? android.app.Activity)?.finishAffinity()
            }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Wyłącz aplikację", "Turn off application"))
            }
            if (onNavigateToCompanion != null) {
                Text(tr("Zaawansowane", "Advanced"), style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onNavigateToCompanion, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("CDM TEST — obecność zegarka", "CDM TEST — watch presence"))
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(tr("Niezawodność w nocy", "Night reliability"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        tr("Dostęp do internetu w uśpieniu Androida (Doze) wymaga wyłączenia optymalizacji baterii dla aplikacji. Samo stałe wybudzenie nie usuwa blokady sieci.",
                           "Internet access during Android sleep (Doze) requires an exemption from battery optimization. A persistent wake lock alone does not remove network blocking."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        if (isIgnoring) tr("Wyjątek baterii: obecnie aktywny", "Battery exemption: currently active")
                        else tr("Wyjątek baterii: nieaktywny — możliwe odcięcie internetu", "Battery exemption: inactive — internet may be blocked"),
                        color = if (isIgnoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        tr("Status dotyczy tej chwili i nie potwierdza trwałego wyjątku. Sprawdź go ponownie po rozłączeniu zegarka i powrocie na ten ekran.",
                           "This is the current status and does not confirm a permanent exemption. Check it again after disconnecting the watch and returning to this screen."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (settings.persistentWakeLockEnabled) tr("Stałe wybudzenie: włączone", "Persistent wake lock: enabled")
                        else tr("Stałe wybudzenie: wyłączone", "Persistent wake lock: disabled"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        if (exactAlarmsAllowed) tr("Dokładne wybudzenia: dozwolone", "Exact wakeups: allowed")
                        else tr("Dokładne wybudzenia: zablokowane", "Exact wakeups: blocked"),
                        color = if (exactAlarmsAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        if (notificationsAllowed) tr("Powiadomienia: dozwolone", "Notifications: allowed")
                        else tr("Powiadomienia: zablokowane", "Notifications: blocked"),
                        color = if (notificationsAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (!isIgnoring) {
                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                    intent.data = android.net.Uri.parse("package:${context.packageName}")
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (e2: Exception) {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        intent.data = android.net.Uri.parse("package:${context.packageName}")
                                        context.startActivity(intent)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tr("Wyłącz Optymalizację Baterii", "Disable Battery Optimization"))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            } catch (_: android.content.ActivityNotFoundException) {
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(android.net.Uri.parse("package:${context.packageName}")))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(tr("Otwórz optymalizację baterii", "Open battery optimization"))
                    }
                    if (!exactAlarmsAllowed && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        OutlinedButton(
                            onClick = {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    .setData(android.net.Uri.parse("package:${context.packageName}"))
                                try {
                                    context.startActivity(intent)
                                } catch (_: android.content.ActivityNotFoundException) {
                                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        .setData(android.net.Uri.parse("package:${context.packageName}")))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tr("Zezwól na dokładne wybudzenia", "Allow exact wakeups"))
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = android.net.Uri.parse("package:${context.packageName}")
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(tr("Otwórz ustawienia aplikacji", "Open app settings"))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tr("Trzymaj wybudzenie", "Persistent WakeLock"), style = MaterialTheme.typography.titleMedium)
                            Text(
                                tr("Utrzymuje procesor aktywny pomiędzy pobraniami. Zwiększa zużycie baterii; nie odblokuje internetu w Doze.",
                                   "Keeps the CPU awake between fetches. Uses more battery; does not unblock internet in Doze."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.persistentWakeLockEnabled,
                            onCheckedChange = {
                                scope.launch {
                                    val newSettings = settings.copy(persistentWakeLockEnabled = it)
                                    settingsStore.saveSettings(newSettings)
                                    dataLayerSender.sendSettings(newSettings)
                                    if (newSettings.isSyncEnabled && com.mohgwatch.phone.data.CredentialStore(context).hasCredentials()) {
                                        GlucoseSyncService.updateWakeLock(context)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            OutlinedButton(onClick = {
                val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(android.net.Uri.parse("package:${context.packageName}")))
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Otwórz ustawienia powiadomień", "Open notification settings"))
            }
        }
    }
}
