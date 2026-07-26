package com.mohgwatch.phone.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun MultiSlider(
    value1: Float,
    value2: Float,
    value3: Float,
    onValueChange: (Float, Float, Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float = 1f,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    var dragState by remember { mutableStateOf<Int?>(null) } // 1, 2, or 3 depending on which thumb is dragged
    
    val range = valueRange.endInclusive - valueRange.start
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 12.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val width = size.width.toFloat()
                        val pos1 = ((value1 - valueRange.start) / range) * width
                        val pos2 = ((value2 - valueRange.start) / range) * width
                        val pos3 = ((value3 - valueRange.start) / range) * width
                        
                        val touchRadius = 40f
                        
                        // Find closest thumb
                        val dist1 = Math.abs(offset.x - pos1)
                        val dist2 = Math.abs(offset.x - pos2)
                        val dist3 = Math.abs(offset.x - pos3)
                        
                        dragState = when {
                            dist1 <= touchRadius && dist1 <= dist2 && dist1 <= dist3 -> 1
                            dist2 <= touchRadius && dist2 <= dist1 && dist2 <= dist3 -> 2
                            dist3 <= touchRadius && dist3 <= dist1 && dist3 <= dist2 -> 3
                            else -> null
                        }
                    },
                    onDragEnd = { dragState = null },
                    onDragCancel = { dragState = null },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val width = size.width.toFloat()
                        val x = change.position.x.coerceIn(0f, width)
                        var newValue = valueRange.start + (x / width) * range
                        
                        if (step > 0) {
                            newValue = (newValue / step).roundToInt() * step
                        }
                        
                        newValue = newValue.coerceIn(valueRange)
                        
                        when (dragState) {
                            1 -> {
                                val constrained = newValue.coerceAtMost(value2 - step)
                                onValueChange(constrained, value2, value3)
                            }
                            2 -> {
                                val constrained = newValue.coerceIn(value1 + step, value3 - step)
                                onValueChange(value1, constrained, value3)
                            }
                            3 -> {
                                val constrained = newValue.coerceAtLeast(value2 + step)
                                onValueChange(value1, value2, constrained)
                            }
                        }
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val trackHeight = 12.dp.toPx()
        val thumbRadius = 10.dp.toPx()
        val centerY = height / 2f
        
        val pos1 = ((value1 - valueRange.start) / range) * width
        val pos2 = ((value2 - valueRange.start) / range) * width
        val pos3 = ((value3 - valueRange.start) / range) * width
        
        val c1 = colors.getOrElse(0) { Color.Red }
        val c2 = colors.getOrElse(1) { Color.Green }
        val c3 = colors.getOrElse(2) { Color.Yellow }
        val c4 = colors.getOrElse(3) { Color.Red }

        // Draw track segments
        // Segment 1 (0 to pos1)
        drawRoundRect(
            color = c1,
            topLeft = Offset(0f, centerY - trackHeight / 2),
            size = Size(pos1, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2)
        )
        // Segment 2 (pos1 to pos2)
        drawRect(
            color = c2,
            topLeft = Offset(pos1, centerY - trackHeight / 2),
            size = Size(pos2 - pos1, trackHeight)
        )
        // Segment 3 (pos2 to pos3)
        drawRect(
            color = c3,
            topLeft = Offset(pos2, centerY - trackHeight / 2),
            size = Size(pos3 - pos2, trackHeight)
        )
        // Segment 4 (pos3 to end)
        drawRoundRect(
            color = c4,
            topLeft = Offset(pos3, centerY - trackHeight / 2),
            size = Size(width - pos3, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2)
        )
        
        // Draw thumbs
        val thumbColor = Color.White
        val strokeColor = Color.Gray
        
        listOf(pos1, pos2, pos3).forEach { pos ->
            drawCircle(
                color = strokeColor,
                radius = thumbRadius + 2f,
                center = Offset(pos, centerY)
            )
            drawCircle(
                color = thumbColor,
                radius = thumbRadius,
                center = Offset(pos, centerY)
            )
        }
    }
}
