package com.mohgwatch.wear.tile

import android.graphics.*
import com.mohgwatch.core.model.AppTheme
import com.mohgwatch.core.model.ThemeMode
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.core.util.InjectionSiteHelper

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

    fun renderTile(settings: UserSettings): Bitmap {
        val bitmap = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val isLight = settings.themeMode == ThemeMode.LIGHT
        val bgColor = if (isLight) Color.parseColor("#F8FAFC") else Color.parseColor("#000000")
        val inactiveSlotBg = if (isLight) Color.parseColor("#E2E8F0") else Color.parseColor("#18212F")
        val inactiveSlotBorder = if (isLight) Color.parseColor("#CBD5E1") else Color.parseColor("#334155")

        val effectiveTheme = if (!settings.syncThemeWithWatch && settings.watchAppTheme != AppTheme.DEFAULT) {
            settings.watchAppTheme
        } else {
            settings.appTheme
        }

        val primaryColor = getPrimaryColor(effectiveTheme, isLight)

        canvas.drawColor(bgColor)

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

    private fun getPrimaryColor(theme: AppTheme, isLight: Boolean): Int {
        return when (theme) {
            AppTheme.DEFAULT, AppTheme.TEAL, AppTheme.MATCH_BACKGROUND, AppTheme.MATCH_GLUCOSE -> Color.parseColor(if (isLight) "#0D9488" else "#2DD4BF")
            AppTheme.BLUE -> Color.parseColor(if (isLight) "#2563EB" else "#60A5FA")
            AppTheme.RED -> Color.parseColor(if (isLight) "#DC2626" else "#F87171")
            AppTheme.GREEN -> Color.parseColor(if (isLight) "#16A34A" else "#4ADE80")
            AppTheme.PURPLE -> Color.parseColor(if (isLight) "#9333EA" else "#C084FC")
            AppTheme.ORANGE -> Color.parseColor(if (isLight) "#EA580C" else "#FB923C")
            AppTheme.PINK -> Color.parseColor(if (isLight) "#DB2777" else "#F472B6")
            AppTheme.AMBER -> Color.parseColor(if (isLight) "#D97706" else "#FBBF24")
            AppTheme.CYAN -> Color.parseColor(if (isLight) "#0891B2" else "#22D3EE")
            AppTheme.MONOCHROME -> Color.parseColor(if (isLight) "#525252" else "#A3A3A3")
            AppTheme.GRAY -> Color.parseColor(if (isLight) "#4B5563" else "#9CA3AF")
            AppTheme.LIGHT_GRAY -> Color.parseColor(if (isLight) "#64748B" else "#CBD5E1")
            AppTheme.BLACK -> Color.parseColor(if (isLight) "#171717" else "#E2E8F0")
        }
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
