package com.mohgwatch.phone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohgwatch.phone.data.WatchState
import com.mohgwatch.phone.service.GlucoseSyncService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNotificationTestsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isWorn by WatchState.isWornOnWrist.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Testy powiadomień") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Weryfikacja czujnika nadgarstka (NOWOŚĆ)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Czujnik nadgarstka (OnePlus Watch 2R)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Sprawdzenie sprzętowego sensora kontaktu ze skórą (LOW_LATENCY_OFFBODY_DETECT). Jeśli zegarek leży na półce, telefon nie zostanie wyciszony.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Aktualny stan:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (isWorn) "NA RĘKU" else "ZDJĘTY Z RĘKI",
                            color = if (isWorn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. O-Haptics na zegarku
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("O-Haptics na zegarku (Wibracje kierunkowe)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Test unikalnych wzorców wibracji wysyłanych bezpośrednio na nadgarstek:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { GlucoseSyncService.testHapticWatch(context, "LOW") },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text("Hipo", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { GlucoseSyncService.testHapticWatch(context, "HIGH") },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text("Hiper", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { GlucoseSyncService.testHapticWatch(context, "STABLE") },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text("Norma", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Testy alertów dźwiękowych (Dzień / Noc)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Alerty dźwiękowe telefonu (Dzień / Noc)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Dzień", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Text("Noc", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Niski
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Niski cukier", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { GlucoseSyncService.testAlertLow(context, "day") }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { }
                        Button(onClick = { GlucoseSyncService.testAlertLow(context, "night") }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { }
                    }
                    // Wysoki
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Wysoki cukier", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { GlucoseSyncService.testAlertHigh(context, "day") }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { }
                        Button(onClick = { GlucoseSyncService.testAlertHigh(context, "night") }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { }
                    }
                    // Rozłączenie
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Rozłączenie", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { GlucoseSyncService.testAlertDisconnect(context, "day") }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { }
                        Button(onClick = { GlucoseSyncService.testAlertDisconnect(context, "night") }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
