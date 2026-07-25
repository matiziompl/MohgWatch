package com.mohgwatch.wear.service

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.mohgwatch.core.model.GlucoseUnit
import com.mohgwatch.core.util.GlucoseFormatter
import com.mohgwatch.wear.R
import com.mohgwatch.wear.data.GlucoseRepository

/**
 * Dostawca komplikacji wyświetlający wartość glukozy na tarczy WFF.
 * Obsługuje typy: SHORT_TEXT (wartość), RANGED_VALUE (pasek zakresu).
 */
class GlucoseComplicationService : SuspendingComplicationDataSourceService() {

    private val repository by lazy { GlucoseRepository(this) }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val reading = repository.getLatest()
        val isStale = repository.isDataStale()

        // Brak danych lub dane > 60 min
        if (reading == null || isStale) {
            return when (request.complicationType) {
                ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("---").build(),
                    contentDescription = PlainComplicationText.Builder("Brak danych glukozy").build()
                ).build()
                else -> null
            }
        }

        val valueText = GlucoseFormatter.format(reading.value, GlucoseUnit.MG_DL)
        val minutesAgo = reading.getMinutesAgo()
        val trendSymbol = reading.trendArrow.symbol

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(valueText).build(),
                    contentDescription = PlainComplicationText.Builder(
                        "Glukoza: $valueText mg/dL $trendSymbol, ${GlucoseFormatter.formatMinutesAgo(minutesAgo)}"
                    ).build()
                ).setTitle(
                    PlainComplicationText.Builder(trendSymbol).build()
                ).build()
            }

            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = reading.value,
                    min = 40f,
                    max = 300f,
                    contentDescription = PlainComplicationText.Builder(
                        "Glukoza: $valueText mg/dL"
                    ).build()
                ).setText(
                    PlainComplicationText.Builder(valueText).build()
                ).setTitle(
                    PlainComplicationText.Builder(trendSymbol).build()
                ).build()
            }

            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("120").build(),
                contentDescription = PlainComplicationText.Builder("Podgląd glukozy").build()
            ).setTitle(
                PlainComplicationText.Builder("→").build()
            ).build()

            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = 120f, min = 40f, max = 300f,
                contentDescription = PlainComplicationText.Builder("Podgląd glukozy").build()
            ).setText(PlainComplicationText.Builder("120").build())
                .setTitle(PlainComplicationText.Builder("→").build())
                .build()

            else -> null
        }
    }
}
