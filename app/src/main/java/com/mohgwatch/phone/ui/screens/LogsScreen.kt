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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.core.content.FileProvider
import com.mohgwatch.core.model.DisconnectLog
import com.mohgwatch.core.model.LogBgColor
import com.mohgwatch.core.model.ThemeMode
import com.mohgwatch.phone.data.LogsStore
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.data.Diagnostics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
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
    var preparingReport by remember { mutableStateOf(false) }
    var reportStatus by remember { mutableStateOf<String?>(null) }

    fun prepareReport(exportFull: Boolean) {
        if (preparingReport) return
        preparingReport = true
        reportStatus = if (exportFull) "Przygotowuję pełne logi…" else "Przygotowuję raport…"
        val hours = reportHours
        scope.launch {
            try {
                if (exportFull) {
                    val file = withContext(Dispatchers.IO) { Diagnostics.export(context, hours) }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.diagnostics.files", file)
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newRawUri("Logi MohgWatch", uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(share, "Eksportuj pełne logi"))
                    reportStatus = "Przygotowano logi z ostatnich $hours godz. Faktyczny zakres i ewentualne braki opisano w pliku."
                } else {
                    val report = withContext(Dispatchers.IO) { Diagnostics.report(context, hours) }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Raport diagnostyczny MohgWatch", report))
                    reportStatus = "Skopiowano raport z ostatnich $hours godz. Wklej go do rozmowy."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                reportStatus = "Nie udało się przygotować lub udostępnić raportu. Spróbuj ponownie; możesz też wybrać krótszy okres."
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
        
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Text("Raport diagnostyczny", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Krótki raport zawiera konfigurację, ostatnią awarię i oś zdarzeń. Pełne logi zachowują szczegóły. Wybierz okres obejmujący nocną przerwę.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(24, 72, 168).forEach { hours ->
                        FilterChip(
                            selected = reportHours == hours,
                            onClick = { reportHours = hours; reportStatus = null },
                            enabled = !preparingReport,
                            label = { Text(if (hours == 168) "7 dni" else "$hours godz.") }
                        )
                    }
                }
                Button(
                    onClick = { prepareReport(exportFull = false) },
                    enabled = !preparingReport,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Kopiuj raport diagnostyczny") }
                OutlinedButton(
                    onClick = { prepareReport(exportFull = true) },
                    enabled = !preparingReport,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Eksportuj pełne logi") }
                Text(
                    "Logi przetrwają restart procesu. Retencja: do 7 dni i 8 MiB. Raport podaje faktyczny zakres i ewentualne obcięcie; luka w logach sama nie dowodzi zabicia procesu.",
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
