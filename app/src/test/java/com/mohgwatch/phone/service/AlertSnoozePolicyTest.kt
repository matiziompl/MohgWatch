package com.mohgwatch.phone.service

import com.mohgwatch.core.model.GlucoseRange
import org.junit.Assert.*
import org.junit.Test

class AlertSnoozePolicyTest {
    private val now = 1_000_000L

    @Test fun `snooze lasts the selected minutes and is capped at thirty`() {
        assertEquals(now + 15 * 60_000L, AlertSnoozePolicy.until(now, 15))
        assertEquals(now + 30 * 60_000L, AlertSnoozePolicy.until(now, 45))
        assertEquals(now, AlertSnoozePolicy.until(now, -3))
    }

    @Test fun `snooze is active only until its end`() {
        val until = AlertSnoozePolicy.until(now, 10)
        assertTrue(AlertSnoozePolicy.isActive(until, now))
        assertTrue(AlertSnoozePolicy.isActive(until, until - 1))
        assertFalse(AlertSnoozePolicy.isActive(until, until))
        assertFalse(AlertSnoozePolicy.isActive(0L, now))
    }

    @Test fun `a clock moved backwards cannot extend the snooze beyond thirty minutes`() {
        val until = AlertSnoozePolicy.until(now, 30)
        assertFalse(AlertSnoozePolicy.isActive(until, now - 60 * 60_000L))
    }

    @Test fun `hazard silenced during snooze is announced to the watch once it ends`() {
        assertEquals(GlucoseRange.LOW, AlertSnoozePolicy.resumedHazard(GlucoseRange.LOW, GlucoseRange.LOW, snoozed = false))
        assertNull(AlertSnoozePolicy.resumedHazard(GlucoseRange.LOW, GlucoseRange.LOW, snoozed = true))
        assertNull(AlertSnoozePolicy.resumedHazard(GlucoseRange.LOW, GlucoseRange.NORMAL, snoozed = false))
        assertNull(AlertSnoozePolicy.resumedHazard(null, GlucoseRange.HIGH, snoozed = false))
    }
}
