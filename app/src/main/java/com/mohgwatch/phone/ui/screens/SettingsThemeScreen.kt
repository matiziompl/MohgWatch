package com.mohgwatch.phone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohgwatch.core.model.AppTheme
import com.mohgwatch.core.model.LogBgColor
import com.mohgwatch.core.model.ThemeMode
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.core.model.WidgetBgColor
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.service.DataLayerSender
import com.mohgwatch.phone.widget.WidgetUpdateHelper
import kotlinx.coroutines.launch

data class ColorOption(
    val id: String,
    val label: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsThemeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val dataLayerSender = remember { DataLayerSender(context) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = UserSettings())
    val isDark = isSystemInDarkTheme() || settings.themeMode == ThemeMode.DARK

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Wygląd") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Wygląd aplikacji
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Wygląd aplikacji", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Tryb", style = MaterialTheme.typography.titleSmall)
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
                    Text("Kolor wiodący", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    val appThemeOptions = AppTheme.entries.filter { it != AppTheme.MATCH_GLUCOSE }.map { theme ->
                        ColorOption(
                            id = theme.name,
                            label = theme.label,
                            color = resolveAppThemeColor(theme, settings, isDark)
                        )
                    }
                    ColorSelector(
                        options = appThemeOptions,
                        selectedId = settings.appTheme.name,
                        mode = settings.colorPickerStyle,
                        onSelect = { selectedName ->
                            val theme = try { AppTheme.valueOf(selectedName) } catch (e: Exception) { AppTheme.DEFAULT }
                            scope.launch {
                                val newSettings = settings.copy(appTheme = theme)
                                settingsStore.saveSettings(newSettings)
                                dataLayerSender.sendSettings(newSettings)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Kolor tła aplikacji", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    val logBgOptions = LogBgColor.entries.map { bg ->
                        ColorOption(
                            id = bg.name,
                            label = bg.label,
                            color = resolveLogBgColor(bg)
                        )
                    }
                    ColorSelector(
                        options = logBgOptions,
                        selectedId = settings.logBackgroundColor.name,
                        mode = settings.colorPickerStyle,
                        onSelect = { selectedName ->
                            val bg = try { LogBgColor.valueOf(selectedName) } catch (e: Exception) { LogBgColor.WHITE }
                            scope.launch {
                                val newSettings = settings.copy(logBackgroundColor = bg)
                                settingsStore.saveSettings(newSettings)
                                dataLayerSender.sendSettings(newSettings)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Wygląd widżetów
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Wygląd widżetów", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Kolor wiodący widżetów", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    val widgetPrimaryOptions = AppTheme.entries.map { theme ->
                        ColorOption(
                            id = theme.name,
                            label = theme.label,
                            color = resolveAppThemeColor(theme, settings, isDark)
                        )
                    }
                    ColorSelector(
                        options = widgetPrimaryOptions,
                        selectedId = settings.widgetPrimaryTheme.name,
                        mode = settings.colorPickerStyle,
                        onSelect = { selectedName ->
                            val theme = try { AppTheme.valueOf(selectedName) } catch (e: Exception) { AppTheme.DEFAULT }
                            scope.launch {
                                val newSettings = settings.copy(widgetPrimaryTheme = theme)
                                settingsStore.saveSettings(newSettings)
                                WidgetUpdateHelper.updateAllWidgets(context)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Kolor tła widżetów", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    val widgetBgOptions = WidgetBgColor.entries.map { bg ->
                        ColorOption(
                            id = bg.name,
                            label = bg.label,
                            color = resolveWidgetBgColor(bg, isDark)
                        )
                    }
                    ColorSelector(
                        options = widgetBgOptions,
                        selectedId = settings.widgetBgColor.name,
                        mode = settings.colorPickerStyle,
                        onSelect = { selectedName ->
                            val bg = try { WidgetBgColor.valueOf(selectedName) } catch (e: Exception) { WidgetBgColor.SYSTEM }
                            scope.launch {
                                val newSettings = settings.copy(widgetBgColor = bg)
                                settingsStore.saveSettings(newSettings)
                                WidgetUpdateHelper.updateAllWidgets(context)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Kolor stężenia glukozy", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    val glucoseColorRawOptions = listOf(
                        "by_range" to "Zależny od poziomu (domyślny)",
                        "theme" to "Motyw systemowy",
                        "light" to "Jasny",
                        "dark" to "Ciemny",
                        "light_gray" to "Jasny szary",
                        "gray" to "Szary",
                        "dark_gray" to "Ciemny szary",
                        "primary" to "Kolor wiodący widżetów",
                        "blue" to "Niebieski",
                        "green" to "Zielony",
                        "amber" to "Bursztynowy",
                        "purple" to "Fioletowy",
                        "red" to "Czerwony"
                    )
                    val glucoseColorOptions = glucoseColorRawOptions.map { (key, label) ->
                        ColorOption(
                            id = key,
                            label = label,
                            color = resolveGlucoseColor(key, settings, isDark)
                        )
                    }
                    ColorSelector(
                        options = glucoseColorOptions,
                        selectedId = settings.widgetGlucoseColor,
                        mode = settings.colorPickerStyle,
                        onSelect = { selectedKey ->
                            scope.launch {
                                val newSettings = settings.copy(widgetGlucoseColor = selectedKey)
                                settingsStore.saveSettings(newSettings)
                                WidgetUpdateHelper.updateAllWidgets(context)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Opcja dobierania kolorów", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    val pickerStyles = listOf(
                        "list" to "Nazwy z podglądem",
                        "grid" to "Tabela"
                    )
                    var pickerStyleExpanded by remember { mutableStateOf(false) }
                    val currentPickerStyleLabel = pickerStyles.find { it.first == settings.colorPickerStyle }?.second ?: "Nazwy z podglądem"

                    ExposedDropdownMenuBox(
                        expanded = pickerStyleExpanded,
                        onExpandedChange = { pickerStyleExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currentPickerStyleLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pickerStyleExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = pickerStyleExpanded,
                            onDismissRequest = { pickerStyleExpanded = false }
                        ) {
                            pickerStyles.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        scope.launch {
                                            val newSettings = settings.copy(colorPickerStyle = key)
                                            settingsStore.saveSettings(newSettings)
                                        }
                                        pickerStyleExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Usuń nagłówek z zakładki status
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Usuń nagłówek z zakładki status", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Ukrywa tytuł 'Status Glukozy' i dopasowuje układ, aby wszystko mieściło się na ekranie bez przewijania",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.hideStatusHeader,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    val newSettings = settings.copy(hideStatusHeader = checked)
                                    settingsStore.saveSettings(newSettings)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Motyw zegarka
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
                        val watchThemeOptions = AppTheme.entries.filter { it != AppTheme.MATCH_GLUCOSE }.map { theme ->
                            ColorOption(
                                id = theme.name,
                                label = theme.label,
                                color = resolveAppThemeColor(theme, settings, isDark)
                            )
                        }
                        ColorSelector(
                            options = watchThemeOptions,
                            selectedId = settings.watchAppTheme.name,
                            mode = settings.colorPickerStyle,
                            onSelect = { selectedName ->
                                val theme = try { AppTheme.valueOf(selectedName) } catch (e: Exception) { AppTheme.DEFAULT }
                                scope.launch {
                                    val newSettings = settings.copy(watchAppTheme = theme)
                                    settingsStore.saveSettings(newSettings)
                                    dataLayerSender.sendSettings(newSettings)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColorPreviewSquare(
    color: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 20.dp,
    isSelected: Boolean = false
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color, RoundedCornerShape(6.dp))
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(size * 0.65f),
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSelector(
    options: List<ColorOption>,
    selectedId: String,
    mode: String,
    onSelect: (String) -> Unit
) {
    val selectedOption = options.find { it.id == selectedId } ?: options.firstOrNull()

    if (mode == "grid") {
        Column {
            if (selectedOption != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    ColorPreviewSquare(color = selectedOption.color, size = 18.dp)
                    Text(
                        selectedOption.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            options.chunked(5).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowOptions.forEach { opt ->
                        val isSelected = opt.id == selectedId
                        ColorPreviewSquare(
                            color = opt.color,
                            size = 34.dp,
                            isSelected = isSelected,
                            modifier = Modifier.clickable { onSelect(opt.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    } else {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedOption?.label ?: "",
                onValueChange = {},
                readOnly = true,
                leadingIcon = selectedOption?.let { { ColorPreviewSquare(color = it.color, size = 18.dp) } },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        leadingIcon = { ColorPreviewSquare(color = opt.color, size = 18.dp) },
                        text = { Text(opt.label) },
                        onClick = {
                            onSelect(opt.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

fun resolveAppThemeColor(theme: AppTheme, settings: UserSettings, isDark: Boolean): Color = when (theme) {
    AppTheme.DEFAULT, AppTheme.TEAL -> if (isDark) Color(0xFF2DD4BF) else Color(0xFF0F766E)
    AppTheme.BLUE -> if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    AppTheme.RED -> if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
    AppTheme.GREEN -> if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
    AppTheme.PURPLE -> if (isDark) Color(0xFFC084FC) else Color(0xFF9333EA)
    AppTheme.ORANGE -> if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C)
    AppTheme.PINK -> if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777)
    AppTheme.AMBER -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    AppTheme.CYAN -> if (isDark) Color(0xFF22D3EE) else Color(0xFF0891B2)
    AppTheme.MONOCHROME -> if (isDark) Color(0xFFA3A3A3) else Color(0xFF404040)
    AppTheme.GRAY -> if (isDark) Color(0xFF9CA3AF) else Color(0xFF4B5563)
    AppTheme.LIGHT_GRAY -> if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    AppTheme.BLACK -> if (isDark) Color(0xFFE5E7EB) else Color(0xFF000000)
    AppTheme.MATCH_BACKGROUND -> resolveLogBgColor(settings.logBackgroundColor)
    AppTheme.MATCH_GLUCOSE -> resolveGlucoseColor(settings.widgetGlucoseColor, settings, isDark)
}

fun resolveLogBgColor(bg: LogBgColor): Color = when (bg) {
    LogBgColor.DEFAULT, LogBgColor.WHITE -> Color(0xFFFFFFFF)
    LogBgColor.GRAY -> Color(0xFFE5E7EB)
    LogBgColor.ORANGE -> Color(0xFFFFEDD5)
    LogBgColor.LIGHT_BLUE -> Color(0xFFE0F2FE)
    LogBgColor.MONOCHROME -> Color(0xFFF3F4F6)
    LogBgColor.PURPLE -> Color(0xFFF3E8FF)
    LogBgColor.DARK_GREEN -> Color(0xFFDCFCE7)
    LogBgColor.CRIMSON -> Color(0xFFFFE4E6)
    LogBgColor.VAPOR -> Color(0xFFFAE8FF)
    LogBgColor.TIDE -> Color(0xFFCCFBF1)
    LogBgColor.PULSE -> Color(0xFFEDE9FE)
    LogBgColor.ACID -> Color(0xFFFEF9C3)
}

fun resolveWidgetBgColor(bg: WidgetBgColor, isDark: Boolean): Color = when (bg) {
    WidgetBgColor.SYSTEM -> if (isDark) Color(0xFF121318) else Color(0xFFF9F8FE)
    WidgetBgColor.LIGHT -> Color(0xFFF9F8FE)
    WidgetBgColor.DARK -> Color(0xFF121318)
    WidgetBgColor.LIGHT_GRAY -> if (isDark) Color(0xFF475569) else Color(0xFFF1F5F9)
    WidgetBgColor.GRAY -> if (isDark) Color(0xFF242424) else Color(0xFFE5E7EB)
    WidgetBgColor.DARK_GRAY -> if (isDark) Color(0xFF1E293B) else Color(0xFF334155)
    WidgetBgColor.ORANGE -> if (isDark) Color(0xFF431407) else Color(0xFFFFEDD5)
    WidgetBgColor.LIGHT_BLUE -> if (isDark) Color(0xFF082F49) else Color(0xFFE0F2FE)
    WidgetBgColor.MONOCHROME -> if (isDark) Color(0xFF111827) else Color(0xFFF3F4F6)
    WidgetBgColor.PURPLE -> if (isDark) Color(0xFF3B0764) else Color(0xFFF3E8FF)
    WidgetBgColor.DARK_GREEN -> if (isDark) Color(0xFF052E16) else Color(0xFFDCFCE7)
    WidgetBgColor.CRIMSON -> if (isDark) Color(0xFF4C0519) else Color(0xFFFFE4E6)
    WidgetBgColor.VAPOR -> if (isDark) Color(0xFF350035) else Color(0xFFFAE8FF)
    WidgetBgColor.TIDE -> if (isDark) Color(0xFF042F2E) else Color(0xFFCCFBF1)
    WidgetBgColor.PULSE -> if (isDark) Color(0xFF2E1065) else Color(0xFFEDE9FE)
    WidgetBgColor.ACID -> if (isDark) Color(0xFF1F2405) else Color(0xFFFEF9C3)
}

fun resolveGlucoseColor(key: String, settings: UserSettings, isDark: Boolean): Color = when (key) {
    "theme" -> if (isDark) Color(0xFFF9FAFB) else Color(0xFF111827)
    "light" -> Color(0xFFFFFFFF)
    "dark" -> Color(0xFF111827)
    "light_gray" -> if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)
    "gray" -> Color(0xFF9CA3AF)
    "dark_gray" -> if (isDark) Color(0xFF64748B) else Color(0xFF334155)
    "primary" -> resolveAppThemeColor(settings.widgetPrimaryTheme, settings, isDark)
    "blue" -> if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    "green" -> if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
    "amber" -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    "purple" -> if (isDark) Color(0xFFC084FC) else Color(0xFF9333EA)
    "red" -> if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
    else -> Color(0xFF10B981) // by_range default (in range)
}
