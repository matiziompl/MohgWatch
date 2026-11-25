package com.mohgwatch.core.model

/**
 * Reprezentuje pojedynczy odczyt glukozy z sensora.
 *
 * @property value Wartość glukozy w mg/dL
 * @property trendArrow Kierunek trendu (rosnący/malejący/stabilny)
 * @property timestamp Czas odczytu w Unix millis
 * @property isHigh Czy wartość jest powyżej normy
 * @property isLow Czy wartość jest poniżej normy
 */
data class GlucoseReading(
    val value: Float,
    val trendArrow: TrendArrow,
    val timestamp: Long,
    val isHigh: Boolean,
    val isLow: Boolean
) {
    /**
     * Zwraca liczbę minut od ostatniego odczytu.
     */
    fun getMinutesAgo(): Long = (System.currentTimeMillis() - timestamp) / 60_000L

    fun isFreshAt(nowMillis: Long, staleAfterMillis: Long = STALE_AFTER_MILLIS): Boolean =
        timestamp > 0L && timestamp <= nowMillis && nowMillis - timestamp < staleAfterMillis

    companion object {
        const val STALE_AFTER_MILLIS = 10 * 60_000L
    }
}
