package com.mohgwatch.wear.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchStaleDataPolicyTest {
    @Test
    fun `cached reading expires at ten minutes since measurement`() {
        assertTrue(WatchStaleDataPolicy.isFresh(1_000_000, 1_599_999))
        assertFalse(WatchStaleDataPolicy.isFresh(1_000_000, 1_600_000))
        assertFalse(WatchStaleDataPolicy.isFresh(1_000_000, 2_000_000))
    }

    @Test
    fun `stale episode alerts once and respects disabled notifications`() {
        assertTrue(WatchStaleDataPolicy.shouldAlert(1_000_000, 1_600_000, true, false))
        assertFalse(WatchStaleDataPolicy.shouldAlert(1_000_000, 1_600_000, true, true))
        assertFalse(WatchStaleDataPolicy.shouldAlert(1_000_000, 1_600_000, false, false))
        assertFalse(WatchStaleDataPolicy.shouldAlert(1_000_000, 1_599_999, true, false))
        assertFalse(WatchStaleDataPolicy.shouldAlert(null, 1_600_000, true, false))
    }

    @Test
    fun `missing invalid and future timestamps do not appear fresh`() {
        assertFalse(WatchStaleDataPolicy.isFresh(null, 1_000_000))
        assertFalse(WatchStaleDataPolicy.isFresh(0, 1_000_000))
        assertFalse(WatchStaleDataPolicy.isFresh(1_000_001, 1_000_000))
    }

    @Test
    fun `selected timeout expires at its exact boundary`() {
        val timestamp = 1_000_000L
        for (minutes in listOf(3, 15, 60)) {
            val timeout = minutes * 60_000L
            assertTrue(WatchStaleDataPolicy.isFresh(timestamp, timestamp + timeout - 1, timeout))
            assertFalse(WatchStaleDataPolicy.isFresh(timestamp, timestamp + timeout, timeout))
            assertTrue(WatchStaleDataPolicy.shouldAlert(timestamp, timestamp + timeout, true, false, timeout))
        }
    }

    @Test
    fun `changing timeout reclassifies the same cached measurement`() {
        val timestamp = 1_000_000L
        val now = timestamp + 12 * 60_000L
        assertFalse(WatchStaleDataPolicy.isFresh(timestamp, now, 3 * 60_000L))
        assertTrue(WatchStaleDataPolicy.isFresh(timestamp, now, 15 * 60_000L))
        assertFalse(WatchStaleDataPolicy.shouldAlert(timestamp, now, true, false, 15 * 60_000L))
    }

    @Test
    fun `future measurement remains invalid with a longer timeout`() {
        assertFalse(WatchStaleDataPolicy.isFresh(1_000_001, 1_000_000, 60 * 60_000L))
        assertFalse(WatchStaleDataPolicy.shouldAlert(null, 1_000_000, true, false, 3 * 60_000L))
    }
}
