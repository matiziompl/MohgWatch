package com.mohgwatch.phone.data

internal data class NightTestSample(
    val startedElapsedMillis: Long,
    val endedElapsedMillis: Long,
    val events: List<DiagnosticEvent>,
    val incomplete: Boolean = false
)

internal object NightTestReport {
    fun summarize(sample: NightTestSample): String {
        val events = sample.events.filter { it.elapsedMillis in sample.startedElapsedMillis..sample.endedElapsedMillis }
            .sortedBy { it.elapsedMillis }
        val starts = events.filter { it.name == "SYNC_START" }.mapNotNull { it.cycleId }.toSet()
        val successes = events.filter { it.name == "SYNC_END" && it.cycleId in starts && it.metric("result") == "success" }
            .mapNotNull { it.cycleId }.toSet()
        val phase = mutableMapOf<String, String>()
        val transport = mutableSetOf<String>()
        val response = mutableSetOf<String>()
        val history = mutableSetOf<String>()
        events.forEachIndexed { index, event ->
            if (event.name == "SYNC_STAGE" && event.cycleId != null) {
                phase[event.cycleId] = if (event.details.contains("history_download")) "history" else "latest"
            }
            val failedHttp = event.name == "HTTP_RESULT" && (event.metric("code")?.toIntOrNull() ?: 0) >= 400
            if (failedHttp || event.name in setOf("HTTP_ERROR", "API_NETWORK_ERROR", "API_RESPONSE_ERROR", "API_ERROR", "HISTORY_ERROR")) {
                val identity = event.cycleId ?: "${event.sessionId}:$index"
                val isHistory = event.name == "HISTORY_ERROR" || event.metric("stage") == "history" || event.metric("endpoint") == "history" ||
                    (event.metric("stage") == null && phase[event.cycleId] == "history")
                when {
                    isHistory -> history.add(identity)
                    event.name == "API_NETWORK_ERROR" || event.name == "HTTP_ERROR" -> transport.add(identity)
                    else -> response.add(identity)
                }
            }
        }
        val fresh = events.filter { it.name == "MEASUREMENT" && it.metric("fresh") == "true" && it.metric("cache") == "false" }
            .distinctBy { it.metric("timestamp") ?: "${it.sessionId}:${it.elapsedMillis}" }
        val points = listOf(sample.startedElapsedMillis) + fresh.map { it.elapsedMillis } + sample.endedElapsedMillis
        val gap = points.zipWithNext { first, second -> (second - first).coerceAtLeast(0L) }.maxOrNull() ?: 0L
        val lateness = events.mapNotNull { (it.metric("latenessMs") ?: it.metric("lateMs"))?.toLongOrNull() }.maxOrNull()
        val duration = events.filter { it.name == "SYNC_END" }.distinctBy { it.cycleId ?: "${it.sessionId}:${it.elapsedMillis}" }
            .sumOf { it.metric("durationMs")?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L }
        val wakeStarts = mutableMapOf<String, Long>()
        var wakeMillis = 0L
        events.forEach { event ->
            val key = event.cycleId ?: return@forEach
            if (event.name == "WAKELOCK_ACQUIRED" && event.metric("type") == "sync") wakeStarts[key] = event.elapsedMillis
            if (event.name == "WAKELOCK_RELEASED" && event.metric("type") == "sync") {
                wakeStarts.remove(key)?.let { wakeMillis += (event.elapsedMillis - it).coerceAtLeast(0L) }
            }
        }
        return buildString {
            appendLine("Czas pomiaru: ${(sample.endedElapsedMillis - sample.startedElapsedMillis).coerceAtLeast(0L) / 1000} s")
            appendLine("Cykle rozpoczęte: ${starts.size}")
            appendLine("Cykle udane: ${successes.size}")
            appendLine("Odpowiedzi HTTP 2xx: ${events.count { it.name == "HTTP_RESULT" && (it.metric("code")?.toIntOrNull() ?: 0) in 200..299 }}")
            appendLine("Alarmy zaplanowane: ${events.count { it.name == "SYNC_ALARM_PLANNED" }}")
            appendLine("Alarmy dostarczone: ${events.count { it.name == "SYNC_ALARM_DELIVERED" }}")
            appendLine("Timery zaplanowane: ${events.count { it.name == "TIMER_PLANNED" }}")
            appendLine("Timery dostarczone: ${events.count { it.name == "TIMER_DELIVERED" }}")
            appendLine("Błędy transportu: ${transport.size}")
            appendLine("Błędy odpowiedzi: ${response.size}")
            appendLine("Błędy historii: ${history.size}")
            appendLine("Nowe świeże pomiary: ${fresh.size}")
            appendLine("Najdłuższa przerwa bez nowego świeżego pomiaru: ${gap / 1000} s")
            appendLine("Największe opóźnienie wyzwalacza: ${lateness?.let { "$it ms" } ?: "brak danych"}")
            appendLine("Zarejestrowany czas cykli: $duration ms")
            appendLine("Zarejestrowany czas krótkich WakeLocków: $wakeMillis ms; niezamknięte pary=${wakeStarts.size}")
            appendLine("Kompletność: ${if (sample.incomplete || wakeStarts.isNotEmpty()) "niepełna" else "pełna"}")
            append("Wyzwalacze są liczone osobno od cykli; brak wpisu nie dowodzi pominięcia zadania przez Androida.")
        }
    }
}
