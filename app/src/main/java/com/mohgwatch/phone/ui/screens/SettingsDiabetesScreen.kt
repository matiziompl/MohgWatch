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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
    
    var tFastFalling by remember(settings) { mutableFloatStateOf(settings.trendThresholdFastFalling) }
    var tFalling by remember(settings) { mutableFloatStateOf(settings.trendThresholdFalling) }
    var tRising by remember(settings) { mutableFloatStateOf(settings.trendThresholdRising) }
    var tFastRising by remember(settings) { mutableFloatStateOf(settings.trendThresholdFastRising) }

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
                    Text("Progi matematyczne strzałki trendu", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val saveTrends = {
                        scope.launch {
                            val newSettings = settings.copy(
                                trendThresholdFastFalling = tFastFalling,
                                trendThresholdFalling = tFalling,
                                trendThresholdRising = tRising,
                                trendThresholdFastRising = tFastRising
                            )
                            settingsStore.saveSettings(newSettings)
                            dataLayerSender.sendSettings(newSettings)
                        }
                    }
                    
                    MultiThumbSlider(
                        valueRange = -10f..10f,
                        values = listOf(tFastFalling, tFalling, tRising, tFastRising),
                        labels = listOf("Szybki spadek", "Spadek", "Wzrost", "Szybki wzrost"),
                        onValuesChange = { newValues ->
                            tFastFalling = newValues[0]
                            tFalling = newValues[1]
                            tRising = newValues[2]
                            tFastRising = newValues[3]
                        },
                        onValuesChangeFinished = { saveTrends() }
                    )
                }
            }
        }
    }
}

@Composable
fun MultiThumbSlider(
    valueRange: ClosedFloatingPointRange<Float>,
    values: List<Float>,
    labels: List<String>,
    onValuesChange: (List<Float>) -> Unit,
    onValuesChangeFinished: () -> Unit
) {
    var sliderWidth by remember { mutableStateOf(0f) }
    var draggingThumb by remember { mutableStateOf<Int?>(null) }
    
    val thumbRadius = 16.dp
    val thumbRadiusPx = with(androidx.compose.ui.platform.LocalDensity.current) { thumbRadius.toPx() }
    val trackHeight = 4.dp
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeTrackColor = MaterialTheme.colorScheme.primary
    val thumbColor = MaterialTheme.colorScheme.primary
    
    val rangeSize = valueRange.endInclusive - valueRange.start
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = thumbRadius)
                .onGloballyPositioned { sliderWidth = it.size.width.toFloat() }
                .pointerInput(sliderWidth) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            // Find closest thumb
                            var closestIdx = -1
                            var minDistance = Float.MAX_VALUE
                            for (i in values.indices) {
                                val valOffset = ((values[i] - valueRange.start) / rangeSize) * sliderWidth
                                val distance = Math.abs(valOffset - offset.x)
                                if (distance < thumbRadiusPx * 2 && distance < minDistance) {
                                    minDistance = distance
                                    closestIdx = i
                                }
                            }
                            if (closestIdx != -1) {
                                draggingThumb = closestIdx
                            }
                        },
                        onDragEnd = {
                            draggingThumb = null
                            onValuesChangeFinished()
                        },
                        onDragCancel = {
                            draggingThumb = null
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            draggingThumb?.let { idx ->
                                val currentVal = values[idx]
                                val currentOffset = ((currentVal - valueRange.start) / rangeSize) * sliderWidth
                                val newOffset = (currentOffset + dragAmount).coerceIn(0f, sliderWidth)
                                var newVal = Math.round((newOffset / sliderWidth) * rangeSize + valueRange.start).toFloat()
                                
                                // Enforce minimum distance of 1.0 between thumbs
                                if (idx > 0) {
                                    newVal = newVal.coerceAtLeast(values[idx - 1] + 1f)
                                }
                                if (idx < values.size - 1) {
                                    newVal = newVal.coerceAtMost(values[idx + 1] - 1f)
                                }
                                
                                val newValues = values.toMutableList()
                                newValues[idx] = newVal
                                onValuesChange(newValues)
                            }
                        }
                    )
                }
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val centerY = size.height / 2f
                
                // Draw background track
                drawLine(
                    color = trackColor,
                    start = androidx.compose.ui.geometry.Offset(0f, centerY),
                    end = androidx.compose.ui.geometry.Offset(size.width, centerY),
                    strokeWidth = trackHeight.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                
                // Draw zero line
                val zeroOffset = ((0f - valueRange.start) / rangeSize) * size.width
                drawLine(
                    color = androidx.compose.ui.graphics.Color.Gray,
                    start = androidx.compose.ui.geometry.Offset(zeroOffset, centerY - 10.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(zeroOffset, centerY + 10.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
                
                // Draw thumbs
                for (v in values) {
                    val px = ((v - valueRange.start) / rangeSize) * size.width
                    drawCircle(
                        color = thumbColor,
                        radius = thumbRadiusPx,
                        center = androidx.compose.ui.geometry.Offset(px, centerY)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in values.indices) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = labels[i],
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${values[i].toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
