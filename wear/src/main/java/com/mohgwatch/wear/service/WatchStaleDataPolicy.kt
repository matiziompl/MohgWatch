package com.mohgwatch.wear.service

import com.mohgwatch.core.model.GlucoseReading

internal object WatchStaleDataPolicy {
    const val TIMEOUT_MS = GlucoseReading.STALE_AFTER_MILLIS

    fun isFresh(timestamp: Long?, now: Long, timeoutMillis: Long = TIMEOUT_MS): Boolean =
        timestamp != null && timestamp > 0 && timestamp <= now && now - timestamp < timeoutMillis

    fun shouldAlert(timestamp: Long?, now: Long, enabled: Boolean, alreadyAlerted: Boolean, timeoutMillis: Long = TIMEOUT_MS): Boolean =
        timestamp != null && !isFresh(timestamp, now, timeoutMillis) && enabled && !alreadyAlerted
}
