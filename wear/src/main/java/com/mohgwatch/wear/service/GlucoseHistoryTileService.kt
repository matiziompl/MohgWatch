package com.mohgwatch.wear.service

import android.graphics.Bitmap
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.wear.data.GlucoseRepository
import com.mohgwatch.wear.tile.ChartRenderer
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream

/**
 * Tile z 12-godzinną historią glukozy (wykres).
 * Dostępny przez przesunięcie w lewo od tarczy.
 */
class GlucoseHistoryTileService : TileService() {

    private val repository by lazy { GlucoseRepository(this) }

    companion object {
        private const val CHART_RESOURCE_ID = "glucose_chart"
        private const val RESOURCES_VERSION = "1"
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val readings = runBlocking { repository.getLast12Hours() }

        val image = LayoutElementBuilders.Image.Builder()
            .setResourceId(CHART_RESOURCE_ID)
            .setWidth(DimensionBuilders.dp(384f))
            .setHeight(DimensionBuilders.dp(384f))
            .build()

        val box = LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .addContent(image)
            .build()

        val layout = LayoutElementBuilders.Layout.Builder()
            .setRoot(box)
            .build()

        val entry = TimelineBuilders.TimelineEntry.Builder()
            .setLayout(layout)
            .build()

        val timeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(entry)
            .build()

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(timeline)
            .setFreshnessIntervalMillis(15 * 60 * 1000L)
            .build()

        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        val readings = runBlocking { repository.getLast12Hours() }
        val settings = runBlocking { com.mohgwatch.wear.data.WearSettingsStore(this@GlucoseHistoryTileService).getSettings() }
        val chartBitmap = ChartRenderer.renderChart(readings, settings)

        val stream = ByteArrayOutputStream()
        chartBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val chartBytes = stream.toByteArray()
        chartBitmap.recycle()

        val inlineImage = ResourceBuilders.InlineImageResource.Builder()
            .setData(chartBytes)
            .setWidthPx(384)
            .setHeightPx(384)
            .setFormat(ResourceBuilders.IMAGE_FORMAT_UNDEFINED)
            .build()

        val imageResource = ResourceBuilders.ImageResource.Builder()
            .setInlineResource(inlineImage)
            .build()

        val resources = ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .addIdToImageMapping(CHART_RESOURCE_ID, imageResource)
            .build()

        return Futures.immediateFuture(resources)
    }
}
