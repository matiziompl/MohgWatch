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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mohgwatch.phone.BuildConfig
import com.mohgwatch.phone.ui.navigation.Screen
import com.mohgwatch.phone.util.tr

@Composable
fun SettingsScreen(onNavigate: (Screen) -> Unit) {
    SettingsMenu(tr("Ustawienia", "Settings"), listOf(
        MenuDestination(Screen.SettingsDiabetes, tr("Glukoza i progi", "Glucose and thresholds"), tr("Jednostka i progi alarmowe", "Unit and alarm thresholds"), Icons.Filled.WaterDrop),
        MenuDestination(Screen.SettingsDoses, tr("Dawki i miejsca wkłuć", "Doses and injection sites"), tr("Dzisiejsze dawki i sugerowane miejsca", "Today's doses and suggested sites"), Icons.Filled.MedicalServices),
        MenuDestination(Screen.SettingsSync, tr("Synchronizacja", "Synchronization"), tr("Częstotliwość pobierania i sterowanie usługą", "Fetch interval and service controls"), Icons.Filled.Sync),
        MenuDestination(Screen.SettingsGeneral, tr("Bateria i praca w tle", "Battery and background"), tr("Wybudzenia i uprawnienia Androida", "Wake protection and Android permissions"), Icons.Filled.BatteryChargingFull),
        MenuDestination(Screen.SettingsTheme, tr("Wygląd telefonu", "Phone appearance"), tr("Język, motyw, kolory i ekran Status", "Language, theme, colors and Status screen"), Icons.Filled.Palette),
        MenuDestination(Screen.SettingsWidgets, tr("Widżety", "Widgets"), tr("Kolory widżetów i wartości glukozy", "Widget and glucose colors"), Icons.Filled.Widgets),
        MenuDestination(Screen.SettingsAccount, tr("Profil", "Profile"), tr("Konto LibreLinkUp, eksport i import ustawień", "LibreLinkUp account, settings export and import"), Icons.Filled.Person),
        MenuDestination(Screen.Logs, tr("Logi i diagnostyka", "Logs and diagnostics"), tr("Historia, kopiowanie raportu i eksport", "History, copy report and export"), Icons.AutoMirrored.Filled.List),
        MenuDestination(Screen.SettingsNotificationTests, tr("Testy i symulacje", "Tests and simulations"), tr("Dźwięki, wibracje i dane testowe", "Sounds, vibrations and test data"), Icons.Filled.Science)
    ), onNavigate, footer = "${tr("Wersja", "Version")} v${BuildConfig.VERSION_NAME}")
}

@Composable
fun NotificationsMenuScreen(onNavigate: (Screen) -> Unit) {
    SettingsMenu(tr("Powiadomienia", "Notifications"), listOf(
        MenuDestination(Screen.NotificationGlucose, tr("Alarmy glukozy", "Glucose alarms"), tr("Włączanie alarmów i powiadomień zegarka", "Enable alarms and watch notifications"), Icons.Filled.NotificationsActive),
        MenuDestination(Screen.NotificationHaptics, tr("Wibracje zegarka", "Watch vibrations"), tr("Niski, wysoki cukier i powrót do zakresu", "Low, high glucose and return to range"), Icons.Filled.Vibration),
        MenuDestination(Screen.NotificationSounds, tr("Dźwięki telefonu", "Phone sounds"), tr("Wybór dźwięków, głośność i opóźnienie", "Sounds, volume and delay"), Icons.AutoMirrored.Filled.VolumeUp),
        MenuDestination(Screen.NotificationNight, tr("Tryb nocny", "Night mode"), tr("Godziny nocy i nocne alarmy telefonu", "Night hours and phone alarms"), Icons.Filled.Bedtime),
        MenuDestination(Screen.NotificationConnection, tr("Brak danych i rozłączenia", "Missing data and disconnections"), tr("Alarmy utraty połączenia i starych pomiarów", "Lost connection and stale reading alarms"), Icons.Filled.WifiOff),
        MenuDestination(Screen.NotificationWrist, tr("Nadgarstek i wyciszanie", "Wrist and muting"), tr("Zdjęcie zegarka i wyciszanie telefonu", "Watch removal and phone muting"), Icons.Filled.Watch)
    ), onNavigate)
}

@Composable
fun NotificationTestsMenuScreen(onBack: () -> Unit, onNavigate: (Screen) -> Unit) {
    SettingsMenu(tr("Testy i symulacje", "Tests and simulations"), listOf(
        MenuDestination(Screen.SettingsTestSounds, tr("Testy dźwięków telefonu", "Phone sound tests"), tr("Niski, wysoki cukier i rozłączenie: dzień oraz noc", "Low, high glucose and disconnection: day and night"), Icons.AutoMirrored.Filled.VolumeUp),
        MenuDestination(Screen.SettingsTestWatch, tr("Testy zegarka", "Watch tests"), tr("Podgląd wzorów wibracji i stan nadgarstka", "Vibration pattern previews and wrist status"), Icons.Filled.Vibration),
        MenuDestination(Screen.SettingsTestSimulation, tr("Symulacja pomiarów i Demo", "Reading simulation and Demo"), tr("Własna wartość glukozy lub dane demonstracyjne", "Custom glucose value or demonstration data"), Icons.Filled.Science)
    ), onNavigate, onBack)
}

private data class MenuDestination(val screen: Screen, val title: String, val description: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMenu(title: String, destinations: List<MenuDestination>, onNavigate: (Screen) -> Unit, onBack: (() -> Unit)? = null, footer: String? = null) {
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
            items(destinations, key = { it.screen.route }) { destination ->
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
