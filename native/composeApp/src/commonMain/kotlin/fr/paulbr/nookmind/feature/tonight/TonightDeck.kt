package fr.paulbr.nookmind.feature.tonight

import fr.paulbr.nookmind.app.AppContainer
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

/**
 * Deals a round from the library: movies of the "To watch" list first, then TMDB recommendations
 * from the movies rated best, then what is trending. Nothing already watched comes up, and nothing
 * in [skip] (the cards of the round before, when dealing again).
 */
suspend fun dealTonightDeck(
    container: AppContainer,
    services: Set<StreamingService>,
    shortOnly: Boolean,
    skip: Set<Int> = emptySet(),
): List<TonightCard> = coroutineScope {
    val library = container.movies.items.value
    val inLibrary = library.mapNotNull { it.tmdbId }.toSet()
    val watchlist = library.filter { it.status == MovieStatus.WANT_TO_WATCH && it.tmdbId != null && it.tmdbId !in skip }.shuffled().take(8)
    val seeds = library.filter { it.status == MovieStatus.WATCHED && it.tmdbId != null }
        .sortedWith(compareByDescending<Movie> { it.rating ?: 0.0 }.thenByDescending { it.watchedDate ?: it.createdAt })
        .take(6).shuffled().take(3)

    val candidates = LinkedHashMap<Int, Pair<TmdbMovie?, CardReason>>()
    watchlist.forEach { candidates[it.tmdbId!!] = null to CardReason.Watchlist }

    // Recommendations of each seed, taken in turn so one movie does not fill the deck.
    val recommended = seeds.map { seed -> async { seed.title to container.tmdb.fetchMovieRecommendations(seed.tmdbId!!) } }.awaitAll()
        .map { (title, list) -> list.filter { it.id !in inLibrary && it.id !in skip && !it.posterPath.isNullOrBlank() }.map { it to CardReason.BecauseYouLiked(title) } }
    var rank = 0
    while (candidates.size < CANDIDATES && recommended.any { rank < it.size }) {
        recommended.forEach { list -> list.getOrNull(rank)?.let { (movie, reason) -> if (movie.id !in candidates) candidates[movie.id] = movie to reason } }
        rank++
    }
    if (candidates.size < CANDIDATES) {
        container.tmdb.fetchTrendingMovies(20)
            .filter { it.id !in inLibrary && it.id !in skip && !it.posterPath.isNullOrBlank() }
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
