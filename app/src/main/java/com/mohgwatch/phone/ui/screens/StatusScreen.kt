package com.mohgwatch.phone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.model.MeasurementColor
import com.mohgwatch.core.model.TrendArrow
import com.mohgwatch.core.util.GlucoseFormatter
import com.mohgwatch.phone.data.CredentialStore
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.service.DataLayerSender
import com.mohgwatch.phone.service.GlucoseSyncService
import com.mohgwatch.phone.service.GlucoseSyncState
import com.mohgwatch.phone.ui.theme.*
import com.mohgwatch.phone.util.tr
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun StatusScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataLayerSender = remember { DataLayerSender(context) }
    val settingsStore = remember { SettingsStore(context) }
    val credentialStore = remember { CredentialStore(context) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = com.mohgwatch.core.model.UserSettings())

    val liveReading by GlucoseSyncState.latestReading.collectAsState()
    val syncStatusText by GlucoseSyncState.syncStatusText.collectAsState()
    val isSyncing by GlucoseSyncState.isSyncing.collectAsState()
    val lastError by GlucoseSyncState.lastError.collectAsState()
    val lastSyncTs by GlucoseSyncState.lastSyncTimestamp.collectAsState()

    var credentialsPresent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        credentialsPresent = credentialStore.hasCredentials()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            tr("Status Glukozy", "Glucose Status"),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Warning card if not logged in
        if (!credentialsPresent) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, null, tint = ErrorRed)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        tr("Brak danych logowania. Przejdź do zakładki Logowanie i zaloguj się na swoje konto LibreLinkUp!", "No login data. Go to the Login tab and log in to your LibreLinkUp account!"),
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Glucose display card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    tr("Ostatni odczyt z LibreLinkUp", "Last reading from LibreLinkUp"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                val reading = liveReading
                if (reading != null) {
                    val valueStr = GlucoseFormatter.format(reading.value, settings.unit)
                    val rangeColor = when {
                        reading.value < settings.lowThreshold -> GlucoseLow
                        reading.value > settings.highThreshold -> GlucoseHigh
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            valueStr,
                            fontSize = 60.sp,
                            fontWeight = FontWeight.Bold,
                            color = rangeColor
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            reading.trendArrow.symbol,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = rangeColor
                        )
                    }

                    Text(
                        settings.unit.shortLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        GlucoseFormatter.formatMinutesAgo(reading.getMinutesAgo()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Text(
                        tr("---", "---"),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        settings.unit.shortLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        syncStatusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (lastError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connection & service status card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isSyncing) SuccessGreen else MaterialTheme.colorScheme.error)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (isSyncing) "Usługa w tle aktywna" else "Usługa w tle zatrzymana",
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Filled.Sync,
                        null,
                        tint = if (isSyncing) SuccessGreen else MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Status: $syncStatusText",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (lastError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (lastSyncTs > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val mins = (System.currentTimeMillis() - lastSyncTs) / 60_000
                    Text(
                        "Ostatnia udana próba: ${GlucoseFormatter.formatMinutesAgo(mins)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Watch, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Bluetooth Data Layer", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(
                        if (liveReading != null) "Wysłano na zegarek" else "Gotowe do wysłania",
                        color = SuccessGreen,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (settings.showDemoButton) {
            Spacer(modifier = Modifier.height(16.dp))

            // Test/Demo Data Button
            OutlinedButton(
                onClick = {
                    val mockVal = Random.nextInt(75, 195).toFloat()
                    val mockReading = GlucoseReading(
                        value = mockVal,
                        trendArrow = TrendArrow.entries.filter { it != TrendArrow.UNKNOWN }.random(),
                        measurementColor = when {
                            mockVal < settings.lowThreshold -> MeasurementColor.LOW
                            mockVal > settings.highThreshold -> MeasurementColor.HIGH
                            else -> MeasurementColor.IN_RANGE
                        },
                        timestamp = System.currentTimeMillis(),
                        isHigh = mockVal > settings.highThreshold,
                        isLow = mockVal < settings.lowThreshold
                    )
                    GlucoseSyncState.latestReading.value = mockReading
                    GlucoseSyncState.syncStatusText.value = "Wysłano testowy odczyt (Demo)"

                    val mockHistory = (1..12).map { i ->
                        GlucoseReading(
                            value = Random.nextInt(80, 170).toFloat(),
                            trendArrow = TrendArrow.STABLE,
                            measurementColor = MeasurementColor.IN_RANGE,
                            timestamp = System.currentTimeMillis() - (i * 15 * 60_000L),
                            isHigh = false,
                            isLow = false
                        )
                    }
                    GlucoseSyncState.history.value = mockHistory

                    scope.launch {
                        dataLayerSender.sendGlucoseReading(mockReading)
                        dataLayerSender.sendGlucoseHistory(mockHistory)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal400)
            ) {
                Icon(Icons.Filled.Bolt, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Wyślij testowy odczyt (Demo)")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    GlucoseSyncService.stop(context)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.Stop, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(tr("Zatrzymaj", "Stop"))
            }
            Button(
                onClick = {
                    GlucoseSyncService.start(context)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(tr("Uruchom", "Start"))
            }
        }
    }
}
