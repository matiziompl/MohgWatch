package com.mohgwatch.core.api

import com.mohgwatch.core.api.dto.LoginResponse
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import org.junit.Assert.*
import org.junit.Test

class LoginResponseParsingTest {
    private val adapter = Moshi.Builder().build().adapter(LoginResponse::class.java)

    @Test fun `successful login needs token expiry and id but not unused ticket duration`() {
        val response = adapter.fromJson("""{"status":0,"data":{"user":{"id":"synthetic-id"},"authTicket":{"token":"synthetic-token","expires":2000000000}}}""")!!

        assertEquals(0, response.status)
        assertEquals("synthetic-id", response.data!!.user!!.id)
        assertEquals("synthetic-token", response.data!!.authTicket!!.token)
        assertEquals(2_000_000_000L, response.data!!.authTicket!!.expires)
    }

    @Test fun `unused ticket duration may be null without preventing login`() {
        val response = adapter.fromJson("""{"status":0,"data":{"user":{"id":"synthetic-id"},"authTicket":{"token":"synthetic-token","expires":2000000000,"duration":null}}}""")!!

        assertEquals("synthetic-token", response.data!!.authTicket!!.token)
        assertEquals(2_000_000_000L, response.data!!.authTicket!!.expires)
    }

    @Test fun `regional redirect omits user and ticket without inventing a session`() {
        val response = adapter.fromJson("""{"status":0,"data":{"redirect":true,"region":"eu"}}""")!!

        assertTrue(response.data!!.redirect)
        assertEquals("eu", response.data!!.region)
        assertNull(response.data!!.user)
        assertNull(response.data!!.authTicket)
    }

    @Test fun `empty and absent response data never invent a user or ticket`() {
        for (payload in listOf("""{"status":2,"data":{}}""", """{"status":2,"data":null}""", """{"status":2}""")) {
            val response = adapter.fromJson(payload)!!
            assertEquals(2, response.status)
            assertNull(response.data?.user)
            assertNull(response.data?.authTicket)
        }
    }

    @Test fun `credentials required for a successful session stay mandatory`() {
        val invalidPayloads = listOf(
            """{"status":0,"data":{"user":{},"authTicket":{"token":"synthetic-token","expires":2000000000,"duration":3600}}}""",
            """{"status":0,"data":{"user":{"id":"synthetic-id"},"authTicket":{"expires":2000000000,"duration":3600}}}""",
            """{"status":0,"data":{"user":{"id":"synthetic-id"},"authTicket":{"token":"synthetic-token","duration":3600}}}""",
            """{"status":0,"data":{"user":{"id":null},"authTicket":{"token":"synthetic-token","expires":2000000000,"duration":3600}}}""",
            """{"status":0,"data":{"user":{"id":"synthetic-id"},"authTicket":{"token":null,"expires":2000000000,"duration":3600}}}""",
            """{"status":0,"data":{"user":{"id":"synthetic-id"},"authTicket":{"token":"synthetic-token","expires":null,"duration":3600}}}""",
            """{"status":0,"data":{"user":{"id":"synthetic-id"},"authTicket":{"token":"synthetic-token","expires":"invalid","duration":3600}}}"""
        )
        for (payload in invalidPayloads) {
            try {
                adapter.fromJson(payload)
                fail("Incomplete credentials were accepted")
            } catch (_: JsonDataException) {
                // A missing credential must not become an empty token, fake id or expiry.
            }
        }
    }
}
