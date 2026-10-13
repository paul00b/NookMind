package fr.paulbr.nookmind.feature.social

import fr.paulbr.nookmind.core.data.FriendItem
import fr.paulbr.nookmind.core.data.firstNameOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class SocialFeedTest {
    private fun item(key: String, status: String, createdAt: String, finishedAt: String? = null) =
        FriendItem("u", "movie", key, "Film $key", status = status, createdAt = createdAt, finishedAt = finishedAt)

    @Test
    fun theWeekKeepsTheLastSevenDaysNewestFirst() {
        val now = Instant.parse("2026-10-11T12:00:00Z")
        val items = listOf(
            item("a", "watched", "2026-09-01T10:00:00+00:00", finishedAt = "2026-10-09"),
            item("b", "watching", "2026-10-10T10:00:00+00:00"),
            item("c", "want_to_watch", "2026-09-20T10:00:00+00:00"),
        )
        val week = weekFeed(items, now)
        assertTrue(week.thisWeek)
        // "a" was added long ago but finished two days ago: it counts, after "b".
        assertEquals(listOf("b", "a"), week.items.map { it.itemKey })
    }

    @Test
    fun aQuietWeekFallsBackOnTheLatestMoves() {
        val now = Instant.parse("2026-10-11T12:00:00Z")
        val items = listOf(item("a", "watched", "2026-08-01T10:00:00+00:00"), item("b", "watched", "2026-09-01T10:00:00+00:00"))
        val week = weekFeed(items, now)
        assertFalse(week.thisWeek)
        assertEquals(listOf("b", "a"), week.items.map { it.itemKey })
    }

    @Test
    fun filtersKeepTheKindsInChipOrder() {
        val items = listOf(
            FriendItem("u", "book", "1"),
            FriendItem("u", "movie", "2"),
            FriendItem("u", "book", "3"),
        )
        assertEquals(listOf("movie", "book"), kindsIn(items))
        assertEquals(listOf("1", "3"), items.ofKind("book").map { it.itemKey })
        assertEquals(3, items.ofKind(null).size)
    }

    @Test
    fun affinityNeedsFiveTitlesInCommonForAPercentage() {
        val mine = listOf(
            MyItem("movie", "1", 5.0), MyItem("movie", "2", 4.0), MyItem("series", "3", null),
            MyItem("book", "4", 2.0), MyItem("movie", "5", 3.0), MyItem("movie", "9", 5.0),
        ).associateBy { it.kind to it.key }
        fun rated(kind: String, key: String, rating: Double?) = FriendItem("lea", kind, key, rating = rating)
        val few = affinityOf("lea", listOf(rated("movie", "1", 5.0), rated("movie", "2", 2.0)), mine)
        assertEquals(2, few.common.size)
        assertNull(few.percent)
        assertEquals(1, few.sameRated)

        val many = affinityOf(
            "lea",
            listOf(
                rated("movie", "1", 5.0), // same: 1
                rated("movie", "2", 3.0), // one star apart: 0.75
                rated("series", "3", 4.0), // I did not rate it: 0.7
                rated("book", "4", 3.0), // one star apart: 0.75
                rated("movie", "5", null), // hidden rating: 0.7
                rated("movie", "9", 4.5), // half a star: 0.875, and "rated the same"
                rated("movie", "7", 5.0), // not mine: ignored
            ),
            mine,
        )
        assertEquals(6, many.common.size)
        assertEquals(2, many.sameRated)
        // (1 + 0.75 + 0.7 + 0.75 + 0.7 + 0.875) / 6 = 0.796
        assertEquals(80, many.percent)
    }

    @Test
    fun affinitiesSkipFriendsWithNothingInCommonAndPutTheClosestFirst() {
        val mine = (1..6).map { MyItem("movie", "$it", 4.0) }.associateBy { it.kind to it.key }
        val library = (1..5).map { FriendItem("tom", "movie", "$it", rating = 4.0) } +
            (1..5).map { FriendItem("lea", "movie", "$it", rating = 1.0) } +
            FriendItem("sam", "movie", "99")
        val list = affinities(library, listOf("sam", "lea", "tom"), mine)
        assertEquals(listOf("tom", "lea"), list.map { it.friendId })
        assertEquals(100, list[0].percent)
    }

    @Test
    fun favouritesAreTheBestRatedFirst() {
        val items = listOf(
            FriendItem("u", "movie", "a", rating = 4.5, createdAt = "2026-10-01T10:00:00Z"),
            FriendItem("u", "movie", "b", rating = 5.0, createdAt = "2026-09-01T10:00:00Z"),
            FriendItem("u", "movie", "c", rating = 4.0, createdAt = "2026-10-05T10:00:00Z"),
            FriendItem("u", "movie", "d", rating = 4.5, createdAt = "2026-10-03T10:00:00Z"),
        )
        assertEquals(listOf("b", "d", "a"), favoritesOf(items).map { it.itemKey })
    }

    @Test
    fun newsIsWhatMovedSinceTheLastVisit() {
        assertTrue(hasNews("2026-10-11T10:00:00Z", "2026-10-10T10:00:00Z"))
        assertFalse(hasNews("2026-10-09T10:00:00Z", "2026-10-10T10:00:00Z"))
        assertFalse(hasNews("2026-10-11T10:00:00Z", null), "first visit: nothing is new")
        assertTrue(hasNews("2026-10-11", "2026-10-10T10:00:00Z"), "a watch date counts at noon")
    }

    @Test
    fun agoPicksTheLargestUnit() {
        val now = Instant.parse("2026-10-11T12:00:00Z")
        assertEquals(AgoUnit.NOW to 0, agoOf("2026-10-11T11:59:40Z", now))
        assertEquals(AgoUnit.MINUTES to 5, agoOf("2026-10-11T11:55:00Z", now))
        assertEquals(AgoUnit.HOURS to 3, agoOf("2026-10-11T08:59:00.123456+00:00", now))
        assertEquals(AgoUnit.DAYS to 2, agoOf("2026-10-09T10:00:00Z", now))
        assertNull(agoOf("2026-06-01T10:00:00Z", now), "too old to say")
        assertNull(agoOf("not a date", now))
    }

    @Test
    fun firstNamesAreTheFirstWord() {
        assertEquals("Léa", firstNameOf("  Léa Martin "))
        assertEquals("", firstNameOf(""))
    }
}
