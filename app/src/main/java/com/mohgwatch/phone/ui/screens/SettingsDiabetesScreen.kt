package com.mohgwatch.phone.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohgwatch.core.model.GlucoseUnit
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.service.DataLayerSender
import com.mohgwatch.phone.ui.theme.GlucoseHigh
import com.mohgwatch.phone.ui.theme.GlucoseLow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

enum class DiabetesSettingsSection { GLUCOSE, DOSES }

@Composable
fun SettingsDiabetesScreen(onBack: () -> Unit, section: DiabetesSettingsSection = DiabetesSettingsSection.GLUCOSE) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val dataLayerSender = remember { DataLayerSender(context) }
    val loadedSettings by settingsStore.settingsFlow.collectAsState(initial = null)
    val settings = loadedSettings ?: UserSettings()
    val doseSaveMutex = remember { Mutex() }

    var unit by remember(settings) { mutableStateOf(settings.unit) }
    var lowThreshold by remember(settings) { mutableFloatStateOf(settings.lowThreshold) }
    var highThreshold by remember(settings) { mutableFloatStateOf(settings.highThreshold) }
    var snoozeMinutes by remember(settings) { mutableFloatStateOf(settings.alertSnoozeMinutes.toFloat()) }

    com.mohgwatch.phone.ui.components.SettingsPage(
        title = if (section == DiabetesSettingsSection.GLUCOSE) "Glukoza i progi" else "Dawki i miejsca wkłuć",
        onBack = onBack
    ) {
        if (section == DiabetesSettingsSection.GLUCOSE) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Jednostka glukozy", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlucoseUnit.entries.forEach { u ->
                            FilterChip(
                                selected = unit == u,
                                onClick = { 
                                    unit = u 
                                    scope.launch {
                                        val newSettings = settings.copy(unit = u)
                                        settingsStore.saveSettings(newSettings)
                                        dataLayerSender.sendSettings(newSettings)
                                    }
                                },
                                label = { Text(u.label) },
                                leadingIcon = if (unit == u) {
                                    { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("Progi glukozy", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ArrowDownward, null, tint = GlucoseLow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Próg niski", modifier = Modifier.weight(1f))
                        Text("${lowThreshold.roundToInt()} mg/dL", color = GlucoseLow, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = lowThreshold,
                        onValueChange = { lowThreshold = it },
                        onValueChangeFinished = {
                            scope.launch {
                                val newSettings = settings.copy(lowThreshold = lowThreshold)
                                settingsStore.saveSettings(newSettings)
                                dataLayerSender.sendSettings(newSettings)
                            }
                        },
                        valueRange = 50f..100f,
                        steps = 9,
                        colors = SliderDefaults.colors(thumbColor = GlucoseLow, activeTrackColor = GlucoseLow)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ArrowUpward, null, tint = GlucoseHigh)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Próg wysoki", modifier = Modifier.weight(1f))
                        Text("${highThreshold.roundToInt()} mg/dL", color = GlucoseHigh, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = highThreshold,
                        onValueChange = { highThreshold = it },
                        onValueChangeFinished = {
                            scope.launch {
                                val newSettings = settings.copy(highThreshold = highThreshold)
                                settingsStore.saveSettings(newSettings)
                                dataLayerSender.sendSettings(newSettings)
                            }
                        },
                        valueRange = 120f..300f,
                        steps = 35,
                        colors = SliderDefaults.colors(thumbColor = GlucoseHigh, activeTrackColor = GlucoseHigh)
                    )
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
                        Text("Wyciszanie alarmów", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(if (snoozeMinutes.roundToInt() == 0) "Wyłączone" else "${snoozeMinutes.roundToInt()} min",
                            fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Przycisk na ekranie Status wycisza alarmy niskiego i wysokiego cukru na telefonie i zegarku na wybrany czas. 0 ukrywa przycisk.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = snoozeMinutes,
                        onValueChange = { snoozeMinutes = it },
                        onValueChangeFinished = {
                            scope.launch {
                                // Fresh settings avoid overwriting a change made on another screen meanwhile.
                                settingsStore.saveSettings(settingsStore.getSettings().copy(alertSnoozeMinutes = snoozeMinutes.roundToInt()))
                            }
                        },
                        valueRange = 0f..30f,
                        steps = 29
                    )
                }
            }

        }
        if (section == DiabetesSettingsSection.DOSES) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Miejsca wstrzyknięć", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Pokaż sugerowane miejsca wkłuć",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.showInjectionSites,
                            enabled = loadedSettings != null,
                            onCheckedChange = { 
                                scope.launch {
                                    val newSettings = doseSaveMutex.withLock {
                                        val updated = settingsStore.getSettings().copy(showInjectionSites = it)
                                        settingsStore.saveSettings(updated)
                                        updated
                                    }
                                    dataLayerSender.sendSettings(newSettings)
                                }
                            }
                        )
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
                    Text("Dzisiejsze dawki", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Wpisz dzisiejsze dawki insuliny dla poszczególnych posiłków i na noc",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    var breakfast by remember { mutableStateOf(DoseInputState()) }
                    var lunch by remember { mutableStateOf(DoseInputState()) }
                    var dinner by remember { mutableStateOf(DoseInputState()) }
                    var night by remember { mutableStateOf(DoseInputState()) }

                    LaunchedEffect(loadedSettings) {
                        loadedSettings?.let { loaded ->
                            breakfast = breakfast.receive(loaded.doseBreakfast)
                            lunch = lunch.receive(loaded.doseLunch)
                            dinner = dinner.receive(loaded.doseDinner)
                            night = night.receive(loaded.doseNight)
                        }
                    }

                    fun saveDose(field: DoseField, input: DoseInputState) {
                        val raw = input.text ?: return
                        scope.launch {
                            doseSaveMutex.withLock {
                                val current = settingsStore.getSettings()
                                settingsStore.saveSettings(current.withDose(field, raw))
                            }
                            com.mohgwatch.phone.widget.WidgetUpdateHelper.updateAllWidgets(context)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = breakfast.text.orEmpty(),
                            onValueChange = {
                                breakfast = breakfast.edit(it)
                                saveDose(DoseField.BREAKFAST, breakfast)
                            },
                            enabled = breakfast.text != null,
                            label = { Text("Śniadanie") },
                            placeholder = { Text("1,1") },
                            suffix = { Text("j") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lunch.text.orEmpty(),
                            onValueChange = {
                                lunch = lunch.edit(it)
                                saveDose(DoseField.LUNCH, lunch)
                            },
                            enabled = lunch.text != null,
                            label = { Text("Obiad") },
                            placeholder = { Text("1,2") },
                            suffix = { Text("j") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = dinner.text.orEmpty(),
                            onValueChange = {
                                dinner = dinner.edit(it)
                                saveDose(DoseField.DINNER, dinner)
                            },
                            enabled = dinner.text != null,
                            label = { Text("Kolacja") },
                            placeholder = { Text("1,3") },
                            suffix = { Text("j") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = night.text.orEmpty(),
                            onValueChange = {
                                night = night.edit(it)
                                saveDose(DoseField.NIGHT, night)
                            },
                            enabled = night.text != null,
                            label = { Text("Na noc") },
                            placeholder = { Text("3") },
                            suffix = { Text("j") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
