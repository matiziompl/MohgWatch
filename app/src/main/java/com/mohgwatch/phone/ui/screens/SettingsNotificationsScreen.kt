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
    ) {
        Text(
            "Powiadomienia",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Powiadomienie na zegarku (> 5 min)", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Wibruj na zegarku przy braku nowych danych przez ponad 5 minut.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = currentSettings.notifyStaleDataWatch,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsStore.saveSettings(currentSettings.copy(notifyStaleDataWatch = checked)) }
                                }
                            )
                        }
                        if (currentSettings.notifyStaleDataWatch) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = { GlucoseSyncService.testAlertStaleDataWatch(context) }) {
                                Text("Testuj powiadomienie na zegarku")
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
                        Text("Testowanie alertów (Dzień / Noc)", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(16.dp))
                        
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
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Opóźnienie głośnego alarmu", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Czas po którym aplikacja zagra głośny alarm, jeśli nie pominiesz cichego powiadomienia.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        var delaySeconds by remember(currentSettings) { mutableIntStateOf(currentSettings.notificationSoundDelaySeconds) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Opóźnienie:", modifier = Modifier.weight(1f))
                            Text("${delaySeconds} s", fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = delaySeconds.toFloat(),
                            onValueChange = { delaySeconds = it.roundToInt() },
                            onValueChangeFinished = {
                                scope.launch { settingsStore.saveSettings(currentSettings.copy(notificationSoundDelaySeconds = delaySeconds)) }
                            },
                            valueRange = 0f..30f,
                            steps = 29
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Zarządzaj powiadomieniami systemu", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Przejdź do ustawień systemu Android, aby zarządzać uprawnieniami lub ukryć niechciane kanały (np. ciche powiadomienie o synchronizacji).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(fallbackIntent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Otwórz ustawienia powiadomień")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
