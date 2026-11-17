package com.mohgwatch.phone.ui.screens

import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class DiagnosticReportRangeTest {
    private val warsaw = ZoneId.of("Europe/Warsaw")
    private val now = 1_800_000_000_123L

    @Test fun blankDatesUseTheExactSelectedPeriodEndingAtTheProvidedNow() {
        assertEquals(
            1_799_913_600_123L to 1_800_000_000_123L,
            parseDiagnosticReportRange("", "", 24, now, warsaw)
        )
    }

    @Test fun explicitOffsetsDistinguishTheRepeatedHourDuringWarsawAutumnTimeChange() {
        assertEquals(
            1_792_888_200_000L to 1_792_891_800_000L,
            parseDiagnosticReportRange("2026-10-25T02:30+02:00", "2026-10-25T02:30+01:00", 24, now, warsaw)
        )
    }

    @Test fun localDatesUseTheProvidedZoneAndInvalidOrReversedDatesAreRejected() {
        assertEquals(
            1_768_479_600_000L to 1_768_483_200_000L,
            parseDiagnosticReportRange("2026-01-15 13:20", "2026-01-15 14:20", 24, now, warsaw)
        )
        assertNull(parseDiagnosticReportRange("2026-02-30 12:00", "2026-03-01 14:20", 24, now, warsaw))
        assertNull(parseDiagnosticReportRange("2026-01-15 15:20", "2026-01-15 14:20", 24, now, warsaw))
    }
}
