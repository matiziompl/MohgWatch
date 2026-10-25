package com.mohgwatch.core.model

enum class GlucoseRange { LOW, NORMAL, HIGH }

object GlucoseAlertPolicy {
    fun range(value: Float, low: Float, high: Float): GlucoseRange = when {
        value < low -> GlucoseRange.LOW
        value > high -> GlucoseRange.HIGH
        else -> GlucoseRange.NORMAL
    }

    fun shouldAlert(previous: GlucoseRange, current: GlucoseRange): Boolean =
        previous != current && current != GlucoseRange.NORMAL

    fun hasRecentWristStatus(timestamp: Long, nowMillis: Long): Boolean =
        timestamp > 0L && timestamp <= nowMillis && nowMillis - timestamp < GlucoseReading.STALE_AFTER_MILLIS
}
