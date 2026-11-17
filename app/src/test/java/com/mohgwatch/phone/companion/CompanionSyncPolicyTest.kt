package com.mohgwatch.phone.companion

import org.junit.Assert.*
import org.junit.Test

class CompanionSyncPolicyTest {
    @Test fun presenceNeverEnablesDisabledMonitoringOrBypassesMissingCredentials() {
        assertFalse(CompanionSyncPolicy.shouldResume(false, true, true, false))
        assertFalse(CompanionSyncPolicy.shouldResume(true, false, true, false))
    }

    @Test fun activeMonitoringUsesPresenceOnlyWhenDueOrServiceIsNotSyncing() {
        assertTrue(CompanionSyncPolicy.shouldResume(true, true, true, true))
        assertTrue(CompanionSyncPolicy.shouldResume(true, true, false, false))
        assertFalse(CompanionSyncPolicy.shouldResume(true, true, false, true))
    }

    @Test fun deviceNamesCannotExposeBluetoothAddresses() {
        assertNull(companionDisplayName("AA:BB:CC:DD:EE:FF"))
        assertEquals("Watch …", companionDisplayName("Watch aa-bb-cc-dd-ee-ff"))
        assertEquals("OnePlus Watch 2R", companionDisplayName("OnePlus Watch 2R"))
    }
}
