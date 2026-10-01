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
import com.mohgwatch.wear.data.WearSettingsStore
import com.mohgwatch.wear.tile.InjectionSiteTileRenderer
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream

/**
 * Tile z rotacją miejsc wkłucia na zegarku (Wear OS).
 * Dostępny przez przesunięcie kafelków w Wear OS.
 * Dostosowuje motyw i kolorystykę do aplikacji mobilnej.
 */
class InjectionSiteTileService : TileService() {

    companion object {
        private const val INJECTION_SITE_RESOURCE_ID = "injection_site_tile"
        private const val RESOURCES_VERSION = "1"
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val image = LayoutElementBuilders.Image.Builder()
            .setResourceId(INJECTION_SITE_RESOURCE_ID)
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
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
            .setFreshnessIntervalMillis(60 * 60 * 1000L) // Odświeżanie co godzinę (data zmienia się o północy)
            .build()

        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        val settings = runBlocking { WearSettingsStore(this@InjectionSiteTileService).getSettings() }
        val tileBitmap = InjectionSiteTileRenderer.renderTile(settings)

        val stream = ByteArrayOutputStream()
        tileBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val tileBytes = stream.toByteArray()
        tileBitmap.recycle()

        val inlineImage = ResourceBuilders.InlineImageResource.Builder()
            .setData(tileBytes)
            .setWidthPx(466)
            .setHeightPx(466)
            .setFormat(ResourceBuilders.IMAGE_FORMAT_UNDEFINED)
            .build()

        val imageResource = ResourceBuilders.ImageResource.Builder()
            .setInlineResource(inlineImage)
            .build()

        val resources = ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .addIdToImageMapping(INJECTION_SITE_RESOURCE_ID, imageResource)
            .build()

        return Futures.immediateFuture(resources)
    }
}
