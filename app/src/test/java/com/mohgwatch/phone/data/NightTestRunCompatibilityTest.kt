package com.mohgwatch.phone.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class NightTestRunCompatibilityTest {
    // Same adapter setup as NightTestPreferenceStore.
    private val adapter = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build().adapter(NightTestRun::class.java)

    @Test fun `run saved by v1_8 with CDM mode still loads after the upgrade`() {
        val saved = """{"id":"r1","mode":"BASELINE","startedAtMillis":1000,"startedElapsedMillis":5,"bootCount":34,
            "configurationFingerprint":"sync=true; associations=[]","batteryStart":58,"pluggedAtStart":0}"""

        val run = adapter.fromJson(saved)!!

        assertEquals("r1", run.id)
        assertEquals(34, run.bootCount)
        assertEquals(58, run.batteryStart)
    }
}
