package com.mohgwatch.phone.data

import org.junit.Assert.*
import org.junit.Test

class DiagnosticQueryTest {
    private val run = "018f1d88-984f-4be7-8357-e96aab5f80cb"
    private val otherRun = "71890684-e638-4ef1-a60c-9d6693e0180f"

    private fun event(name: String, details: String = "", cycle: String? = "cycle-1") =
        DiagnosticEvent(1_800_000_000_000L, 50_000L, "session", 123, cycle, name, details)

    @Test fun runIdIsReadFromItsPrefixWithoutAttributingLegacyEventsToARun() {
        assertEquals(run, event("SYNC_START", "runId=$run; trigger=alarm").runId())
        assertNull(event("SYNC_START", "trigger=alarm").runId())
        assertNull(event("SYNC_START", "previous runId=$run; trigger=alarm").runId())
    }

    @Test fun categoryIncludesRelatedSchedulingAndHttpEvents() {
        val examples = mapOf(
            DiagnosticCategory.SYNC to listOf("SYNC_START", "TIMER_PLANNED", "WAKELOCK_ACQUIRED", "WATCHDOG_START"),
            DiagnosticCategory.NETWORK to listOf("NETWORK_AVAILABLE", "NETWORK_LOST", "NETWORK_CAPABILITIES"),
            DiagnosticCategory.API to listOf("API_REQUEST", "API_NETWORK_ERROR", "HTTP_RESPONSE")
        )
        for ((category, names) in examples) {
            for (name in names) {
                assertTrue("$category must include $name", DiagnosticFilter(category = category).matches(event(name)))
                assertTrue(DiagnosticFilter().matches(event(name)))
                for (other in examples.keys.filter { it != category }) {
                    assertFalse("$other must exclude $name", DiagnosticFilter(category = other).matches(event(name)))
                }
            }
        }
    }

    @Test fun categoryCycleRunAndErrorFiltersAreAppliedTogether() {
        val filter = DiagnosticFilter(DiagnosticCategory.API, "cycle-1", run, errorsOnly = true)
        assertTrue(filter.matches(event("API_RESPONSE_ERROR", "runId=$run; stage=latest code=503")))
        assertFalse(filter.matches(event("API_RESPONSE_ERROR", "runId=$otherRun; stage=latest code=503")))
        assertFalse(filter.matches(event("API_RESPONSE_ERROR", "runId=$run; stage=latest code=503", "cycle-2")))
        assertFalse(filter.matches(event("NETWORK_BLOCKED", "runId=$run; blocked=true")))
        assertFalse(filter.matches(event("API_RESPONSE", "runId=$run; stage=latest code=200")))
        assertFalse(filter.matches(event("API_RESPONSE_ERROR", "stage=latest code=503")))
    }

    @Test fun recoveredNetworkEventsAreNotClassifiedAsFailures() {
        val filter = DiagnosticFilter(errorsOnly = true)
        listOf("SYNC_ERROR", "SYNC_TIMEOUT", "API_NETWORK_ERROR", "NETWORK_BLOCKED").forEach {
            assertTrue("Missing failure: $it", filter.matches(event(it)))
        }
        listOf("NETWORK_UNBLOCKED", "NETWORK_BLOCKED_CHANGED", "API_RESPONSE", "SYNC_END").forEach {
            assertFalse("False failure: $it", filter.matches(event(it, "blocked=false result=success")))
        }
    }

    @Test fun apiErrorsFilterIncludesFailedHttpResponsesAndExcludesSuccessfulOnes() {
        val filter = DiagnosticFilter(category = DiagnosticCategory.API, errorsOnly = true)
        assertTrue(filter.matches(event("HTTP_RESULT", "code=401 durationMs=100")))
        assertTrue(filter.matches(event("HTTP_RESULT", "code=503 durationMs=100")))
        assertFalse(filter.matches(event("HTTP_RESULT", "code=200 durationMs=100")))
    }
}
