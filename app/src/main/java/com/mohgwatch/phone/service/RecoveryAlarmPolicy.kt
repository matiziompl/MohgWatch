package com.mohgwatch.phone.service

internal data class RecoveryAlarmState(
    val enabled: Boolean = true,
    val dueElapsedMillis: Long? = null,
    val armedAtElapsedMillis: Long? = null,
    val bootCount: Int? = null,
    val epoch: Long = 0L,
    val sequence: Long = 0L
)

internal object RecoveryAlarmPolicy {
    fun isCurrent(state: RecoveryAlarmState, expectedEpoch: Long?): Boolean =
        state.enabled && (expectedEpoch == null || state.epoch == expectedEpoch)

    fun enable(state: RecoveryAlarmState): RecoveryAlarmState =
        state.copy(enabled = true, epoch = state.epoch + 1L)

    fun cancel(state: RecoveryAlarmState, expectedEpoch: Long? = null): RecoveryAlarmState? {
        if (expectedEpoch != null && state.epoch != expectedEpoch) return null
        return state.copy(enabled = false, dueElapsedMillis = null, armedAtElapsedMillis = null,
            epoch = state.epoch + 1L, sequence = state.sequence + 1L)
    }

    fun schedule(state: RecoveryAlarmState, nowElapsedMillis: Long, bootCount: Int?, delayMillis: Long,
                 expectedEpoch: Long? = null): RecoveryAlarmState? {
        if (!isCurrent(state, expectedEpoch)) return null
        val rebooted = (state.bootCount != null && bootCount != null && state.bootCount != bootCount) ||
            (state.armedAtElapsedMillis != null && nowElapsedMillis < state.armedAtElapsedMillis)
        val previousDue = state.dueElapsedMillis.takeUnless { rebooted }
        val candidate = nowElapsedMillis + delayMillis.coerceAtLeast(1_000L)
        val due = previousDue?.let { minOf(it, candidate) } ?: candidate
        return state.copy(dueElapsedMillis = due, armedAtElapsedMillis = nowElapsedMillis,
            bootCount = bootCount, sequence = if (due != previousDue) state.sequence + 1L else state.sequence)
    }

    fun consume(state: RecoveryAlarmState, epoch: Long?, sequence: Long?): RecoveryAlarmState? {
        if (!state.enabled) return null
        if (epoch != null && state.epoch != epoch) return null
        if (sequence != null && state.sequence != sequence) return null
        if (epoch == null && state.epoch != 0L) return null
        if (sequence == null && state.sequence != 0L) return null
        return state.copy(dueElapsedMillis = null, armedAtElapsedMillis = null)
    }

    fun afterBoot(state: RecoveryAlarmState, bootCount: Int?): RecoveryAlarmState =
        state.copy(dueElapsedMillis = null, armedAtElapsedMillis = null, bootCount = bootCount,
            sequence = state.sequence + 1L)
}
