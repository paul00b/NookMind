package fr.paulbr.nookmind.feature.tonight

import fr.paulbr.nookmind.core.model.TmdbMovie
import kotlin.test.Test
import kotlin.test.assertEquals

class TonightSeenTest {
    @Test
    fun aCardISwipeSeenIsToldAndTakenBackWithIt() {
        val state = TonightState()
        val seen = mutableListOf<Int>()
        val unseen = mutableListOf<Int>()
        state.onSeen = { seen += it.tmdbId }
        state.onUnseen = { unseen += it.tmdbId }
        state.startRound(listOf(movie(1), movie(2), movie(3)))

        state.decide(Verdict.YES)
        state.decide(Verdict.SEEN)
        assertEquals(listOf(2), seen)

        state.undo()
        assertEquals(listOf(2), unseen)
        state.decide(Verdict.NO)
        assertEquals(listOf(2), seen, "a no is not a seen")
    }

    @Test
    fun whatThePartnerSwipesOnMyPhoneStaysOutOfMyLibrary() {
        val state = TonightState()
        val seen = mutableListOf<Int>()
        state.onSeen = { seen += it.tmdbId }
        state.startRound(listOf(movie(1)))
        state.decide(Verdict.YES)
        state.startPartnerTurn()
        state.decide(Verdict.SEEN)
        assertEquals(emptyList(), seen)
    }

    private fun movie(id: Int) = TonightCard(
        movie = TmdbMovie(id = id, title = "Film $id", posterPath = "/m$id.jpg"),
        posterUrl = null, year = "2020", runtime = 100, director = null, genres = emptyList(),
        provider = null, providersLink = null, reason = CardReason.Trending,
    )
}
