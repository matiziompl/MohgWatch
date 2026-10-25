package com.mohgwatch.core.api

import com.mohgwatch.core.api.LibreLinkUpClient.ApiResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SessionRecoveryTest {
    @Test fun `expired session is renewed once and reading is retried immediately`() = runTest {
        var fetches = 0
        var logins = 0
        val result = fetchWithSessionRecovery(
            fetch = { if (++fetches == 1) ApiResult.LoginRequired else ApiResult.Success(123) },
            recoverSession = { logins++; ApiResult.Success(Unit) }
        )
        assertEquals(ApiResult.Success(123), result)
        assertEquals(2, fetches)
        assertEquals(1, logins)
    }

    @Test fun `rejected replacement token does not cause another login`() = runTest {
        var fetches = 0
        var logins = 0
        val result = fetchWithSessionRecovery<Int>(
            fetch = { fetches++; ApiResult.LoginRequired },
            recoverSession = { logins++; ApiResult.Success(Unit) }
        )
        assertSame(ApiResult.LoginRequired, result)
        assertEquals(2, fetches)
        assertEquals(1, logins)
    }

    @Test fun `network failure during relogin remains a network failure`() = runTest {
        var fetches = 0
        val result = fetchWithSessionRecovery<Int>(
            fetch = { fetches++; ApiResult.LoginRequired },
            recoverSession = { ApiResult.NetworkError() }
        )
        assertEquals(ApiResult.NetworkError(), result)
        assertEquals(1, fetches)
    }

    @Test fun `working session never logs in again`() = runTest {
        val result = fetchWithSessionRecovery(
            fetch = { ApiResult.Success(123) },
            recoverSession = { error("Unexpected login") }
        )
        assertEquals(ApiResult.Success(123), result)
    }

    @Test fun `cancellation is propagated during recovery`() = runTest {
        val cancelled = CancellationException("Stopping synchronization")
        try {
            fetchWithSessionRecovery<Int>(
                fetch = { ApiResult.LoginRequired },
                recoverSession = { throw cancelled }
            )
            fail("Cancellation was swallowed")
        } catch (actual: CancellationException) {
            assertSame(cancelled, actual)
        }
    }
}
