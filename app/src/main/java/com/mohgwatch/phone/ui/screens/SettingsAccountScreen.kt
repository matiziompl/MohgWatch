package com.mohgwatch.phone.ui.screens

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.core.content.FileProvider
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.phone.data.CredentialStore
import com.mohgwatch.phone.data.SettingsExport
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.service.GlucoseSyncService
import com.mohgwatch.phone.service.DataLayerSender
import com.mohgwatch.phone.ui.theme.ErrorRed
import com.mohgwatch.phone.widget.WidgetUpdateHelper
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAccountScreen(onBack: () -> Unit, onNavigateToLogin: () -> Unit) {
    val context = LocalContext.current
    val credentialStore = remember { CredentialStore(context) }
    val credentials by credentialStore.credentialsFlow.collectAsState(initial = null)
    val settingsStore = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()
    var settingsBusy by remember { mutableStateOf(false) }
    var settingsStatus by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<UserSettings?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            settingsBusy = false
            settingsStatus = null
        } else {
            settingsBusy = true
            scope.launch {
                settingsStatus = "Sprawdzam plik ustawień…"
                try {
                    pendingImport = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use(SettingsExport::read)
                            ?: throw IllegalArgumentException("Cannot open settings file")
                    }
                    settingsStatus = null
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    settingsStatus = "Nie udało się odczytać ustawień. Wybierz poprawny plik JSON wyeksportowany z MohgWatch. Bieżące ustawienia zachowano."
                    settingsBusy = false
                }
            }
        }
    }

    pendingImport?.let { imported ->
        AlertDialog(
            onDismissRequest = {
                pendingImport = null
                settingsBusy = false
            },
            title = { Text("Zastąpić ustawienia?") },
            text = { Text("Import zastąpi bieżące ustawienia, w tym progi glukozy, alarmy i synchronizację. Dane logowania pozostaną bez zmian.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        var saved = false
                        settingsStatus = "Importuję ustawienia…"
                        try {
                            withContext(Dispatchers.IO) { settingsStore.saveSettings(imported) }
                            saved = true
                            if (!imported.isSyncEnabled) {
                                GlucoseSyncService.stop(context)
                            } else if (withContext(Dispatchers.IO) { credentialStore.hasCredentials() }) {
                                GlucoseSyncService.start(context)
                            }
                            withContext(Dispatchers.IO) {
                                DataLayerSender(context).sendSettings(imported)
                                WidgetUpdateHelper.updateAllWidgets(context)
                            }
                            settingsStatus = "Ustawienia zaimportowano na telefonie."
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            settingsStatus = if (saved) {
                                "Ustawienia zapisano na telefonie. Nie udało się odświeżyć usługi, zegarka lub widżetów. Otwórz Synchronizację i spróbuj ponownie."
                            } else {
                                "Nie udało się zapisać ustawień. Spróbuj ponownie."
                            }
                        } finally {
                            settingsBusy = false
                        }
                    }
                }) { Text("Importuj ustawienia") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingImport = null
                    settingsBusy = false
                }) { Text("Anuluj") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Profil") },
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
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (credentials != null) {
                        Text("Jesteś zalogowany", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Zalogowano jako: ${credentials?.email ?: "Nieznany"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                GlucoseSyncService.logout(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Wyloguj")
                        }
                    } else {
                        Text("Brak autoryzacji", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Zaloguj się, aby pobierać dane z LibreLinkUp.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToLogin,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Zaloguj")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Ustawienia aplikacji", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Eksport zapisuje ustawienia do pliku JSON, a import przywraca je z takiego pliku. Hasło i dane logowania nie są dołączane.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                enabled = !settingsBusy,
                onClick = {
                    settingsBusy = true
                    settingsStatus = "Przygotowuję ustawienia…"
                    scope.launch {
                        try {
                            val file = withContext(Dispatchers.IO) {
                                SettingsExport.write(File(context.cacheDir, "settings-export"),
                                    settingsStore.getSettings(), System.currentTimeMillis())
                            }
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.diagnostics.files", file)
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                clipData = ClipData.newRawUri("Ustawienia MohgWatch", uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(share, "Eksportuj ustawienia"))
                            settingsStatus = "Przygotowano ustawienia. Wybierz aplikację, aby zapisać lub udostępnić plik."
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            settingsStatus = "Nie udało się wyeksportować ustawień. Spróbuj ponownie."
                        } finally {
                            settingsBusy = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Eksportuj ustawienia")
            }
            OutlinedButton(
                enabled = !settingsBusy,
                onClick = {
                    settingsBusy = true
                    settingsStatus = null
                    try {
                        importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                    } catch (_: Exception) {
                        settingsBusy = false
                        settingsStatus = "Nie udało się otworzyć wyboru pliku. Spróbuj ponownie."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Importuj ustawienia")
            }
            settingsStatus?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
    }
}
