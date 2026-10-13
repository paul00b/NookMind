package fr.paulbr.nookmind.feature.tonight

import fr.paulbr.nookmind.core.data.FriendItem
import fr.paulbr.nookmind.core.model.TmdbMovie
import fr.paulbr.nookmind.core.model.WatchProvider
import fr.paulbr.nookmind.core.network.AppJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TonightSeriesTest {

    @Test
    fun theLengthFilterKeepsMiniSeriesOrThreeSeasonsAtMost() {
        val mini = series(1, seasons = 1, miniseries = true)
        val one = series(2, seasons = 1)
        val three = series(3, seasons = 3)
        val long = series(4, seasons = 7)
        val all = listOf(mini, one, three, long)
        assertEquals(listOf(1, 2), pick(all, SeriesLength.MINI))
        assertEquals(listOf(1, 2, 3), pick(all, SeriesLength.SHORT))
        assertEquals(listOf(1, 2, 3, 4), pick(all, SeriesLength.ANY))
    }

    @Test
    fun seriesUnderWayComeAfterTheNewOnes() {
        val cards = listOf(series(1, seasons = 2), series(2, seasons = 2), series(3, seasons = 2))
        val deck = pickDeck(cards, emptySet(), null, kind = TonightKind.SERIES, later = setOf(1))
        assertEquals(listOf(2, 3, 1), deck.map { it.tmdbId })
    }

    @Test
    fun seriesMoodsUseTheTvGenresAndLeaveOutHorrorAndRomance() {
        assertTrue(matchesGenres(listOf(80), setOf(TonightGenre.THRILLER), TonightKind.SERIES), "crime reads as thriller")
        assertTrue(matchesGenres(listOf(10765), setOf(TonightGenre.SCIFI), TonightKind.SERIES))
        assertFalse(matchesGenres(listOf(878), setOf(TonightGenre.SCIFI), TonightKind.SERIES), "a movie genre id is not a series one")
        val offered = TonightGenre.offeredFor(TonightKind.SERIES)
        assertFalse(TonightGenre.HORROR in offered)
        assertFalse(TonightGenre.ROMANCE in offered)
        assertEquals(TonightGenre.entries, TonightGenre.offeredFor(TonightKind.MOVIE))
    }

    @Test
    fun switchingToSeriesDropsTheMoodsSeriesDoNotHave() {
        val state = TonightState()
        state.genres = setOf(TonightGenre.HORROR, TonightGenre.COMEDY)
        state.switchKind(TonightKind.SERIES)
        assertEquals(setOf(TonightGenre.COMEDY), state.genres)
        assertEquals(TonightKind.SERIES, state.kind)
    }

    @Test
    fun aSeriesDeckTravelsByNameAndComesBackAsSeries() {
        val deck = encodeDeck(listOf(series(1396, seasons = 5).copy(ended = true)), myId = "paul", partnerId = "lea")
        val shared = deck[0] as JsonObject
        assertEquals(JsonPrimitive("series"), shared["kind"])
        assertEquals(JsonPrimitive("Série 1396"), shared["name"])
        assertFalse("title" in shared, "no title: the versions before series cannot read it")

        val back = decodeDeck(deck, myId = "lea", partnerName = "Paul").single()
        assertEquals(TonightKind.SERIES, back.kind)
        assertEquals("Série 1396", back.title)
        assertEquals(5, back.seasons)
        assertEquals(true, back.ended)
        assertFalse(needsUpdate(deck))
    }

    @Test
    fun theVersionBeforeSeriesCannotReadASeriesDeck() {
        val deck = encodeDeck(listOf(series(1, seasons = 1)), myId = "paul", partnerId = "lea")
        // What 2.0.11 decoded a deck with: a required title.
        assertFailsWith<Exception> { AppJson.decodeFromJsonElement(ListSerializer(OldSharedCard.serializer()), deck) }
        // Movie decks still read as before.
        val movies = encodeDeck(listOf(series(2, seasons = null).copy(kind = TonightKind.MOVIE)), myId = "paul", partnerId = "lea")
        assertEquals("Série 2", AppJson.decodeFromJsonElement(ListSerializer(OldSharedCard.serializer()), movies).single().title)
    }

    @Test
    fun aDeckOfAnUnknownKindAsksForAnUpdate() {
        val deck: JsonArray = buildJsonArray {
            add(buildJsonObject { put("id", 1); put("name", "Un livre"); put("kind", "book") })
        }
        assertTrue(needsUpdate(deck))
        assertTrue(decodeDeck(deck, "paul", "Léa").isEmpty())
    }

    @Test
    fun aFriendsSeriesTasteCountsWhatTheyAreWatching() {
        val items = listOf(
            FriendItem("lea", "series", "1", "A", status = "want_to_watch"),
            FriendItem("lea", "series", "2", "B", status = "watching", rating = 4.5),
            FriendItem("lea", "series", "3", "C", status = "watched", rating = 3.0),
            FriendItem("lea", "movie", "4", "D", status = "want_to_watch"),
        )
        val taste = partnerTasteOf("Léa", items, TonightKind.SERIES)
        assertEquals(listOf(1), taste.watchlist)
        assertEquals(setOf(3), taste.watched)
        assertEquals(setOf(2), taste.started)
        assertEquals(listOf(2, 3), taste.favourites.map { it.first }, "under way and rated best first")
        assertEquals(setOf(1, 2, 3), taste.library)
    }

    @Serializable
    private data class OldSharedCard(val id: Int, val title: String)

    private fun pick(cards: List<TonightCard>, length: SeriesLength) =
        pickDeck(cards, emptySet(), null, kind = TonightKind.SERIES, length = length).map { it.tmdbId }

    private fun series(id: Int, seasons: Int?, miniseries: Boolean = false) = TonightCard(
        movie = TmdbMovie(id = id, title = "Série $id", posterPath = "/s$id.jpg"),
        posterUrl = "https://image.tmdb.org/t/p/w780/s$id.jpg", year = "2022", runtime = 50, director = "Someone", genres = listOf("Drama"),
        provider = WatchProvider(8, "Netflix", "/n.png"), providersLink = null, reason = CardReason.Trending,
        kind = TonightKind.SERIES, seasons = seasons, miniseries = miniseries,
    )
}
