package com.mohgwatch.phone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.phone.data.NightTestManager
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.util.tr
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun NightTestCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember(context) { NightTestManager.get(context) }
    val state by manager.state.collectAsState()
    val settings by remember(context) { SettingsStore(context).settingsFlow }.collectAsState(initial = UserSettings())
    var showReport by rememberSaveable { mutableStateOf(false) }
    val run = state.activeRun

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tr("Pomiar nocny", "Night measurement"), style = MaterialTheme.typography.titleMedium)
            if (run != null) {
                Text(tr("Pomiar trwa od ${nightTestTime(run.startedAtMillis)}", "Running since ${nightTestTime(run.startedAtMillis)}"),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Button(onClick = { scope.launch { manager.stop() } }, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Zakończ pomiar nocny", "Stop night measurement"))
                }
            } else {
                Text(tr("Rozpocznij przed snem, zakończ zaraz po przebudzeniu. Nie podłączaj ładowarki, jeśli chcesz porównać zużycie baterii.",
                    "Start before sleep, stop right after waking up. Leave the charger unplugged to compare battery use."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { scope.launch { manager.start() } },
                    enabled = state.ready && settings.isSyncEnabled, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Rozpocznij pomiar nocny", "Start night measurement"))
                }
            }
            Text(tr("Zmiana synchronizacji, interwału, stałego wybudzenia lub limitu świeżości przerywa pomiar.",
                "Changing synchronization, interval, persistent wake lock or freshness limit interrupts the measurement."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            state.lastReport?.let { report ->
                TextButton(onClick = { showReport = !showReport }) {
                    Text(if (showReport) tr("Ukryj ostatnie podsumowanie", "Hide last summary")
                        else tr("Pokaż ostatnie podsumowanie", "Show last summary"))
                }
                if (showReport) SelectionContainer { Text(report, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

fun nightTestTime(millis: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))
