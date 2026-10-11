package fr.paulbr.nookmind.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShareLinksTest {
    private val base = "https://nookmind.example.app/"

    @Test
    fun linksCarryTheSharerAndTheLanguage() {
        assertEquals("https://nookmind.example.app/s/movie/438631?by=Paul&l=fr", ShareLinks.linkFor(base, "movie", "438631", "Paul Broussolle", "fr-FR"))
        assertEquals("https://nookmind.example.app/s/book/zyTCAlFPjgYC?by=L%C3%A9a&l=en", ShareLinks.linkFor(base, "book", "zyTCAlFPjgYC", "Léa", "en-US"))
        assertEquals("https://nookmind.example.app/s/series/1396?l=fr", ShareLinks.linkFor(base, "series", "1396", "  ", "fr"))
    }

    @Test
    fun noLinkWithoutADomainOrAUsableKey() {
        assertNull(ShareLinks.linkFor(null, "movie", "1", "Paul", "fr"))
        assertNull(ShareLinks.linkFor(base, "podcast", "1", "Paul", "fr"))
        assertNull(ShareLinks.linkFor(base, "book", "a/b", "Paul", "fr"))
    }

    @Test
    fun bothKindsOfLinksLeadToTheAddRoute() {
        assertEquals("/add/movie/438631", ShareLinks.routeOf("nookmind://add/movie/438631"))
        assertEquals("/add/book/zyTCAlFPjgYC", ShareLinks.routeOf("https://nookmind.example.app/s/book/zyTCAlFPjgYC?by=Paul&l=fr"))
        assertEquals("/add/series/1396", ShareLinks.routeOf("https://x.vercel.app/s/series/1396/"))
        assertNull(ShareLinks.routeOf("https://x.vercel.app/library"))
        assertNull(ShareLinks.routeOf("com.googleusercontent.apps.123:/oauth2redirect"))
        assertNull(ShareLinks.routeOf("nookmind://add/podcast/1"))
    }

    @Test
    fun addRoutesParse() {
        assertEquals("movie" to "438631", ShareLinks.parseRoute("/add/movie/438631"))
        assertNull(ShareLinks.parseRoute("/add/movie"))
        assertNull(ShareLinks.parseRoute("/library"))
    }
}
