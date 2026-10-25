package com.mohgwatch.phone.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohgwatch.core.model.TrendDirection
import com.mohgwatch.core.model.TrendHaptics
import com.mohgwatch.core.model.TrendVibrationPattern
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.ui.components.SettingsPage
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class NotificationSettingsSection(val title: String) {
    GLUCOSE("Alarmy glukozy"),
    HAPTICS("Wibracje zegarka"),
    SOUNDS("Dźwięki telefonu"),
    NIGHT("Tryb nocny"),
    CONNECTION("Brak danych i rozłączenia"),
    WRIST("Nadgarstek i wyciszanie")
}

@Composable
fun SettingsNotificationsScreen(
    section: NotificationSettingsSection,
    onBack: () -> Unit = {},
    onChangeThresholds: () -> Unit = {},
    onTestHaptics: () -> Unit = {},
    onTestSounds: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val dataLayerSender = remember { com.mohgwatch.phone.service.DataLayerSender(context) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = null)

    SettingsPage(title = section.title, onBack = onBack) {
        val currentSettings = settings
        if (currentSettings == null) {
            CircularProgressIndicator()
        } else {
            val latestSettings by rememberUpdatedState(currentSettings)
            fun save(updated: UserSettings, syncWithWatch: Boolean = false) {
                scope.launch {
                    settingsStore.saveSettings(updated)
                    if (syncWithWatch) dataLayerSender.sendSettings(updated)
                }
            }

            val disconnectSoundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    @Suppress("DEPRECATION")
                    val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                    save(latestSettings.copy(disconnectSoundUri = uri?.toString()))
                }
            }
            val lowGlucoseSoundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    @Suppress("DEPRECATION")
                    val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                    save(latestSettings.copy(lowGlucoseSoundUri = uri?.toString()))
                }
            }
            val highGlucoseSoundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    @Suppress("DEPRECATION")
                    val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                    save(latestSettings.copy(highGlucoseSoundUri = uri?.toString()))
                }
            }

            when (section) {
                NotificationSettingsSection.GLUCOSE -> {
                    NotificationToggle(
                        title = "Alarmy poza zakresem",
                        description = "Włącz alarmy telefonu i zegarka po przekroczeniu progów z Ustawienia → Glukoza i progi.",
                        checked = currentSettings.notifyOutOfRange,
                        onCheckedChange = { save(currentSettings.copy(notifyOutOfRange = it), true) }
                    )
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    NotificationToggle(
                        title = "Zwykłe powiadomienia na zegarku",
                        description = "Wyświetlaj powiadomienie z odczytem glukozy na zegarku. Wibracje można ustawić osobno.",
                        checked = currentSettings.watchNotifyOutOfRange,
                        onCheckedChange = { save(currentSettings.copy(watchNotifyOutOfRange = it), true) }
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onChangeThresholds, modifier = Modifier.fillMaxWidth()) {
                        Text("Zmień progi")
                    }
                }

                NotificationSettingsSection.HAPTICS -> {
                    Text(
                        "Wibracje uruchamia wejście poniżej lub powyżej progów alarmowych z Ustawienia → Glukoza i progi. Wartości równe progom są w zakresie. Sama zmiana strzałki nie wibruje. Mocne i długie wzory używają maksymalnej siły silnika.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!currentSettings.notifyOutOfRange) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Niski i wysoki cukier nie wibrują, gdy „Alarmy poza zakresem” są wyłączone. Ich indywidualne ustawienia pozostają zapisane.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TrendDirection.entries.forEach { direction ->
                        Spacer(Modifier.height(16.dp))
                        val enabled = when (direction) {
                            TrendDirection.RISING -> currentSettings.alertHighHapticEnabled
                            TrendDirection.FALLING -> currentSettings.alertLowHapticEnabled
                            TrendDirection.STABLE -> currentSettings.trendStableEnabled
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(direction.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Switch(checked = enabled, onCheckedChange = { checked ->
                                val updated = when (direction) {
                                    TrendDirection.RISING -> currentSettings.copy(alertHighHapticEnabled = checked)
                                    TrendDirection.FALLING -> currentSettings.copy(alertLowHapticEnabled = checked)
                                    TrendDirection.STABLE -> currentSettings.copy(trendStableEnabled = checked)
                                }
                                save(updated, true)
                            })
                        }
                        TrendVibrationPattern.entries.forEach { pattern ->
                            val selected = TrendHaptics.patternFor(currentSettings, direction) == pattern
                            Row(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                                    selected = selected,
                                    enabled = enabled || direction != TrendDirection.STABLE,
                                    role = Role.RadioButton,
                                    onClick = {
                                        val updated = when (direction) {
                                            TrendDirection.RISING -> currentSettings.copy(trendRisingPattern = pattern)
                                            TrendDirection.FALLING -> currentSettings.copy(trendFallingPattern = pattern)
                                            TrendDirection.STABLE -> currentSettings.copy(trendStablePattern = pattern)
                                        }
                                        save(updated, true)
                                    }
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    enabled = enabled || direction != TrendDirection.STABLE,
                                    onClick = null
                                )
                                Text(pattern.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Dotychczasowy wzór „Spada” oznacza niski cukier, „Rośnie” wysoki, a „Stabilnie” powrót do zakresu. Powrót do zakresu jest opcjonalny i nie przerywa trwającego alarmu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onTestHaptics, modifier = Modifier.fillMaxWidth()) {
                        Text("Testuj wibracje")
                    }
                }

                NotificationSettingsSection.SOUNDS -> {
                    if (currentSettings.notifyOutOfRange) {
                        SoundPickerButton("Wybierz dźwięk dla niskiego cukru") {
                            launchRingtonePicker(lowGlucoseSoundLauncher, currentSettings.lowGlucoseSoundUri)
                        }
                        Spacer(Modifier.height(8.dp))
                        SoundPickerButton("Wybierz dźwięk dla wysokiego cukru") {
                            launchRingtonePicker(highGlucoseSoundLauncher, currentSettings.highGlucoseSoundUri)
                        }
                    } else {
                        Text("Wybór dźwięków glukozy jest dostępny po włączeniu „Alarmy poza zakresem”.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (currentSettings.notifyDisconnect) {
                        Spacer(Modifier.height(8.dp))
                        SoundPickerButton("Wybierz dźwięk rozłączenia") {
                            launchRingtonePicker(disconnectSoundLauncher, currentSettings.disconnectSoundUri)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    var volume by remember(currentSettings.notificationVolume) {
                        mutableFloatStateOf(currentSettings.notificationVolume)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Głośność dzienna", modifier = Modifier.weight(1f))
                        Text("${(volume * 100).roundToInt()}%", fontWeight = FontWeight.Bold)
                    }
                    Slider(value = volume, onValueChange = { volume = it },
                        onValueChangeFinished = { save(currentSettings.copy(notificationVolume = volume)) },
                        valueRange = 0f..1f, steps = 99)
                    Spacer(Modifier.height(24.dp))
                    Text("Opóźnienie głośnego alarmu", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Czas po którym aplikacja zagra głośny alarm, jeśli nie pominiesz cichego powiadomienia.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    var delaySeconds by remember(currentSettings.notificationSoundDelaySeconds) {
                        mutableIntStateOf(currentSettings.notificationSoundDelaySeconds)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Opóźnienie:", modifier = Modifier.weight(1f))
                        Text("$delaySeconds s", fontWeight = FontWeight.Bold)
                    }
                    Slider(value = delaySeconds.toFloat(), onValueChange = { delaySeconds = it.roundToInt() },
                        onValueChangeFinished = { save(currentSettings.copy(notificationSoundDelaySeconds = delaySeconds)) },
                        valueRange = 0f..30f, steps = 29)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onTestSounds, modifier = Modifier.fillMaxWidth()) {
                        Text("Testuj dźwięki")
                    }
                }

                NotificationSettingsSection.NIGHT -> {
                    NotificationToggle("Tryb nocny głośności", checked = currentSettings.nightModeEnabled,
                        onCheckedChange = { save(currentSettings.copy(nightModeEnabled = it)) })
                    if (currentSettings.nightModeEnabled) {
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(value = currentSettings.nightStartTime,
                                onValueChange = { save(currentSettings.copy(nightStartTime = it)) },
                                label = { Text("Od (HH:mm)") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = currentSettings.nightEndTime,
                                onValueChange = { save(currentSettings.copy(nightEndTime = it)) },
                                label = { Text("Do (HH:mm)") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                        Spacer(Modifier.height(24.dp))
                        var nightVolume by remember(currentSettings.nightNotificationVolume) {
                            mutableFloatStateOf(currentSettings.nightNotificationVolume)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Głośność nocna", modifier = Modifier.weight(1f))
                            Text("${(nightVolume * 100).roundToInt()}%", fontWeight = FontWeight.Bold)
                        }
                        Slider(value = nightVolume, onValueChange = { nightVolume = it },
                            onValueChangeFinished = { save(currentSettings.copy(nightNotificationVolume = nightVolume)) },
                            valueRange = 0f..1f, steps = 99)
                        Spacer(Modifier.height(16.dp))
                        NotificationToggle(
                            title = "Zwykłe powiadomienia telefonu w nocy",
                            description = "Odtwarzaj alerty telefonu w czasie nocy nawet wtedy, gdy zegarek jest założony na ręku (pomija wyciszanie przez zegarek).",
                            checked = currentSettings.nightPhoneNotificationsEnabled,
                            onCheckedChange = { save(currentSettings.copy(nightPhoneNotificationsEnabled = it)) }
                        )
                    }
                }

                NotificationSettingsSection.CONNECTION -> {
                    NotificationToggle(
                        title = "Rozłączenie / Błędy sieci",
                        description = "Alerty o braku nowych odczytów (problem z siecią, wygaśnięcie sesji LibreLinkUp).",
                        checked = currentSettings.notifyDisconnect,
                        onCheckedChange = { save(currentSettings.copy(notifyDisconnect = it)) }
                    )
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    var staleMinutes by remember(currentSettings.staleDataMinutes) {
                        mutableFloatStateOf(currentSettings.staleDataMinutes.coerceIn(1, 60).toFloat())
                    }
                    Text("Dane przeterminowane po: ${staleMinutes.roundToInt()} min", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = staleMinutes,
                        onValueChange = { staleMinutes = it },
                        onValueChangeFinished = {
                            save(latestSettings.copy(staleDataMinutes = staleMinutes.roundToInt().coerceIn(1, 60)), true)
                        },
                        valueRange = 1f..60f,
                        steps = 58
                    )
                    Text(
                        "Po tym czasie komplikacje zegarka pokazują - -. Ten sam limit obowiązuje dla aktualności pomiarów i alarmu braku danych.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    NotificationToggle(
                        title = "Brak nowych danych na zegarku",
                        description = "Powiadom na zegarku po upływie wybranego limitu aktualności. Zegarek sprawdza to także bez telefonu.",
                        checked = currentSettings.notifyStaleDataWatch,
                        onCheckedChange = { save(currentSettings.copy(notifyStaleDataWatch = it), true) }
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onOpenDiagnostics, modifier = Modifier.fillMaxWidth()) {
                        Text("Otwórz diagnostykę")
                    }
                }

                NotificationSettingsSection.WRIST -> {
                    NotificationToggle(
                        title = "Obecność zegarka w powiadomieniu",
                        description = "Pokazuj informację 'Na ręku' lub 'Zdjęty' w stałym powiadomieniu telefonu.",
                        checked = currentSettings.showWristStatusInNotification,
                        onCheckedChange = { save(currentSettings.copy(showWristStatusInNotification = it)) }
                    )
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    val watchWristEnabled = currentSettings.watchWristNotifyMode != "none"
                    NotificationToggle(
                        title = "Powiadomienia na zegarku o zdjęciu z ręki",
                        description = "Wysyłaj powiadomienie na zegarek informujące o zdjęciu zegarka z ręki.",
                        checked = watchWristEnabled,
                        onCheckedChange = { save(currentSettings.copy(watchWristNotifyMode = if (it) "both" else "none"), true) }
                    )
                    if (watchWristEnabled) {
                        Spacer(Modifier.height(8.dp))
                        // Wrap the two choices vertically so long labels fit at larger font sizes.
                        listOf("both" to "Założenie i zdjęcie", "off_only" to "Tylko zdjęcie").forEach { (mode, label) ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                                selected = currentSettings.watchWristNotifyMode == mode,
                                role = Role.RadioButton,
                                onClick = { save(currentSettings.copy(watchWristNotifyMode = mode), true) }
                            ), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = currentSettings.watchWristNotifyMode == mode, onClick = null)
                                Text(label)
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    NotificationToggle(
                        title = "Wycisz telefon przy obecności zegarka",
                        description = "Gdy zegarek jest połączony i założony na ręku, telefon nie odtwarza głośnego alarmu. W razie rozłączenia telefon zadzwoni normalnie.",
                        checked = currentSettings.mutePhoneWhenWatchConnected,
                        onCheckedChange = { save(currentSettings.copy(mutePhoneWhenWatchConnected = it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationToggle(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SoundPickerButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.MusicNote, null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

private fun launchRingtonePicker(launcher: ActivityResultLauncher<Intent>, existingUri: String?) {
    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        existingUri?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it)) }
    }
    launcher.launch(intent)
}
