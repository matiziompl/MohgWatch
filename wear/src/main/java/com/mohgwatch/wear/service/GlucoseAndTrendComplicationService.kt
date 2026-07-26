package com.mohgwatch.wear.service

class GlucoseAndTrendComplicationService : BaseGlucoseComplicationService() {
    override val showGlucose = true
    override val showTrend = true
    override val isColorCoded = false
    override val trendOnTop = false
}
