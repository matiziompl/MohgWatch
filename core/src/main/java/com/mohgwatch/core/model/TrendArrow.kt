package com.mohgwatch.core.model

/**
 * Strzałka trendu glukozy z LibreLinkUp API.
 *
 * @property apiValue Wartość z API (1-5)
 * @property symbol Symbol Unicode strzałki
 * @property description Opis po polsku
 */
enum class TrendArrow(val apiValue: Int, val symbol: String, val description: String) {
    FALLING_FAST(1, "↓", "Szybki spadek"),
    FALLING(2, "↘", "Spadek"),
    STABLE(3, "→", "Stabilny"),
    RISING(4, "↗", "Wzrost"),
    RISING_FAST(5, "↑", "Szybki wzrost"),
    UNKNOWN(0, "?", "Nieznany");

    companion object {
        /** Mapuje wartość z API na enum */
        fun fromApiValue(value: Int): TrendArrow =
            entries.find { it.apiValue == value } ?: UNKNOWN
    }
}
