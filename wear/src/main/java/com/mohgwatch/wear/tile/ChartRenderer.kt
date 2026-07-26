package com.mohgwatch.wear.tile

import android.graphics.*
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.core.util.GlucoseFormatter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Renderuje wykres glukozy 12h na Bitmap dla Tile na zegarku.
 * Profesjonalny, AMOLED-friendly design z kolorystycznymi strefami.
 */
object ChartRenderer {

    private const val CHART_SIZE = 384
    private const val PADDING = 70f
    private const val CHART_LEFT = PADDING
    private const val CHART_RIGHT = CHART_SIZE - PADDING
    private const val CHART_TOP = 60f
    private const val CHART_BOTTOM = CHART_SIZE - 90f

    private val bgColor = Color.parseColor("#000000")
    private val axisColor = Color.WHITE
    private val outOfRangeColor = Color.parseColor("#EF4444") // Red
    private val graphColor = Color.WHITE

    fun renderChart(
        readings: List<GlucoseReading>,
        settings: UserSettings
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(CHART_SIZE, CHART_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(bgColor)

        if (readings.isEmpty()) {
            val textNoData = if (settings.language == "en" || (settings.language == "system" && java.util.Locale.getDefault().language == "en")) "No data" else "Brak danych"
            drawNoData(canvas, textNoData)
            clipCircle(canvas, bitmap)
            return bitmap
        }

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val boundaryHour = ((currentHour / 3) + 1) * 3
        
        cal.set(Calendar.HOUR_OF_DAY, boundaryHour)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        
        val maxTime = cal.timeInMillis
        val minTime = maxTime - 6 * 60 * 60 * 1000L
        val timeRange = (maxTime - minTime).coerceAtLeast(1L)
        val filteredReadings = readings.filter { it.timestamp >= minTime && it.timestamp <= maxTime }

        // Sztywny zakres (0-300)
        val minVal = 0f
        val maxVal = 300f
        val valRange = 300f

        val themeColor = getThemeColor(settings)
        val safeZoneColor = Color.argb(60, 16, 185, 129) // Green with alpha

        // 1. Draw Target Zone
        val highY = valueToY(settings.highThreshold, minVal, valRange)
        val lowY = valueToY(settings.lowThreshold, minVal, valRange)
        canvas.drawRect(
            CHART_LEFT, highY, CHART_RIGHT, lowY,
            Paint().apply { color = safeZoneColor; style = Paint.Style.FILL }
        )

        // 1.5 Draw Horizontal Grid (100, 150, 200, 250)
        val gridPaint = Paint().apply {
            color = Color.parseColor("#33FFFFFF") // 20% white
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        for (v in listOf(100f, 150f, 200f, 250f)) {
            val y = valueToY(v, minVal, valRange)
            canvas.drawLine(CHART_LEFT, y, CHART_RIGHT, y, gridPaint)
        }

        val linePaint = Paint().apply { strokeWidth = 0.5f; style = Paint.Style.STROKE; isAntiAlias = true }
        val textPaint = Paint().apply { 
            textSize = 18f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD 
        }

        // 2. Red 200 line & text (zamiast starego 300/180)
        val y200 = valueToY(200f, minVal, valRange)
        linePaint.color = outOfRangeColor
        canvas.drawLine(CHART_LEFT, y200, CHART_RIGHT, y200, linePaint)
        textPaint.color = Color.WHITE
        canvas.drawText("200", CHART_SIZE / 2f, y200 - 8f, textPaint)

        // 3. Theme highThreshold line
        linePaint.color = Color.parseColor("#10B981") // Green
        canvas.drawLine(CHART_LEFT, highY, CHART_RIGHT, highY, linePaint)

        // 4. Theme lowThreshold line
        linePaint.color = Color.parseColor("#10B981") // Green
        canvas.drawLine(CHART_LEFT, lowY, CHART_RIGHT, lowY, linePaint)

        // 5. Red 50 line & text
        val y50 = valueToY(50f, minVal, valRange)
        linePaint.color = outOfRangeColor
        canvas.drawLine(CHART_LEFT, y50, CHART_RIGHT, y50, linePaint)
        textPaint.color = Color.WHITE
        canvas.drawText("50", CHART_SIZE / 2f, y50 + 20f, textPaint)

        // 6. Bottom Axis
        val axisY = CHART_BOTTOM
        linePaint.color = axisColor
        linePaint.strokeWidth = 0.5f
        canvas.drawLine(CHART_LEFT, axisY, CHART_RIGHT, axisY, linePaint)

        // 7. Time Axis Ticks
        drawTimeAxis(canvas, minTime, maxTime, axisY)

        // 8. Graph Line
        drawGlucoseGraph(canvas, filteredReadings, minTime, timeRange, minVal, valRange)

        clipCircle(canvas, bitmap)
        return bitmap
    }

    private fun drawTimeAxis(canvas: Canvas, minTime: Long, maxTime: Long, y: Float) {
        val textPaint = Paint().apply { 
            color = Color.WHITE
            textSize = 16f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD 
        }
        val gridLinePaint = Paint().apply { 
            color = Color.parseColor("#33FFFFFF")
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
            isAntiAlias = true 
        }

        val timeRange = maxTime - minTime
        val cal = Calendar.getInstance().apply { timeInMillis = maxTime }
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // Rysuj pionowe linie oddzielające co 3 godziny (grid)
        var currentTick = cal.timeInMillis
        val hourFmt = SimpleDateFormat("H", Locale.getDefault())
        val y50 = CHART_BOTTOM - ((50f - 0f) / 300f) * (CHART_BOTTOM - CHART_TOP)
        
        while (currentTick >= minTime) {
            val x = timeToX(currentTick, minTime, timeRange)
            canvas.drawLine(x, CHART_BOTTOM, x, CHART_TOP, gridLinePaint)
            
            val hourStr = hourFmt.format(Date(currentTick))
            val displayStr = if (hourStr == "0") "24" else hourStr
            
            val currentPaint = Paint(textPaint)
            if (currentTick == minTime) {
                currentPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(displayStr, x + 5f, y50 + 38f, currentPaint)
            } else if (currentTick == maxTime) {
                currentPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(displayStr, x - 5f, y50 + 38f, currentPaint)
            } else {
                canvas.drawText(displayStr, x, y50 + 38f, currentPaint)
            }
            
            currentTick -= 3 * 60 * 60 * 1000L
        }
    }

    private fun drawGlucoseGraph(
        canvas: Canvas,
        readings: List<GlucoseReading>,
        minTime: Long, timeRange: Long,
        minVal: Float, valRange: Float
    ) {
        if (readings.size < 2) return

        val path = Path()
        var first = true

        for (reading in readings) {
            val x = timeToX(reading.timestamp, minTime, timeRange)
            val y = valueToY(reading.value, minVal, valRange)

            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
        }

        val linePaint = Paint().apply {
            color = graphColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        canvas.drawPath(path, linePaint)
    }

    private fun drawNoData(canvas: Canvas, text: String) {
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 24f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(text, CHART_SIZE / 2f, CHART_SIZE / 2f, paint)
    }

    private fun clipCircle(canvas: Canvas, bitmap: Bitmap) {
        val maskPaint = Paint().apply {
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        val maskCanvas = Canvas(bitmap)
        val circleBitmap = Bitmap.createBitmap(CHART_SIZE, CHART_SIZE, Bitmap.Config.ARGB_8888)
        val cc = Canvas(circleBitmap)
        cc.drawCircle(
            CHART_SIZE / 2f, CHART_SIZE / 2f, CHART_SIZE / 2f,
            Paint().apply { color = Color.WHITE; isAntiAlias = true }
        )
        maskCanvas.drawBitmap(circleBitmap, 0f, 0f, maskPaint)
        circleBitmap.recycle()
    }

    private fun timeToX(timestamp: Long, minTime: Long, timeRange: Long): Float {
        val ratio = (timestamp - minTime).toFloat() / timeRange
        return CHART_LEFT + ratio * (CHART_RIGHT - CHART_LEFT)
    }

    private fun valueToY(value: Float, minVal: Float, valRange: Float): Float {
        // Clamping value to avoid drawing outside
        val clampedValue = value.coerceIn(minVal, minVal + valRange)
        val ratio = (clampedValue - minVal) / valRange
        return CHART_BOTTOM - ratio * (CHART_BOTTOM - CHART_TOP)
    }

    private fun getThemeColor(settings: UserSettings): Int {
        return when (settings.appTheme) {
            com.mohgwatch.core.model.AppTheme.DEFAULT -> Color.parseColor("#3B82F6") // Blue
            com.mohgwatch.core.model.AppTheme.MONOCHROME -> Color.parseColor("#9CA3AF")
            com.mohgwatch.core.model.AppTheme.GRAY -> Color.parseColor("#6B7280")
            com.mohgwatch.core.model.AppTheme.LIGHT_GRAY -> Color.parseColor("#D1D5DB")
            com.mohgwatch.core.model.AppTheme.BLACK -> Color.parseColor("#4B5563")
            com.mohgwatch.core.model.AppTheme.RED -> Color.parseColor("#EF4444")
            com.mohgwatch.core.model.AppTheme.GREEN -> Color.parseColor("#10B981")
            com.mohgwatch.core.model.AppTheme.AMBER -> Color.parseColor("#F59E0B")
            else -> Color.parseColor("#3B82F6")
        }
    }
}
