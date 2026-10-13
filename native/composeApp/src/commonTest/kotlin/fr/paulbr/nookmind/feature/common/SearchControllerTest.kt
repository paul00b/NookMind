package fr.paulbr.nookmind.feature.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SearchControllerTest {
    @Test
    fun aFailedSearchShowsTheMessage() = runTest {
        val search = SearchController<String>(3)
        search.run("Thilliez", { throw IllegalStateException("Failed to fetch books (400)") }, "TIMEOUT")
        assertEquals("TIMEOUT", search.error)
        assertFalse(search.searching)
    }

    @Test
    fun aSearchTheNextKeystrokeCancelledShowsNoMessage() = runTest {
        val search = SearchController<String>(3)
        val job = launch {
            search.run("Thil", {
                // What an engine can do with a cancelled request: fail it as a network error.
                try {
                    awaitCancellation()
                } catch (e: CancellationException) {
                    throw IllegalStateException("cancelled")
                }
            }, "TIMEOUT")
        }
        runCurrent()
        job.cancelAndJoin()
        assertNull(search.error)
        assertFalse(search.searching)
    }

    @Test
    fun clearingTheFieldClearsTheMessage() = runTest {
        val search = SearchController<String>(3)
        search.error = "TIMEOUT"
        search.run("Th", { listOf("x") }, "TIMEOUT")
        assertNull(search.error)
        search.error = "TIMEOUT"
        search.close()
        assertNull(search.error)
    }
}
