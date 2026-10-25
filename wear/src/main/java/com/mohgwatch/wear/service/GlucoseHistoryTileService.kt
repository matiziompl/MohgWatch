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
import com.mohgwatch.wear.data.GlucoseRepository
import com.mohgwatch.wear.data.WearSettingsStore
import com.mohgwatch.wear.tile.TileImageCache
import com.mohgwatch.wear.tile.ChartRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

class GlucoseHistoryTileService : TileService() {
    private val repository by lazy { GlucoseRepository(this) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val imageCache by lazy { TileImageCache(File(cacheDir, "glucose_tile_charts")) }

    companion object {
        private const val CHART_RESOURCE_ID = "glucose_chart"
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> = future {
        val version = renderChartResource()
        val image = LayoutElementBuilders.Image.Builder()
            .setResourceId(CHART_RESOURCE_ID)
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .build()
        val box = LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .addContent(image)
            .build()
        val entry = TimelineBuilders.TimelineEntry.Builder()
            .setLayout(LayoutElementBuilders.Layout.Builder().setRoot(box).build())
            .build()
        TileBuilders.Tile.Builder()
            .setResourcesVersion(version)
            .setTileTimeline(TimelineBuilders.Timeline.Builder().addTimelineEntry(entry).build())
            .setFreshnessIntervalMillis(WearSettingsStore(this@GlucoseHistoryTileService).getSettings().staleAfterMillis)
            .build()
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> = future {
        val requestedVersion = requestParams.version
        val bytes = imageCache.read(requestedVersion)
        if (bytes == null) {
            TileService.getUpdater(this@GlucoseHistoryTileService).requestUpdate(GlucoseHistoryTileService::class.java)
            error("Wykres usunięty z pamięci podręcznej; zamówiono aktualizację")
        }
        val inlineImage = ResourceBuilders.InlineImageResource.Builder()
            .setData(bytes)
            .setWidthPx(384)
            .setHeightPx(384)
            .setFormat(ResourceBuilders.IMAGE_FORMAT_UNDEFINED)
            .build()
        ResourceBuilders.Resources.Builder()
            .setVersion(requestedVersion)
            .addIdToImageMapping(
                CHART_RESOURCE_ID,
                ResourceBuilders.ImageResource.Builder().setInlineResource(inlineImage).build()
            )
            .build()
    }

    private suspend fun renderChartResource(): String {
        val readings = repository.getLast12Hours()
        val settings = WearSettingsStore(this).getSettings()
        val bitmap = ChartRenderer.renderChart(readings, settings)
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
                } catch (e: Exception) {
                    completer.setException(e)
                }
            }
            completer.addCancellationListener({ job.cancel() }, java.util.concurrent.Executor { it.run() })
            "MohgWatch glucose tile"
        }
}
