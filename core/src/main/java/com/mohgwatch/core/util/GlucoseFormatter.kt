package com.mohgwatch.core.util

import com.mohgwatch.core.model.GlucoseUnit

/**
 * Formatowanie wartości glukozy i czasu.
 */
object GlucoseFormatter {
    private const val MMOL_CONVERSION_FACTOR = 18.0182f

    fun mgDlToMmol(mgDl: Float): Float = mgDl / MMOL_CONVERSION_FACTOR
    fun mmolToMgDl(mmol: Float): Float = mmol * MMOL_CONVERSION_FACTOR

    /** Formatuje wartość bez jednostki */
    fun format(value: Float, unit: GlucoseUnit): String = when (unit) {
        GlucoseUnit.MG_DL -> "${value.toInt()}"
        GlucoseUnit.MMOL_L -> "%.1f".format(mgDlToMmol(value))
    }

    /** Formatuje wartość z jednostką */
    fun formatWithUnit(value: Float, unit: GlucoseUnit): String =
        "${format(value, unit)} ${unit.shortLabel}"

    /** Formatuje czas od ostatniego odczytu (po polsku) */
    fun formatMinutesAgo(minutes: Long): String = when {
        minutes < 1 -> "teraz"
        minutes == 1L -> "1 min temu"
        minutes < 60 -> "$minutes min temu"
        minutes < 120 -> "1h ${minutes % 60} min temu"
        else -> "${minutes / 60}h ${minutes % 60} min temu"
    }
}
