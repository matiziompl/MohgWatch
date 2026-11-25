package com.mohgwatch.phone.ui.navigation

enum class NavigationTab { STATUS, NOTIFICATIONS, SETTINGS }

/** Stable destinations and their owner in the bottom navigation. */
sealed class Screen(val route: String, val tab: NavigationTab? = NavigationTab.SETTINGS) {
    data object Login : Screen("login", null)
    data object Status : Screen("status", NavigationTab.STATUS)
    data object Settings : Screen("settings")
    data object SettingsNotifications : Screen("settings/notifications", NavigationTab.NOTIFICATIONS)

    data object NotificationGlucose : Screen("notifications/glucose", NavigationTab.NOTIFICATIONS)
    data object NotificationHaptics : Screen("notifications/haptics", NavigationTab.NOTIFICATIONS)
    data object NotificationSounds : Screen("notifications/sounds", NavigationTab.NOTIFICATIONS)
    data object NotificationNight : Screen("notifications/night", NavigationTab.NOTIFICATIONS)
    data object NotificationConnection : Screen("notifications/connection", NavigationTab.NOTIFICATIONS)
    data object NotificationWrist : Screen("notifications/wrist", NavigationTab.NOTIFICATIONS)

    data object SettingsDiabetes : Screen("settings/diabetes")
    data object SettingsDoses : Screen("settings/doses")
    data object SettingsSync : Screen("settings/sync")
    data object SettingsTheme : Screen("settings/theme")
    data object SettingsWidgets : Screen("settings/widgets")
    data object SettingsAccount : Screen("settings/account")
    data object Logs : Screen("settings/logs")
    data object Diagnostics : Screen("settings/diagnostics")
    data object SettingsTestSounds : Screen("settings/tests/sounds")
    data object SettingsTestWatch : Screen("settings/tests/watch")
    data object SettingsTestSimulation : Screen("settings/tests/simulation")

    companion object {
        val notificationPages: List<Screen> get() = listOf(NotificationGlucose, NotificationHaptics,
            NotificationSounds, NotificationNight, NotificationConnection, NotificationWrist)
        val settingsPages: List<Screen> get() = listOf(SettingsDiabetes, SettingsDoses, SettingsSync,
            SettingsTheme, SettingsWidgets, SettingsAccount, Logs, Diagnostics)
        val testPages: List<Screen> get() = listOf(SettingsTestSounds, SettingsTestWatch, SettingsTestSimulation)
        val all: List<Screen> get() = listOf(Login, Status, Settings, SettingsNotifications) +
            notificationPages + settingsPages + testPages

        fun tabForRoute(route: String?): NavigationTab? = all.firstOrNull { it.route == route }?.tab
    }
}
