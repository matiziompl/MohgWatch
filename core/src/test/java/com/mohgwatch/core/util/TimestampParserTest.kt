package com.mohgwatch.core.util

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

class TimestampParserTest {
    @Test fun localTimestampUsesCurrentZoneAfterADeviceZoneChange() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            assertEquals(Instant.parse("2026-10-24T01:00:00Z").toEpochMilli(),
                TimestampParser.parseTimestamp("10/24/2026 1:00:00 AM"))
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Warsaw"))
            assertEquals(Instant.parse("2026-10-23T23:00:00Z").toEpochMilli(),
                TimestampParser.parseTimestamp("10/24/2026 1:00:00 AM"))
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test fun factoryTimestampPreservesFractionsAndExplicitOffsets() {
        assertEquals(Instant.parse("2026-10-24T01:00:00.999Z").toEpochMilli(),
            TimestampParser.parseFactoryTimestamp("2026-10-24T01:00:00.999"))
        assertEquals(Instant.parse("2026-10-23T23:00:00Z").toEpochMilli(),
            TimestampParser.parseFactoryTimestamp("2026-10-24T01:00:00+02:00"))
    }

    @Test fun factoryTimestampWithoutOffsetAlwaysUsesUtc() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            assertEquals(Instant.parse("2026-10-24T01:00:00Z").toEpochMilli(),
                TimestampParser.parseFactoryTimestamp("2026-10-24T01:00:00"))
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test fun invalidDatesAndTrailingGarbageCannotBecomeMeasurements() {
        assertNull(TimestampParser.parseTimestamp("2/30/2026 1:00:00 AM"))
        assertNull(TimestampParser.parseFactoryTimestamp("2026-02-30T01:00:00"))
        assertNull(TimestampParser.parseFactoryTimestamp("2026-10-24T01:00:00garbage"))
    }

    @Test fun emptyTimestampIsUnknownAndIsoFallbackRemainsSupported() {
        assertNull(TimestampParser.parseTimestamp(null))
        assertNull(TimestampParser.parseTimestamp(" "))
        assertNull(TimestampParser.parseFactoryTimestamp(null))
        assertEquals(Instant.parse("2026-10-24T01:00:00Z").toEpochMilli(),
            TimestampParser.parseTimestamp("2026-10-24T01:00:00"))
    }
}
