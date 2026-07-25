package com.mohgwatch.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.ui.navigation.Screen
import com.mohgwatch.phone.ui.screens.*
import com.mohgwatch.phone.ui.screens.studio.*
import com.mohgwatch.phone.ui.theme.MohgWatchTheme
import com.mohgwatch.phone.util.LocalAppLanguage
import com.mohgwatch.phone.util.tr
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MohgWatchAppContent()
        }
    }
}

@Composable
fun MohgWatchAppContent() {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = UserSettings())

    CompositionLocalProvider(LocalAppLanguage provides settings.language) {
        MohgWatchTheme(appTheme = settings.appTheme, themeMode = settings.themeMode) {
            MohgWatchNavigation()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MohgWatchNavigation() {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    val bottomNavItems = listOf(
        Triple(Screen.Status, Icons.Filled.MonitorHeart, tr("Status", "Status")),
        Triple(Screen.Chart, Icons.Filled.BarChart, tr("Wykres", "Chart")),
        Triple(Screen.Studio, Icons.Filled.Brush, tr("Studio", "Studio")),
        Triple(Screen.Settings, Icons.Filled.Settings, tr("Ustawienia", "Settings"))
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute != Screen.Login.route) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavItems.forEach { (screen, icon, label) ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route ||
                                    (screen == Screen.Studio && currentRoute?.startsWith("studio") == true) ||
                                    (screen == Screen.Settings && currentRoute?.startsWith("settings") == true),
                            onClick = {
                                if (currentRoute != screen.route) {
                                    if (screen == Screen.Settings && currentRoute?.startsWith("settings") == true) {
                                        navController.popBackStack(Screen.Settings.route, inclusive = false)
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(Screen.Status.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Status.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(onLoginSuccess = {
                    navController.popBackStack()
                })
            }
            composable(Screen.Status.route) {
                StatusScreen()
            }
            composable(Screen.Chart.route) {
                ChartScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToDiabetes = { navController.navigate(Screen.SettingsDiabetes.route) },
                    onNavigateToAccount = { navController.navigate(Screen.SettingsAccount.route) },
                    onNavigateToTheme = { navController.navigate(Screen.SettingsTheme.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.SettingsNotifications.route) },
                    onNavigateToGeneral = { navController.navigate(Screen.SettingsGeneral.route) }
                )
            }
            composable(Screen.SettingsDiabetes.route) {
                SettingsDiabetesScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsAccount.route) {
                SettingsAccountScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }
            composable(Screen.SettingsTheme.route) {
                SettingsThemeScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsNotifications.route) {
                SettingsNotificationsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsGeneral.route) {
                SettingsGeneralScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Studio.route) {
                StudioHomeScreen(
                    onNavigateToPresets = { navController.navigate(Screen.PresetGallery.route) },
                    onNavigateToEditor = { navController.navigate(Screen.WatchFaceEditor.route) },
                    onNavigateToImport = { navController.navigate(Screen.ImportExport.route) }
                )
            }
            composable(Screen.PresetGallery.route) {
                PresetGalleryScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.WatchFaceEditor.route) {
                WatchFaceEditorScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.ImportExport.route) {
                ImportExportScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
