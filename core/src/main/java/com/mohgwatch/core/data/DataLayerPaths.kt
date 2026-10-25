package com.mohgwatch.core.data

/**
 * Stałe ścieżek Wearable Data Layer API dla komunikacji telefon ↔ zegarek.
 */
object DataLayerPaths {
    const val GLUCOSE_DATA = "/mohgwatch/glucose"
    const val GLUCOSE_HISTORY = "/mohgwatch/glucose_history"
    const val SETTINGS = "/mohgwatch/settings"
    const val CONNECTION_STATUS = "/mohgwatch/connection_status"
    const val STALE_DATA_ALERT = "/mohgwatch/stale_data_alert"
    const val HAPTIC_ALERT = "/mohgwatch/haptic_alert"
    const val WRIST_STATUS = "/mohgwatch/wrist_status"
    const val REQUEST_WRIST_CHECK = "/mohgwatch/request_wrist_check"

    /** Klucze używane w DataMap */
    object Keys {
        const val STALE_MINUTES = "stale_minutes"
        const val HAPTIC_TYPE = "haptic_type"
        const val HAPTIC_PATTERN = "haptic_pattern"
        const val HAPTIC_PREVIEW = "haptic_preview"
        const val MEASUREMENT_TIMESTAMP = "measurement_timestamp"
        const val IS_WORN = "is_worn"
        const val SENSOR_VALUE = "sensor_value"
        const val SENSOR_STATUS_TEXT = "sensor_status_text"
        // Glucose
        const val GLUCOSE_VALUE = "glucose_value"
        const val TREND_ARROW = "trend_arrow"
        const val MEASUREMENT_COLOR = "measurement_color"
        const val TIMESTAMP = "timestamp"
        const val IS_HIGH = "is_high"
        const val IS_LOW = "is_low"

        // Settings
        const val UNIT = "unit"
        const val LOW_THRESHOLD = "low_threshold"
        const val HIGH_THRESHOLD = "high_threshold"
        const val POLL_INTERVAL = "poll_interval"
        const val LANGUAGE = "language"
        const val WATCH_WRIST_NOTIFY_MODE = "watch_wrist_notify_mode"
        const val WATCH_NOTIFY_OUT_OF_RANGE = "watch_notify_out_of_range"
        const val NOTIFY_OUT_OF_RANGE = "notify_out_of_range"
        const val NOTIFY_STALE_DATA_WATCH = "notify_stale_data_watch"
        const val STALE_DATA_MINUTES = "stale_data_minutes"
        const val ALERT_LOW_HAPTIC_ENABLED = "alert_low_haptic_enabled"
        const val ALERT_HIGH_HAPTIC_ENABLED = "alert_high_haptic_enabled"
        const val TREND_STABLE_ENABLED = "trend_stable_enabled"
        const val TREND_RISING_PATTERN = "trend_rising_pattern"
        const val TREND_FALLING_PATTERN = "trend_falling_pattern"
        const val TREND_STABLE_PATTERN = "trend_stable_pattern"
        const val SHOW_NOTIFICATION = "show_notification"

        // History (arrays)
        const val HISTORY_VALUES = "history_values"
        const val HISTORY_TIMESTAMPS = "history_timestamps"
        const val HISTORY_TRENDS = "history_trends"
        const val HISTORY_COLORS = "history_colors"

        // Connection
        const val IS_CONNECTED = "is_connected"
        const val LAST_SYNC = "last_sync"

        // Unique update key (forces DataLayer to propagate even if values are same)
        const val UPDATE_ID = "update_id"
    }
}
