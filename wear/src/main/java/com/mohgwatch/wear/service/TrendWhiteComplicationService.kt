package com.mohgwatch.wear.service
class TrendWhiteComplicationService : BaseGlucoseComplicationService() {
    override val showGlucose = false
    override val showTrend = true
    override val isColorCoded = false
}
