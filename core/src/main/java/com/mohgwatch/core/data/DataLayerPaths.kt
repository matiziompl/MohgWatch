package com.mohgwatch.core.data

/**
 * Stałe ścieżek Wearable Data Layer API dla komunikacji telefon ↔ zegarek.
 */
object DataLayerPaths {
    const val GLUCOSE_DATA = "/mohgwatch/glucose"
    const val GLUCOSE_HISTORY = "/mohgwatch/glucose_history"
    const val CREDENTIALS = "/mohgwatch/credentials"
    const val SETTINGS = "/mohgwatch/settings"
    const val WATCH_FACE = "/mohgwatch/watchface"
    const val CONNECTION_STATUS = "/mohgwatch/connection_status"

    /** Klucze używane w DataMap */
    object Keys {
        // Glucose
        const val GLUCOSE_VALUE = "glucose_value"
        const val TREND_ARROW = "trend_arrow"
        const val MEASUREMENT_COLOR = "measurement_color"
        const val TIMESTAMP = "timestamp"
        const val IS_HIGH = "is_high"
        const val IS_LOW = "is_low"
        const val PATIENT_ID = "patient_id"

        // Credentials
        const val TOKEN = "token"
        const val USER_ID = "user_id"
        const val REGION = "region"
        const val EXPIRES = "expires"

        // Settings
        const val UNIT = "unit"
        const val LOW_THRESHOLD = "low_threshold"
        const val HIGH_THRESHOLD = "high_threshold"
        const val PRESET = "preset"
        const val POLL_INTERVAL = "poll_interval"
        const val SHOW_DEMO_BUTTON = "show_demo_button"
        const val APP_THEME = "app_theme"
        const val THEME_MODE = "theme_mode"
        const val SYNC_THEME_WITH_WATCH = "sync_theme_with_watch"
        const val WATCH_APP_THEME = "watch_app_theme"
        const val LANGUAGE = "language"

        // History (arrays)
        const val HISTORY_VALUES = "history_values"
        const val HISTORY_TIMESTAMPS = "history_timestamps"
        const val HISTORY_TRENDS = "history_trends"
        const val HISTORY_COLORS = "history_colors"
        const val HISTORY_IS_HIGH = "history_is_high"
        const val HISTORY_IS_LOW = "history_is_low"

        // Connection
        const val IS_CONNECTED = "is_connected"
        const val LAST_SYNC = "last_sync"

        // Unique update key (forces DataLayer to propagate even if values are same)
        const val UPDATE_ID = "update_id"
    }
}
