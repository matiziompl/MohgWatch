package com.mohgwatch.phone.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.core.content.FileProvider
import com.mohgwatch.core.model.DisconnectLog
import com.mohgwatch.core.model.LogBgColor
import com.mohgwatch.core.model.ThemeMode
import com.mohgwatch.phone.data.LogsStore
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.data.Diagnostics
import com.mohgwatch.phone.data.DiagnosticCategory
import com.mohgwatch.phone.data.DiagnosticFilter
import com.mohgwatch.phone.util.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val logsStore = remember { LogsStore(context) }
    val settingsStore = remember { SettingsStore(context) }
    val logs by logsStore.logsFlow.collectAsState(initial = emptyList())
    val settings by settingsStore.settingsFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var reportHours by rememberSaveable { mutableIntStateOf(24) }
    var reportCategory by rememberSaveable { mutableStateOf(DiagnosticCategory.ALL) }
    var reportErrorsOnly by rememberSaveable { mutableStateOf(false) }
    var reportCycleId by rememberSaveable { mutableStateOf("") }
    var reportRunId by rememberSaveable { mutableStateOf("") }
    var reportFrom by rememberSaveable { mutableStateOf("") }
    var reportTo by rememberSaveable { mutableStateOf("") }
    var advancedFiltersExpanded by rememberSaveable { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var preparingReport by remember { mutableStateOf(false) }
    var reportStatus by remember { mutableStateOf<String?>(null) }
    val categoryLabels = mapOf(
        DiagnosticCategory.ALL to tr("Wszystkie", "All"),
        DiagnosticCategory.SYNC to tr("Synchronizacja", "Synchronization"),
        DiagnosticCategory.NETWORK to tr("Sieć", "Network"),
        DiagnosticCategory.API to "API",
        DiagnosticCategory.CDM to "CDM"
    )
    val invalidRange = tr("Nieprawidłowy zakres dat. Użyj ISO z przesunięciem strefy lub yyyy-MM-dd HH:mm. Początek nie może być późniejszy niż koniec.",
        "Invalid date range. Use ISO with a time zone offset or yyyy-MM-dd HH:mm. The start must not be later than the end.")
    val preparingExport = tr("Przygotowuję pełne logi…", "Preparing full logs…")
    val preparingSummary = tr("Przygotowuję raport…", "Preparing report…")
    val exportTitle = tr("Eksportuj pełne logi", "Export full logs")
    val exportReady = tr("Przygotowano logi dla wybranego zakresu i filtrów. Faktyczny zakres i ewentualne braki opisano w pliku.",
        "Logs are ready for the selected range and filters. The file describes actual coverage and any gaps.")
    val reportReady = tr("Skopiowano raport dla wybranego zakresu i filtrów. Wklej go do rozmowy.",
        "The report for the selected range and filters was copied. Paste it into the conversation.")
    val reportFailed = tr("Nie udało się przygotować lub udostępnić raportu. Spróbuj ponownie; możesz też wybrać krótszy okres.",
        "The report could not be prepared or shared. Try again or select a shorter period.")

    fun prepareReport(exportFull: Boolean) {
        if (preparingReport) return
        val range = parseDiagnosticReportRange(reportFrom, reportTo, reportHours, System.currentTimeMillis())
        if (range == null) {
            reportStatus = invalidRange
            return
        }
        val filter = DiagnosticFilter(
            category = reportCategory,
            cycleId = reportCycleId.trim().takeIf(String::isNotEmpty),
            runId = reportRunId.trim().takeIf(String::isNotEmpty),
            errorsOnly = reportErrorsOnly
        )
        val (fromMillis, toMillis) = range
        preparingReport = true
        reportStatus = if (exportFull) preparingExport else preparingSummary
        scope.launch {
            try {
                if (exportFull) {
                    val file = withContext(Dispatchers.IO) { Diagnostics.export(context, fromMillis, toMillis, filter) }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.diagnostics.files", file)
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newRawUri("Logi MohgWatch", uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(share, exportTitle))
                    reportStatus = exportReady
                } else {
                    val report = withContext(Dispatchers.IO) { Diagnostics.report(context, fromMillis, toMillis, filter) }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Raport diagnostyczny MohgWatch", report))
                    reportStatus = reportReady
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                reportStatus = reportFailed
            } finally {
                preparingReport = false
            }
        }
    }
    
    val bgColorEnum = settings?.logBackgroundColor ?: LogBgColor.WHITE
    
    val isDark = when (settings?.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> isSystemInDarkTheme()
    }
    
    val cardColor = when (bgColorEnum) {
        LogBgColor.WHITE -> if (isDark) Color(0xFF1E1E1E) else Color.White
        LogBgColor.GRAY -> if (isDark) Color(0xFF2D2D2D) else Color(0xFFF0F0F0)
        LogBgColor.ORANGE -> if (isDark) Color(0xFF4A2B11) else Color(0xFFFFF0E0)
        LogBgColor.LIGHT_BLUE -> if (isDark) Color(0xFF0C2D48) else Color(0xFFE3F2FD)
        LogBgColor.MONOCHROME -> if (isDark) Color(0xFF121212) else Color(0xFFE0E0E0)
        LogBgColor.PURPLE -> if (isDark) Color(0xFF301934) else Color(0xFFF3E5F5)
        LogBgColor.DARK_GREEN -> if (isDark) Color(0xFF0F3119) else Color(0xFFE8F5E9)
        LogBgColor.CRIMSON -> if (isDark) Color(0xFF3D0014) else Color(0xFFFFB3CA)
        LogBgColor.VAPOR -> if (isDark) Color(0xFF3D003D) else Color(0xFFFFB3F0)
        LogBgColor.TIDE -> if (isDark) Color(0xFF00303D) else Color(0xFFB3F4FF)
        LogBgColor.PULSE -> if (isDark) Color(0xFF1B003D) else Color(0xFFD4B3FF)
        LogBgColor.ACID -> if (isDark) Color(0xFF263300) else Color(0xFFF0FFB3)
        LogBgColor.DEFAULT -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isDark) Color.White else Color.Black

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Logi i diagnostyka") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        
        LazyColumn(modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp)) {
            item {
                Text(tr("Raport diagnostyczny", "Diagnostic report"), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    tr("Krótki raport zawiera konfigurację, ostatnią awarię i oś zdarzeń. Pełne logi zachowują szczegóły. Wybierz okres obejmujący nocną przerwę.",
                        "The short report includes configuration, the latest failure and an event timeline. Full logs retain details. Choose a period covering the overnight gap."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(24, 72, 168).forEach { hours ->
                        FilterChip(
                            selected = reportHours == hours && reportFrom.isBlank() && reportTo.isBlank(),
                            onClick = {
                                reportHours = hours
                                reportFrom = ""
                                reportTo = ""
                                reportStatus = null
                            },
                            enabled = !preparingReport,
                            label = { Text(if (hours == 168) tr("7 dni", "7 days") else tr("$hours godz.", "$hours hours")) }
                        )
                    }
                }
                Box {
                    OutlinedButton(onClick = { categoryMenuExpanded = true }, enabled = !preparingReport,
                        modifier = Modifier.fillMaxWidth()) {
                        val label = categoryLabels.getValue(reportCategory)
                        Text(tr("Kategoria: $label", "Category: $label"))
                    }
                    DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                        DiagnosticCategory.entries.forEach { category ->
                            DropdownMenuItem(text = { Text(categoryLabels.getValue(category)) }, onClick = {
                                reportCategory = category
                                categoryMenuExpanded = false
                                reportStatus = null
                            })
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                        value = reportErrorsOnly, enabled = !preparingReport, role = Role.Switch,
                        onValueChange = { reportErrorsOnly = it; reportStatus = null }
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tr("Tylko błędy", "Errors only"), modifier = Modifier.weight(1f))
                    Switch(checked = reportErrorsOnly, onCheckedChange = null, enabled = !preparingReport)
                }
                TextButton(onClick = { advancedFiltersExpanded = !advancedFiltersExpanded },
                    enabled = !preparingReport, modifier = Modifier.fillMaxWidth()) {
                    Text(if (advancedFiltersExpanded) tr("Ukryj dodatkowe filtry", "Hide additional filters")
                        else tr("Dodatkowe filtry i zakres dat", "Additional filters and date range"))
                }
                if (!advancedFiltersExpanded && listOf(reportCycleId, reportRunId, reportFrom, reportTo).any(String::isNotBlank)) {
                    Text(tr("Dodatkowe filtry są aktywne.", "Additional filters are active."),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (advancedFiltersExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = reportCycleId, onValueChange = { reportCycleId = it; reportStatus = null },
                            label = { Text(tr("Identyfikator cyklu (opcjonalnie)", "Cycle ID (optional)")) },
                            enabled = !preparingReport, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = reportRunId, onValueChange = { reportRunId = it; reportStatus = null },
                            label = { Text(tr("Identyfikator pomiaru (opcjonalnie)", "Run ID (optional)")) },
                            enabled = !preparingReport, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Text(tr("Daty: ISO z przesunięciem strefy (np. 2026-10-25T08:00+01:00) lub yyyy-MM-dd HH:mm w bieżącej strefie. Puste pola korzystają z wybranego okresu do chwili naciśnięcia przycisku.",
                            "Dates: ISO with a time zone offset (e.g. 2026-10-25T08:00+01:00) or yyyy-MM-dd HH:mm in the current zone. Blank fields use the selected period ending when you tap the button."),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(value = reportFrom, onValueChange = { reportFrom = it; reportStatus = null },
                            label = { Text(tr("Od (opcjonalnie)", "From (optional)")) },
                            enabled = !preparingReport, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = reportTo, onValueChange = { reportTo = it; reportStatus = null },
                            label = { Text(tr("Do (opcjonalnie)", "To (optional)")) },
                            enabled = !preparingReport, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
                Button(
                    onClick = { prepareReport(exportFull = false) },
                    enabled = !preparingReport,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(tr("Kopiuj raport diagnostyczny", "Copy diagnostic report")) }
                OutlinedButton(
                    onClick = { prepareReport(exportFull = true) },
                    enabled = !preparingReport,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(exportTitle) }
                Text(
                    tr("Logi przetrwają restart procesu. Retencja: do 7 dni i 8 MiB. Raport podaje faktyczny zakres i ewentualne obcięcie; luka w logach sama nie dowodzi zabicia procesu.",
                        "Logs survive process restarts. Retention: up to 7 days and 8 MiB. The report states actual coverage and any truncation; a log gap alone does not prove the process was killed."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                reportStatus?.let { status ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        status,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 20.dp))
                Text("Historia rozłączeń", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
            }
            if (logs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Brak zapisanych rozłączeń (>5 min).")
                    }
                }
            } else {
                items(logs.reversed()) { log ->
                    LogCard(log = log, backgroundColor = cardColor, textColor = textColor)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Słownik powodów rozłączeń:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        com.mohgwatch.core.model.DisconnectReason.entries.forEach { reason ->
                            val desc = when (reason) {
                                com.mohgwatch.core.model.DisconnectReason.NETWORK_ERROR -> "Brak połączenia z internetem (Wi-Fi lub dane komórkowe)."
                                com.mohgwatch.core.model.DisconnectReason.ANDROID_NETWORK_BLOCKED -> "Android ograniczył sieć aplikacji. Sprawdź wyjątek od optymalizacji baterii i ustawienia danych w tle."
                                com.mohgwatch.core.model.DisconnectReason.AUTH_ERROR -> "Nieprawidłowe dane logowania (zmieniono hasło lub wygasła sesja)."
                                com.mohgwatch.core.model.DisconnectReason.API_ERROR -> "Błąd lub przerwa techniczna po stronie serwerów LibreLinkUp."
                                com.mohgwatch.core.model.DisconnectReason.SYSTEM_KILLED -> "Historyczna klasyfikacja przerwy. Sama luka w logach nie potwierdza zabicia procesu; sprawdź raport diagnostyczny i zapis poprzedniego zakończenia procesu."
                                com.mohgwatch.core.model.DisconnectReason.UNKNOWN -> "Nieznany powód utraty łączności."
                            }
                            Text("- ${reason.label}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(desc, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

internal fun parseDiagnosticReportRange(
    fromText: String,
    toText: String,
    hours: Int,
    nowMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): Pair<Long, Long>? {
    fun parse(text: String): Long? {
        val value = text.trim()
        val instant = runCatching { Instant.parse(value) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(value, DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm", Locale.ROOT)
                    .withResolverStyle(ResolverStyle.STRICT)).atZone(zoneId).toInstant()
            }.getOrNull()
        return instant?.let { runCatching { it.toEpochMilli() }.getOrNull() }
    }
    val from = if (fromText.isBlank()) nowMillis - hours.coerceIn(1, 168) * 3_600_000L
        else parse(fromText) ?: return null
    val to = if (toText.isBlank()) nowMillis else parse(toText) ?: return null
    return if (from <= to) Pair(from, to) else null
}

@Composable
fun LogCard(log: DisconnectLog, backgroundColor: Color, textColor: Color) {
    val sdf = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
    val startStr = sdf.format(Date(log.disconnectTime))
    val endStr = log.reconnectTime?.let { sdf.format(Date(it)) } ?: "Trwa..."
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(12.dp).background(Color(0xFFE53935), RoundedCornerShape(6.dp)))
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.size(12.dp).background(Color(0xFF43A047), RoundedCornerShape(6.dp)))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Od: $startStr", color = textColor, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Do: $endStr", color = textColor, style = MaterialTheme.typography.bodyMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = log.reason.label, color = textColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                if (log.reconnectTime != null) {
                    Text(text = "${log.getDurationMinutes()} min", color = textColor, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
