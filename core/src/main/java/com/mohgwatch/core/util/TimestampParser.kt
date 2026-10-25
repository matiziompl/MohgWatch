package com.mohgwatch.core.util

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.util.Locale

/** Local LibreLinkUp timestamps follow the current device zone; factory timestamps default to UTC. */
object TimestampParser {
    private val localFormat = DateTimeFormatterBuilder().parseCaseInsensitive()
        .appendPattern("M/d/uuuu h:mm:ss a").toFormatter(Locale.US)
        .withResolverStyle(ResolverStyle.STRICT)

    fun parseTimestamp(timestamp: String?): Long? {
        if (timestamp.isNullOrBlank()) return null
        return try {
            LocalDateTime.parse(timestamp, localFormat).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: DateTimeException) {
            parseFactoryTimestamp(timestamp)
        }
    }

    fun parseFactoryTimestamp(timestamp: String?): Long? {
        if (timestamp.isNullOrBlank()) return null
        return try {
            DateTimeFormatter.ISO_DATE_TIME.parse(timestamp, Instant::from).toEpochMilli()
        } catch (_: DateTimeException) {
            try {
                LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (_: DateTimeException) {
                null
            }
        }
    }
}
