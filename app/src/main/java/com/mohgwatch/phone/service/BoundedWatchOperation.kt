package com.mohgwatch.phone.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout

/** A stalled Data Layer Task must not hold the sync job or its recovery timer. */
internal suspend fun <T> boundedWatchOperation(
    timeoutMillis: Long = 10_000L,
    onFailure: (Throwable) -> Unit = {},
    operation: suspend () -> T
): T? = try {
    withTimeout(timeoutMillis) { operation() }
} catch (timeout: TimeoutCancellationException) {
    // Only our own deadline is recoverable; a parent deadline must keep cancelling.
    currentCoroutineContext().ensureActive()
    onFailure(timeout)
    null
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    onFailure(failure)
    null
}
