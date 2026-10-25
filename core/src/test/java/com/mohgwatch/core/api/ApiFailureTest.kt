package com.mohgwatch.core.api

import com.mohgwatch.core.api.LibreLinkUpClient.ApiResult
import com.mohgwatch.core.api.dto.LoginResponse
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ApiFailureTest {
    private val adapter = Moshi.Builder().build().adapter(LoginResponse::class.java)

    @Test fun `unexpected login payload is a response error rather than missing internet`() {
        val failure = try {
            adapter.fromJson("""{"status":0,"data":{"user":{"id":"private-user"},"authTicket":{"token":"private-token","expires":"invalid","duration":3600}}}""")
            error("Expected a parser failure")
        } catch (failure: JsonDataException) { failure }

        val events = mutableListOf<String>()
        val result = classifyApiFailure(failure) { event, details -> events += "$event $details" }
        assertTrue(result is ApiResult.Error)
        val message = (result as ApiResult.Error).message
        assertFalse(message.contains("private-token"))
        assertFalse(message.contains("private-user"))
        assertFalse(message.contains("invalid"))
        assertEquals(1, events.size)
        assertTrue(events.single().contains("API_RESPONSE_ERROR"))
        assertTrue(events.single().contains("jsonPath=$.data.authTicket.expires"))
        assertFalse(events.single().contains("private-token"))
        assertFalse(events.single().contains("private-user"))
        assertFalse(events.single().contains("invalid"))
    }

    @Test fun `missing session field diagnostics report schema names without response values`() {
        val failure = try {
            adapter.fromJson("""{"status":0,"data":{"user":{"id":"private-user"},"authTicket":{"token":"private-token","duration":3600}}}""")
            error("Expected missing expires")
        } catch (failure: JsonDataException) { failure }
        val events = mutableListOf<String>()
        classifyApiFailure(failure) { _, details -> events += details }
        assertTrue(events.single().contains("jsonPath=$.data.authTicket"))
        assertTrue(events.single().contains("missingField=expires"))
        assertFalse(events.single().contains("private-user"))
        assertFalse(events.single().contains("private-token"))
    }

    @Test fun `unrecognized error paths never copy arbitrary account keys to diagnostics`() {
        val events = mutableListOf<String>()
        classifyApiFailure(JsonDataException("private-token at path $.accounts.private-user")) { _, details -> events += details }
        assertFalse(events.single().contains("private-token"))
        assertFalse(events.single().contains("private-user"))
    }

    @Test fun `application status is separate from HTTP and requires account action before a session`() {
        assertNull(loginApplicationFailure(0))
        assertSame(ApiResult.LoginRequired, loginApplicationFailure(2))
        val action = loginApplicationFailure(4)
        assertTrue(action is ApiResult.Error)
        assertEquals(0, (action as ApiResult.Error).code)
        assertTrue(action.message.contains("oficjalnej aplikacji LibreLinkUp"))
        val unknown = loginApplicationFailure(17)
        assertTrue(unknown is ApiResult.Error)
        assertEquals(0, (unknown as ApiResult.Error).code)
    }

    @Test fun `malformed JSON does not become a retried network failure`() {
        val result = classifyApiFailure(JsonEncodingException("token=private-token"))
        assertTrue(result is ApiResult.Error)
        assertFalse((result as ApiResult.Error).message.contains("private-token"))
    }

    @Test fun `truncated login response does not imply missing internet`() {
        val failure = try {
            adapter.fromJson("""{"status":0,"data":{"authTicket":{"token":"private-token","expires":3600""")
            error("Expected a truncated response failure")
        } catch (failure: IOException) { failure }
        val result = classifyApiFailure(failure)
        assertTrue(result is ApiResult.Error)
        assertFalse((result as ApiResult.Error).message.contains("private-token"))
    }

    @Test fun `HTTP 430 preserves server rejection and gives a manual next step in every region`() {
        for (region in listOf(null, "eu")) {
            val result = loginHttpFailure(430, region)
            assertTrue(result is ApiResult.Error)
            val error = result as ApiResult.Error
            assertEquals(430, error.code)
            assertTrue(error.message.contains("430"))
            assertTrue(error.message.contains("oficjalnej aplikacji LibreLinkUp"))
            assertTrue(error.message.contains("raport diagnostyczny"))
        }
    }

    @Test fun `HTTP 430 is returned without automatically repeating login`() = runTest {
        var attempts = 0
        val result = LibreLinkUpClient().withRetry<Unit>(maxAttempts = 3) {
            attempts++
            loginHttpFailure(430)
        }
        assertTrue(result is ApiResult.Error)
        assertEquals(430, (result as ApiResult.Error).code)
        assertEquals(1, attempts)
    }

    @Test fun `DNS timeout and TLS keep distinct safe reasons`() {
        val dns = classifyApiFailure(UnknownHostException("private-account")) as ApiResult.NetworkError
        val timeout = classifyApiFailure(SocketTimeoutException("private-account")) as ApiResult.NetworkError
        val tls = classifyApiFailure(SSLHandshakeException("private-account")) as ApiResult.NetworkError
        assertEquals(NetworkFailureKind.DNS, dns.kind)
        assertEquals(NetworkFailureKind.TIMEOUT, timeout.kind)
        assertEquals(NetworkFailureKind.TLS, tls.kind)
        assertNotEquals(dns.message, timeout.message)
        assertNotEquals(timeout.message, tls.message)
        assertTrue(dns.retryable)
        assertTrue(timeout.retryable)
        assertFalse(tls.retryable)
        assertFalse(listOf(dns, timeout, tls).any { it.message.contains("private-account") })
    }

    @Test fun `IO connection loss remains eligible for bounded retry`() {
        val result = classifyApiFailure(IOException("Authorization: private-token")) as ApiResult.NetworkError
        assertEquals(NetworkFailureKind.CONNECTION, result.kind)
        assertTrue(result.retryable)
        assertFalse(result.message.contains("private-token"))
    }

    @Test fun `configuration failure is not reported as a connection failure`() {
        val result = classifyApiFailure(IllegalArgumentException("password=private-password"))
        assertTrue(result is ApiResult.Error)
        assertFalse((result as ApiResult.Error).message.contains("private-password"))
    }

    @Test fun `cancellation is never converted into an API error`() {
        val cancelled = CancellationException("cancelled")
        try {
            classifyApiFailure(cancelled)
            fail("Cancellation was swallowed")
        } catch (actual: CancellationException) {
            assertSame(cancelled, actual)
        }
    }

    @Test fun `TLS handshake failure is not retried`() = runTest {
        var attempts = 0
        val result = LibreLinkUpClient().withRetry<Unit>(maxAttempts = 3) {
            attempts++
            classifyApiFailure(SSLHandshakeException("certificate"))
        }
        assertTrue(result is ApiResult.NetworkError)
        assertEquals(1, attempts)
    }

    @Test fun `transient network failure stops after three attempts`() = runTest {
        var attempts = 0
        val result = LibreLinkUpClient().withRetry<Unit>(maxAttempts = 3) {
            attempts++
            classifyApiFailure(UnknownHostException("unavailable"))
        }
        assertTrue(result is ApiResult.NetworkError)
        assertEquals(3, attempts)
    }

    @Test fun `response format failure is returned without retry`() = runTest {
        var attempts = 0
        val result = LibreLinkUpClient().withRetry<Unit>(maxAttempts = 3) {
            attempts++
            classifyApiFailure(JsonDataException("invalid payload"))
        }
        assertTrue(result is ApiResult.Error)
        assertEquals(1, attempts)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun `cancelling retry delay prevents later attempts`() = runTest {
        var attempts = 0
        val job = launch {
            LibreLinkUpClient().withRetry<Unit>(maxAttempts = 3) {
                attempts++
                classifyApiFailure(IOException("unavailable"))
            }
        }
        runCurrent()
        job.cancel()
        job.join()
        assertTrue(job.isCancelled)
        assertEquals(1, attempts)
    }
}
