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
                        Text("${lowThreshold.toInt()} mg/dL", color = GlucoseLow, fontWeight = FontWeight.Bold)
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
                        Text("${highThreshold.toInt()} mg/dL", color = GlucoseHigh, fontWeight = FontWeight.Bold)
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
        }
    }
}
