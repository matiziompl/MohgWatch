package com.mohgwatch.phone.data

import org.junit.Assert.*
import org.junit.Test

class NightTestPolicyTest {
    private val configuration = "sync=true; intervalMinutes=5; persistentWakeLock=false; staleMinutes=15"

    @Test fun processRestartOnSameBootAndConfigurationContinuesTheTest() {
        assertNull(NightTestPolicy.interruptionReason(12, 12, configuration, configuration))
    }

    @Test fun configurationChangeOnSameBootInterruptsTheTest() {
        assertEquals(
            "INTERRUPTED_CONFIGURATION_CHANGE",
            NightTestPolicy.interruptionReason(12, 12, configuration, configuration.replace("intervalMinutes=5", "intervalMinutes=1"))
        )
    }

    @Test fun rebootInterruptsTheTestAndTakesPriorityOverConfigurationChanges() {
        assertEquals("INTERRUPTED_REBOOT", NightTestPolicy.interruptionReason(12, 13, configuration, configuration))
        assertEquals("INTERRUPTED_REBOOT", NightTestPolicy.interruptionReason(12, 13, configuration, "sync=false"))
    }

    @Test fun unknownBootCannotCertifyThatElapsedClocksAreComparable() {
        for ((initial, current) in listOf(null to null, null to 12, 12 to null)) {
            assertEquals(
                "Unknown boot: initial=$initial current=$current",
                "INTERRUPTED_BOOT_UNKNOWN",
                NightTestPolicy.interruptionReason(initial, current, configuration, configuration)
            )
        }
    }
}
