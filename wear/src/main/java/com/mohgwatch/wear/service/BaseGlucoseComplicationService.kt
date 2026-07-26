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
            textSize = 42f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        val valueText = GlucoseFormatter.format(reading.value, GlucoseUnit.MG_DL)
        
        val drawable = ContextCompat.getDrawable(this, getTrendDrawableRes(reading.trendArrow))!!
        drawable.setTint(color)
        
        if (isTrendOnTop) {
            drawable.setBounds(30, 9, 70, 49)
            drawable.draw(canvas)
            canvas.drawText(valueText, 50f, 77f, paint)
        } else {
            canvas.drawText(valueText, 50f, 53f, paint)
            drawable.setBounds(30, 51, 70, 91)
            drawable.draw(canvas)
        }
        
        return Icon.createWithBitmap(bitmap)
    }

    private fun buildData(request: ComplicationRequest, reading: GlucoseReading): ComplicationData? {
        val valueText = GlucoseFormatter.format(reading.value, GlucoseUnit.MG_DL)
        val trendSymbol = reading.trendArrow.symbol
        
        val textColor = if (isColorCoded) getGlucoseColor(reading) else Color.WHITE
        
        val primaryStr = if (trendOnTop) {
            if (showGlucose) valueText else trendSymbol
        } else {
            trendSymbol
        }

        val textSpan = SpannableString(primaryStr)
        if (isColorCoded) {
            textSpan.setSpan(ForegroundColorSpan(textColor), 0, textSpan.length, 0)
        }
        val mainText = PlainComplicationText.Builder(textSpan).build()
        val descText = PlainComplicationText.Builder("Glukoza: $valueText, Trend: $trendSymbol").build()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                val builder = ShortTextComplicationData.Builder(mainText, descText)
                builder.build()
            }
            ComplicationType.SMALL_IMAGE -> {
                if (showGlucose && showTrend) {
                    val icon = createCombinedIcon(reading, isColorCoded, trendOnTop)
                    SmallImageComplicationData.Builder(
                        smallImage = SmallImage.Builder(icon, SmallImageType.PHOTO).build(),
                        contentDescription = descText
                    ).build()
                } else if (!showGlucose && showTrend) {
                    if (isColorCoded) {
                        val icon = createColoredIcon(getTrendDrawableRes(reading.trendArrow), getGlucoseColor(reading))
                        SmallImageComplicationData.Builder(
                            smallImage = SmallImage.Builder(icon, SmallImageType.PHOTO).build(),
                            contentDescription = descText
                        ).build()
                    } else {
                        val icon = Icon.createWithResource(this, getTrendDrawableRes(reading.trendArrow))
                        SmallImageComplicationData.Builder(
                            smallImage = SmallImage.Builder(icon, SmallImageType.ICON).build(),
                            contentDescription = descText
                        ).build()
                    }
                } else null
            }
            ComplicationType.MONOCHROMATIC_IMAGE -> {
                if (showGlucose && showTrend && !isColorCoded) {
                    val icon = createCombinedIcon(reading, false, trendOnTop)
                    MonochromaticImageComplicationData.Builder(
                        monochromaticImage = MonochromaticImage.Builder(icon).build(),
                        contentDescription = descText
                    ).build()
                } else if (!showGlucose && showTrend && !isColorCoded) {
                    val icon = Icon.createWithResource(this, getTrendDrawableRes(reading.trendArrow))
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
        val primaryStr = if (trendOnTop) {
            if (showGlucose) "120" else "→"
        } else {
            "→"
        }

        val textSpan = SpannableString(primaryStr)
        if (isColorCoded) {
            textSpan.setSpan(ForegroundColorSpan(Color.parseColor("#10B981")), 0, textSpan.length, 0)
        }
        val mainText = PlainComplicationText.Builder(textSpan).build()
        val descText = PlainComplicationText.Builder("Podgląd").build()

        val mockReading = GlucoseReading(
            value = 120f,
            trendArrow = TrendArrow.STABLE,
            measurementColor = com.mohgwatch.core.model.MeasurementColor.IN_RANGE,
            timestamp = System.currentTimeMillis(),
            isHigh = false,
            isLow = false
        )

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                val builder = ShortTextComplicationData.Builder(mainText, descText)
                builder.build()
            }
            ComplicationType.SMALL_IMAGE -> {
                if (showGlucose && showTrend) {
                    val icon = createCombinedIcon(mockReading, isColorCoded, trendOnTop)
                    SmallImageComplicationData.Builder(
                        smallImage = SmallImage.Builder(icon, SmallImageType.PHOTO).build(),
                        contentDescription = descText
                    ).build()
                } else if (!showGlucose && showTrend) {
                    if (isColorCoded) {
                        val icon = createColoredIcon(R.drawable.ic_trend_stable, Color.parseColor("#10B981"))
                        SmallImageComplicationData.Builder(
                            smallImage = SmallImage.Builder(icon, SmallImageType.PHOTO).build(),
                            contentDescription = descText
                        ).build()
                    } else {
                        val icon = Icon.createWithResource(this, R.drawable.ic_trend_stable)
                        SmallImageComplicationData.Builder(
                            smallImage = SmallImage.Builder(icon, SmallImageType.ICON).build(),
                            contentDescription = descText
                        ).build()
                    }
                } else null
            }
            ComplicationType.MONOCHROMATIC_IMAGE -> {
                if (showGlucose && showTrend && !isColorCoded) {
                    val icon = createCombinedIcon(mockReading, false, trendOnTop)
                    MonochromaticImageComplicationData.Builder(
                        monochromaticImage = MonochromaticImage.Builder(icon).build(),
                        contentDescription = descText
                    ).build()
                } else if (!showGlucose && showTrend && !isColorCoded) {
                    val icon = Icon.createWithResource(this, R.drawable.ic_trend_stable)
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
