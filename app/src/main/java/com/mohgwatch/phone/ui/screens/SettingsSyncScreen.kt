package com.mohgwatch.phone.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.phone.data.CredentialStore
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.service.DataLayerSender
import com.mohgwatch.phone.service.GlucoseSyncService
import com.mohgwatch.phone.ui.components.NightReadinessCard
import com.mohgwatch.phone.ui.components.SettingsPage
import com.mohgwatch.phone.ui.components.rememberNightReadiness
import com.mohgwatch.phone.util.tr
import kotlinx.coroutines.launch

/** Everything that keeps glucose data flowing in the background, including overnight readiness. */
@Composable
fun SettingsSyncScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val dataLayerSender = remember { DataLayerSender(context) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = UserSettings())
    val readiness = rememberNightReadiness()
    var advancedOpen by rememberSaveable { mutableStateOf(false) }

    fun save(newSettings: UserSettings, afterSave: () -> Unit) {
        scope.launch {
            settingsStore.saveSettings(newSettings)
            dataLayerSender.sendSettings(newSettings)
            if (newSettings.isSyncEnabled && CredentialStore(context).hasCredentials()) afterSave()
        }
    }

    SettingsPage(title = tr("Synchronizacja i bateria", "Sync and battery"), onBack = onBack) {
        NightReadinessCard(readiness)

        SyncCard {
            Text(tr("Synchronizacja", "Synchronization"), style = MaterialTheme.typography.titleMedium)
            Text(tr("Jak często odpytywać LibreLinkUp", "How often to poll LibreLinkUp"),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3, 5).forEach { interval ->
                    FilterChip(
                        selected = settings.pollIntervalMinutes == interval,
                        onClick = { save(settings.copy(pollIntervalMinutes = interval)) { GlucoseSyncService.forceSync(context) } },
                        label = { Text("${interval}m") },
                        modifier = Modifier.weight(1f)
                    )
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
        }

        Row(
            Modifier.fillMaxWidth().clickable { advancedOpen = !advancedOpen }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(tr("Zaawansowane", "Advanced"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(if (advancedOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (advancedOpen) tr("Zwiń", "Collapse") else tr("Rozwiń", "Expand"))
        }
        if (advancedOpen) {
            SyncCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Trzymaj wybudzenie", "Persistent WakeLock"), style = MaterialTheme.typography.titleMedium)
                        Text(
                            tr("Utrzymuje procesor aktywny pomiędzy pobraniami. Zwiększa zużycie baterii; testy nocne nie wykazały potrzeby jego włączania.",
                                "Keeps the CPU awake between fetches. Uses more battery; night tests showed no need for it."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.persistentWakeLockEnabled,
                        onCheckedChange = { save(settings.copy(persistentWakeLockEnabled = it)) { GlucoseSyncService.updateWakeLock(context) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
