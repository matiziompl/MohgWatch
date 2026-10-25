package com.mohgwatch.core.api

import com.mohgwatch.core.api.dto.LoginResponse
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import org.junit.Assert.*
import org.junit.Test

class LoginResponseEnvelopeTest {
    private val adapter = Moshi.Builder().add(LoginResponseAdapterFactory).build()
        .adapter(LoginResponse::class.java)

    @Test fun `account action response is not decoded as a successful session`() {
        val response = adapter.fromJson("""{"status":4,"data":{"step":{"type":"tou"},"user":{"accountType":"pat"},"authTicket":{"token":"synthetic-token"}}}""")!!
        assertEquals(4, response.status)
        assertNull(response.data)
    }

    @Test fun `application status is recognized even after the data member`() {
        val response = adapter.fromJson("""{"data":{"user":{"country":"PL"},"authTicket":{}},"status":4}""")!!
        assertEquals(4, response.status)
        assertNull(response.data)
    }

    @Test fun `rejected login may carry an array instead of session data`() {
        val response = adapter.fromJson("""{"status":2,"data":[]}""")!!
        assertEquals(2, response.status)
        assertNull(response.data)
    }

    @Test fun `successful regional redirect retains its target`() {
        val response = adapter.fromJson("""{"status":0,"data":{"redirect":true,"region":"EU2"}}""")!!
        assertTrue(response.data!!.redirect)
        assertEquals("EU2", response.data!!.region)
        assertNull(response.data!!.authTicket)
    }

    @Test fun `success still rejects a missing required session credential`() {
        try {
            adapter.fromJson("""{"status":0,"data":{"user":{"id":"synthetic-id"},"authTicket":{"expires":2000000000,"duration":3600}}}""")
            fail("An absent token must not become a successful login")
        } catch (_: JsonDataException) { }
    }

    @Test fun `missing or invalid status cannot be assumed to mean success`() {
        for (payload in listOf("""{"data":{}}""", """{"status":null,"data":{}}""", """{"status":"invalid","data":{}}""")) {
            try {
                adapter.fromJson(payload)
                fail("A valid application status is required")
            } catch (_: JsonDataException) { }
        }
    }
}
