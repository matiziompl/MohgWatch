package com.mohgwatch.core.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConnectionsResponse(
    val status: Int,
    val data: List<Connection>?
)

@JsonClass(generateAdapter = true)
data class Connection(
    val patientId: String,
    val glucoseMeasurement: GlucoseMeasurement? = null
)

@JsonClass(generateAdapter = true)
data class GlucoseMeasurement(
    @Json(name = "Value") val value: Float,
    @Json(name = "ValueInMgPerDl") val valueInMgPerDl: Float,
    @Json(name = "Timestamp") val timestamp: String? = null,
    @Json(name = "FactoryTimestamp") val factoryTimestamp: String? = null,
    // Graph points omit TrendArrow; only the current measurement carries it.
    @Json(name = "TrendArrow") val trendArrow: Int? = null,
    @Json(name = "isHigh") val isHigh: Boolean = false,
    @Json(name = "isLow") val isLow: Boolean = false
)
