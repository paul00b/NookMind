package fr.paulbr.nookmind.feature.social

import fr.paulbr.nookmind.core.data.FriendItem
import kotlin.time.Clock
import kotlin.time.Instant

/** What friends are in the middle of: a series being watched, a book being read. */
private val IN_PROGRESS = setOf("watching", "reading")

/** When an item last moved: added, or finished on a later day. */
internal fun activityOf(item: FriendItem): String = maxOf(item.createdAt, item.finishedAt.orEmpty())

/** "En ce moment": what friends are watching or reading, the latest first. */
fun nowPlaying(items: List<FriendItem>, limit: Int = 12): List<FriendItem> =
    items.filter { it.status in IN_PROGRESS }.sortedByDescending { it.createdAt }.take(limit)

/** The salon: everything else friends added or finished, newest activity first. */
fun salonFeed(items: List<FriendItem>, limit: Int = 30): List<FriendItem> =
    items.filter { it.status !in IN_PROGRESS }.sortedByDescending(::activityOf).take(limit)

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
