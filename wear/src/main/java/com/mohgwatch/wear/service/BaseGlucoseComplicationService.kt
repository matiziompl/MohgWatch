package com.mohgwatch.wear.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.model.GlucoseUnit
import com.mohgwatch.core.model.TrendArrow
import com.mohgwatch.core.util.GlucoseFormatter
import com.mohgwatch.wear.R
import com.mohgwatch.wear.data.GlucoseRepository

abstract class BaseGlucoseComplicationService : SuspendingComplicationDataSourceService() {
    
    protected val repository by lazy { GlucoseRepository(this) }
    
    abstract val showGlucose: Boolean
    abstract val showTrend: Boolean
    abstract val isColorCoded: Boolean
    open val trendOnTop: Boolean = true

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val reading = repository.getLatest()
        val isStale = repository.isDataStale()

        if (reading == null || isStale) {
            return buildEmptyData(request)
        }

        return buildData(request, reading)
    }
    
    private fun buildEmptyData(request: ComplicationRequest): ComplicationData? {
        val text = PlainComplicationText.Builder("---").build()
        val desc = PlainComplicationText.Builder("Brak danych").build()
        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(text, desc).build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(text, desc).build()
            ComplicationType.SMALL_IMAGE -> if (showTrend && !showGlucose) {
                SmallImageComplicationData.Builder(
                    smallImage = SmallImage.Builder(
                        image = Icon.createWithResource(this, R.drawable.ic_trend_stable),
                        type = SmallImageType.ICON
                    ).build(),
                    contentDescription = desc
                ).build()
            } else null
            else -> null
        }
    }

    private fun getGlucoseColor(reading: GlucoseReading): Int {
        return when (reading.measurementColor) {
            com.mohgwatch.core.model.MeasurementColor.LOW,
            com.mohgwatch.core.model.MeasurementColor.VERY_HIGH -> Color.parseColor("#EF4444") // Red
            com.mohgwatch.core.model.MeasurementColor.HIGH -> Color.parseColor("#F59E0B") // Yellow
            else -> Color.parseColor("#10B981") // Green
        }
    }
    
    private fun getTrendDrawableRes(trend: TrendArrow): Int = when (trend) {
        TrendArrow.RISING_FAST -> R.drawable.ic_trend_rising_fast
        TrendArrow.RISING -> R.drawable.ic_trend_rising
        TrendArrow.STABLE -> R.drawable.ic_trend_stable
        TrendArrow.FALLING -> R.drawable.ic_trend_falling
        TrendArrow.FALLING_FAST -> R.drawable.ic_trend_falling_fast
        TrendArrow.UNKNOWN -> R.drawable.ic_trend_stable
    }

    private fun createColoredIcon(resId: Int, color: Int): Icon {
        val drawable = ContextCompat.getDrawable(this, resId)!!
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.setTint(color)
        drawable.draw(canvas)
        return Icon.createWithBitmap(bitmap)
    }

    private fun createCombinedIcon(reading: GlucoseReading, colorCoded: Boolean, isTrendOnTop: Boolean): Icon {
        val width = 100
        val height = 100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val color = if (colorCoded) getGlucoseColor(reading) else Color.WHITE
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = 34f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val fm = paint.fontMetrics
        
        val valueText = GlucoseFormatter.format(reading.value, GlucoseUnit.MG_DL)
        val drawable = ContextCompat.getDrawable(this, getTrendDrawableRes(reading.trendArrow))!!
        drawable.setTint(color)
        
        if (isTrendOnTop) {
            drawable.setBounds(34, 12, 66, 44)
            drawable.draw(canvas)
            val textBaselineY = 68f - (fm.ascent + fm.descent) / 2
            canvas.drawText(valueText, 50f, textBaselineY, paint)
        } else {
            val textBaselineY = 32f - (fm.ascent + fm.descent) / 2
            canvas.drawText(valueText, 50f, textBaselineY, paint)
            drawable.setBounds(34, 56, 66, 88)
            drawable.draw(canvas)
        }
        
        return Icon.createWithBitmap(bitmap)
    }

    private fun createTrendIcon(trend: TrendArrow, color: Int): Icon {
        val width = 100
        val height = 100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val drawable = ContextCompat.getDrawable(this, getTrendDrawableRes(trend))!!
        drawable.setTint(color)
        drawable.setBounds(16, 16, 84, 84)
        drawable.draw(canvas)
        return Icon.createWithBitmap(bitmap)
    }

    private fun createGlucoseOnlyIcon(reading: GlucoseReading, color: Int): Icon {
        val width = 100
        val height = 100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = 44f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val fm = paint.fontMetrics
        val valueText = GlucoseFormatter.format(reading.value, GlucoseUnit.MG_DL)
        val textBaselineY = 50f - (fm.ascent + fm.descent) / 2
        canvas.drawText(valueText, 50f, textBaselineY, paint)
        return Icon.createWithBitmap(bitmap)
    }

    private fun buildData(request: ComplicationRequest, reading: GlucoseReading): ComplicationData? {
        val valueText = GlucoseFormatter.format(reading.value, GlucoseUnit.MG_DL)
        val trendSymbol = reading.trendArrow.symbol
        val textColor = if (isColorCoded) getGlucoseColor(reading) else Color.WHITE

        val glucoseSpan = SpannableString(valueText).apply {
            if (isColorCoded) setSpan(ForegroundColorSpan(textColor), 0, length, 0)
        }
        val trendSpan = SpannableString(trendSymbol).apply {
            if (isColorCoded) setSpan(ForegroundColorSpan(textColor), 0, length, 0)
        }

        val glucoseComplicationText = PlainComplicationText.Builder(glucoseSpan).build()
        val trendComplicationText = PlainComplicationText.Builder(trendSpan).build()
        val descText = PlainComplicationText.Builder("Glukoza: $valueText, Trend: $trendSymbol").build()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                if (showGlucose && !showTrend) {
                    ShortTextComplicationData.Builder(glucoseComplicationText, descText).build()
                } else if (!showGlucose && showTrend) {
                    ShortTextComplicationData.Builder(trendComplicationText, descText).build()
                } else {
                    null
                }
            }
            ComplicationType.SMALL_IMAGE -> {
                val icon = if (showGlucose && showTrend) {
                    createCombinedIcon(reading, isColorCoded, trendOnTop)
                } else if (showTrend) {
                    createTrendIcon(reading.trendArrow, if (isColorCoded) getGlucoseColor(reading) else Color.WHITE)
                } else {
                    createGlucoseOnlyIcon(reading, if (isColorCoded) getGlucoseColor(reading) else Color.WHITE)
                }
                SmallImageComplicationData.Builder(
                    smallImage = SmallImage.Builder(icon, SmallImageType.PHOTO).build(),
                    contentDescription = descText
                ).build()
            }
            ComplicationType.MONOCHROMATIC_IMAGE -> {
                if (!isColorCoded) {
                    val icon = if (showGlucose && showTrend) {
                        createCombinedIcon(reading, false, trendOnTop)
                    } else if (showTrend) {
                        createTrendIcon(reading.trendArrow, Color.WHITE)
                    } else {
                        createGlucoseOnlyIcon(reading, Color.WHITE)
                    }
                    MonochromaticImageComplicationData.Builder(
                        monochromaticImage = MonochromaticImage.Builder(icon).build(),
                        contentDescription = descText
                    ).build()
                } else null
            }
            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val mockReading = GlucoseReading(
            value = 120f,
            trendArrow = TrendArrow.STABLE,
            measurementColor = com.mohgwatch.core.model.MeasurementColor.IN_RANGE,
            timestamp = System.currentTimeMillis(),
            isHigh = false,
            isLow = false
        )
        val valueText = "120"
        val trendSymbol = "→"
        val textColor = if (isColorCoded) Color.parseColor("#10B981") else Color.WHITE

        val glucoseSpan = SpannableString(valueText).apply {
            if (isColorCoded) setSpan(ForegroundColorSpan(textColor), 0, length, 0)
        }
        val trendSpan = SpannableString(trendSymbol).apply {
            if (isColorCoded) setSpan(ForegroundColorSpan(textColor), 0, length, 0)
        }

        val glucoseComplicationText = PlainComplicationText.Builder(glucoseSpan).build()
        val trendComplicationText = PlainComplicationText.Builder(trendSpan).build()
        val descText = PlainComplicationText.Builder("Podgląd").build()

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                if (showGlucose && !showTrend) {
                    ShortTextComplicationData.Builder(glucoseComplicationText, descText).build()
                } else if (!showGlucose && showTrend) {
                    ShortTextComplicationData.Builder(trendComplicationText, descText).build()
                } else {
                    null
                }
            }
            ComplicationType.SMALL_IMAGE -> {
                val icon = if (showGlucose && showTrend) {
                    createCombinedIcon(mockReading, isColorCoded, trendOnTop)
                } else if (showTrend) {
                    createTrendIcon(mockReading.trendArrow, textColor)
                } else {
                    createGlucoseOnlyIcon(mockReading, textColor)
                }
                SmallImageComplicationData.Builder(
                    smallImage = SmallImage.Builder(icon, SmallImageType.PHOTO).build(),
                    contentDescription = descText
                ).build()
            }
            ComplicationType.MONOCHROMATIC_IMAGE -> {
                if (!isColorCoded) {
                    val icon = if (showGlucose && showTrend) {
                        createCombinedIcon(mockReading, false, trendOnTop)
                    } else if (showTrend) {
                        createTrendIcon(mockReading.trendArrow, Color.WHITE)
                    } else {
                        createGlucoseOnlyIcon(mockReading, Color.WHITE)
                    }
                    MonochromaticImageComplicationData.Builder(
                        monochromaticImage = MonochromaticImage.Builder(icon).build(),
                        contentDescription = descText
                    ).build()
                } else null
            }
            else -> null
        }
    }
}
