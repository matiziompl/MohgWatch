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
fun SettingsChartScreen(onBack: () -> Unit) {
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
            title = { Text("Ustawienia Wykresu") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        settings?.let { currentSettings ->
            var lowThreshold by remember(currentSettings) { mutableFloatStateOf(currentSettings.lowThreshold) }
            var highThreshold by remember(currentSettings) { mutableFloatStateOf(currentSettings.highThreshold) }
            var veryHighThreshold by remember(currentSettings) { mutableFloatStateOf(currentSettings.veryHighThreshold) }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Przedziały kolorów wykresu w aplikacji",
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
                            "Progi używane do kolorowania punktów na wykresie głównym w telefonie.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        MultiSlider(
                            value1 = lowThreshold,
                            value2 = highThreshold,
                            value3 = veryHighThreshold,
                            onValueChange = { v1, v2, v3 ->
                                lowThreshold = v1
                                highThreshold = v2
                                veryHighThreshold = v3
                                scope.launch {
                                    settingsStore.saveSettings(currentSettings.copy(
                                        lowThreshold = v1,
                                        highThreshold = v2,
                                        veryHighThreshold = v3
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
                        Text("Poniżej ${lowThreshold.roundToInt()} mg/dL", color = GlucoseLow)
                        Text("Od ${lowThreshold.roundToInt()} do ${highThreshold.roundToInt()} mg/dL", color = GlucoseInRange)
                        Text("Od ${highThreshold.roundToInt()} do ${veryHighThreshold.roundToInt()} mg/dL", color = GlucoseHigh)
                        Text("Powyżej ${veryHighThreshold.roundToInt()} mg/dL", color = GlucoseVeryHigh)
                    }
                }
            }
        }
    }
}
