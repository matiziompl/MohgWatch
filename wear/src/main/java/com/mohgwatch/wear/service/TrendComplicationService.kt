package com.mohgwatch.wear.service

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.mohgwatch.core.model.TrendArrow
import com.mohgwatch.wear.R
import com.mohgwatch.wear.data.GlucoseRepository

/**
 * Dostawca komplikacji wyświetlający strzałkę trendu glukozy.
 * Obsługuje: SHORT_TEXT (symbol), SMALL_IMAGE (ikona strzałki), MONOCHROMATIC_IMAGE.
 */
class TrendComplicationService : SuspendingComplicationDataSourceService() {

    private val repository by lazy { GlucoseRepository(this) }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val reading = repository.getLatest()
        val isStale = repository.isDataStale()

        if (reading == null || isStale) {
            return when (request.complicationType) {
                ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("?").build(),
                    contentDescription = PlainComplicationText.Builder("Brak danych trendu").build()
                ).build()
                else -> null
            }
        }

        val trendIcon = getTrendDrawable(reading.trendArrow)
        val trendSymbol = reading.trendArrow.symbol
        val trendDesc = reading.trendArrow.description

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(trendSymbol).build(),
                contentDescription = PlainComplicationText.Builder("Trend: $trendDesc").build()
            ).build()

            ComplicationType.SMALL_IMAGE -> SmallImageComplicationData.Builder(
                smallImage = SmallImage.Builder(
                    image = Icon.createWithResource(this, trendIcon),
                    type = SmallImageType.ICON
                ).build(),
                contentDescription = PlainComplicationText.Builder("Trend: $trendDesc").build()
            ).build()

            ComplicationType.MONOCHROMATIC_IMAGE -> MonochromaticImageComplicationData.Builder(
                monochromaticImage = MonochromaticImage.Builder(
                    Icon.createWithResource(this, trendIcon)
                ).build(),
                contentDescription = PlainComplicationText.Builder("Trend: $trendDesc").build()
            ).build()

            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("→").build(),
                contentDescription = PlainComplicationText.Builder("Trend stabilny").build()
            ).build()

            ComplicationType.SMALL_IMAGE -> SmallImageComplicationData.Builder(
                smallImage = SmallImage.Builder(
                    image = Icon.createWithResource(this, R.drawable.ic_trend_stable),
                    type = SmallImageType.ICON
                ).build(),
                contentDescription = PlainComplicationText.Builder("Trend stabilny").build()
            ).build()

            else -> null
        }
    }

    private fun getTrendDrawable(trend: TrendArrow): Int = when (trend) {
        TrendArrow.RISING_FAST -> R.drawable.ic_trend_rising_fast
        TrendArrow.RISING -> R.drawable.ic_trend_rising
        TrendArrow.STABLE -> R.drawable.ic_trend_stable
        TrendArrow.FALLING -> R.drawable.ic_trend_falling
        TrendArrow.FALLING_FAST -> R.drawable.ic_trend_falling_fast
        TrendArrow.UNKNOWN -> R.drawable.ic_trend_stable
    }
}
