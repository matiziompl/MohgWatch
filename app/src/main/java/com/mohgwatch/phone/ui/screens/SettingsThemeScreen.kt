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
import androidx.compose.ui.unit.dp
import com.mohgwatch.core.model.AppTheme
import com.mohgwatch.core.model.ThemeMode
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.service.DataLayerSender
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsThemeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val dataLayerSender = remember { DataLayerSender(context) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = UserSettings())

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Motyw") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
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
                    Text("Tryb", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { 
                                    scope.launch {
                                        val newSettings = settings.copy(themeMode = mode)
                                        settingsStore.saveSettings(newSettings)
                                        dataLayerSender.sendSettings(newSettings)
                                    }
                                },
                                label = { Text(mode.label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Kolor wiodący", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var themeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = themeExpanded,
                        onExpandedChange = { themeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = settings.appTheme.label,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false }
                        ) {
                            AppTheme.entries.forEach { theme ->
                                DropdownMenuItem(
                                    text = { Text(theme.label) },
                                    onClick = {
                                        scope.launch {
                                            val newSettings = settings.copy(appTheme = theme)
                                            settingsStore.saveSettings(newSettings)
                                            dataLayerSender.sendSettings(newSettings)
                                        }
                                        themeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Kolor tła aplikacji", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var logBgExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = logBgExpanded,
                        onExpandedChange = { logBgExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = settings.logBackgroundColor.label,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = logBgExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = logBgExpanded,
                            onDismissRequest = { logBgExpanded = false }
                        ) {
                            com.mohgwatch.core.model.LogBgColor.entries.forEach { logBg ->
                                DropdownMenuItem(
                                    text = { Text(logBg.label) },
                                    onClick = {
                                        scope.launch {
                                            val newSettings = settings.copy(logBackgroundColor = logBg)
                                            settingsStore.saveSettings(newSettings)
                                            dataLayerSender.sendSettings(newSettings)
                                        }
                                        logBgExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Motyw zegarka", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Synchronizuj z zegarkiem",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.syncThemeWithWatch,
                            onCheckedChange = {
                                scope.launch {
                                    val newSettings = settings.copy(syncThemeWithWatch = it)
                                    settingsStore.saveSettings(newSettings)
                                    dataLayerSender.sendSettings(newSettings)
                                }
                            }
                        )
                    }

                    if (!settings.syncThemeWithWatch) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Kolor wiodący zegarka", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        var watchThemeExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = watchThemeExpanded,
                            onExpandedChange = { watchThemeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = settings.watchAppTheme.label,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = watchThemeExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = watchThemeExpanded,
                                onDismissRequest = { watchThemeExpanded = false }
                            ) {
                                AppTheme.entries.forEach { theme ->
                                    DropdownMenuItem(
                                        text = { Text(theme.label) },
                                        onClick = {
                                            scope.launch {
                                                val newSettings = settings.copy(watchAppTheme = theme)
                                                settingsStore.saveSettings(newSettings)
                                                dataLayerSender.sendSettings(newSettings)
                                            }
                                            watchThemeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
