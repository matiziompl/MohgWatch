package com.mohgwatch.core.model

/**
 * Kolor pomiaru z LibreLinkUp API — określa zakres glukozy.
 */
enum class MeasurementColor(val apiValue: Int, val label: String) {
    IN_RANGE(1, "W normie"),
    HIGH(2, "Wysoki"),
    LOW(3, "Niski"),
    UNKNOWN(0, "Nieznany");

    companion object {
        fun fromApiValue(value: Int): MeasurementColor =
            entries.find { it.apiValue == value } ?: UNKNOWN
    }
}
