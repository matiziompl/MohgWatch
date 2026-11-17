package com.mohgwatch.wear.service

import com.mohgwatch.core.model.GlucoseAlertPolicy
import com.mohgwatch.core.model.GlucoseRange
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.model.UserSettings

internal enum class GlucoseNotificationUpdate { IGNORE, CANCEL, LOW, HIGH }

internal object WatchGlucoseNotificationPolicy {
    fun alert(
        settings: UserSettings,
        type: String,
        showNotificationExplicit: Boolean,
        measurementTimestamp: Long?,
        latestMeasurementTimestamp: Long?,
        now: Long
    ): GlucoseNotificationUpdate {
        if (type !in setOf("LOW", "HIGH", "STABLE")) return GlucoseNotificationUpdate.IGNORE
        if (measurementTimestamp != null &&
            (!WatchStaleDataPolicy.isFresh(measurementTimestamp, now, settings.staleAfterMillis) ||
                latestMeasurementTimestamp != null && measurementTimestamp < latestMeasurementTimestamp)) {
            return GlucoseNotificationUpdate.IGNORE
        }
        if (type == "STABLE" || !settings.notifyOutOfRange ||
            !(showNotificationExplicit || settings.watchNotifyOutOfRange)) return GlucoseNotificationUpdate.CANCEL
        return if (type == "LOW") GlucoseNotificationUpdate.LOW else GlucoseNotificationUpdate.HIGH
    }

    fun refresh(
        hasVisibleNotification: Boolean,
        reading: GlucoseReading?,
        settings: UserSettings,
        now: Long,
        visibleMeasurementTimestamp: Long = 0L
    ): GlucoseNotificationUpdate {
        if (!hasVisibleNotification) return GlucoseNotificationUpdate.IGNORE
        if (!settings.notifyOutOfRange || !settings.watchNotifyOutOfRange) return GlucoseNotificationUpdate.CANCEL
        if ((reading?.timestamp ?: 0L) < visibleMeasurementTimestamp &&
            WatchStaleDataPolicy.isFresh(visibleMeasurementTimestamp, now, settings.staleAfterMillis)) {
            return GlucoseNotificationUpdate.IGNORE
        }
        if (reading == null ||
            !reading.value.isFinite() || !reading.isFreshAt(now, settings.staleAfterMillis)) {
            return GlucoseNotificationUpdate.CANCEL
        }
        return when (GlucoseAlertPolicy.range(reading.value, settings.lowThreshold, settings.highThreshold)) {
            GlucoseRange.LOW -> GlucoseNotificationUpdate.LOW
            GlucoseRange.HIGH -> GlucoseNotificationUpdate.HIGH
            GlucoseRange.NORMAL -> GlucoseNotificationUpdate.CANCEL
        }
    }
}
