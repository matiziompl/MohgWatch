package com.mohgwatch.wear.service
class GlucoseTrendWhiteComplicationService : BaseGlucoseComplicationService() {
    override val showGlucose = true
    override val showTrend = true
    override val isColorCoded = false
}
