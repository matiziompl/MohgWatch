package com.mohgwatch.phone.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.service.GlucoseSyncService
import com.mohgwatch.phone.ui.theme.GlucoseHigh
import com.mohgwatch.phone.ui.theme.GlucoseLow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNotificationsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Powiadomienia") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        settings?.let { currentSettings ->
            var alertLowThreshold by remember(currentSettings) { mutableFloatStateOf(currentSettings.alertLowThreshold) }
            var alertHighThreshold by remember(currentSettings) { mutableFloatStateOf(currentSettings.alertHighThreshold) }

            val disconnectSoundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    @Suppress("DEPRECATION")
                    val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                    scope.launch { settingsStore.saveSettings(currentSettings.copy(disconnectSoundUri = uri?.toString())) }
                }
            }
            
            val lowGlucoseSoundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    @Suppress("DEPRECATION")
                    val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                    scope.launch { settingsStore.saveSettings(currentSettings.copy(lowGlucoseSoundUri = uri?.toString())) }
                }
            }

            val highGlucoseSoundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    @Suppress("DEPRECATION")
                    val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                    scope.launch { settingsStore.saveSettings(currentSettings.copy(highGlucoseSoundUri = uri?.toString())) }
                }
            }

            fun launchRingtonePicker(launcher: androidx.activity.result.ActivityResultLauncher<Intent>, existingUri: String?) {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    existingUri?.let {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                    }
                }
                launcher.launch(intent)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Alerty o glukozie",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Poza docelowym zakresem", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Otrzymuj alerty, gdy glukoza przekroczy wybrane progi alarmowe.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = currentSettings.notifyOutOfRange,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsStore.saveSettings(currentSettings.copy(notifyOutOfRange = checked)) }
                                }
                            )
                        }

                        if (currentSettings.notifyOutOfRange) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Próg Niski
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ArrowDownward, null, tint = GlucoseLow)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Alarm Niskiego Cukru", modifier = Modifier.weight(1f))
                                Text("${alertLowThreshold.roundToInt()} mg/dL", color = GlucoseLow, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = alertLowThreshold,
                                onValueChange = { alertLowThreshold = it.roundToInt().toFloat() },
                                onValueChangeFinished = {
                                    scope.launch { settingsStore.saveSettings(currentSettings.copy(alertLowThreshold = alertLowThreshold)) }
                                },
                                valueRange = 50f..100f,
                                steps = 49,
                                colors = SliderDefaults.colors(thumbColor = GlucoseLow, activeTrackColor = GlucoseLow)
                            )
                            OutlinedButton(onClick = { launchRingtonePicker(lowGlucoseSoundLauncher, currentSettings.lowGlucoseSoundUri) }) {
                                Icon(Icons.Filled.MusicNote, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Wybierz dźwięk niskiego cukru")
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Próg Wysoki
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ArrowUpward, null, tint = GlucoseHigh)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Alarm Wysokiego Cukru", modifier = Modifier.weight(1f))
                                Text("${alertHighThreshold.roundToInt()} mg/dL", color = GlucoseHigh, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = alertHighThreshold,
                                onValueChange = { alertHighThreshold = it.roundToInt().toFloat() },
                                onValueChangeFinished = {
                                    scope.launch { settingsStore.saveSettings(currentSettings.copy(alertHighThreshold = alertHighThreshold)) }
                                },
                                valueRange = 120f..300f,
                                steps = 179,
                                colors = SliderDefaults.colors(thumbColor = GlucoseHigh, activeTrackColor = GlucoseHigh)
                            )
                            OutlinedButton(onClick = { launchRingtonePicker(highGlucoseSoundLauncher, currentSettings.highGlucoseSoundUri) }) {
                                Icon(Icons.Filled.MusicNote, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Wybierz dźwięk wysokiego cukru")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Problemy z połączeniem",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Rozłączenie / Błędy", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Alerty o braku nowych odczytów (np. problem z siecią, wygaśnięcie sesji).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = currentSettings.notifyDisconnect,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsStore.saveSettings(currentSettings.copy(notifyDisconnect = checked)) }
                                }
                            )
                        }
                        if (currentSettings.notifyDisconnect) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = { launchRingtonePicker(disconnectSoundLauncher, currentSettings.disconnectSoundUri) }) {
                                Icon(Icons.Filled.MusicNote, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Wybierz dźwięk rozłączenia")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "Głośność powiadomień",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        var volume by remember(currentSettings) { mutableFloatStateOf(currentSettings.notificationVolume) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Głośność Dzienna", modifier = Modifier.weight(1f))
                            Text("${(volume * 100).roundToInt()}%", fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = volume,
                            onValueChange = { volume = it },
                            onValueChangeFinished = {
                                scope.launch { settingsStore.saveSettings(currentSettings.copy(notificationVolume = volume)) }
                            },
                            valueRange = 0f..1f,
                            steps = 99
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tryb nocny głośności", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = currentSettings.nightModeEnabled,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsStore.saveSettings(currentSettings.copy(nightModeEnabled = checked)) }
                                }
                            )
                        }
                        
                        if (currentSettings.nightModeEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedTextField(
                                    value = currentSettings.nightStartTime,
                                    onValueChange = { scope.launch { settingsStore.saveSettings(currentSettings.copy(nightStartTime = it)) } },
                                    label = { Text("Od (HH:mm)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = currentSettings.nightEndTime,
                                    onValueChange = { scope.launch { settingsStore.saveSettings(currentSettings.copy(nightEndTime = it)) } },
                                    label = { Text("Do (HH:mm)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            var nightVolume by remember(currentSettings) { mutableFloatStateOf(currentSettings.nightNotificationVolume) }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Głośność Nocna", modifier = Modifier.weight(1f))
                                Text("${(nightVolume * 100).roundToInt()}%", fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = nightVolume,
                                onValueChange = { nightVolume = it },
                                onValueChangeFinished = {
                                    scope.launch { settingsStore.saveSettings(currentSettings.copy(nightNotificationVolume = nightVolume)) }
                                },
                                valueRange = 0f..1f,
                                steps = 99
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "Ustawienia systemowe",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Włącz ignorowanie trybu 'Nie Przeszkadzać' (DND) w systemowych ustawieniach kanałów powiadomień.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Skonfiguruj Alerty (DND)")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                GlucoseSyncService.testAlertLow(context)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Testuj Alert Niskiego Cukru")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                GlucoseSyncService.testAlertHigh(context)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Testuj Alert Wysokiego Cukru")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                GlucoseSyncService.testAlertDisconnect(context)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Testuj Alert Rozłączenia")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
