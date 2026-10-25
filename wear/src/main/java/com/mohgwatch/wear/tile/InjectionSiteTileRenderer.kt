package com.mohgwatch.wear.tile

import android.graphics.*
import androidx.compose.ui.graphics.toArgb
import com.mohgwatch.core.util.InjectionSiteHelper
import com.mohgwatch.wear.ui.theme.Teal400

/**
 * Renderuje kafel (Tile) rotacji miejsc wkłucia dla Wear OS w rozdzielczości 466x466 px (OnePlus Watch 2R).
 * Czysty, minimalistyczny design dopasowany do okrągłego ekranu AMOLED:
 * brak jakichkolwiek napisów wewnątrz i na zewnątrz kafelków, obniżony i wycentrowany.
 *
 * Układ siatki 2x3 odpowiada naturalnej perspektywie patrzenia na własny brzuch/uda:
 * Wiersz górny: Lewy dół / Prawy dół
 * Wiersz środkowy: Lewy środek / Prawy środek
 * Wiersz dolny: Lewa góra / Prawa góra
 */
object InjectionSiteTileRenderer {

    private const val TILE_SIZE = 466

    fun renderTile(): Bitmap {
        val bitmap = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val inactiveSlotBg = Color.parseColor("#18212F")
        val inactiveSlotBorder = Color.parseColor("#334155")
        val primaryColor = Teal400.toArgb()

        canvas.drawColor(Color.BLACK)

        val todayIndex = InjectionSiteHelper.getTodayCycleIndex()

        // Siatka 2x3 kafelków (odwrócona perspektywa góra-dół) dopasowana do okrągłego ekranu
        val slotWidth = 116f
        val slotHeight = 72f
        val cornerRadius = 20f
        val colGap = 16f
        val rowGap = 16f

        val totalGridWidth = 2 * slotWidth + colGap
        val startX = (TILE_SIZE - totalGridWidth) / 2f
        // Lekko obniżone kafelki (startY = 124f zamiast wycentrowanego 109f)
        val startY = 124f

        val slotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        val gridSlots = InjectionSiteHelper.GRID_SLOTS // [[0, 1], [2, 3], [4, 5]]

        for (row in gridSlots.indices) {
            for (col in gridSlots[row].indices) {
                val slotIndex = gridSlots[row][col]
                val isToday = (slotIndex == todayIndex)

                val left = startX + col * (slotWidth + colGap)
                val top = startY + row * (slotHeight + rowGap)
                val right = left + slotWidth
                val bottom = top + slotHeight
                val rect = RectF(left, top, right, bottom)

                if (isToday) {
                    slotPaint.color = primaryColor
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, slotPaint)
                } else {
                    slotPaint.color = inactiveSlotBg
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, slotPaint)

                    borderPaint.color = inactiveSlotBorder
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
                }
            }
        }

        // Przycięcie do koła dla idealnego spasowania z okrągłym ekranem AMOLED
        clipCircle(canvas, bitmap)

        return bitmap
    }

    private fun clipCircle(canvas: Canvas, bitmap: Bitmap) {
        val path = Path().apply {
            addCircle(TILE_SIZE / 2f, TILE_SIZE / 2f, TILE_SIZE / 2f, Path.Direction.CCW)
        }
        val maskBitmap = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(maskBitmap)
        maskCanvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK })

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(maskBitmap, 0f, 0f, paint)
        maskBitmap.recycle()
    }
}
