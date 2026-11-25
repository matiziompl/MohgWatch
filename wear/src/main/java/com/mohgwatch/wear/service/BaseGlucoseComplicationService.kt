package com.mohgwatch.wear.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationDataTimeline
import androidx.wear.watchface.complications.datasource.TimeInterval
import androidx.wear.watchface.complications.datasource.TimelineEntry
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.model.GlucoseUnit
import com.mohgwatch.core.model.TrendArrow
import com.mohgwatch.core.util.GlucoseFormatter
import com.mohgwatch.wear.R
import com.mohgwatch.wear.data.GlucoseRepository
import com.mohgwatch.wear.data.WearSettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant

abstract class BaseGlucoseComplicationService : ComplicationDataSourceService() {
    
    protected val repository by lazy { GlucoseRepository(this) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    abstract val showGlucose: Boolean
    abstract val showTrend: Boolean
    open val trendOnTop: Boolean = true

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onComplicationRequest(request: ComplicationRequest, listener: ComplicationRequestListener) {
        scope.launch {
            val empty = buildEmptyData(request) ?: run {
                listener.onComplicationData(null)
                return@launch
            }
            try {
                val settings = WearSettingsStore(this@BaseGlucoseComplicationService).getSettings()
                val reading = repository.getLatest()
                if (reading == null || !WatchStaleDataPolicy.isFresh(reading.timestamp, System.currentTimeMillis(), settings.staleAfterMillis)) {
                    listener.onComplicationData(empty)
                    return@launch
                }
                val data = buildData(request, reading, settings.unit, settings.staleAfterMillis) ?: run {
                    listener.onComplicationData(empty)
                    return@launch
                }
                // The watch face switches to the default even if no further callback arrives.
                listener.onComplicationDataTimeline(ComplicationDataTimeline(empty, listOf(
                    TimelineEntry(TimeInterval(
                        Instant.ofEpochMilli(reading.timestamp),
                        Instant.ofEpochMilli(reading.timestamp + settings.staleAfterMillis)
                    ), data)
                )))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("MohgWatchComplication", "Nie można odczytać danych komplikacji", error)
                listener.onComplicationData(empty)
            }
        }
    }
    
    private fun buildEmptyData(request: ComplicationRequest): ComplicationData? {
        val text = PlainComplicationText.Builder("- -").build()
        val desc = PlainComplicationText.Builder("Brak danych").build()
        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(text, desc).build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(text, desc).build()
            ComplicationType.SMALL_IMAGE -> {
                SmallImageComplicationData.Builder(
                    smallImage = SmallImage.Builder(
                        image = createTextIcon("- -", Color.WHITE),
                        type = SmallImageType.PHOTO
                    ).build(),
                    contentDescription = desc
                ).build()
            }
            ComplicationType.MONOCHROMATIC_IMAGE -> MonochromaticImageComplicationData.Builder(
                monochromaticImage = MonochromaticImage.Builder(
                    createTextIcon("- -", Color.WHITE)
                ).build(),
                contentDescription = desc
            ).build()
            else -> null
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

    private fun createCombinedIcon(reading: GlucoseReading, isTrendOnTop: Boolean, unit: GlucoseUnit = GlucoseUnit.MG_DL): Icon {
        val width = 100
        val height = 100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val color = Color.WHITE
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = 34f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val fm = paint.fontMetrics
        
        val valueText = GlucoseFormatter.format(reading.value, unit)
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

    private fun createTextIcon(valueText: String, color: Int): Icon {
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
        val textBaselineY = 50f - (fm.ascent + fm.descent) / 2
        canvas.drawText(valueText, 50f, textBaselineY, paint)
        return Icon.createWithBitmap(bitmap)
    }

    private fun buildData(request: ComplicationRequest, reading: GlucoseReading, unit: GlucoseUnit, timeoutMillis: Long): ComplicationData? {
        val validTimeRange = TimeRange.between(
            Instant.ofEpochMilli(reading.timestamp),
            Instant.ofEpochMilli(reading.timestamp + timeoutMillis)
        )
        val valueText = GlucoseFormatter.format(reading.value, unit)
        val trendSymbol = reading.trendArrow.symbol
        val glucoseComplicationText = PlainComplicationText.Builder(valueText).build()
        val trendComplicationText = PlainComplicationText.Builder(trendSymbol).build()
        val descText = PlainComplicationText.Builder("Glukoza: $valueText ${unit.label}, Trend: $trendSymbol").build()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                if (showGlucose && !showTrend) {
                    ShortTextComplicationData.Builder(glucoseComplicationText, descText)
                        .setValidTimeRange(validTimeRange).build()
                } else if (!showGlucose && showTrend) {
                    ShortTextComplicationData.Builder(trendComplicationText, descText)
                        .setValidTimeRange(validTimeRange).build()
                } else {
                    null
                }
            }
            ComplicationType.LONG_TEXT -> {
                val text = if (showGlucose && showTrend) "$valueText $trendSymbol" else if (showGlucose) valueText else trendSymbol
                LongTextComplicationData.Builder(PlainComplicationText.Builder(text).build(), descText)
                    .setValidTimeRange(validTimeRange).build()
            }
            ComplicationType.SMALL_IMAGE -> {
                val icon = if (showGlucose && showTrend) {
                    createCombinedIcon(reading, trendOnTop, unit)
                } else if (showTrend) {
                    createTrendIcon(reading.trendArrow, Color.WHITE)
                } else {
                    createTextIcon(valueText, Color.WHITE)
                }
                SmallImageComplicationData.Builder(
                    smallImage = SmallImage.Builder(icon, SmallImageType.PHOTO).build(),
                    contentDescription = descText
                ).setValidTimeRange(validTimeRange).build()
            }
            ComplicationType.MONOCHROMATIC_IMAGE -> {
                val icon = if (showGlucose && showTrend) {
                    createCombinedIcon(reading, trendOnTop, unit)
                } else if (showTrend) {
                    createTrendIcon(reading.trendArrow, Color.WHITE)
                } else {
                    createTextIcon(valueText, Color.WHITE)
                }
                MonochromaticImageComplicationData.Builder(
                    monochromaticImage = MonochromaticImage.Builder(icon).build(),
                    contentDescription = descText
                ).setValidTimeRange(validTimeRange).build()
            }
            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val mockReading = GlucoseReading(
            value = 120f,
            trendArrow = TrendArrow.STABLE,
            timestamp = System.currentTimeMillis(),
            isHigh = false,
            isLow = false
        )
        val valueText = "120"
        val trendSymbol = "→"
        val glucoseComplicationText = PlainComplicationText.Builder(valueText).build()
        val trendComplicationText = PlainComplicationText.Builder(trendSymbol).build()
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
            ComplicationType.LONG_TEXT -> {
                val text = if (showGlucose && showTrend) "$valueText $trendSymbol" else if (showGlucose) valueText else trendSymbol
                LongTextComplicationData.Builder(PlainComplicationText.Builder(text).build(), descText).build()
            }
            ComplicationType.SMALL_IMAGE -> {
                val icon = if (showGlucose && showTrend) {
                    createCombinedIcon(mockReading, trendOnTop)
                } else if (showTrend) {
                    createTrendIcon(mockReading.trendArrow, Color.WHITE)
                } else {
                    createTextIcon(valueText, Color.WHITE)
                }
                SmallImageComplicationData.Builder(
                    smallImage = SmallImage.Builder(icon, SmallImageType.PHOTO).build(),
                    contentDescription = descText
                ).build()
            }
            ComplicationType.MONOCHROMATIC_IMAGE -> {
                val icon = if (showGlucose && showTrend) {
                    createCombinedIcon(mockReading, trendOnTop)
                } else if (showTrend) {
                    createTrendIcon(mockReading.trendArrow, Color.WHITE)
                } else {
                    createTextIcon(valueText, Color.WHITE)
                }
                MonochromaticImageComplicationData.Builder(
                    monochromaticImage = MonochromaticImage.Builder(icon).build(),
                    contentDescription = descText
                ).build()
            }
            else -> null
        }
    }
}
