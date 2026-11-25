package com.mohgwatch.phone.service

import android.content.Context
import com.mohgwatch.core.model.GlucoseRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal object AlertSnoozePolicy {
    const val MAX_MINUTES = 30
    private const val MAX_MILLIS = MAX_MINUTES * 60_000L

    fun until(now: Long, minutes: Int): Long = now + minutes.coerceIn(0, MAX_MINUTES) * 60_000L

    /** The cap also protects against a wall clock moved backwards during a snooze. */
    fun isActive(until: Long, now: Long): Boolean = now < until && until - now <= MAX_MILLIS

    /** A low/high hazard silenced during the snooze must still reach the watch once the snooze ends. */
    fun resumedHazard(suppressed: GlucoseRange?, current: GlucoseRange, snoozed: Boolean): GlucoseRange? =
        suppressed?.takeIf { !snoozed && it == current }
}

/** Temporary silence for low and high glucose alarms on the phone and the watch. */
object AlertSnooze {
    private const val PREFS = "mohgwatch_alert_snooze"
    private val untilState = MutableStateFlow<Long?>(null)

    @Synchronized fun until(context: Context): StateFlow<Long?> {
        if (untilState.value == null) {
            untilState.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("until", 0L)
        }
        return untilState
    }

    fun isActive(context: Context, now: Long): Boolean = AlertSnoozePolicy.isActive(until(context).value ?: 0L, now)

    @Synchronized fun set(context: Context, untilMillis: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong("until", untilMillis).apply()
        untilState.value = untilMillis
    }
}
