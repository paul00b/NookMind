package fr.paulbr.nookmind.feature.tonight

import fr.paulbr.nookmind.core.model.TmdbMovie
import fr.paulbr.nookmind.core.model.WatchProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TonightGameTest {
    private val y = Verdict.YES
    private val n = Verdict.NO
    private val l = Verdict.LOVE
    private val s = Verdict.SEEN

    @Test
    fun oneSharedYesIsAMatch() {
        assertEquals(Outcome.Match(2), outcomeOf(4, mapOf(0 to y, 1 to n, 2 to y), mapOf(0 to n, 1 to y, 2 to l)))
    }

    @Test
    fun severalSharedYesesPlayAFinalFavouritesFirst() {
        val outcome = outcomeOf(5, mapOf(0 to y, 1 to y, 3 to y), mapOf(0 to y, 1 to y, 3 to l))
        assertEquals(Outcome.Final(3, 0), outcome)
    }

    @Test
    fun noSharedYesFallsBackOnMaybes() {
        assertEquals(Outcome.Maybes(1, 0), outcomeOf(3, mapOf(0 to y, 1 to l, 2 to n), mapOf(0 to n, 1 to n, 2 to s)))
        assertEquals(Outcome.Single(2), outcomeOf(3, mapOf(0 to n, 2 to y), mapOf(0 to n, 2 to n)))
        assertEquals(Outcome.None, outcomeOf(3, mapOf(0 to n, 1 to s), mapOf(0 to n, 1 to n)))
    }

    @Test
    fun deckPutsTickedServicesFirstAndDropsLongMovies() {
        val netflix = WatchProvider(8, "Netflix")
        val disney = WatchProvider(337, "Disney Plus")
        val a = card(1, 150, disney)
        val b = card(2, 110, netflix)
        val c = card(3, null, null)
        val d = card(4, 95, netflix)
        assertEquals(listOf(2, 4, 1, 3), pickDeck(listOf(a, b, c, d), setOf(StreamingService.NETFLIX), null).map { it.tmdbId })
        assertEquals(listOf(2, 4, 3), pickDeck(listOf(a, b, c, d), setOf(StreamingService.NETFLIX), 120).map { it.tmdbId })
        assertEquals(listOf(1, 2), pickDeck(listOf(a, b, c, d), emptySet(), null, size = 2).map { it.tmdbId })
    }

    @Test
    fun providersAreRecognisedByName() {
        val prime = WatchProvider(119, "Amazon Prime Video")
        val canal = WatchProvider(381, "Canal+")
        assertEquals("Prime Video", providerLabel(prime))
        assertEquals(canal, pickProvider(listOf(prime, canal), setOf(StreamingService.CANAL)))
        assertEquals(prime, pickProvider(listOf(prime, canal), emptySet()))
        assertNull(pickProvider(emptyList(), setOf(StreamingService.CANAL)))
    }

    @Test
    fun dragsTurnIntoVerdicts() {
        fun v(dx: Float, dy: Float, vx: Float = 0f, vy: Float = 0f) = verdictOf(dx, dy, vx, vy, threshold = 100f, minFling = 40f, flingVelocity = 1000f)
        assertEquals(Verdict.YES, v(120f, 10f))
        assertEquals(Verdict.NO, v(-120f, 30f))
        assertEquals(Verdict.LOVE, v(5f, -110f))
        assertNull(v(5f, 110f), "down asks a little more")
        assertEquals(Verdict.SEEN, v(5f, 120f))
        assertEquals(Verdict.YES, v(50f, 0f, vx = 1500f), "a quick flick")
        assertNull(v(30f, 0f, vx = 1500f), "too short to be a flick")
        assertNull(v(60f, 40f))
    }

    @Test
    fun runtimeReadsLikeACinemaListing() {
        assertEquals("2 h 08", formatRuntime(128))
        assertEquals("54 min", formatRuntime(54))
        assertNull(formatRuntime(0))
    }

    private fun card(id: Int, runtime: Int?, provider: WatchProvider?) = TonightCard(
        movie = TmdbMovie(id = id, title = "Film $id"),
        posterUrl = null, year = null, runtime = runtime, director = null, genres = emptyList(),
        provider = provider, providersLink = null, reason = CardReason.Trending,
    )
}
