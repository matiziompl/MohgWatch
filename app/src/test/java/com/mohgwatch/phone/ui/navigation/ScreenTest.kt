package com.mohgwatch.phone.ui.navigation

import org.junit.Assert.*
import org.junit.Test

class ScreenTest {
    @Test fun notificationDetailsKeepTheNotificationsTabSelected() {
        for (route in listOf("notifications/glucose", "notifications/haptics", "notifications/sounds",
            "notifications/night", "notifications/connection", "notifications/wrist", "settings/notifications")) {
            assertEquals("Wrong bottom tab for $route", NavigationTab.NOTIFICATIONS, Screen.tabForRoute(route))
        }
    }

    @Test fun settingsDetailsAndCrossLinksSelectSettingsInsteadOfNotifications() {
        for (route in listOf("settings/diabetes", "settings/doses", "settings/sync",
            "settings/theme", "settings/widgets", "settings/account", "settings/logs",
            "settings/diagnostics", "settings/tests/sounds", "settings/tests/watch", "settings/tests/simulation")) {
            assertEquals(NavigationTab.SETTINGS, Screen.tabForRoute(route))
        }
    }

    @Test fun loginAndUnknownRoutesDoNotHighlightAnUnrelatedTab() {
        assertNull(Screen.tabForRoute(null))
        assertNull(Screen.tabForRoute("login"))
        assertNull(Screen.tabForRoute("settings-does-not-exist"))
        assertNull(Screen.tabForRoute("settings/watch"))
        // Removed in v1.9: CDM experiment and the separate battery page.
        assertNull(Screen.tabForRoute("settings/sync/companion"))
        assertNull(Screen.tabForRoute("settings/general"))
        assertEquals(NavigationTab.STATUS, Screen.tabForRoute("status"))
    }
}
