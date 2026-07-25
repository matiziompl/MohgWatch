package com.mohgwatch.core.model

/**
 * Strzałka trendu glukozy z LibreLinkUp API.
 *
 * @property apiValue Wartość z API (1-5)
 * @property symbol Symbol Unicode strzałki
 * @property description Opis po polsku
 */
enum class TrendArrow(val apiValue: Int, val symbol: String, val description: String) {
    RISING_FAST(1, "↑", "Szybki wzrost"),
    RISING(2, "↗", "Wzrost"),
    STABLE(3, "→", "Stabilny"),
    FALLING(4, "↘", "Spadek"),
    FALLING_FAST(5, "↓", "Szybki spadek"),
    UNKNOWN(0, "?", "Nieznany");

    companion object {
        /** Mapuje wartość z API na enum */
        fun fromApiValue(value: Int): TrendArrow =
            entries.find { it.apiValue == value } ?: UNKNOWN
    }
}
