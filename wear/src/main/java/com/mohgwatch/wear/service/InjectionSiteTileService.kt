package com.mohgwatch.wear.service

import android.graphics.Bitmap
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import com.mohgwatch.wear.tile.InjectionSiteTileRenderer
import com.mohgwatch.wear.tile.TileImageCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Tile z rotacją miejsc wkłucia na zegarku (Wear OS).
 * Dostępny przez przesunięcie kafelków w Wear OS.
 */
class InjectionSiteTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val imageCache by lazy { TileImageCache(File(cacheDir, "injection_site_tile_images")) }

    companion object {
        private const val INJECTION_SITE_RESOURCE_ID = "injection_site_tile"
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> = future {
        val version = renderImageResource()
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

        TileBuilders.Tile.Builder()
            .setResourcesVersion(version)
            .setTileTimeline(timeline)
            .setFreshnessIntervalMillis(60 * 60 * 1000L) // Odświeżanie co godzinę (data zmienia się o północy)
            .build()
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> = future {
        val version = requestParams.version
        val tileBytes = imageCache.read(version)
        if (tileBytes == null) {
            TileService.getUpdater(this@InjectionSiteTileService).requestUpdate(InjectionSiteTileService::class.java)
            error("Obraz miejsc wkłuć usunięty z pamięci podręcznej; zamówiono aktualizację")
        }
        val inlineImage = ResourceBuilders.InlineImageResource.Builder()
            .setData(tileBytes)
            .setWidthPx(466)
            .setHeightPx(466)
            .setFormat(ResourceBuilders.IMAGE_FORMAT_UNDEFINED)
            .build()

        val imageResource = ResourceBuilders.ImageResource.Builder()
            .setInlineResource(inlineImage)
            .build()

        ResourceBuilders.Resources.Builder()
            .setVersion(version)
            .addIdToImageMapping(INJECTION_SITE_RESOURCE_ID, imageResource)
            .build()
    }

    private fun renderImageResource(): String {
        val bitmap = InjectionSiteTileRenderer.renderTile()
        val bytes = try {
            ByteArrayOutputStream().use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                stream.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
        return imageCache.put(bytes)
    }

    private fun <T : Any> future(block: suspend () -> T): ListenableFuture<T> =
        CallbackToFutureAdapter.getFuture { completer ->
            val job = scope.launch {
                try {
                    completer.set(block())
                } catch (failure: Exception) {
                    completer.setException(failure)
                }
            }
            completer.addCancellationListener({ job.cancel() }, java.util.concurrent.Executor { it.run() })
            "MohgWatch injection site tile"
        }

}
