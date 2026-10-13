package fr.paulbr.nookmind.core.network

import fr.paulbr.nookmind.core.network.GoogleBooksApi.BusyException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class GoogleBooksRetryTest {
    @Test
    fun aBusyAnswerIsAskedAgain() = runTest {
        var calls = 0
        val books = GoogleBooksApi.retryWhenBusy {
            calls++
            if (calls < 3) throw BusyException(503)
            "Sharko"
        }
        assertEquals("Sharko", books)
        assertEquals(3, calls)
    }

    @Test
    fun itGivesUpAfterThreeTries() = runTest {
        var calls = 0
        assertFailsWith<BusyException> { GoogleBooksApi.retryWhenBusy<Unit> { calls++; throw BusyException(503) } }
        assertEquals(3, calls)
    }

    @Test
    fun anyOtherFailureIsNotAskedAgain() = runTest {
        var calls = 0
        val failure = assertFailsWith<IllegalStateException> {
            GoogleBooksApi.retryWhenBusy<Unit> { calls++; throw IllegalStateException("Failed to fetch books (400)") }
        }
        assertFalse(failure is BusyException)
        assertEquals(1, calls)
    }
}
