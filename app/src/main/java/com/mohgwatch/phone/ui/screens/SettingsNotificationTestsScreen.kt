package com.mohgwatch.phone.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mohgwatch.phone.ui.components.SettingsPage
import com.mohgwatch.phone.data.WatchState
import com.mohgwatch.phone.service.GlucoseSyncService
import com.mohgwatch.phone.service.DataLayerSender
import com.mohgwatch.core.model.TrendDirection
import com.mohgwatch.core.model.TrendArrow
import com.mohgwatch.core.model.TrendVibrationPattern
import kotlinx.coroutines.launch

enum class NotificationTestSection(val title: String) {
    SOUNDS("Testy dźwięków telefonu"),
    WATCH("Testy zegarka"),
    SIMULATION("Symulacja pomiarów i Demo")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNotificationTestsScreen(section: NotificationTestSection, onBack: () -> Unit) {
    val context = LocalContext.current
    val isWorn by WatchState.isWornOnWrist.collectAsState()
    var testGlucoseInput by rememberSaveable { mutableStateOf("55") }
    var testTrend by rememberSaveable { mutableStateOf(TrendArrow.STABLE) }
    var trendMenuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val dataLayerSender = remember { DataLayerSender(context) }

    SettingsPage(title = section.title, onBack = onBack) {
        if (section == NotificationTestSection.WATCH) {
            // Informacyjny podgląd czujnika nadgarstka.
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

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Podgląd wzorów alarmów progowych", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Każdy przycisk odtwarza wskazany wzór na zegarku, także gdy jego alarm jest wyłączony. Podgląd testuje tylko wibracje i nie wyświetla karty z wynikiem — ją sprawdzisz w symulacji pomiarów. Podgląd nie przerywa trwającego alarmu. Długie i mocne wzory używają maksymalnej siły silnika.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TrendDirection.entries.forEach { direction ->
                        Spacer(Modifier.height(12.dp))
                        Text(direction.label, style = MaterialTheme.typography.titleSmall)
                        TrendVibrationPattern.entries.forEach { pattern ->
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        dataLayerSender.sendHapticAlert(direction.hapticType, pattern = pattern, preview = true)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${direction.label}: ${pattern.label}")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

        }
        if (section == NotificationTestSection.SOUNDS) {
            // Testy alertów dźwiękowych (Dzień / Noc).
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
                        Button(onClick = { GlucoseSyncService.testAlertLow(context, "day") }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Test") }
                        Button(onClick = { GlucoseSyncService.testAlertLow(context, "night") }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Test") }
                    }
                    // Wysoki
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Wysoki cukier", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { GlucoseSyncService.testAlertHigh(context, "day") }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Test") }
                        Button(onClick = { GlucoseSyncService.testAlertHigh(context, "night") }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Test") }
                    }
                    // Rozłączenie
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Rozłączenie", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { GlucoseSyncService.testAlertDisconnect(context, "day") }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Test") }
                        Button(onClick = { GlucoseSyncService.testAlertDisconnect(context, "night") }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Test") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

        }
        if (section == NotificationTestSection.SIMULATION) {
            // Kontrolowane wstrzyknięcie odczytu korzysta z logiki prawdziwych alarmów.
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Symulacja pomiaru", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Wpisz wartość glukozy, aby sprawdzić alerty telefonu, wibracje zegarka i widżety. Wyślij kolejno wartość w zakresie, poniżej dolnego progu, powyżej górnego i ponownie w zakresie. Progi pochodzą z Ustawienia → Glukoza i progi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = testGlucoseInput,
                        onValueChange = { testGlucoseInput = it },
                        label = { Text("Wartość glukozy (mg/dL)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = trendMenuExpanded,
                        onExpandedChange = { trendMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "${testTrend.symbol} ${testTrend.description}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Strzałka trendu") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(trendMenuExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = trendMenuExpanded,
                            onDismissRequest = { trendMenuExpanded = false }
                        ) {
                            TrendArrow.entries.forEach { trend ->
                                DropdownMenuItem(
                                    text = { Text("${trend.symbol} ${trend.description}") },
                                    onClick = { testTrend = trend; trendMenuExpanded = false }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val v = testGlucoseInput.trim().toFloatOrNull()
                            if (v != null && v.isFinite() && v > 0f) {
                                GlucoseSyncService.injectReading(context, v, testTrend)
                                Toast.makeText(context, "Wstrzyknięto pomiar: ${v.toInt()} mg/dL", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Podaj poprawną liczbę glukozy", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Wyślij pomiar jako nowy odczyt")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            DemoReadingTest()
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
