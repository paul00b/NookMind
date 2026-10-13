package fr.paulbr.nookmind.feature.social

import fr.paulbr.nookmind.core.data.FriendItem
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** When an item last moved: added, or finished on a later day. */
internal fun activityOf(item: FriendItem): String = maxOf(item.createdAt, item.finishedAt.orEmpty())

enum class AgoUnit { NOW, MINUTES, HOURS, DAYS }

/** How long ago [iso] was, in the largest unit that fits; null when unreadable or over two months. */
fun agoOf(iso: String, now: Instant = Clock.System.now()): Pair<AgoUnit, Int>? {
    val then = runCatching { Instant.parse(iso) }.getOrNull() ?: return null
    val minutes = (now - then).inWholeMinutes
    return when {
        minutes < 1 -> AgoUnit.NOW to 0
        minutes < 60 -> AgoUnit.MINUTES to minutes.toInt()
        minutes < 24 * 60 -> AgoUnit.HOURS to (minutes / 60).toInt()
        minutes < 60L * 24 * 60 -> AgoUnit.DAYS to (minutes / (24 * 60)).toInt()
        else -> null
    }
}

/** [iso] as an instant: a timestamp, or a day ("2026-10-09", a movie's watch date) taken at noon UTC. */
internal fun instantOf(iso: String): Instant? =
    runCatching { Instant.parse(if (iso.length == 10) "${iso}T12:00:00Z" else iso) }.getOrNull()

// ── Cette semaine ──────────────────────────────────────────────────────────

/** "Cette semaine", or the latest moves under another title when the week was quiet. */
data class WeekFeed(val thisWeek: Boolean, val items: List<FriendItem>)

/** What friends did in the last seven days, newest first; the latest moves otherwise. */
fun weekFeed(items: List<FriendItem>, now: Instant = Clock.System.now(), limit: Int = 30, fallback: Int = 12): WeekFeed {
    val sorted = items.sortedByDescending(::activityOf)
    val week = sorted.filter { item -> instantOf(activityOf(item))?.let { now - it <= 7.days } == true }
    return if (week.isNotEmpty()) WeekFeed(true, week.take(limit)) else WeekFeed(false, sorted.take(fallback))
}

/** The filter over movies, series and books: null is everything. */
fun List<FriendItem>.ofKind(kind: String?): List<FriendItem> = if (kind == null) this else filter { it.kind == kind }

/** The kinds present in [items], in the order of the filter chips. */
fun kindsIn(items: List<FriendItem>): List<String> = listOf("movie", "series", "book").filter { k -> items.any { it.kind == k } }

// ── Profiles ───────────────────────────────────────────────────────────────

/** One of my movies, series or books, comparable to a friend's items by (kind, key). */
data class MyItem(val kind: String, val key: String, val rating: Double?)

/** Below this many titles in common, a percentage says nothing: only the count shows. */
const val AFFINITY_MIN_COMMON = 5

/** How close a friend's tastes are to mine, from the titles we both have. */
data class Affinity(
    val friendId: String,
    /** Their side of the titles we share, their latest moves first. */
    val common: List<FriendItem>,
    /** Titles we both rated within half a star. */
    val sameRated: Int,
    /** 0..100, or null under [AFFINITY_MIN_COMMON] titles in common. */
    val percent: Int?,
)

/**
 * Each title in common scores 1 for the same rating, down to 0 four stars apart; a title one of us
 * has not rated (or hides the rating of) counts 0.7, sharing it being a sign already.
 */
fun affinityOf(friendId: String, theirs: List<FriendItem>, mine: Map<Pair<String, String>, MyItem>): Affinity {
    val pairs = theirs.sortedByDescending(::activityOf).mapNotNull { item -> mine[item.kind to item.itemKey]?.let { item to it } }
    val scores = pairs.map { (their, my) ->
        val a = their.rating
        val b = my.rating
        if (a != null && b != null && a > 0 && b > 0) (1 - abs(a - b) / 4).coerceIn(0.0, 1.0) else 0.7
    }
    val same = pairs.count { (their, my) ->
        val a = their.rating
        val b = my.rating
        a != null && b != null && abs(a - b) <= 0.5
    }
    val percent = if (pairs.size >= AFFINITY_MIN_COMMON) (scores.average() * 100).roundToInt() else null
    return Affinity(friendId, pairs.map { it.first }, same, percent)
}

/** Every friend with at least one title in common, the closest first. */
fun affinities(library: List<FriendItem>, friendIds: List<String>, mine: Map<Pair<String, String>, MyItem>): List<Affinity> =
    friendIds.map { id -> affinityOf(id, library.filter { it.owner == id }, mine) }
        .filter { it.common.isNotEmpty() }
        .sortedWith(compareByDescending<Affinity> { it.percent ?: -1 }.thenByDescending { it.common.size })

/** A friend's favourites: what they rated 4.5 or 5, the best and latest first. */
fun favoritesOf(items: List<FriendItem>, limit: Int = 4): List<FriendItem> =
    items.filter { (it.rating ?: 0.0) >= 4.5 }
        .sortedWith(compareByDescending<FriendItem> { it.rating }.thenByDescending(::activityOf))
        .take(limit)

/** When a friend last added or finished something, to order them and to tell what is new. */
fun latestActivityOf(items: List<FriendItem>, friendId: String): String? =
    items.filter { it.owner == friendId }.maxOfOrNull(::activityOf)

/** Whether a friend moved since [seenAt], the last visit of the Social tab (none: nothing is new). */
fun hasNews(latest: String?, seenAt: String?): Boolean {
    val then = seenAt?.let(::instantOf) ?: return false
    val last = latest?.let(::instantOf) ?: return false
    return last > then
}
