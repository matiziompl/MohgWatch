package com.mohgwatch.core.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Parser timestampów z LibreLinkUp API.
 * API zwraca dwa formaty:
 * - Timestamp: "M/d/yyyy h:mm:ss a" (12-godzinny, lokalny)
 * - FactoryTimestamp: "yyyy-MM-dd'T'HH:mm:ss" (24-godzinny, UTC)
 */
object TimestampParser {

    private val timestampFormat = SimpleDateFormat("M/d/yyyy h:mm:ss a", Locale.US)

    private val factoryFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Parsuje Timestamp z API (format lokalny 12h).
     * @return Unix millis lub null w razie błędu
     */
    fun parseTimestamp(timestamp: String?): Long? {
        if (timestamp.isNullOrBlank()) return null
        return try {
            timestampFormat.parse(timestamp)?.time
        } catch (e: Exception) {
            // Fallback: spróbuj ISO format
            try {
                factoryFormat.parse(timestamp)?.time
            } catch (e2: Exception) {
                null
            }
        }
    }

    /**
     * Parsuje FactoryTimestamp z API (format UTC ISO).
     * @return Unix millis lub null w razie błędu
     */
    fun parseFactoryTimestamp(timestamp: String?): Long? {
        if (timestamp.isNullOrBlank()) return null
        return try {
            factoryFormat.parse(timestamp)?.time
        } catch (e: Exception) {
            try {
                isoFormat.parse(timestamp)?.time
            } catch (e2: Exception) {
                null
            }
        }
    }
}
