package com.mohgwatch.core.model

enum class WristStatus(val polishLabel: String, val englishLabel: String) {
    ON_WRIST("Na ręku", "On wrist"),
    OFF_WRIST("Zdjęty", "Off wrist"),
    UNKNOWN("Brak danych", "No data");

    fun at(timestamp: Long, nowMillis: Long): WristStatus =
        if (GlucoseAlertPolicy.hasRecentWristStatus(timestamp, nowMillis)) this else UNKNOWN

    companion object {
        fun fromSensorValue(value: Float): WristStatus = when (value) {
            1f -> ON_WRIST
            0f -> OFF_WRIST
            else -> UNKNOWN
        }
    }
}
