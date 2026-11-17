package com.mohgwatch.phone.data

import org.junit.Assert.*
import org.junit.Test

class NightTestReportTest {
    private val wallStart = 1_800_000_000_000L

    private fun event(
        name: String,
        elapsed: Long,
        cycle: String? = "cycle-1",
        details: String = "",
        wall: Long = wallStart + elapsed
    ) = DiagnosticEvent(wall, elapsed, "session", 123, cycle, name, details, "Europe/Warsaw")

    private fun report(events: List<DiagnosticEvent>, incomplete: Boolean = false) =
        NightTestReport.summarize(NightTestSample(wallStart, 10_000L, 110_000L, events, incomplete))

    private fun assertMetric(report: String, key: String, expected: Int) {
        assertTrue("Missing $key=$expected in:\n$report", report.lineSequence().any { it.trim() == "$key: $expected" })
    }

    private fun assertGap(report: String, expectedSeconds: Int) {
        assertTrue(report, report.contains("Najdłuższa przerwa bez nowego świeżego pomiaru: $expectedSeconds s"))
    }

    @Test fun plannedAndDeliveredTriggersDoNotInflateActualCycleCounts() {
        val summary = report(listOf(
            event("SYNC_ALARM_PLANNED", 11_000L),
            event("SYNC_ALARM_PLANNED", 12_000L),
            event("SYNC_ALARM_DELIVERED", 13_000L),
            event("TIMER_PLANNED", 14_000L),
            event("TIMER_DELIVERED", 15_000L),
            event("SYNC_START", 20_000L),
            event("SYNC_START", 21_000L),
            event("SYNC_END", 30_000L, details = "result=success"),
            event("SYNC_END", 31_000L, details = "result=success"),
            event("SYNC_START", 40_000L, "cycle-2"),
            event("SYNC_END", 50_000L, "orphan-cycle", "result=success")
        ))
        assertMetric(summary, "Alarmy zaplanowane", 2)
        assertMetric(summary, "Alarmy dostarczone", 1)
        assertMetric(summary, "Timery zaplanowane", 1)
        assertMetric(summary, "Timery dostarczone", 1)
        assertMetric(summary, "Cykle rozpoczęte", 2)
        assertMetric(summary, "Cykle udane", 1)
    }

    @Test fun optionalHistoryFailuresDoNotTurnSuccessfulLatestReadingIntoFailedCycle() {
        val summary = report(listOf(
            event("SYNC_START", 20_000L),
            event("MEASUREMENT", 25_000L, details = "timestamp=100 ageMs=1000 fresh=true cache=false"),
            event("API_RESPONSE_ERROR", 26_000L, details = "stage=history code=503"),
            event("API_RESPONSE_ERROR", 27_000L, details = "stage=history code=503"),
            event("SYNC_END", 30_000L, details = "result=success")
        ))
        assertMetric(summary, "Cykle udane", 1)
        assertMetric(summary, "Błędy transportu", 0)
        assertMetric(summary, "Błędy odpowiedzi", 0)
        assertMetric(summary, "Błędy historii", 1)
    }

    @Test fun mainTransportResponseAndHistoryFailuresAreCountedSeparatelyByCycle() {
        val summary = report(listOf(
            event("API_NETWORK_ERROR", 20_000L, "transport", "stage=latest type=IOException"),
            event("API_NETWORK_ERROR", 21_000L, "transport", "stage=latest type=IOException"),
            event("API_RESPONSE_ERROR", 30_000L, "response", "stage=latest code=500"),
            event("API_RESPONSE_ERROR", 31_000L, "response", "stage=latest code=500"),
            event("API_NETWORK_ERROR", 40_000L, "history", "stage=history type=IOException")
        ))
        assertMetric(summary, "Błędy transportu", 1)
        assertMetric(summary, "Błędy odpowiedzi", 1)
        assertMetric(summary, "Błędy historii", 1)
        assertMetric(summary, "Cykle udane", 0)
    }

    @Test fun failuresWithoutCycleIdsRemainSeparateObservations() {
        val summary = report(listOf(
            event("API_NETWORK_ERROR", 20_000L, null, "stage=latest type=IOException"),
            event("API_NETWORK_ERROR", 21_000L, null, "stage=latest type=IOException")
        ))
        assertMetric(summary, "Błędy transportu", 2)
    }

    @Test fun longestFreshReadingGapIncludesStartAndEndOfTest() {
        val summary = report(listOf(
            event("MEASUREMENT", 20_000L, details = "timestamp=100 ageMs=1000 fresh=true cache=false"),
            event("MEASUREMENT", 50_000L, details = "timestamp=200 ageMs=1000 fresh=true cache=false")
        ))
        assertGap(summary, 60)
    }

    @Test fun delayBeforeFirstFreshReadingCanBeTheLongestGap() {
        val summary = report(listOf(
            event("MEASUREMENT", 80_000L, details = "timestamp=100 ageMs=1000 fresh=true cache=false"),
            event("MEASUREMENT", 90_000L, details = "timestamp=200 ageMs=1000 fresh=true cache=false")
        ))
        assertGap(summary, 70)
    }

    @Test fun staleCachedAndIgnoredReadingsDoNotResetFreshReadingGap() {
        val summary = report(listOf(
            event("MEASUREMENT", 20_000L, details = "timestamp=100 ageMs=1000 fresh=true cache=false"),
            event("MEASUREMENT", 50_000L, details = "timestamp=200 ageMs=1000 fresh=true cache=true"),
            event("MEASUREMENT", 70_000L, details = "timestamp=300 ageMs=1200000 fresh=false cache=false"),
            event("MEASUREMENT_IGNORED", 100_000L, details = "timestamp=100 ageMs=1000 duplicateOrOlder=true")
        ))
        assertGap(summary, 90)
    }

    @Test fun testWithoutNewFreshReadingReportsItsWholeDurationAsGap() {
        assertGap(report(emptyList()), 100)
        assertGap(report(listOf(event("MEASUREMENT", 50_000L, details = "fresh=false cache=false"))), 100)
    }

    @Test fun wallClockChangesDoNotAffectElapsedReadingGaps() {
        val summary = report(listOf(
            event("MEASUREMENT", 30_000L, details = "timestamp=100 ageMs=1000 fresh=true cache=false", wall = wallStart + 3_600_000L),
            event("MEASUREMENT", 70_000L, details = "timestamp=200 ageMs=1000 fresh=true cache=false", wall = wallStart - 3_600_000L)
        ))
        assertGap(summary, 40)
    }

    @Test fun incompleteSourceDataCannotBeReportedAsComplete() {
        assertTrue(report(emptyList()).contains("Kompletność: pełna"))
        assertTrue(report(emptyList(), incomplete = true).contains("Kompletność: niepełna"))
    }

    @Test fun recoveredHttpAuthenticationFailureIsObservedWithoutFailingTheCycle() {
        val summary = report(listOf(
            event("SYNC_START", 20_000L),
            event("HTTP_RESULT", 21_000L, details = "stage=latest code=401 durationMs=100"),
            event("HTTP_RESULT", 22_000L, details = "stage=login code=200 durationMs=100"),
            event("HTTP_RESULT", 23_000L, details = "stage=latest code=200 durationMs=100"),
            event("SYNC_END", 30_000L, details = "result=success")
        ))
        assertMetric(summary, "Błędy odpowiedzi", 1)
        assertMetric(summary, "Błędy transportu", 0)
        assertMetric(summary, "Cykle udane", 1)
    }

    @Test fun failedHistoryHttpResponseDoesNotCountAsMainReadingFailure() {
        val summary = report(listOf(
            event("SYNC_START", 20_000L),
            event("MEASUREMENT", 21_000L, details = "timestamp=100 ageMs=1000 fresh=true cache=false"),
            event("SYNC_STAGE", 22_000L, details = "history_download"),
            event("HTTP_RESULT", 23_000L, details = "stage=history code=503 durationMs=100"),
            event("SYNC_END", 30_000L, details = "result=success")
        ))
        assertMetric(summary, "Błędy historii", 1)
        assertMetric(summary, "Błędy odpowiedzi", 0)
        assertMetric(summary, "Cykle udane", 1)
    }

    @Test fun historyTransportObservationsIncludeUnclassifiedCallsWithoutDoubleCountingClassifiedCalls() {
        val summary = report(listOf(
            event("SYNC_STAGE", 20_000L, details = "history_download"),
            event("HTTP_ERROR", 21_000L, details = "stage=history causes=UnknownHostException durationMs=100"),
            event("API_NETWORK_ERROR", 22_000L, details = "stage=history kind=DNS causes=UnknownHostException"),
            event("HTTP_ERROR", 40_000L, "cycle-2", "stage=history causes=SocketTimeoutException durationMs=30000")
        ))
        assertMetric(summary, "Błędy historii", 2)
        assertMetric(summary, "Błędy transportu", 0)
        assertMetric(summary, "Błędy odpowiedzi", 0)
    }
}
