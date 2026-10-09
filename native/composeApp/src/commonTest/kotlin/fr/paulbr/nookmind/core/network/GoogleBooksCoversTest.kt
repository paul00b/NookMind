package fr.paulbr.nookmind.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoogleBooksCoversTest {
    @Test
    fun fallsBackToZoom1() {
        assertEquals(
            "https://books.google.com/books/content?id=GAk3AwEACAAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api",
            GoogleBooksCovers.zoom1Fallback("https://books.google.com/books/content?id=GAk3AwEACAAJ&printsec=frontcover&img=1&zoom=2&source=gbs_api"),
        )
        assertEquals(
            "http://books.google.fr/books/content?id=x&zoom=1",
            GoogleBooksCovers.zoom1Fallback("http://books.google.fr/books/content?id=x&zoom=3"),
        )
    }

    @Test
    fun leavesOtherUrlsAlone() {
        assertNull(GoogleBooksCovers.zoom1Fallback("https://books.google.com/books/content?id=x&img=1&zoom=1&source=gbs_api"))
        assertNull(GoogleBooksCovers.zoom1Fallback("https://books.google.com/books/content?id=x&img=1"))
        assertNull(GoogleBooksCovers.zoom1Fallback("https://image.tmdb.org/t/p/w342/poster.jpg?zoom=2"))
    }

    @Test
    fun recognisesThePlaceholder() {
        assertTrue(GoogleBooksCovers.isPlaceholder(300, 391))
        assertTrue(GoogleBooksCovers.isPlaceholder(575, 750))
        assertFalse(GoogleBooksCovers.isPlaceholder(300, 444))
        assertFalse(GoogleBooksCovers.isPlaceholder(128, 213))
    }
}
