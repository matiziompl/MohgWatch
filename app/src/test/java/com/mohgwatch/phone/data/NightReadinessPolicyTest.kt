package com.mohgwatch.phone.data

import org.junit.Assert.*
import org.junit.Test

class NightReadinessPolicyTest {
    private val fingerprint = "OnePlus/CPH2709/16"
    private val confirmed = OemChecklist(sleepStandbyOff = true, backgroundActivityAllowed = true, systemFingerprint = fingerprint)
    private fun inputs(checklist: OemChecklist = confirmed, oem: Boolean = true, battery: Boolean = true,
                       exact: Boolean = true, notifications: Boolean = true) =
        ReadinessInputs(battery, exact, notifications, oem, checklist, fingerprint)

    @Test fun `fully prepared phone has no issues`() {
        assertEquals(emptyList<ReadinessIssue>(), NightReadinessPolicy.issues(inputs()))
    }

    @Test fun `android permissions are reported independently`() {
        assertEquals(listOf(ReadinessIssue.BATTERY_OPTIMIZATION, ReadinessIssue.EXACT_ALARMS, ReadinessIssue.NOTIFICATIONS),
            NightReadinessPolicy.issues(inputs(battery = false, exact = false, notifications = false)))
    }

    @Test fun `manual oem items must be confirmed only on the ColorOS family`() {
        assertEquals(listOf(ReadinessIssue.OEM_SLEEP_STANDBY, ReadinessIssue.OEM_BACKGROUND_ACTIVITY),
            NightReadinessPolicy.issues(inputs(OemChecklist())))
        assertEquals(emptyList<ReadinessIssue>(), NightReadinessPolicy.issues(inputs(OemChecklist(), oem = false)))
    }

    @Test fun `system update invalidates manual confirmations`() {
        val beforeUpdate = confirmed.copy(systemFingerprint = "OnePlus/CPH2709/15")
        assertEquals(listOf(ReadinessIssue.OEM_SLEEP_STANDBY, ReadinessIssue.OEM_BACKGROUND_ACTIVITY),
            NightReadinessPolicy.issues(inputs(beforeUpdate)))
    }

    @Test fun `detected outage stays an issue on every device until acknowledged`() {
        val outage = NightOutage(1_000L, 1_000L + 30 * 60_000L)
        assertTrue(ReadinessIssue.NIGHT_OUTAGE in NightReadinessPolicy.issues(inputs(confirmed.copy(outage = outage))))
        assertTrue(ReadinessIssue.NIGHT_OUTAGE in NightReadinessPolicy.issues(inputs(OemChecklist(outage = outage), oem = false)))
    }

    @Test fun `only long losses that started while asleep count as outages`() {
        val lost = 10_000L
        val long = lost + NightReadinessPolicy.OUTAGE_THRESHOLD_MILLIS
        assertEquals(NightOutage(lost, long), NightReadinessPolicy.outageOf(lost, startedWhileAsleep = true, restoredAtMillis = long))
        assertNull(NightReadinessPolicy.outageOf(lost, startedWhileAsleep = true, restoredAtMillis = long - 1))
        assertNull(NightReadinessPolicy.outageOf(lost, startedWhileAsleep = false, restoredAtMillis = long))
        assertNull(NightReadinessPolicy.outageOf(null, startedWhileAsleep = true, restoredAtMillis = long))
    }

    @Test fun `ColorOS family is recognised by manufacturer or brand`() {
        assertTrue(NightReadinessPolicy.isOemFamily("OnePlus", "OnePlus"))
        assertTrue(NightReadinessPolicy.isOemFamily("OPPO", "oppo"))
        assertTrue(NightReadinessPolicy.isOemFamily("realme", "realme"))
        assertFalse(NightReadinessPolicy.isOemFamily("Google", "google"))
    }
}
