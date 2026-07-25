package com.mohgwatch.core.api.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GraphResponse(
    val status: Int,
    val data: GraphData?
)

@JsonClass(generateAdapter = true)
data class GraphData(
    val connection: Connection? = null,
    val graphData: List<GlucoseMeasurement>? = null
)
