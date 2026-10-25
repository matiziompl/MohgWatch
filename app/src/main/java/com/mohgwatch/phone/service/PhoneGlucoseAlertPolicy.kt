package com.mohgwatch.phone.service

import com.mohgwatch.core.model.GlucoseAlertPolicy
import com.mohgwatch.core.model.GlucoseRange
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.model.UserSettings

internal object PhoneGlucoseAlertPolicy {
    fun hazard(reading: GlucoseReading?, now: Long, low: Float, high: Float, staleAfterMillis: Long = GlucoseReading.STALE_AFTER_MILLIS): GlucoseRange? {
        if (reading == null || !reading.isFreshAt(now, staleAfterMillis)) return null
        return GlucoseAlertPolicy.range(reading.value, low, high).takeUnless { it == GlucoseRange.NORMAL }
    }

    fun shouldMute(settings: UserSettings, connected: Boolean, worn: Boolean, wristTimestamp: Long, now: Long): Boolean =
        settings.shouldMutePhoneAlert(connected, worn && GlucoseAlertPolicy.hasRecentWristStatus(wristTimestamp, now))
}
