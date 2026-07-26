package com.mohgwatch.phone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.ui.theme.GlucoseHigh
import com.mohgwatch.phone.ui.theme.GlucoseLow
import com.mohgwatch.phone.ui.theme.GlucoseInRange
import com.mohgwatch.phone.ui.theme.GlucoseVeryHigh
import com.mohgwatch.phone.ui.components.MultiSlider
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsWatchScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Tarcza Zegarka") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        settings?.let { currentSettings ->
            var watchLowThreshold by remember(currentSettings) { mutableFloatStateOf(currentSettings.watchLowThreshold) }
            var watchHighThreshold by remember(currentSettings) { mutableFloatStateOf(currentSettings.watchHighThreshold) }
            var watchVeryHighThreshold by remember(currentSettings) { mutableFloatStateOf(currentSettings.watchVeryHighThreshold) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Przedziały kolorów kompilacji",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Progi używane tylko na tarczy zegarka (niezależne od głównych limitów).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        MultiSlider(
                            value1 = watchLowThreshold,
                            value2 = watchHighThreshold,
                            value3 = watchVeryHighThreshold,
                            onValueChange = { v1, v2, v3 ->
                                watchLowThreshold = v1
                                watchHighThreshold = v2
                                watchVeryHighThreshold = v3
                                scope.launch {
                                    settingsStore.saveSettings(currentSettings.copy(
                                        watchLowThreshold = v1,
                                        watchHighThreshold = v2,
                                        watchVeryHighThreshold = v3
                                    ))
                                }
                            },
                            valueRange = 0f..500f,
                            step = 5f,
                            colors = listOf(GlucoseLow, GlucoseInRange, GlucoseHigh, GlucoseVeryHigh)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Aktualne przedziały:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Poniżej ${watchLowThreshold.roundToInt()} mg/dL", color = GlucoseLow)
                        Text("Od ${watchLowThreshold.roundToInt()} do ${watchHighThreshold.roundToInt()} mg/dL", color = GlucoseInRange)
                        Text("Od ${watchHighThreshold.roundToInt()} do ${watchVeryHighThreshold.roundToInt()} mg/dL", color = GlucoseHigh)
                        Text("Powyżej ${watchVeryHighThreshold.roundToInt()} mg/dL", color = GlucoseVeryHigh)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Wygląd Kompilacji",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Pozycja Strzałki Trendu", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = currentSettings.watchTrendArrowStyle == "next_to_value",
                                onClick = { scope.launch { settingsStore.saveSettings(currentSettings.copy(watchTrendArrowStyle = "next_to_value")) } }
                            )
                            Text("Obok wartości (standard)")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = currentSettings.watchTrendArrowStyle == "under_value",
                                onClick = { scope.launch { settingsStore.saveSettings(currentSettings.copy(watchTrendArrowStyle = "under_value")) } }
                            )
                            Text("Pod wartością")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Czcionka Glukozy", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = currentSettings.watchGlucoseFont == "default",
                                onClick = { scope.launch { settingsStore.saveSettings(currentSettings.copy(watchGlucoseFont = "default")) } }
                            )
                            Text("Domyślna")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = currentSettings.watchGlucoseFont == "bold",
                                onClick = { scope.launch { settingsStore.saveSettings(currentSettings.copy(watchGlucoseFont = "bold")) } }
                            )
                            Text("Pogrubiona")
                        }
                    }
                }
            }
        }
    }
}
