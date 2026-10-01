package com.mohgwatch.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class InjectionSiteHelperTest {

    @Test
    fun testCycleIndexCalculation() {
        assertEquals(0, InjectionSiteHelper.getCycleIndex(0L))
        assertEquals(1, InjectionSiteHelper.getCycleIndex(1L))
        assertEquals(5, InjectionSiteHelper.getCycleIndex(5L))
        assertEquals(0, InjectionSiteHelper.getCycleIndex(6L))
        assertEquals(1, InjectionSiteHelper.getCycleIndex(7L))
    }

    @Test
    fun testShortSiteNames() {
        assertEquals("Lewy dół (brzuch/udo)", InjectionSiteHelper.getShortSiteName(0))
        assertEquals("Prawy dół (brzuch/udo)", InjectionSiteHelper.getShortSiteName(1))
        assertEquals("Lewy środek (brzuch/udo)", InjectionSiteHelper.getShortSiteName(2))
        assertEquals("Prawy środek (brzuch/udo)", InjectionSiteHelper.getShortSiteName(3))
        assertEquals("Lewa góra (brzuch/udo)", InjectionSiteHelper.getShortSiteName(4))
        assertEquals("Prawa góra (brzuch/udo)", InjectionSiteHelper.getShortSiteName(5))
    }

    @Test
    fun testInvertedGridMapping() {
        // User field-of-view perspective: looking down at own body,
        // lower abdomen is in top field of vision:
        // Row 0 (Top): Slot 0 (Left), Slot 1 (Right)
        // Row 1 (Mid): Slot 2 (Left), Slot 3 (Right)
        // Row 2 (Bottom): Slot 4 (Left), Slot 5 (Right)
        val grid = InjectionSiteHelper.GRID_SLOTS
        assertEquals(listOf(0, 1), grid[0])
        assertEquals(listOf(2, 3), grid[1])
        assertEquals(listOf(4, 5), grid[2])
    }
}
