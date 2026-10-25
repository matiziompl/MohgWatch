package com.mohgwatch.wear.service

internal class HapticPriorityGate {
    private var criticalGeneration = 0L
    private var protectedUntilMillis = 0L

    fun optionalToken(): Long = synchronized(this) { criticalGeneration }

    fun playCritical(nowMillis: Long, durationMillis: Long, play: () -> Unit) = synchronized(this) {
        criticalGeneration++
        protectedUntilMillis = maxOf(protectedUntilMillis, nowMillis + durationMillis)
        play()
    }

    fun playOptional(token: Long, nowMillis: Long, play: () -> Unit): Boolean = synchronized(this) {
        if (token != criticalGeneration || nowMillis < protectedUntilMillis) return@synchronized false
        play()
        true
    }
}

internal val watchHapticPriority = HapticPriorityGate()
