package com.mohgwatch.phone.data

import org.junit.Assert.*
import org.junit.Test

class NetworkObservationPolicyTest {
    @Test fun repeatedIdenticalCapabilitiesAreRecordedOnce() {
        val observations = NetworkObservationPolicy()
        assertTrue(observations.shouldRecord("101", "transport=WIFI validated=true"))
        assertFalse(observations.shouldRecord("101", "transport=WIFI validated=true"))
    }

    @Test fun changedCapabilitiesAreRecordedBeforeSuppressingTheirRepeats() {
        val observations = NetworkObservationPolicy()
        observations.shouldRecord("101", "transport=WIFI validated=false")
        assertTrue(observations.shouldRecord("101", "transport=WIFI validated=true"))
        assertFalse(observations.shouldRecord("101", "transport=WIFI validated=true"))
    }

    @Test fun observationsOfDifferentNetworksRemainIndependent() {
        val observations = NetworkObservationPolicy()
        assertTrue(observations.shouldRecord("101", "transport=WIFI validated=true"))
        assertTrue(observations.shouldRecord("202", "transport=WIFI validated=true"))
        assertFalse(observations.shouldRecord("101", "transport=WIFI validated=true"))
        assertTrue(observations.shouldRecord("202", "transport=WIFI validated=false"))
        assertFalse(observations.shouldRecord("101", "transport=WIFI validated=true"))
    }

    @Test fun lostNetworkCanReturnWithIdenticalCapabilities() {
        val observations = NetworkObservationPolicy()
        observations.shouldRecord("101", "transport=WIFI validated=true")
        observations.shouldRecord("202", "transport=MOBILE validated=true")
        observations.lost("101")
        assertTrue(observations.shouldRecord("101", "transport=WIFI validated=true"))
        assertFalse(observations.shouldRecord("202", "transport=MOBILE validated=true"))
    }
}
