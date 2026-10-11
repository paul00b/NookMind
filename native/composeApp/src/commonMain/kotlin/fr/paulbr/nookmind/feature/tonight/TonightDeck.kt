package fr.paulbr.nookmind.feature.tonight

import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.data.FriendItem
import fr.paulbr.nookmind.core.model.Movie
import fr.paulbr.nookmind.core.model.MovieStatus
import fr.paulbr.nookmind.core.model.TmdbMovie
import fr.paulbr.nookmind.core.network.TmdbApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

/** How many movies get their details fetched before the ten cards are picked among them. */
private const val CANDIDATES = 18
private const val CARD_TIMEOUT_MS = 8_000L

/** What the deck knows of the friend it is dealt for: the movies they share. */
class PartnerTaste(
    val name: String,
    /** Their "To watch" list, as TMDB ids. */
    val watchlist: List<Int>,
    /** What they have already watched. */
    val watched: Set<Int>,
    /** Every movie of their library, watched or not. */
    val library: Set<Int>,
    /** Their best rated movies, best first, as seeds for recommendations. */
    val favourites: List<Pair<Int, String>>,
)

/** The taste of a friend from what they share of their movies. */
fun partnerTasteOf(name: String, items: List<FriendItem>): PartnerTaste {
    val movies = items.filter { it.kind == "movie" }.mapNotNull { item -> item.itemKey.toIntOrNull()?.let { it to item } }
    val watched = movies.filter { it.second.status == MovieStatus.WATCHED.key }
    return PartnerTaste(
        name = name,
        watchlist = movies.filter { it.second.status == MovieStatus.WANT_TO_WATCH.key }.map { it.first },
        watched = watched.map { it.first }.toSet(),
        library = movies.map { it.first }.toSet(),
        favourites = watched
            .sortedWith(compareByDescending<Pair<Int, FriendItem>> { it.second.rating ?: 0.0 }.thenByDescending { it.second.finishedAt ?: it.second.createdAt })
            .map { it.first to it.second.title },
    )
}

/**
 * The watchlist part of a deck for two: movies on both lists first, then one of mine and one of
 * theirs in turn, up to [limit]. A movie the other one has already seen stays out.
 */
fun mergeWatchlists(mine: List<Int>, theirs: List<Int>, mySeen: Set<Int>, theirSeen: Set<Int>, partnerName: String, limit: Int = 8): List<Pair<Int, CardReason>> {
    val picked = LinkedHashMap<Int, CardReason>()
    mine.filter { it in theirs }.forEach { if (picked.size < limit) picked[it] = CardReason.BothWatchlists }
    val a = mine.filter { it !in picked && it !in theirSeen }
    val b = theirs.filter { it !in picked && it !in mySeen }
    var i = 0
    while (picked.size < limit && (i < a.size || i < b.size)) {
        a.getOrNull(i)?.let { if (picked.size < limit && it !in picked) picked[it] = CardReason.Watchlist }
        b.getOrNull(i)?.let { if (picked.size < limit && it !in picked) picked[it] = CardReason.PartnerWatchlist(partnerName) }
        i++
    }
    return picked.toList()
}

/**
 * Deals a round from the library: movies of the "To watch" list first, then TMDB recommendations
 * from the movies rated best, then what is trending. Nothing already watched comes up, and nothing
 * in [skip] (the cards of the round before, when dealing again). With a [partner], their list and
 * their favourites count as much as mine.
 */
suspend fun dealTonightDeck(
    container: AppContainer,
    services: Set<StreamingService>,
    shortOnly: Boolean,
    skip: Set<Int> = emptySet(),
    partner: PartnerTaste? = null,
): List<TonightCard> = coroutineScope {
    val library = container.movies.items.value
    val inLibrary = library.mapNotNull { it.tmdbId }.toSet()
    val myWatchlist = library.filter { it.status == MovieStatus.WANT_TO_WATCH && it.tmdbId != null && it.tmdbId !in skip }.map { it.tmdbId!! }.shuffled()
    val mySeeds = library.filter { it.status == MovieStatus.WATCHED && it.tmdbId != null }
        .sortedWith(compareByDescending<Movie> { it.rating ?: 0.0 }.thenByDescending { it.watchedDate ?: it.createdAt })
        .take(6).shuffled().take(if (partner == null) 3 else 2)
        .map { Triple(it.tmdbId!!, it.title, null as String?) }
    val theirSeeds = partner?.favourites?.take(6)?.shuffled()?.take(2)?.map { Triple(it.first, it.second, partner.name) }.orEmpty()

    val candidates = LinkedHashMap<Int, Pair<TmdbMovie?, CardReason>>()
    if (partner == null) {
        myWatchlist.take(8).forEach { candidates[it] = null to CardReason.Watchlist }
    } else {
        val myWatched = library.filter { it.status == MovieStatus.WATCHED }.mapNotNull { it.tmdbId }.toSet()
        val theirWatchlist = partner.watchlist.filter { it !in skip }.shuffled()
        mergeWatchlists(myWatchlist, theirWatchlist, myWatched, partner.watched, partner.name).forEach { (id, reason) -> candidates[id] = null to reason }
    }
    val known = inLibrary + partner?.library.orEmpty() + skip

    // Recommendations of each seed, taken in turn so one movie does not fill the deck.
    val recommended = (mySeeds + theirSeeds).map { (id, title, by) ->
        async {
            val reason = if (by == null) CardReason.BecauseYouLiked(title) else CardReason.PartnerLiked(by, title)
            container.tmdb.fetchMovieRecommendations(id).filter { it.id !in known && !it.posterPath.isNullOrBlank() }.map { it to reason }
        }
    }.awaitAll()
    var rank = 0
    while (candidates.size < CANDIDATES && recommended.any { rank < it.size }) {
        recommended.forEach { list -> list.getOrNull(rank)?.let { (movie, reason) -> if (movie.id !in candidates) candidates[movie.id] = movie to reason } }
        rank++
    }
    if (candidates.size < CANDIDATES) {
        container.tmdb.fetchTrendingMovies(20)
            .filter { it.id !in known && !it.posterPath.isNullOrBlank() }
            .forEach { if (candidates.size < CANDIDATES && it.id !in candidates) candidates[it.id] = it to CardReason.Trending }
    }

    val byTmdbId = library.filter { it.tmdbId != null }.associateBy { it.tmdbId!! }
    val cards = candidates.entries.map { (id, entry) ->
        async { withTimeoutOrNull(CARD_TIMEOUT_MS) { cardFor(container, id, entry.first, byTmdbId[id], entry.second, services) } }
    }.awaitAll().filterNotNull()
    pickDeck(cards, services, if (shortOnly) 120 else null)
}

private suspend fun cardFor(
    container: AppContainer,
    tmdbId: Int,
    listed: TmdbMovie?,
    saved: Movie?,
    reason: CardReason,
    services: Set<StreamingService>,
): TonightCard? = coroutineScope {
    val details = async { container.tmdb.fetchMovieDetails(tmdbId) }
    val providers = async { container.tmdb.fetchMovieWatchProviders(tmdbId) }
    val movie = details.await()
        ?: listed
        ?: saved?.let { TmdbMovie(id = tmdbId, title = it.title, overview = it.description.orEmpty(), releaseDate = it.releaseDate.orEmpty(), runtime = it.runtime) }
        ?: return@coroutineScope null
    val watch = providers.await()
    TonightCard(
        movie = movie,
        posterUrl = TmdbApi.posterUrl(movie.posterPath, "w780") ?: saved?.posterUrl,
        year = movie.releaseDate.take(4).takeIf { it.length == 4 } ?: saved?.releaseDate?.take(4),
        runtime = movie.runtime?.takeIf { it > 0 } ?: saved?.runtime,
        director = movie.credits?.crew?.firstOrNull { it.job == "Director" }?.name ?: saved?.director?.takeIf { it.isNotBlank() },
        genres = movie.genres?.map { it.name }.orEmpty(),
        provider = pickProvider(watch.flatrate, services),
        providersLink = watch.link,
        reason = reason,
    )
}
