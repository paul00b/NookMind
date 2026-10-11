package fr.paulbr.nookmind.feature.social

import fr.paulbr.nookmind.core.data.FriendItem
import fr.paulbr.nookmind.core.data.firstNameOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class SocialFeedTest {
    private fun item(key: String, status: String, createdAt: String, finishedAt: String? = null) =
        FriendItem("u", "movie", key, "Film $key", status = status, createdAt = createdAt, finishedAt = finishedAt)

    @Test
    fun inProgressItemsGoToRightNowAndTheRestToTheSalon() {
        val items = listOf(
            item("a", "watched", "2026-10-01T10:00:00+00:00", finishedAt = "2026-10-09"),
            item("b", "watching", "2026-10-05T10:00:00+00:00"),
            item("c", "want_to_watch", "2026-10-08T10:00:00+00:00"),
            item("d", "reading", "2026-10-02T10:00:00+00:00"),
        )
        assertEquals(listOf("b", "d"), nowPlaying(items).map { it.itemKey })
        // "a" was finished after "c" was added: the latest move comes first.
        assertEquals(listOf("a", "c"), salonFeed(items).map { it.itemKey })
        assertEquals(1, salonFeed(items, limit = 1).size)
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
