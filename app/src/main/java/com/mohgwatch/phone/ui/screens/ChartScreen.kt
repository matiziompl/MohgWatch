package com.mohgwatch.phone.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.service.GlucoseSyncState
import com.mohgwatch.phone.ui.theme.GlucoseHigh
import com.mohgwatch.phone.ui.theme.GlucoseLow
import com.mohgwatch.phone.ui.theme.GlucoseInRange
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen() {
    val context = LocalContext.current
    val history by GlucoseSyncState.history.collectAsState()
    val settingsStore = remember { SettingsStore(context) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = UserSettings())

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Dzisiaj, 1 = 24h

    // Filter history based on tab
    val filteredHistory = remember(history, selectedTab) {
        val now = System.currentTimeMillis()
        if (selectedTab == 0) {
            // Dzisiaj (od północy)
            val cal = Calendar.getInstance()
            cal.timeInMillis = now
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val midnight = cal.timeInMillis
            history.filter { it.timestamp >= midnight }
        } else {
            // Ostatnie 24h
            val yesterday = now - 24 * 60 * 60 * 1000L
            history.filter { it.timestamp >= yesterday }
        }
    }.sortedBy { it.timestamp }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Wykres Glukozy") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(modifier = Modifier.padding(16.dp)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Dzisiaj") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Ostatnie 24h") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (filteredHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Brak danych do wyświetlenia na wykresie.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    GlucoseChart(
                        readings = filteredHistory,
                        settings = settings,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GlucoseChart(
    readings: List<GlucoseReading>,
    settings: UserSettings,
    modifier: Modifier = Modifier
) {
    var touchX by remember { mutableStateOf<Float?>(null) }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val surfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary

    val inverseSurface = MaterialTheme.colorScheme.inverseSurface.toArgb()
    val inverseOnSurface = MaterialTheme.colorScheme.inverseOnSurface.toArgb()
    val onSurface = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        touchX = offset.x
                        tryAwaitRelease()
                        touchX = null
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> touchX = offset.x },
                    onDragEnd = { touchX = null },
                    onDragCancel = { touchX = null },
                    onDrag = { change, _ -> touchX = change.position.x }
                )
            }
    ) {
        if (readings.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height

        // Oś Y: Zostawiamy trochę miejsca na etykiety
        val paddingLeft = 100f
        val paddingBottom = 60f
        val paddingTop = 60f
        val chartWidth = width - paddingLeft
        val chartHeight = height - paddingBottom - paddingTop

        val minTime = readings.first().timestamp
        val maxTime = readings.last().timestamp
        val timeRange = maxOf(maxTime - minTime, 1L)

        val minY = 0f
        val maxY = 300f
        val yRange = maxY - minY

        val textPaint = Paint().apply {
            color = surfaceColor
            textSize = 32f
            textAlign = Paint.Align.RIGHT
            alpha = 150
            typeface = Typeface.DEFAULT
        }

        val gridLineColor = Color.Gray.copy(alpha = 0.2f)
        val ySteps = listOf(50f, 100f, 150f, 200f, 250f, 300f)

        drawIntoCanvas { canvas ->
            ySteps.forEach { step ->
                val y = paddingTop + chartHeight - ((step - minY) / yRange * chartHeight)
                drawLine(
                    color = gridLineColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(width, y),
                    strokeWidth = 2f
                )
                canvas.nativeCanvas.drawText(
                    step.toInt().toString(),
                    paddingLeft - 20f,
                    y + 10f,
                    textPaint
                )
            }

            val highY = paddingTop + chartHeight - ((settings.highThreshold - minY) / yRange * chartHeight)
            val lowY = paddingTop + chartHeight - ((settings.lowThreshold - minY) / yRange * chartHeight)
            
            drawRect(
                color = GlucoseInRange.copy(alpha = 0.15f),
                topLeft = Offset(paddingLeft, highY),
                size = androidx.compose.ui.geometry.Size(chartWidth, lowY - highY)
            )

            val points = readings.map { reading ->
                val x = paddingLeft + ((reading.timestamp - minTime).toFloat() / timeRange * chartWidth)
                val y = paddingTop + chartHeight - ((reading.value - minY) / yRange * chartHeight)
                Offset(x, y)
            }

            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { point ->
                    lineTo(point.x, point.y)
                }
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 6f)
            )

            points.forEachIndexed { index, point ->
                val reading = readings[index]
                val color = when {
                    reading.value > settings.highThreshold -> GlucoseHigh
                    reading.value < settings.lowThreshold -> GlucoseLow
                    else -> GlucoseInRange
                }
                drawCircle(
                    color = color,
                    radius = 8f,
                    center = point
                )
            }

            canvas.nativeCanvas.drawText(
                timeFormatter.format(Date(minTime)),
                paddingLeft,
                height - 10f,
                textPaint.apply { textAlign = Paint.Align.LEFT }
            )
            canvas.nativeCanvas.drawText(
                timeFormatter.format(Date(maxTime)),
                width,
                height - 10f,
                textPaint.apply { textAlign = Paint.Align.RIGHT }
            )

            touchX?.let { tx ->
                val clampedTx = tx.coerceIn(paddingLeft, width)
                val ratio = (clampedTx - paddingLeft) / chartWidth
                val targetTime = minTime + (ratio * timeRange).toLong()
                val nearestReading = readings.minByOrNull { Math.abs(it.timestamp - targetTime) }

                nearestReading?.let { nearest ->
                    val nearestX = paddingLeft + ((nearest.timestamp - minTime).toFloat() / timeRange * chartWidth)
                    val nearestY = paddingTop + chartHeight - ((nearest.value - minY) / yRange * chartHeight)

                    drawLine(
                        color = onSurface,
                        start = Offset(nearestX, paddingTop),
                        end = Offset(nearestX, paddingTop + chartHeight),
                        strokeWidth = 3f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    drawCircle(
                        color = onSurface,
                        radius = 12f,
                        center = Offset(nearestX, nearestY)
                    )

                    val text = "${nearest.value.toInt()} mg/dL o ${timeFormatter.format(Date(nearest.timestamp))}"
                    val bubblePaint = Paint().apply {
                        color = inverseSurface
                        isAntiAlias = true
                    }
                    val textBubblePaint = Paint().apply {
                        color = inverseOnSurface
                        textSize = 36f
                        textAlign = Paint.Align.CENTER
                        isFakeBoldText = true
                    }

                    val textWidth = textBubblePaint.measureText(text)
                    val bubbleRect = android.graphics.RectF(
                        nearestX - textWidth / 2 - 20f,
                        paddingTop - 60f,
                        nearestX + textWidth / 2 + 20f,
                        paddingTop - 10f
                    )
                    
                    if (bubbleRect.left < 0) {
                        bubbleRect.offset(-bubbleRect.left + 10f, 0f)
                    } else if (bubbleRect.right > width) {
                        bubbleRect.offset(width - bubbleRect.right - 10f, 0f)
                    }

                    canvas.nativeCanvas.drawRoundRect(bubbleRect, 16f, 16f, bubblePaint)
                    canvas.nativeCanvas.drawText(
                        text,
                        bubbleRect.centerX(),
                        bubbleRect.centerY() + 12f,
                        textBubblePaint
                    )
                }
            }
        }
    }
}
