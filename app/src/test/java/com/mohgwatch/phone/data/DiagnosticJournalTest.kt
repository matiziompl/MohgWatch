package com.mohgwatch.phone.data

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DiagnosticJournalTest {
    @get:Rule val temporary = TemporaryFolder()
    private val now = 1_800_000_000_000L

    private fun event(time: Long = now, details: String = "wynik=OK", name: String = "SYNC_END") =
        DiagnosticEvent(time, 5_000L, "session-one", 123, "cycle-7", name, details)

    @Test fun persistedEventsSurviveASecondJournalInstanceWithBothClocksAndCycle() {
        val directory = temporary.newFolder()
        DiagnosticJournal(directory).append(event())
        val restored = DiagnosticJournal(directory).read(now - 1, now + 1)
        assertEquals(listOf(event()), restored.events)
        assertEquals(0L, restored.discardedEvents)
    }

    @Test fun reportSelectsRequestedWindowAndKeepsFailureWhenTimelineIsShortened() {
        val journal = DiagnosticJournal(temporary.newFolder())
        journal.append(event(now - 90_000_000, "older-outside-period"))
        journal.append(event(now - 10_000, "network-blocked", "SYNC_ERROR"))
        repeat(80) { journal.append(event(now - 9_000 + it, "stage-$it " + "x".repeat(80), "SYNC_STAGE")) }
        val report = journal.report(now - 86_400_000, now, "sync=true", maxChars = 2_000)
        assertTrue(report.contains("network-blocked"))
        assertFalse(report.contains("older-outside-period"))
        assertTrue(report.contains("Obcięcie: tak"))
        assertTrue(report.contains("Zakres żądany:"))
        assertTrue(report.contains("Zakres zapisanych zdarzeń:"))
        assertTrue(report.contains("sync=true"))
        assertTrue(report.length <= 2_000)
    }

    @Test fun retentionAndSizeDiscardOldestEventsAndPersistTheirCount() {
        val directory = temporary.newFolder()
        val journal = DiagnosticJournal(directory, maxBytes = 2_048, retentionMillis = 60_000)
        journal.append(event(now - 60_001, "expired"))
        journal.append(event(now, "fresh"))
        repeat(80) { journal.append(event(now + it, "entry-$it " + "x".repeat(40))) }
        val restored = DiagnosticJournal(directory, maxBytes = 2_048, retentionMillis = 60_000)
            .read(now - 100_000, now + 100_000)
        assertFalse(restored.events.any { it.details == "expired" })
        assertTrue(restored.events.last().details.startsWith("entry-79"))
        assertTrue(restored.discardedEvents > 0)
        assertTrue(File(directory, "events.tsv").length() <= 2_048)
    }

    @Test fun credentialsAndAccountPayloadAreNotPersistedOrReturned() {
        val directory = temporary.newFolder()
        val journal = DiagnosticJournal(directory)
        journal.append(event(details = "password=secret-password token=secret-token Authorization: Bearer secret-bearer cookie=secret-cookie email=user@example.org"))
        journal.append(event(details = "{\"data\":{\"account\":{\"name\":\"Sensitive Person\"}},\"access_token\":\"secret-json\"}"))
        journal.append(event(details = "Cookie: sid=secret-sid; session=secret-session; extra=secret-extra"))
        val stored = File(directory, "events.tsv").readText()
        listOf("secret-password", "secret-token", "secret-bearer", "secret-cookie", "user@example.org", "Sensitive Person", "secret-json", "secret-sid", "secret-session", "secret-extra").forEach {
            assertFalse("Sensitive value leaked: $it", stored.contains(it))
        }
        assertTrue(journal.read(now - 1, now + 1).events.isNotEmpty())
    }

    @Test fun appendAfterTornTailPreservesNewEvents() {
        val directory = temporary.newFolder()
        val journal = DiagnosticJournal(directory)
        journal.append(event())
        File(directory, "events.tsv").appendText("torn-tail")
        journal.append(event(now + 1, "next-after-restart"))
        assertEquals(listOf("wynik=OK", "next-after-restart"), journal.read(now - 1, now + 1).events.map { it.details })
        assertEquals(1, journal.read(now - 1, now + 1).unreadableLines)
    }

    @Test fun malformedTailDoesNotDiscardEarlierNightAndIsReported() {
        val directory = temporary.newFolder()
        val journal = DiagnosticJournal(directory)
        journal.append(event())
        File(directory, "events.tsv").appendText("unfinished-after-process-exit")
        val window = DiagnosticJournal(directory).read(now - 1, now + 1)
        assertEquals(listOf(event()), window.events)
        assertEquals(1, window.unreadableLines)
        assertTrue(journal.report(now - 1, now + 1, "").contains("nieczytelne=1"))
    }

    @Test fun reportDoesNotInferKilledProcessFromGapInEvents() {
        val journal = DiagnosticJournal(temporary.newFolder())
        journal.append(event(now - 8 * 3_600_000))
        journal.append(event(now, "restart", "SERVICE_START"))
        val report = journal.report(now - 24 * 3_600_000, now, "")
        assertTrue(report.contains("Luka w logach nie dowodzi zakończenia procesu"))
        assertTrue(report.contains("poprzednie zakończenie: nieznane"))
    }

    @Test fun recoveredNetworkDoesNotReplaceTheLastObservedFailure() {
        val journal = DiagnosticJournal(temporary.newFolder())
        journal.append(event(now - 2, "actual-failure", "SYNC_ERROR"))
        journal.append(event(now - 1, "blocked=false", "NETWORK_UNBLOCKED"))
        journal.append(event(now, "blocked=false", "NETWORK_BLOCKED_CHANGED"))
        val summary = journal.report(now - 10, now + 1, "").lineSequence()
            .first { it.startsWith("Ostatnia awaria") }
        assertTrue(summary.contains("actual-failure"))
        assertFalse(summary.contains("blocked=false"))
        journal.append(event(now + 1, "blocked=true", "NETWORK_BLOCKED"))
        val blockedSummary = journal.report(now - 10, now + 1, "").lineSequence()
            .first { it.startsWith("Ostatnia awaria") }
        assertTrue(blockedSummary.contains("NETWORK_BLOCKED blocked=true"))
    }
}
