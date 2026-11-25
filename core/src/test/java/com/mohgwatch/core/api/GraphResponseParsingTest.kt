package com.mohgwatch.core.api

import com.mohgwatch.core.api.dto.GraphResponse
import com.mohgwatch.core.model.TrendArrow
import com.squareup.moshi.Moshi
import org.junit.Assert.*
import org.junit.Test

class GraphResponseParsingTest {
    private val adapter = Moshi.Builder().build().adapter(GraphResponse::class.java)

    @Test fun `graph points without trend arrow are parsed as history`() {
        // LibreLinkUp sends TrendArrow only for the current measurement, not for graph points.
        val json = """
            {"status":0,"data":{
              "connection":{"patientId":"p1","glucoseMeasurement":{"Value":101,"ValueInMgPerDl":101,
                "Timestamp":"11/24/2026 1:05:00 AM","TrendArrow":3,"MeasurementColor":1}},
              "graphData":[
                {"FactoryTimestamp":"11/24/2026 12:00:00 AM","Timestamp":"11/24/2026 1:00:00 AM",
                 "type":0,"ValueInMgPerDl":98,"MeasurementColor":1,"GlucoseUnits":1,"Value":98,
                 "isHigh":false,"isLow":false}
              ]}}
        """.trimIndent()

        val points = adapter.fromJson(json)!!.data!!.graphData!!

        assertEquals(1, points.size)
        val reading = points.single().toGlucoseReading()!!
        assertEquals(98f, reading.value)
        assertEquals(TrendArrow.UNKNOWN, reading.trendArrow)
    }
}
