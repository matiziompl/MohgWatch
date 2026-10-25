package com.mohgwatch.core.model

object GlucoseHistoryPolicy {
    const val RETENTION_MILLIS = 24 * 60 * 60_000L

    fun merge(
        existing: List<GlucoseReading>, incoming: List<GlucoseReading>, nowMillis: Long
    ): List<GlucoseReading> = (existing + incoming)
        .filter { it.timestamp > 0 && it.timestamp in (nowMillis - RETENTION_MILLIS)..nowMillis }
        .associateBy { it.timestamp }
        .values.sortedBy { it.timestamp }
}
