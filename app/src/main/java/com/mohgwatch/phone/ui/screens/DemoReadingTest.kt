package com.mohgwatch.phone.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.model.TrendArrow
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.service.DataLayerSender
import com.mohgwatch.phone.service.GlucoseSyncState
import com.mohgwatch.phone.widget.MohgWatchWidget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
internal fun DemoReadingTest() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataLayerSender = remember { DataLayerSender(context) }
    val settingsStore = remember { SettingsStore(context) }
    val loadedSettings by settingsStore.settingsFlow.collectAsState(initial = null)
    val settings = loadedSettings ?: UserSettings()
    var savingVisibility by remember { mutableStateOf(false) }
    var visibilityError by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text("Pokaż przycisk Demo", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Switch(
            checked = settings.showDemoButton,
            enabled = loadedSettings != null && !savingVisibility,
            onCheckedChange = { visible ->
                savingVisibility = true
                visibilityError = false
                scope.launch {
                    try {
                        val updated = settingsStore.getSettings().copy(showDemoButton = visible)
                        settingsStore.saveSettings(updated)
                        dataLayerSender.sendSettings(updated)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        visibilityError = true
                    } finally {
                        savingVisibility = false
                    }
                }
            }
        )
    }
    if (visibilityError) {
        Text("Nie udało się zapisać widoczności Demo. Spróbuj ponownie.", color = MaterialTheme.colorScheme.error)
    }

    if (loadedSettings != null && settings.showDemoButton) {
        Text("Dane demonstracyjne", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Demo losuje odczyt i przykładową historię do podglądu aplikacji, widżetu i zegarka. Do sprawdzania progów użyj symulacji pomiaru powyżej.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                val mockValue = Random.nextInt(75, 195).toFloat()
                val mockReading = GlucoseReading(
                    value = mockValue,
                    trendArrow = TrendArrow.entries.filter { it != TrendArrow.UNKNOWN }.random(),
                    timestamp = System.currentTimeMillis(),
                    isHigh = mockValue > settings.highThreshold,
                    isLow = mockValue < settings.lowThreshold
                )
                GlucoseSyncState.latestReading.value = mockReading
                GlucoseSyncState.syncStatusText.value = "Wysłano testowy odczyt (Demo)"
                val mockHistory = (1..12).map { index ->
                    GlucoseReading(
                        value = Random.nextInt(80, 170).toFloat(),
                        trendArrow = TrendArrow.STABLE,
                        timestamp = System.currentTimeMillis() - index * 15 * 60_000L,
                        isHigh = false,
                        isLow = false
                    )
                }
                GlucoseSyncState.history.value = mockHistory
                scope.launch {
                    try {
                        MohgWatchWidget().updateAll(context)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Widget failures must not prevent sending the preview to the watch.
                    }
                    dataLayerSender.sendGlucoseReading(mockReading)
                    dataLayerSender.sendGlucoseHistory(mockHistory)
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text("Wyślij testowy odczyt (Demo)")
        }
    }
}
