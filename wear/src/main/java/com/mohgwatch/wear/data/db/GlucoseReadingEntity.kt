package com.mohgwatch.wear.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mohgwatch.core.model.GlucoseReading
import com.mohgwatch.core.model.TrendArrow

@Entity(tableName = "readings", indices = [Index(value = ["timestamp"], unique = true)])
data class GlucoseReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val value: Float,
    val trendArrow: Int,
    val timestamp: Long,
    val isHigh: Boolean,
    val isLow: Boolean
) {
    fun toGlucoseReading(): GlucoseReading = GlucoseReading(
        value = value,
        trendArrow = TrendArrow.fromApiValue(trendArrow),
        timestamp = timestamp,
        isHigh = isHigh,
        isLow = isLow
    )

    companion object {
        fun fromGlucoseReading(reading: GlucoseReading): GlucoseReadingEntity =
            GlucoseReadingEntity(
                value = reading.value,
                trendArrow = reading.trendArrow.apiValue,
                timestamp = reading.timestamp,
                isHigh = reading.isHigh,
                isLow = reading.isLow
            )
    }
}
