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
    data class PartnerWatchlist(val name: String) : CardReason
    data object BothWatchlists : CardReason
    data class BecauseYouLiked(val title: String) : CardReason
    data class PartnerLiked(val name: String, val title: String) : CardReason
    data object Trending : CardReason
    /** A favourite of the genres picked in the setup. */
    data object Popular : CardReason
}

/** What a deck is made of: movies, or series. */
enum class TonightKind(val key: String) {
    MOVIE("movie"),
    SERIES("series"),
    ;

    companion object {
        fun fromKey(key: String?): TonightKind? = entries.firstOrNull { it.key == key }
    }
}

/** How much of a series the players are up for: a mini-series, three seasons at most, or anything. */
enum class SeriesLength(val key: String) {
    MINI("mini"),
    SHORT("short"),
    ANY("any"),
    ;

    companion object {
        fun fromKey(key: String?): SeriesLength = entries.firstOrNull { it.key == key } ?: ANY
    }
}

/**
 * One card of the deck: a movie or a series, where to watch it, and why it was picked. A series
 * travels in [movie] too (its name as the title, its first air date as the release date); then
 * [runtime] is the length of an episode, [director] its creator, and [seasons], [ended] and
 * [miniseries] say how long it runs.
 */
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
    val kind: TonightKind = TonightKind.MOVIE,
    val seasons: Int? = null,
    val ended: Boolean? = null,
    val miniseries: Boolean = false,
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
 * The moods the host can pick before dealing, each one a few TMDB genres: [tmdbIds] for movies,
 * [tvIds] for series, which TMDB sorts differently (no horror nor romance there).
 */
enum class TonightGenre(val key: String, val tmdbIds: Set<Int>, val tvIds: Set<Int>) {
    ACTION("action", setOf(28, 12), setOf(10759)),
    COMEDY("comedy", setOf(35), setOf(35)),
    DRAMA("drama", setOf(18), setOf(18)),
    THRILLER("thriller", setOf(53, 80, 9648), setOf(80, 9648)),
    SCIFI("scifi", setOf(878, 14), setOf(10765)),
    HORROR("horror", setOf(27), emptySet()),
    ROMANCE("romance", setOf(10749), emptySet()),
    ANIMATION("animation", setOf(16, 10751), setOf(16, 10751, 10762)),
    DOCUMENTARY("documentary", setOf(99), setOf(99)),
    ;

    fun idsFor(kind: TonightKind): Set<Int> = if (kind == TonightKind.SERIES) tvIds else tmdbIds

    companion object {
        fun fromKeys(keys: Set<String>): Set<TonightGenre> = entries.filter { it.key in keys }.toSet()

        /** The moods offered for [kind]. */
        fun offeredFor(kind: TonightKind): List<TonightGenre> = entries.filter { it.idsFor(kind).isNotEmpty() }
    }
}

/** Whether a title of [genreIds] fits the picked [genres] (any of them; nothing picked fits all). */
fun matchesGenres(genreIds: Collection<Int>, genres: Set<TonightGenre>, kind: TonightKind = TonightKind.MOVIE): Boolean =
    genres.isEmpty() || genres.any { genre -> genre.idsFor(kind).any { it in genreIds } }

/** Whether a series card fits [length]: a mini-series is one season (or TMDB says so), short is three at most. */
fun fitsLength(card: TonightCard, length: SeriesLength): Boolean = when (length) {
    SeriesLength.ANY -> true
    SeriesLength.MINI -> card.miniseries || card.seasons == 1
    SeriesLength.SHORT -> (card.seasons ?: 1) <= 3
}

/** The TMDB genres of a card: from its details, or from the list it came from. */
val TonightCard.genreIds: List<Int> get() = movie.genres?.map { it.id }?.takeIf { it.isNotEmpty() } ?: movie.genreIds.orEmpty()

/** The two deck sizes on offer. */
val DECK_SIZES = listOf(10, 20)

/**
 * The [size] cards of a round, from [candidates] already in order of preference: titles on a ticked
 * service come first, then the rest; [maxRuntime] drops the long movies (an unknown length stays),
 * [length] the series too long, [genres] the ones of another kind. The ids in [later] (series one
 * of the players has started) come after all the others.
 */
fun pickDeck(
    candidates: List<TonightCard>,
    services: Set<StreamingService>,
    maxRuntime: Int?,
    size: Int = DECK_SIZE,
    genres: Set<TonightGenre> = emptySet(),
    kind: TonightKind = TonightKind.MOVIE,
    length: SeriesLength = SeriesLength.ANY,
    later: Set<Int> = emptySet(),
): List<TonightCard> {
    val fitting = candidates.filter { card ->
        (maxRuntime == null || (card.runtime ?: 0) <= maxRuntime) &&
            matchesGenres(card.genreIds, genres, kind) &&
            (kind != TonightKind.SERIES || fitsLength(card, length))
    }
    val (fresh, started) = fitting.partition { it.tmdbId !in later }
    fun ranked(cards: List<TonightCard>): List<TonightCard> {
        if (services.isEmpty()) return cards
        val (available, elsewhere) = cards.partition { card -> card.provider?.let { p -> services.any { it.matches(p) } } == true }
        return available + elsewhere
    }
    return (ranked(fresh) + ranked(started)).take(size)
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
    /** One movie both said yes to, or the one they loved more than the others. */
    data class Match(val index: Int) : Outcome
    /** Several shared yeses loved as much: the two best play a final. */
    data class Final(val a: Int, val b: Int) : Outcome
    /** No shared yes, but movies one of them wanted: the two best play extra time. */
    data class Maybes(val a: Int, val b: Int) : Outcome
    data class Single(val index: Int) : Outcome
    data object None : Outcome
}

/**
 * Favourites first, then the order of the deck. Among the movies both said yes to, a shared
 * favourite beats a single one, which beats a plain yes: the one loved most wins outright, and
 * only a tie at the top goes to a final.
 */
fun outcomeOf(size: Int, mine: Map<Int, Verdict>, theirs: Map<Int, Verdict>): Outcome {
    fun yes(verdicts: Map<Int, Verdict>, i: Int) = verdicts[i]?.isYes == true
    fun loves(i: Int) = (if (mine[i] == Verdict.LOVE) 1 else 0) + (if (theirs[i] == Verdict.LOVE) 1 else 0)
    val order = compareByDescending<Int> { loves(it) }.thenBy { it }
    val both = (0 until size).filter { yes(mine, it) && yes(theirs, it) }.sortedWith(order)
    if (both.size == 1) return Outcome.Match(both[0])
    if (both.size >= 2) return if (loves(both[0]) > loves(both[1])) Outcome.Match(both[0]) else Outcome.Final(both[0], both[1])
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
