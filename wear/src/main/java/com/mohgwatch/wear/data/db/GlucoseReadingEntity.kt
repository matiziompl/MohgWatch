package com.mohgwatch.wear.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.model.MeasurementColor
import com.mohgwatch.core.model.TrendArrow

@Entity(tableName = "readings")
data class GlucoseReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val value: Float,
    val trendArrow: Int,
    val measurementColor: Int,
    val timestamp: Long,
    val isHigh: Boolean,
    val isLow: Boolean
) {
    fun toGlucoseReading(): GlucoseReading = GlucoseReading(
        value = value,
        trendArrow = TrendArrow.fromApiValue(trendArrow),
        measurementColor = MeasurementColor.fromApiValue(measurementColor),
        timestamp = timestamp,
        isHigh = isHigh,
        isLow = isLow
    )

    companion object {
        fun fromGlucoseReading(reading: GlucoseReading): GlucoseReadingEntity =
            GlucoseReadingEntity(
                value = reading.value,
                trendArrow = reading.trendArrow.apiValue,
                measurementColor = reading.measurementColor.apiValue,
                timestamp = reading.timestamp,
                isHigh = reading.isHigh,
                isLow = reading.isLow
            )
    }
}
