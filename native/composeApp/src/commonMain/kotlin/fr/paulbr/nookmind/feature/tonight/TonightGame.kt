package fr.paulbr.nookmind.feature.tonight

import fr.paulbr.nookmind.core.model.TmdbMovie
import fr.paulbr.nookmind.core.model.WatchProvider

/** What a player says about a card. A favourite counts as a yes, and wins a tie. */
enum class Verdict {
    YES, NO, LOVE, SEEN;

    val isYes: Boolean get() = this == YES || this == LOVE
}

enum class Player { ME, PARTNER }

/** Why a movie is in the deck, shown on its card. */
sealed interface CardReason {
    data object Watchlist : CardReason
    data class BecauseYouLiked(val title: String) : CardReason
    data object Trending : CardReason
}

/** One card of the deck: a movie, where to watch it, and why it was picked. */
data class TonightCard(
    val movie: TmdbMovie,
    val posterUrl: String?,
    val year: String?,
    val runtime: Int?,
    val director: String?,
    val genres: List<String>,
    val provider: WatchProvider?,
    val providersLink: String?,
    val reason: CardReason,
) {
    val tmdbId: Int get() = movie.id
    val title: String get() = movie.title
}

/** The streaming services offered in the setup, recognised by name since their ids change by country. */
enum class StreamingService(val key: String, val label: String, private val names: List<String>) {
    NETFLIX("netflix", "Netflix", listOf("netflix")),
    PRIME("prime", "Prime Video", listOf("prime video")),
    DISNEY("disney", "Disney+", listOf("disney")),
    CANAL("canal", "Canal+", listOf("canal")),
    MAX("max", "Max", listOf("max")),
    APPLE("apple", "Apple TV+", listOf("apple tv")),
    PARAMOUNT("paramount", "Paramount+", listOf("paramount")),
    ;

    fun matches(provider: WatchProvider): Boolean {
        val name = provider.providerName.lowercase()
        return names.any { name.contains(it) }
    }

    companion object {
        fun fromKeys(keys: Set<String>): Set<StreamingService> = entries.filter { it.key in keys }.toSet()
    }
}

/** Short label of a provider for a card ("Amazon Prime Video" reads "Prime Video"). */
fun providerLabel(provider: WatchProvider): String =
    StreamingService.entries.firstOrNull { it.matches(provider) }?.label ?: provider.providerName

/** The provider to show on a card: one of the ticked services first, else the first one streaming it. */
fun pickProvider(providers: List<WatchProvider>, services: Set<StreamingService>): WatchProvider? =
    providers.firstOrNull { p -> services.any { it.matches(p) } } ?: providers.firstOrNull()

/**
 * The [size] cards of a round, from [candidates] already in order of preference: films on a ticked
 * service come first, then the rest; [maxRuntime] drops the long ones (an unknown length stays).
 */
fun pickDeck(candidates: List<TonightCard>, services: Set<StreamingService>, maxRuntime: Int?, size: Int = DECK_SIZE): List<TonightCard> {
    val fitting = candidates.filter { card -> maxRuntime == null || (card.runtime ?: 0) <= maxRuntime }
    if (services.isEmpty()) return fitting.take(size)
    val (available, elsewhere) = fitting.partition { card -> card.provider?.let { p -> services.any { it.matches(p) } } == true }
    return (available + elsewhere).take(size)
}

const val DECK_SIZE = 10

/**
 * The verdict a released drag stands for, or null to put the card back: far enough along one axis
 * ([threshold]), or flicked fast enough ([flingVelocity], per second) after at least [minFling].
 * Down asks a little more than the rest, so a lazy thumb does not mark a movie as seen.
 */
fun verdictOf(dx: Float, dy: Float, vx: Float, vy: Float, threshold: Float, minFling: Float, flingVelocity: Float): Verdict? {
    if (kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
        if (dy < -threshold || (vy < -flingVelocity && dy < -minFling)) return Verdict.LOVE
        if (dy > threshold * 1.15f || (vy > flingVelocity && dy > minFling)) return Verdict.SEEN
    } else {
        if (dx > threshold || (vx > flingVelocity && dx > minFling)) return Verdict.YES
        if (dx < -threshold || (vx < -flingVelocity && dx < -minFling)) return Verdict.NO
    }
    return null
}

/** Where a round ends up once both players have been through the deck. */
sealed interface Outcome {
    /** One movie both said yes to. */
    data class Match(val index: Int) : Outcome
    /** Several shared yeses: the two best play a final. */
    data class Final(val a: Int, val b: Int) : Outcome
    /** No shared yes, but movies one of them wanted: the two best play extra time. */
    data class Maybes(val a: Int, val b: Int) : Outcome
    data class Single(val index: Int) : Outcome
    data object None : Outcome
}

/** Favourites first, then the order of the deck. */
fun outcomeOf(size: Int, mine: Map<Int, Verdict>, theirs: Map<Int, Verdict>): Outcome {
    fun yes(verdicts: Map<Int, Verdict>, i: Int) = verdicts[i]?.isYes == true
    fun loves(i: Int) = (if (mine[i] == Verdict.LOVE) 1 else 0) + (if (theirs[i] == Verdict.LOVE) 1 else 0)
    val order = compareByDescending<Int> { loves(it) }.thenBy { it }
    val both = (0 until size).filter { yes(mine, it) && yes(theirs, it) }.sortedWith(order)
    if (both.size == 1) return Outcome.Match(both[0])
    if (both.size >= 2) return Outcome.Final(both[0], both[1])
    val maybes = (0 until size).filter { yes(mine, it) != yes(theirs, it) }.sortedWith(order)
    return when {
        maybes.size >= 2 -> Outcome.Maybes(maybes[0], maybes[1])
        maybes.size == 1 -> Outcome.Single(maybes[0])
        else -> Outcome.None
    }
}

/** "2 h 28", "54 min", or null. */
fun formatRuntime(minutes: Int?): String? = when {
    minutes == null || minutes <= 0 -> null
    minutes < 60 -> "$minutes min"
    else -> "${minutes / 60} h ${(minutes % 60).toString().padStart(2, '0')}"
}

/** Initial of a name for an avatar, "?" when blank. */
fun initialOf(name: String): String = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
