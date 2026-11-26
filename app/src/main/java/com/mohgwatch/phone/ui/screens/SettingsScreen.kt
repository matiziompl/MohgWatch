package com.mohgwatch.phone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mohgwatch.phone.BuildConfig
import com.mohgwatch.phone.ui.components.NightTestCard
import com.mohgwatch.phone.ui.navigation.Screen
import com.mohgwatch.phone.util.tr

@Composable
fun SettingsScreen(onNavigate: (Screen) -> Unit) {
    SettingsMenu(tr("Ustawienia", "Settings"), listOf(
        MenuGroup(tr("Cukrzyca", "Diabetes"), listOf(
            MenuDestination(Screen.SettingsDiabetes, tr("Glukoza i progi", "Glucose and thresholds"), tr("Jednostka i progi alarmowe", "Unit and alarm thresholds"), Icons.Filled.WaterDrop),
            MenuDestination(Screen.SettingsDoses, tr("Dawki i miejsca wkłuć", "Doses and injection sites"), tr("Dzisiejsze dawki i sugerowane miejsca", "Today's doses and suggested sites"), Icons.Filled.MedicalServices))),
        MenuGroup(tr("Działanie w tle", "Background"), listOf(
            MenuDestination(Screen.SettingsSync, tr("Synchronizacja i bateria", "Sync and battery"), tr("Interwał, gotowość na noc i uprawnienia Androida", "Interval, night readiness and Android permissions"), Icons.Filled.Sync))),
        MenuGroup(tr("Wygląd", "Appearance"), listOf(
            MenuDestination(Screen.SettingsTheme, tr("Telefon", "Phone"), tr("Język, motyw i kolory", "Language, theme and colors"), Icons.Filled.Palette),
            MenuDestination(Screen.SettingsWidgets, tr("Widżety", "Widgets"), tr("Kolory widżetów i wartości glukozy", "Widget and glucose colors"), Icons.Filled.Widgets),
            MenuDestination(Screen.SettingsStatus, tr("Status", "Status"), tr("Nagłówek i przyciski ekranu Status", "Status screen header and buttons"), Icons.Filled.Dashboard))),
        MenuGroup(tr("Konto", "Account"), listOf(
            MenuDestination(Screen.SettingsAccount, tr("Profil", "Profile"), tr("Konto LibreLinkUp, eksport i import ustawień", "LibreLinkUp account, settings export and import"), Icons.Filled.Person))),
        MenuGroup(tr("Diagnostyka", "Diagnostics"), listOf(
            MenuDestination(Screen.Diagnostics, tr("Diagnostyka i testy", "Diagnostics and tests"), tr("Pomiar nocny, logi, testy i symulacje", "Night measurement, logs, tests and simulations"), Icons.Filled.Science)))
    ), onNavigate, footer = "${tr("Wersja", "Version")} v${BuildConfig.VERSION_NAME}")
}

@Composable
fun NotificationsMenuScreen(onNavigate: (Screen) -> Unit) {
    SettingsMenu(tr("Powiadomienia", "Notifications"), listOf(MenuGroup(null, listOf(
        MenuDestination(Screen.NotificationGlucose, tr("Alarmy glukozy", "Glucose alarms"), tr("Włączanie alarmów i powiadomień zegarka", "Enable alarms and watch notifications"), Icons.Filled.NotificationsActive),
        MenuDestination(Screen.NotificationHaptics, tr("Wibracje zegarka", "Watch vibrations"), tr("Niski, wysoki cukier i powrót do zakresu", "Low, high glucose and return to range"), Icons.Filled.Vibration),
        MenuDestination(Screen.NotificationSounds, tr("Dźwięki telefonu", "Phone sounds"), tr("Wybór dźwięków, głośność i opóźnienie", "Sounds, volume and delay"), Icons.AutoMirrored.Filled.VolumeUp),
        MenuDestination(Screen.NotificationNight, tr("Tryb nocny", "Night mode"), tr("Godziny nocy i nocne alarmy telefonu", "Night hours and phone alarms"), Icons.Filled.Bedtime),
        MenuDestination(Screen.NotificationConnection, tr("Brak danych i rozłączenia", "Missing data and disconnections"), tr("Alarmy utraty połączenia i starych pomiarów", "Lost connection and stale reading alarms"), Icons.Filled.WifiOff),
        MenuDestination(Screen.NotificationWrist, tr("Nadgarstek i wyciszanie", "Wrist and muting"), tr("Zdjęcie zegarka i wyciszanie telefonu", "Watch removal and phone muting"), Icons.Filled.Watch)
    ))), onNavigate)
}

/** One place for everything that checks the app: overnight measurement, logs, tests and simulations. */
@Composable
fun DiagnosticsMenuScreen(onBack: () -> Unit, onNavigate: (Screen) -> Unit) {
    SettingsMenu(tr("Diagnostyka i testy", "Diagnostics and tests"), listOf(
        MenuGroup(tr("Diagnostyka", "Diagnostics"), listOf(
            MenuDestination(Screen.Logs, tr("Logi i diagnostyka", "Logs and diagnostics"), tr("Historia, kopiowanie raportu i eksport", "History, copy report and export"), Icons.AutoMirrored.Filled.List))),
        MenuGroup(tr("Testy i symulacje", "Tests and simulations"), listOf(
            MenuDestination(Screen.SettingsTestSounds, tr("Testy dźwięków telefonu", "Phone sound tests"), tr("Niski, wysoki cukier i rozłączenie: dzień oraz noc", "Low, high glucose and disconnection: day and night"), Icons.AutoMirrored.Filled.VolumeUp),
            MenuDestination(Screen.SettingsTestWatch, tr("Testy zegarka", "Watch tests"), tr("Podgląd wzorów wibracji i stan nadgarstka", "Vibration pattern previews and wrist status"), Icons.Filled.Vibration),
            MenuDestination(Screen.SettingsTestSimulation, tr("Symulacja pomiarów i Demo", "Reading simulation and Demo"), tr("Własna wartość glukozy lub dane demonstracyjne", "Custom glucose value or demonstration data"), Icons.Filled.Science)))
    ), onNavigate, onBack, top = { NightTestCard() })
}

private data class MenuGroup(val title: String?, val destinations: List<MenuDestination>)
private data class MenuDestination(val screen: Screen, val title: String, val description: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMenu(title: String, groups: List<MenuGroup>, onNavigate: (Screen) -> Unit, onBack: (() -> Unit)? = null,
                         footer: String? = null, top: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                if (onBack != null) IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Wstecz", "Back"))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (top != null) item(key = "top") { top() }
            groups.forEach { group ->
                if (group.title != null) item(key = "group:${group.title}") {
                    Text(group.title.uppercase(), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp).semantics { heading() })
                }
                items(group.destinations, key = { it.screen.route }) { destination ->
                    Card(
                        onClick = { onNavigate(destination.screen) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(destination.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(destination.title, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(destination.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (footer != null) {
                item(key = "version") {
                    Text(
                        text = footer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}
