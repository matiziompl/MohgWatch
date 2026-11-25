package com.mohgwatch.phone.data

internal enum class DiagnosticCategory { ALL, SYNC, NETWORK, API }

internal data class DiagnosticFilter(
    val category: DiagnosticCategory = DiagnosticCategory.ALL,
    val cycleId: String? = null,
    val runId: String? = null,
    val errorsOnly: Boolean = false
) {
    fun matches(event: DiagnosticEvent): Boolean {
        val inCategory = when (category) {
            DiagnosticCategory.ALL -> true
            DiagnosticCategory.SYNC -> listOf("SYNC_", "TIMER_", "WAKELOCK_", "WATCHDOG_", "WATCH_").any(event.name::startsWith)
            DiagnosticCategory.NETWORK -> event.name.startsWith("NETWORK_")
            DiagnosticCategory.API -> event.name.startsWith("API_") || event.name.startsWith("HTTP_")
        }
        return inCategory && (cycleId.isNullOrBlank() || event.cycleId == cycleId.trim()) &&
            (runId.isNullOrBlank() || event.runId() == runId.trim()) && (!errorsOnly || event.isFailure())
    }
}

private val runPrefix = Regex("^runId=([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}); ")
internal fun DiagnosticEvent.runId(): String? = runPrefix.find(details)?.groupValues?.get(1)
internal fun DiagnosticEvent.isFailure(): Boolean = name == "NETWORK_BLOCKED" ||
    (name == "HTTP_RESULT" && (metric("code")?.toIntOrNull() ?: 0) >= 400) ||
    listOf("ERROR", "FAIL", "TIMEOUT", "REJECTED").any(name::contains) ||
    (name == "SYNC_END" && Regex("\\bresult=(failed|error|timeout)\\b").containsMatchIn(details))

internal fun DiagnosticEvent.metric(name: String): String? =
    Regex("(?:^|[ ;])${Regex.escape(name)}=([^ ;]+)").find(details)?.groupValues?.get(1)
