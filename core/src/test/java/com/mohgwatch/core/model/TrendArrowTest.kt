package com.mohgwatch.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TrendArrowTest {

    @Test
    fun testAbbottLibreLinkUpTrendArrowMapping() {
        // Abbott LibreLinkUp API standard mapping:
        // 1: Falling Quickly (↓)
        // 2: Falling (↘)
        // 3: Stable (→)
        // 4: Rising (↗)
        // 5: Rising Quickly (↑)
        assertEquals(TrendArrow.FALLING_FAST, TrendArrow.fromApiValue(1))
        assertEquals("↓", TrendArrow.fromApiValue(1).symbol)

        assertEquals(TrendArrow.FALLING, TrendArrow.fromApiValue(2))
        assertEquals("↘", TrendArrow.fromApiValue(2).symbol)

        assertEquals(TrendArrow.STABLE, TrendArrow.fromApiValue(3))
        assertEquals("→", TrendArrow.fromApiValue(3).symbol)

        assertEquals(TrendArrow.RISING, TrendArrow.fromApiValue(4))
        assertEquals("↗", TrendArrow.fromApiValue(4).symbol)

        assertEquals(TrendArrow.RISING_FAST, TrendArrow.fromApiValue(5))
        assertEquals("↑", TrendArrow.fromApiValue(5).symbol)

        assertEquals(TrendArrow.UNKNOWN, TrendArrow.fromApiValue(0))
        assertEquals(TrendArrow.UNKNOWN, TrendArrow.fromApiValue(99))
    }
}
