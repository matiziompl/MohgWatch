package com.mohgwatch.phone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDiabetesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val dataLayerSender = remember { DataLayerSender(context) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = UserSettings())

    var unit by remember(settings) { mutableStateOf(settings.unit) }
    var lowThreshold by remember(settings) { mutableFloatStateOf(settings.lowThreshold) }
    var highThreshold by remember(settings) { mutableFloatStateOf(settings.highThreshold) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Cukrzyca") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
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
                            onCheckedChange = { 
                                scope.launch {
                                    val newSettings = settings.copy(showInjectionSites = it)
                                    settingsStore.saveSettings(newSettings)
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

                    var breakfast by remember(settings.doseBreakfast) { mutableStateOf(settings.doseBreakfast) }
                    var lunch by remember(settings.doseLunch) { mutableStateOf(settings.doseLunch) }
                    var dinner by remember(settings.doseDinner) { mutableStateOf(settings.doseDinner) }
                    var night by remember(settings.doseNight) { mutableStateOf(settings.doseNight) }

                    fun formatDose(raw: String): String {
                        val t = raw.trim()
                        if (t.isEmpty()) return ""
                        val num = t.replace(".", ",")
                        return if (!num.endsWith("j", ignoreCase = true)) "${num}j" else num
                    }

                    fun saveDoses(b: String, l: String, d: String, n: String) {
                        scope.launch {
                            val newSettings = settings.copy(
                                doseBreakfast = formatDose(b),
                                doseLunch = formatDose(l),
                                doseDinner = formatDose(d),
                                doseNight = formatDose(n)
                            )
                            settingsStore.saveSettings(newSettings)
                            com.mohgwatch.phone.widget.WidgetUpdateHelper.updateAllWidgets(context)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = breakfast,
                            onValueChange = {
                                breakfast = it
                                saveDoses(breakfast, lunch, dinner, night)
                            },
                            label = { Text("Śniadanie") },
                            placeholder = { Text("1,1j") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lunch,
                            onValueChange = {
                                lunch = it
                                saveDoses(breakfast, lunch, dinner, night)
                            },
                            label = { Text("Obiad") },
                            placeholder = { Text("1,2j") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = dinner,
                            onValueChange = {
                                dinner = it
                                saveDoses(breakfast, lunch, dinner, night)
                            },
                            label = { Text("Kolacja") },
                            placeholder = { Text("1,3j") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = night,
                            onValueChange = {
                                night = it
                                saveDoses(breakfast, lunch, dinner, night)
                            },
                            label = { Text("Na noc") },
                            placeholder = { Text("3j") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
