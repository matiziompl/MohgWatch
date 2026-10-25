package com.mohgwatch.wear.service

import com.mohgwatch.core.model.GlucoseReading

internal class LatestGlucoseAlertGate {
    private var generation = 0L
    var lastEventTimestamp = 0L
        private set
    var lastMeasurementTimestamp = 0L
        private set

    fun restoreLastEventTimestamp(timestamp: Long) = synchronized(this) {
        lastEventTimestamp = maxOf(lastEventTimestamp, timestamp)
    }

    fun restoreLastMeasurementTimestamp(timestamp: Long) = synchronized(this) {
        lastMeasurementTimestamp = maxOf(lastMeasurementTimestamp, timestamp)
    }

    fun begin(type: String, eventTimestamp: Long? = null, measurementTimestamp: Long? = null, nowMillis: Long? = null,
              measurementTimeoutMillis: Long = GlucoseReading.STALE_AFTER_MILLIS): Long? = synchronized(this) {
        if (type != "LOW" && type != "HIGH" && type != "STABLE") return@synchronized null
        if (eventTimestamp != null) {
            if (eventTimestamp <= lastEventTimestamp) return@synchronized null
            // The envelope uses the phone clock; the watch clock may be slightly behind.
            if (nowMillis != null && !isFresh(eventTimestamp, nowMillis, futureToleranceMillis = 30_000L)) return@synchronized null
        }
        if (measurementTimestamp != null) {
            if (measurementTimestamp <= lastMeasurementTimestamp) return@synchronized null
            if (nowMillis != null && !isFresh(measurementTimestamp, nowMillis, timeoutMillis = measurementTimeoutMillis)) return@synchronized null
        }
        if (eventTimestamp != null) lastEventTimestamp = eventTimestamp
        if (measurementTimestamp != null) lastMeasurementTimestamp = measurementTimestamp
        ++generation
    }

    private fun isFresh(timestamp: Long, nowMillis: Long, futureToleranceMillis: Long = 0L,
                        timeoutMillis: Long = GlucoseReading.STALE_AFTER_MILLIS): Boolean =
        timestamp > 0 && timestamp <= nowMillis + futureToleranceMillis &&
            nowMillis - timestamp < timeoutMillis

    fun applyIfCurrent(token: Long, update: () -> Unit): Boolean = synchronized(this) {
        if (token != generation) return@synchronized false
        update()
        true
    }
}
