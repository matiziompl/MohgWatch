package com.mohgwatch.phone.service

import com.google.android.gms.tasks.TaskCompletionSource
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BoundedWatchOperationTest {
    @Test fun unfinishedWatchTaskTimesOutAndNextOperationCanRun() = runTest {
        val task = TaskCompletionSource<String>()
        val failures = mutableListOf<Throwable>()
        val result = async { boundedWatchOperation(1_000L, failures::add) { task.task.await() } }
        runCurrent()
        advanceTimeBy(1_001L)
        runCurrent()
        assertTrue("Unfinished Data Layer task must stop blocking the cycle", result.isCompleted)
        assertNull(result.await())
        assertTrue(failures.single() is TimeoutCancellationException)
        assertEquals("next cycle", boundedWatchOperation { "next cycle" })
    }

    @Test fun parentTimeoutIsNotConvertedIntoAnOperationFailure() = runTest {
        val failures = mutableListOf<Throwable>()
        var returned = false
        withTimeoutOrNull(100L) {
            boundedWatchOperation(10_000L, failures::add) { awaitCancellation() }
            returned = true
        }
        assertFalse(returned)
        assertTrue(failures.isEmpty())
    }

    @Test fun explicitCancellationIsPropagated() = runTest {
        try {
            boundedWatchOperation<String> { throw CancellationException("stop") }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }

    @Test fun failedOperationIsReportedAndDoesNotPreventAnotherOperation() = runTest {
        val failures = mutableListOf<Throwable>()
        assertNull(boundedWatchOperation<String>(onFailure = failures::add) { throw IllegalStateException() })
        assertEquals(1, failures.size)
        assertEquals("ok", boundedWatchOperation { "ok" })
    }
}
