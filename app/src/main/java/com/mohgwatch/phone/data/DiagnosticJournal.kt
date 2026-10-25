package com.mohgwatch.phone.data

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal data class DiagnosticEvent(
    val wallMillis: Long,
    val elapsedMillis: Long,
    val sessionId: String,
    val processId: Int,
    val cycleId: String?,
    val name: String,
    val details: String,
    val zoneId: String = ZoneId.systemDefault().id
)

internal data class DiagnosticWindow(
    val events: List<DiagnosticEvent>,
    val discardedEvents: Long = 0,
    val unreadableLines: Int = 0,
    val retainedFromMillis: Long? = null,
    val retainedToMillis: Long? = null
)

internal class DiagnosticJournal(
    private val directory: File,
    private val maxBytes: Long = 8 * 1024 * 1024,
    private val retentionMillis: Long = 7 * 24 * 3_600_000L
) {
    private val eventsFile = File(directory, "events.tsv")
    private val discardedFile = File(directory, "discarded.txt")
    private var lastPrunedAt: Long? = null

    @Synchronized
    fun append(event: DiagnosticEvent) {
        directory.mkdirs()
        // A process may have ended halfway through its final record.
        if (eventsFile.exists() && eventsFile.length() > 0) {
            java.io.RandomAccessFile(eventsFile, "r").use {
                it.seek(it.length() - 1)
                if (it.readByte().toInt() != 10) eventsFile.appendText("\n")
            }
        }
        eventsFile.appendText(encode(event) + "\n", Charsets.UTF_8)
        val last = lastPrunedAt
        if (eventsFile.length() > maxBytes || last == null ||
            event.wallMillis - last >= minOf(3_600_000L, retentionMillis) || event.wallMillis < last) {
            compact(event.wallMillis)
            lastPrunedAt = event.wallMillis
        }
    }

    @Synchronized
    fun read(fromMillis: Long, toMillis: Long): DiagnosticWindow {
        if (!eventsFile.exists()) return DiagnosticWindow(emptyList())
        var unreadable = 0
        val retained = eventsFile.readLines(Charsets.UTF_8).mapNotNull {
            decode(it).also { event -> if (event == null) unreadable++ }
        }
        return DiagnosticWindow(
            events = retained.filter { it.wallMillis in fromMillis..toMillis },
            discardedEvents = discardedFile.takeIf { it.exists() }?.readText()?.trim()?.toLongOrNull() ?: 0,
            unreadableLines = unreadable,
            retainedFromMillis = retained.minOfOrNull { it.wallMillis },
            retainedToMillis = retained.maxOfOrNull { it.wallMillis }
        )
    }

    fun report(fromMillis: Long, toMillis: Long, configuration: String, maxChars: Int = 12_000): String {
        val window = read(fromMillis, toMillis)
        val failure = window.events.lastOrNull {
            it.name == "NETWORK_BLOCKED" || listOf("ERROR", "FAIL", "TIMEOUT").any { marker -> marker in it.name }
        }
        val previousExit = window.events.lastOrNull { it.name == "PROCESS_PREVIOUS_EXIT" }
        val heading = buildString {
            appendLine("MohgWatch — raport diagnostyczny")
            appendLine("Zakres żądany: ${formatTime(fromMillis)} — ${formatTime(toMillis)}")
            appendLine("Zakres zapisanych zdarzeń: ${window.retainedFromMillis?.let(::formatTime) ?: "brak"} — ${window.retainedToMillis?.let(::formatTime) ?: "brak"}")
            appendLine("Retencja: ${retentionMillis / 3_600_000} h / $maxBytes bajtów; usunięte=${window.discardedEvents}; nieczytelne=${window.unreadableLines}")
            appendLine("Konfiguracja bieżąca (chwila raportu, nie rekonstrukcja nocy):")
            appendLine(DiagnosticSanitizer.clean(configuration, 4_000))
            appendLine("Ostatnia awaria (obserwacja): ${failure?.let(::readable) ?: "brak zarejestrowanego błędu w wybranym okresie"}")
            appendLine("poprzednie zakończenie: ${previousExit?.details ?: "nieznane"}")
            appendLine("Luka w logach nie dowodzi zakończenia procesu ani jego przyczyny. Stan bieżący nie odtwarza stanu nocnego. Brak automatycznych hipotez.")
        }
        val timeline = mutableListOf<String>()
        var length = heading.length + 220
        for (event in window.events.asReversed()) {
            val line = readable(event)
            if (length + line.length + 1 > maxChars) break
            timeline.add(line)
            length += line.length + 1
        }
        return buildString {
            append(heading)
            appendLine("Obcięcie: ${if (timeline.size < window.events.size || window.discardedEvents > 0 || window.unreadableLines > 0) "tak" else "nie"}; oś ${timeline.size}/${window.events.size} zdarzeń z okresu; licznik usuniętych dotyczy całego dziennika.")
            appendLine("Oś zdarzeń (od najstarszego z pokazanych):")
            timeline.asReversed().forEach { appendLine(it) }
        }.take(maxChars)
    }

    private fun compact(nowMillis: Long) {
        val lines = eventsFile.readLines(Charsets.UTF_8)
        val recent = lines.filter { decode(it)?.wallMillis?.let { time -> time >= nowMillis - retentionMillis } == true }
        val targetBytes = if (eventsFile.length() > maxBytes) maxBytes * 3 / 4 else maxBytes
        var bytes = 0L
        val kept = recent.asReversed().takeWhile {
            bytes += it.toByteArray(Charsets.UTF_8).size + 1
            bytes <= targetBytes
        }.asReversed()
        val removed = lines.size - kept.size
        if (removed == 0) return
        val previous = discardedFile.takeIf { it.exists() }?.readText()?.trim()?.toLongOrNull() ?: 0
        val temporary = File(directory, "events.tmp")
        temporary.writeText(kept.joinToString("\n", postfix = if (kept.isEmpty()) "" else "\n"), Charsets.UTF_8)
        Files.move(temporary.toPath(), eventsFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        discardedFile.writeText((previous + removed).toString())
    }

    private fun encode(event: DiagnosticEvent) = listOf(
        event.wallMillis.toString(), formatTime(event.wallMillis, event.zoneId), event.elapsedMillis.toString(),
        DiagnosticSanitizer.clean(event.sessionId, 80), event.processId.toString(),
        DiagnosticSanitizer.clean(event.cycleId.orEmpty(), 80), DiagnosticSanitizer.clean(event.name, 80),
        DiagnosticSanitizer.clean(event.details)
    ).joinToString("\t")

    private fun decode(line: String): DiagnosticEvent? = try {
        val parts = line.split('\t', limit = 8)
        if (parts.size != 8) null else DiagnosticEvent(
            parts[0].toLong(), parts[2].toLong(), parts[3], parts[4].toInt(),
            parts[5].ifEmpty { null }, parts[6], parts[7], ZonedDateTime.parse(parts[1]).zone.id
        )
    } catch (_: RuntimeException) { null }

    private fun readable(event: DiagnosticEvent) =
        "${formatTime(event.wallMillis, event.zoneId)} elapsed=${event.elapsedMillis} pid=${event.processId} session=${event.sessionId} cycle=${event.cycleId ?: "-"} ${event.name} ${event.details}"

    companion object {
        fun formatTime(millis: Long, zoneId: String = ZoneId.systemDefault().id): String =
            DateTimeFormatter.ISO_ZONED_DATE_TIME.format(Instant.ofEpochMilli(millis).atZone(ZoneId.of(zoneId)))
    }
}

internal object DiagnosticSanitizer {
    private val structuredBody = Regex("(?s)\\{.*")
    private val cookieHeader = Regex("(?im)\\b(?:cookie|set-cookie)\\s*[:=].*")
    private val authorization = Regex("(?i)(authorization\\s*[:=]\\s*)(?:(?:bearer|basic)\\s+)?[^\\s,;]+")
    private val secrets = Regex("(?i)(\\b(?:password|passwd|pwd|hasło|token|access[_-]?token|refresh[_-]?token|cookie|set-cookie|email|username|login|account[_-]?id)\\b\\s*[:=]\\s*)(?:\"[^\"]*\"|'[^']*'|[^\\s,;]+)")
    private val bearer = Regex("(?i)\\bbearer\\s+[^\\s,;]+")
    private val email = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val urlCredentials = Regex("(?i)(https?://)[^/\\s@]+@")

    fun clean(value: String, maxChars: Int = 1_600): String {
        var safe = structuredBody.replace(value, "[pominięto treść strukturalną]")
        safe = cookieHeader.replace(safe, "Cookie: [ukryto]")
        safe = authorization.replace(safe) { "${it.groupValues[1]}[ukryto]" }
        safe = secrets.replace(safe) { "${it.groupValues[1]}[ukryto]" }
        safe = bearer.replace(safe, "Bearer [ukryto]")
        safe = email.replace(safe, "[ukryto adres]")
        safe = urlCredentials.replace(safe) { "${it.groupValues[1]}[ukryto]@" }
        safe = safe.replace('\t', ' ').replace("\r", "").replace("\n", " ↵ ")
        return if (safe.length <= maxChars) safe else safe.take((maxChars - 24).coerceAtLeast(0)) + " [szczegóły obcięte]"
    }
}
