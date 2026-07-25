package com.mohgwatch.core.model

/**
 * Reprezentuje pojedynczy odczyt glukozy z sensora.
 *
 * @property value Wartość glukozy w mg/dL
 * @property trendArrow Kierunek trendu (rosnący/malejący/stabilny)
 * @property measurementColor Kolor zakresu (norma/wysoki/niski)
 * @property timestamp Czas odczytu w Unix millis
 * @property isHigh Czy wartość jest powyżej normy
 * @property isLow Czy wartość jest poniżej normy
 */
data class GlucoseReading(
    val value: Float,
    val trendArrow: TrendArrow,
    val measurementColor: MeasurementColor,
    val timestamp: Long,
    val isHigh: Boolean,
    val isLow: Boolean
) {
    /** Wartość glukozy przeliczona na mmol/L */
    val valueInMmol: Float get() = value / MMOL_CONVERSION_FACTOR

    /**
     * Formatuje wartość glukozy w wybranej jednostce.
     */
    fun getFormattedValue(unit: GlucoseUnit): String = when (unit) {
        GlucoseUnit.MG_DL -> "${value.toInt()}"
        GlucoseUnit.MMOL_L -> "%.1f".format(valueInMmol)
    }

    /**
     * Zwraca liczbę minut od ostatniego odczytu.
     */
    fun getMinutesAgo(): Long = (System.currentTimeMillis() - timestamp) / 60_000L

    /**
     * Określa kolor na podstawie progów.
     */
    fun getRangeColor(lowThreshold: Float, highThreshold: Float): RangeColor = when {
        value < lowThreshold -> RangeColor.LOW
        value > highThreshold -> RangeColor.HIGH
        else -> RangeColor.IN_RANGE
    }

    companion object {
        const val MMOL_CONVERSION_FACTOR = 18.0182f
    }
}

/** Kolor zakresu glukozy do wyświetlenia na UI */
enum class RangeColor {
    IN_RANGE,  // Zielony
    HIGH,      // Żółty/pomarańczowy
    LOW        // Czerwony
}
