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
import com.mohgwatch.core.util.GlucoseFormatter
import com.mohgwatch.phone.data.CredentialStore
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.service.GlucoseSyncService
import com.mohgwatch.phone.service.GlucoseSyncState
import com.mohgwatch.phone.ui.theme.*
import com.mohgwatch.phone.util.tr

@Composable
fun StatusScreen() {
    val context = LocalContext.current
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

    val compact = false
    val screenPaddingHorizontal = if (compact) 16.dp else 24.dp
    val screenPaddingVertical = if (compact) 10.dp else 24.dp
    val cardPadding = if (compact) 12.dp else 20.dp
    val cardSpacing = if (compact) 8.dp else 16.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = screenPaddingHorizontal, vertical = screenPaddingVertical),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!settings.hideStatusHeader) {
            Text(
                tr("Status Glukozy", "Glucose Status"),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Glucose display card (Card 1)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    tr("Ostatni odczyt z LibreLinkUp", "Last reading from LibreLinkUp"),
                    style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(if (compact) 6.dp else 12.dp))

                val reading = liveReading
                if (reading != null) {
                    val valueStr = GlucoseFormatter.format(reading.value, settings.unit)
                    val rangeColor = when {
                        reading.value < settings.lowThreshold -> GlucoseLow
                        reading.value > settings.veryHighThreshold -> GlucoseVeryHigh
                        reading.value > settings.highThreshold -> GlucoseHigh
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            valueStr,
                            fontSize = if (compact) 48.sp else 60.sp,
                            fontWeight = FontWeight.Bold,
                            color = rangeColor
                        )
                        Spacer(modifier = Modifier.width(if (compact) 8.dp else 12.dp))
                        Text(
                            reading.trendArrow.symbol,
                            fontSize = if (compact) 38.sp else 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = rangeColor
                        )
                    }

                    Text(
                        settings.unit.shortLabel,
                        style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(if (compact) 4.dp else 8.dp))
                    Text(
                        GlucoseFormatter.formatMinutesAgo(reading.getMinutesAgo()),
                        style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Text(
                        tr("---", "---"),
                        fontSize = if (compact) 48.sp else 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        settings.unit.shortLabel,
                        style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(if (compact) 4.dp else 8.dp))
                    Text(
                        syncStatusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (lastError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(cardSpacing))

        // Connection status.
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(cardPadding)) {
                if (!credentialsPresent) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ErrorRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(if (compact) 8.dp else 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = ErrorRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            tr("Brak danych logowania. Otwórz Ustawienia → Profil.", "No login data. Open Settings → Profile."),
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(if (compact) 8.dp else 16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isSyncing) SuccessGreen else MaterialTheme.colorScheme.error)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        if (isSyncing) "Usługa w tle aktywna" else "Usługa w tle zatrzymana",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Filled.Sync,
                        null,
                        modifier = Modifier.size(if (compact) 18.dp else 24.dp),
                        tint = if (isSyncing) SuccessGreen else MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(if (compact) 6.dp else 12.dp))
                Text(
                    "Status: $syncStatusText",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (lastError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (lastSyncTs > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    val mins = (System.currentTimeMillis() - lastSyncTs) / 60_000
                    Text(
                        "Ostatnia udana próba: ${GlucoseFormatter.formatMinutesAgo(mins)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(if (compact) 8.dp else 16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(if (compact) 8.dp else 16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Watch, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Bluetooth Data Layer", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(
                        if (liveReading != null) "Wysłano na zegarek" else "Gotowe do wysłania",
                        color = SuccessGreen,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
            }
        }

        if (settings.showInjectionSites) {
            Spacer(modifier = Modifier.height(cardSpacing))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(cardPadding)) {
                    val sites = com.mohgwatch.core.util.InjectionSiteHelper.getSiteName(
                        com.mohgwatch.core.util.InjectionSiteHelper.getTodayCycleIndex()
                    )
                    Text(
                        text = "Dzisiejsze miejsca zastrzyków:",
                        style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sites,
                        style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(if (compact) 12.dp else 24.dp))

        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    GlucoseSyncService.forceSync(context)
                },
                modifier = Modifier.weight(1f).height(if (compact) 42.dp else 48.dp),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = androidx.compose.ui.graphics.Color.White)
            ) {
                Icon(Icons.Filled.Sync, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(tr("Synchronizuj", "Sync"), style = MaterialTheme.typography.bodyMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(if (compact) 8.dp else 12.dp))
        Button(
            onClick = { GlucoseSyncService.clearNotifications(context) },
            modifier = Modifier.fillMaxWidth().height(if (compact) 36.dp else 40.dp),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color.DarkGray),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Przerwij wszystkie alerty", fontSize = if (compact) 14.sp else 16.sp)
        }
    }
}
