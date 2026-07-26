package com.mohgwatch.phone.ui.navigation

/**
 * Trasy nawigacyjne aplikacji telefonicznej MoghWatch.
 */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Status : Screen("status")
    data object Chart : Screen("chart")
    data object Settings : Screen("settings")
    data object SettingsDiabetes : Screen("settings/diabetes")
    data object SettingsAccount : Screen("settings/account")
    data object SettingsTheme : Screen("settings/theme")
    data object SettingsNotifications : Screen("settings/notifications")
    data object SettingsGeneral : Screen("settings/general")
    data object SettingsChart : Screen("settings/chart")
    data object SettingsWatch : Screen("settings/watch")
}
