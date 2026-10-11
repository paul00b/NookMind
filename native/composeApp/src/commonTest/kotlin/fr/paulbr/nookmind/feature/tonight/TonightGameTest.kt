package fr.paulbr.nookmind.feature.tonight

import fr.paulbr.nookmind.core.model.TmdbMovie
import fr.paulbr.nookmind.core.model.WatchProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun theMovieLovedMostWinsOutright() {
        // A shared favourite beats a single one, which beats a plain yes.
        assertEquals(Outcome.Match(1), outcomeOf(5, mapOf(0 to y, 1 to l, 3 to y), mapOf(0 to y, 1 to l, 3 to l)))
        assertEquals(Outcome.Match(3), outcomeOf(5, mapOf(0 to y, 1 to y, 3 to y), mapOf(0 to y, 1 to y, 3 to l)))
    }

    @Test
    fun aTieAtTheTopPlaysAFinal() {
        assertEquals(Outcome.Final(0, 1), outcomeOf(5, mapOf(0 to y, 1 to y, 3 to y), mapOf(0 to y, 1 to y, 3 to y)))
        assertEquals(Outcome.Final(0, 2), outcomeOf(4, mapOf(0 to l, 2 to y), mapOf(0 to y, 2 to l)))
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
    fun genresFilterTheDeck() {
        val comedy = card(1, 100, null, genres = listOf(35))
        val horror = card(2, 100, null, genres = listOf(27, 53))
        val unknown = card(3, 100, null)
        assertTrue(matchesGenres(listOf(27), emptySet()), "nothing picked fits all")
        assertTrue(matchesGenres(listOf(53), setOf(TonightGenre.THRILLER)))
        assertEquals(listOf(2), pickDeck(listOf(comedy, horror, unknown), emptySet(), null, genres = setOf(TonightGenre.HORROR)).map { it.tmdbId })
        assertEquals(listOf(1, 2), pickDeck(listOf(comedy, horror, unknown), emptySet(), null, genres = setOf(TonightGenre.COMEDY, TonightGenre.THRILLER)).map { it.tmdbId })
    }

    @Test
    fun aBiggerDeckLooksAtMoreMovies() {
        assertEquals(18, candidatesFor(10, emptySet()))
        assertTrue(candidatesFor(20, emptySet()) >= 30)
        assertTrue(candidatesFor(20, setOf(TonightGenre.COMEDY)) > candidatesFor(20, emptySet()))
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

    private fun card(id: Int, runtime: Int?, provider: WatchProvider?, genres: List<Int>? = null) = TonightCard(
        movie = TmdbMovie(id = id, title = "Film $id", genreIds = genres),
        posterUrl = null, year = null, runtime = runtime, director = null, genres = emptyList(),
        provider = provider, providersLink = null, reason = CardReason.Trending,
    )
}
