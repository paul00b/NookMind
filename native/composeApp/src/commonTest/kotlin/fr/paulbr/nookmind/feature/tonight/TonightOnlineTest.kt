package fr.paulbr.nookmind.feature.tonight

import fr.paulbr.nookmind.core.data.FriendItem
import fr.paulbr.nookmind.core.model.TmdbMovie
import fr.paulbr.nookmind.core.model.WatchProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TonightOnlineTest {

    @Test
    fun watchlistsForTwoPutSharedMoviesFirstThenTakeTurns() {
        val merged = mergeWatchlists(
            mine = listOf(1, 2, 3, 4),
            theirs = listOf(9, 3, 8, 7),
            mySeen = emptySet(),
            theirSeen = emptySet(),
            partnerName = "Léa",
            limit = 6,
        )
        assertEquals(listOf(3, 1, 9, 2, 8, 4), merged.map { it.first })
        assertEquals(CardReason.BothWatchlists, merged[0].second)
        assertEquals(CardReason.Watchlist, merged[1].second)
        assertEquals(CardReason.PartnerWatchlist("Léa"), merged[2].second)
    }

    @Test
    fun aMovieTheOtherOneHasSeenStaysOut() {
        val merged = mergeWatchlists(listOf(1, 2), listOf(5, 6), mySeen = setOf(5), theirSeen = setOf(2), partnerName = "Tom")
        assertEquals(listOf(1, 6), merged.map { it.first })
    }

    @Test
    fun partnerTasteReadsTheirSharedMovies() {
        val items = listOf(
            FriendItem("u", "movie", "10", "Ten", status = "want_to_watch", createdAt = "2026-10-01"),
            FriendItem("u", "movie", "11", "Eleven", status = "watched", rating = 3.0, createdAt = "2026-10-02"),
            FriendItem("u", "movie", "12", "Twelve", status = "watched", rating = 5.0, createdAt = "2026-09-01"),
            FriendItem("u", "series", "13", "A series", status = "watched", rating = 5.0, createdAt = "2026-10-03"),
            FriendItem("u", "movie", "not-an-id", "Broken", status = "watched", createdAt = "2026-10-04"),
        )
        val taste = partnerTasteOf("Léa", items)
        assertEquals(listOf(10), taste.watchlist)
        assertEquals(setOf(11, 12), taste.watched)
        assertEquals(setOf(10, 11, 12), taste.library)
        assertEquals(listOf(12 to "Twelve", 11 to "Eleven"), taste.favourites)
    }

    @Test
    fun theSharedDeckSaysReasonsFromEachSide() {
        val cards = listOf(
            card(1, CardReason.Watchlist),
            card(2, CardReason.PartnerWatchlist("Léa")),
            card(3, CardReason.BothWatchlists),
            card(4, CardReason.BecauseYouLiked("Heat")),
            card(5, CardReason.PartnerLiked("Léa", "Amélie")),
            card(6, CardReason.Trending),
        )
        val deck = encodeDeck(cards, myId = "paul", partnerId = "lea")

        // On Paul's phone, as dealt.
        assertEquals(cards.map { it.reason }, decodeDeck(deck, myId = "paul", partnerName = "Léa").map { it.reason })

        // On Léa's phone, mine and theirs swap.
        val hers = decodeDeck(deck, myId = "lea", partnerName = "Paul")
        assertEquals(
            listOf(
                CardReason.PartnerWatchlist("Paul"),
                CardReason.Watchlist,
                CardReason.BothWatchlists,
                CardReason.PartnerLiked("Paul", "Heat"),
                CardReason.BecauseYouLiked("Amélie"),
                CardReason.Trending,
            ),
            hers.map { it.reason },
        )
        val first = hers.first()
        assertEquals("Film 1", first.title)
        assertEquals("/p1.jpg", first.movie.posterPath)
        assertEquals("Netflix", first.provider?.providerName)
        assertEquals("Someone", first.director)
        assertEquals(124, first.runtime)
    }

    @Test
    fun aBrokenDeckDecodesToNothing() {
        assertTrue(decodeDeck(kotlinx.serialization.json.JsonPrimitive("nope"), "paul", "Léa").isEmpty())
    }

    @Test
    fun verdictsAndWaysTravelAsKeys() {
        Verdict.entries.forEach { assertEquals(it, verdictOfKey(it.key)) }
        assertNull(verdictOfKey("maybe"))
        assertEquals(MatchVia.SHARED_LOVE, matchViaOf("shared_love"))
        assertEquals(MatchVia.CHANCE, matchViaOf("chance"))
        assertEquals(MatchVia.MATCH, matchViaOf(null))
    }

    @Test
    fun onlineEachPlayerWaitsForTheOtherThenTheVerdictsAreRevealed() {
        val sent = mutableListOf<String>()
        val state = TonightState("Léa").apply {
            remote = TonightRemote("s", isHost = true, partnerId = "lea")
            sync = object : TonightSync {
                override fun vote(card: Int, verdict: Verdict) { sent += "vote $card ${verdict.key}" }
                override fun undo(card: Int) { sent += "undo $card" }
            }
        }
        state.startRound(listOf(card(1), card(2), card(3)))
        state.decide(Verdict.YES)
        state.decide(Verdict.NO)
        state.undo()
        state.decide(Verdict.LOVE)
        state.decide(Verdict.NO)
        assertEquals(TonightStep.WAITING, state.step, "online, no hand-off")
        assertEquals(listOf("vote 0 yes", "vote 1 no", "undo 1", "vote 1 love", "vote 2 no"), sent)

        // Taking the last card back leaves the waiting room.
        state.undo()
        assertEquals(TonightStep.SWIPE, state.step)
        state.decide(Verdict.NO)

        state.reveal(mapOf(0 to Verdict.NO, 1 to Verdict.NO, 2 to Verdict.YES))
        assertEquals(TonightStep.DUEL, state.step)
        assertEquals(Outcome.Maybes(1, 0), state.outcome)
    }

    @Test
    fun aRevealWithASharedYesStillEndsOnTheTicket() {
        val state = TonightState("Léa").apply { remote = TonightRemote("s", isHost = false, partnerId = "paul") }
        state.startRound(listOf(card(1), card(2)))
        state.decide(Verdict.LOVE)
        state.decide(Verdict.NO)
        state.reveal(mapOf(0 to Verdict.LOVE, 1 to Verdict.NO))
        assertEquals(TonightStep.MATCH, state.step)
        assertEquals(MatchVia.SHARED_LOVE, state.via)
        assertEquals(0, state.winner)
    }

    private fun card(id: Int, reason: CardReason = CardReason.Trending) = TonightCard(
        movie = TmdbMovie(id = id, title = "Film $id", posterPath = "/p$id.jpg"),
        posterUrl = "https://image.tmdb.org/t/p/w780/p$id.jpg", year = "2001", runtime = 124, director = "Someone", genres = listOf("Drama"),
        provider = WatchProvider(8, "Netflix", "/n.png"), providersLink = "https://www.themoviedb.org/movie/$id/watch", reason = reason,
    )
}
