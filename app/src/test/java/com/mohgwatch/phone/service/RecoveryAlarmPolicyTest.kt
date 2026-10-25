package com.mohgwatch.phone.service

import org.junit.Assert.*
import org.junit.Test

class RecoveryAlarmPolicyTest {
    @Test fun fasterTimerCyclesCannotPostponeTheRecoveryAlarm() {
        var state = RecoveryAlarmState()
        state = RecoveryAlarmPolicy.schedule(state, 0L, 7, 60_000L)!!
        for (elapsed in listOf(15_000L, 30_000L, 45_000L, 59_999L)) {
            state = RecoveryAlarmPolicy.schedule(state, elapsed, 7, 60_000L)!!
            assertEquals(60_000L, state.dueElapsedMillis)
        }
    }

    @Test fun processRestartPreservesTheAbsoluteDeadlineAndAnOverdueAlarmIsStillDue() {
        val persisted = RecoveryAlarmState(dueElapsedMillis = 60_000L, armedAtElapsedMillis = 30_000L, bootCount = 7)
        assertEquals(60_000L, RecoveryAlarmPolicy.schedule(persisted.copy(), 45_000L, 7, 60_000L)!!.dueElapsedMillis)
        assertEquals(60_000L, RecoveryAlarmPolicy.schedule(persisted.copy(), 120_000L, 7, 60_000L)!!.dueElapsedMillis)
    }

    @Test fun explicitStopAndObsoleteEpochPreventRearming() {
        assertNull(RecoveryAlarmPolicy.schedule(RecoveryAlarmState(enabled = false), 30_000L, 7, 60_000L))
        assertNull(RecoveryAlarmPolicy.schedule(RecoveryAlarmState(epoch = 4L), 30_000L, 7, 60_000L, expectedEpoch = 3L))
    }
    @Test fun bootChangeOrElapsedClockResetDiscardsDeadlineFromPreviousBoot() {
        val previous = RecoveryAlarmState(dueElapsedMillis = 600_000L, armedAtElapsedMillis = 30_000L, bootCount = 7)
        assertEquals(160_000L, RecoveryAlarmPolicy.schedule(previous, 100_000L, 8, 60_000L)!!.dueElapsedMillis)
        assertEquals(61_000L, RecoveryAlarmPolicy.schedule(previous.copy(bootCount = null), 1_000L, null, 60_000L)!!.dueElapsedMillis)
    }

    @Test fun watchdogCanBringAnAlarmForwardButNotDelayItAgain() {
        val previous = RecoveryAlarmState(dueElapsedMillis = 300_000L, armedAtElapsedMillis = 0L, bootCount = 7)
        val earlier = RecoveryAlarmPolicy.schedule(previous, 30_000L, 7, 1_000L)!!
        assertEquals(31_000L, earlier.dueElapsedMillis)
        assertEquals(31_000L, RecoveryAlarmPolicy.schedule(earlier, 30_500L, 7, 300_000L)!!.dueElapsedMillis)
        assertEquals(31_000L, RecoveryAlarmPolicy.schedule(previous, 30_000L, 7, -100L)!!.dueElapsedMillis)
    }

    @Test fun deliveredAlarmIsConsumedBeforeTheNextDeadlineIsCreated() {
        val armed = RecoveryAlarmPolicy.schedule(RecoveryAlarmState(epoch = 4L), 0L, 7, 60_000L)!!
        val consumed = RecoveryAlarmPolicy.consume(armed, 4L, armed.sequence)!!
        assertNull(consumed.dueElapsedMillis)
        val next = RecoveryAlarmPolicy.schedule(consumed, 75_000L, 7, 60_000L, 4L)!!
        assertEquals(135_000L, next.dueElapsedMillis)
        assertNull(RecoveryAlarmPolicy.consume(next, 4L, armed.sequence))
    }

    @Test fun stopInvalidatesAlreadyDispatchedDeliveryAndResumeWork() {
        val armed = RecoveryAlarmPolicy.schedule(RecoveryAlarmState(epoch = 4L), 0L, 7, 60_000L)!!
        val stopped = RecoveryAlarmPolicy.cancel(armed)!!
        assertFalse(stopped.enabled)
        assertNull(RecoveryAlarmPolicy.consume(stopped, armed.epoch, armed.sequence))
        assertFalse(RecoveryAlarmPolicy.isCurrent(stopped, armed.epoch))
        assertNull(RecoveryAlarmPolicy.schedule(stopped, 60_000L, 7, 60_000L, armed.epoch))
    }

    @Test fun explicitRestartInvalidatesStaleSettingsReadsWithoutDisablingTheNewSession() {
        val stopped = RecoveryAlarmPolicy.cancel(RecoveryAlarmState(epoch = 4L))!!
        val restarted = RecoveryAlarmPolicy.enable(stopped)
        assertTrue(restarted.enabled)
        assertNull(RecoveryAlarmPolicy.cancel(restarted, expectedEpoch = 4L))
        assertFalse(RecoveryAlarmPolicy.isCurrent(restarted, expectedEpoch = 4L))
        assertTrue(RecoveryAlarmPolicy.isCurrent(restarted, expectedEpoch = restarted.epoch))
    }

    @Test fun bootDoesNotEnableAnExplicitlyStoppedSessionAndUnknownBootStillResetsTheDeadline() {
        val stopped = RecoveryAlarmState(enabled = false, epoch = 3L)
        val rebooted = RecoveryAlarmPolicy.afterBoot(stopped, 8)
        assertFalse(rebooted.enabled)
        assertNull(RecoveryAlarmPolicy.schedule(rebooted, 1_000L, 8, 60_000L))
        val unknownBoot = RecoveryAlarmPolicy.afterBoot(RecoveryAlarmState(dueElapsedMillis = 600_000L), null)
        assertNull(unknownBoot.dueElapsedMillis)
    }
}
