package com.mohgwatch.phone.data

internal class NetworkObservationPolicy {
    private val observations = mutableMapOf<String, String>()
    @Synchronized fun shouldRecord(network: String, capabilities: String): Boolean =
        observations.put(network, capabilities) != capabilities
    @Synchronized fun lost(network: String) { observations.remove(network) }
}
