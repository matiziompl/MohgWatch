package com.mohgwatch.phone

import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mohgwatch.core.model.UserSettings
import com.mohgwatch.phone.data.SettingsStore
import com.mohgwatch.phone.ui.navigation.Screen
import com.mohgwatch.phone.ui.screens.*
import com.mohgwatch.phone.ui.theme.MohgWatchTheme
import com.mohgwatch.phone.util.LocalAppLanguage
import com.mohgwatch.phone.util.tr

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        lifecycleScope.launch {
            val credentialStore = com.mohgwatch.phone.data.CredentialStore(this@MainActivity)
            if (credentialStore.hasCredentials() && SettingsStore(this@MainActivity).getSettings().isSyncEnabled) {
                com.mohgwatch.phone.service.GlucoseSyncService.start(this@MainActivity)
            }
        }

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
        MohgWatchTheme(
            appTheme = settings.appTheme,
            themeMode = settings.themeMode,
            logBackgroundColor = settings.logBackgroundColor
        ) {
            val backgroundModifier = com.mohgwatch.phone.ui.theme.LocalAppBackgroundBrush.current?.let { brush ->
                Modifier.background(brush)
            } ?: Modifier.background(MaterialTheme.colorScheme.background)

            Box(modifier = Modifier.fillMaxSize().then(backgroundModifier)) {
                MohgWatchNavigation()
            }
        }
    }
}

@Composable
fun MohgWatchNavigation() {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val selectedTab = Screen.tabForRoute(currentRoute)
    val onBack: () -> Unit = { navController.popBackStack() }
    val onNavigate: (Screen) -> Unit = { screen ->
        navController.navigate(screen.route) { launchSingleTop = true }
    }
    val bottomNavItems = listOf(
        Triple(Screen.Status, Icons.Filled.MonitorHeart, tr("Status", "Status")),
        Triple(Screen.SettingsNotifications, Icons.Filled.Notifications, tr("Powiadomienia", "Notifications")),
        Triple(Screen.Settings, Icons.Filled.Settings, tr("Ustawienia", "Settings"))
    )

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (selectedTab != null) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        bottomNavItems.forEach { (screen, icon, label) ->
                            NavigationBarItem(
                                selected = selectedTab == screen.tab,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(Screen.Status.route)
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                icon = { Icon(icon, contentDescription = null) },
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
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = Screen.Status.route,
            modifier = Modifier.padding(padding)) {
            composable(Screen.Login.route) { LoginScreen(onLoginSuccess = { navController.popBackStack() }) }
            composable(Screen.Status.route) { StatusScreen() }
            composable(Screen.Settings.route) { SettingsScreen(onNavigate) }
            composable(Screen.SettingsNotifications.route) { NotificationsMenuScreen(onNavigate) }
            composable(Screen.Logs.route) { LogsScreen(onBack) }
            composable(Screen.SettingsDiabetes.route) { SettingsDiabetesScreen(onBack) }
            composable(Screen.SettingsDoses.route) {
                SettingsDiabetesScreen(onBack, DiabetesSettingsSection.DOSES)
            }
            composable(Screen.SettingsAccount.route) {
                SettingsAccountScreen(onBack = onBack, onNavigateToLogin = { onNavigate(Screen.Login) })
            }
            composable(Screen.SettingsTheme.route) { SettingsThemeScreen(AppearanceSettingsSection.PHONE, onBack) }
            composable(Screen.SettingsWidgets.route) { SettingsThemeScreen(AppearanceSettingsSection.WIDGETS, onBack) }
            composable(Screen.SettingsGeneral.route) { SettingsGeneralScreen(onBack, GeneralSettingsSection.BACKGROUND) }
            composable(Screen.SettingsSync.route) {
                SettingsGeneralScreen(onBack, GeneralSettingsSection.SYNC,
                    onNavigateToCompanion = { onNavigate(Screen.SettingsCompanion) })
            }
            composable(Screen.SettingsCompanion.route) {
                SettingsCompanionScreen(onBack = onBack,
                    onOpenLogs = { onNavigate(Screen.Logs) },
                    onOpenBatterySettings = { onNavigate(Screen.SettingsGeneral) })
            }
            composable(Screen.SettingsNotificationTests.route) { NotificationTestsMenuScreen(onBack, onNavigate) }
            listOf(
                Screen.SettingsTestSounds to NotificationTestSection.SOUNDS,
                Screen.SettingsTestWatch to NotificationTestSection.WATCH,
                Screen.SettingsTestSimulation to NotificationTestSection.SIMULATION
            ).forEach { (screen, section) ->
                composable(screen.route) { SettingsNotificationTestsScreen(section, onBack) }
            }
            listOf(
                Screen.NotificationGlucose to NotificationSettingsSection.GLUCOSE,
                Screen.NotificationHaptics to NotificationSettingsSection.HAPTICS,
                Screen.NotificationSounds to NotificationSettingsSection.SOUNDS,
                Screen.NotificationNight to NotificationSettingsSection.NIGHT,
                Screen.NotificationConnection to NotificationSettingsSection.CONNECTION,
                Screen.NotificationWrist to NotificationSettingsSection.WRIST
            ).forEach { (screen, section) ->
                composable(screen.route) {
                    SettingsNotificationsScreen(section, onBack,
                        onChangeThresholds = { onNavigate(Screen.SettingsDiabetes) },
                        onTestHaptics = { onNavigate(Screen.SettingsTestWatch) },
                        onTestSounds = { onNavigate(Screen.SettingsTestSounds) },
                        onOpenDiagnostics = { onNavigate(Screen.Logs) })
                }
            }
        }
    }
}
