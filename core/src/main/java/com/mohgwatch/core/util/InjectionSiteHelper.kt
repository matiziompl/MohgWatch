package com.mohgwatch.core.util

import java.time.LocalDate

object InjectionSiteHelper {

    /**
     * Układ siatki z perspektywy patrzenia w dół na własne ciało:
     * Wiersz 0 (góra w polu widzenia): Lewy dół (0), Prawy dół (1)
     * Wiersz 1 (środek): Lewy środek (2), Prawy środek (3)
     * Wiersz 2 (dół w polu widzenia): Lewa góra (4), Prawa góra (5)
     */
    val GRID_SLOTS: List<List<Int>> = listOf(
        listOf(0, 1),
        listOf(2, 3),
        listOf(4, 5)
    )

    fun getTodayCycleIndex(): Int {
        val epochDay = LocalDate.now().toEpochDay()
        return getCycleIndex(epochDay)
    }

    fun getCycleIndex(epochDay: Long): Int {
        val mod = (epochDay % 6).toInt()
        return if (mod < 0) mod + 6 else mod
    }

    fun getSiteName(cycleIndex: Int): String = when (cycleIndex) {
        0 -> "Lewa dolna część brzucha - Dół lewego uda"
        1 -> "Prawa dolna część brzucha - Dół prawego uda"
        2 -> "Lewa środkowa część brzucha - Środek lewego uda"
        3 -> "Prawa środkowa część brzucha - Środek prawego uda"
        4 -> "Lewa górna część brzucha - Góra lewego uda"
        5 -> "Prawa górna część brzucha - Góra prawego uda"
        else -> ""
    }

    fun getShortSiteName(cycleIndex: Int): String = when (cycleIndex) {
        0 -> "Lewy dół (brzuch/udo)"
        1 -> "Prawy dół (brzuch/udo)"
        2 -> "Lewy środek (brzuch/udo)"
        3 -> "Prawy środek (brzuch/udo)"
        4 -> "Lewa góra (brzuch/udo)"
        5 -> "Prawa góra (brzuch/udo)"
        else -> ""
    }
}
