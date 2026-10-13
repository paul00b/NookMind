package fr.paulbr.nookmind.feature.tonight

import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.data.FriendItem
import fr.paulbr.nookmind.core.model.Movie
import fr.paulbr.nookmind.core.model.MovieStatus
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesStatus
import fr.paulbr.nookmind.core.model.TmdbMovie
import fr.paulbr.nookmind.core.model.TmdbSeries
import fr.paulbr.nookmind.core.network.TmdbApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

private const val CARD_TIMEOUT_MS = 8_000L

/** How many movies get their details fetched before the [size] cards are picked among them. */
internal fun candidatesFor(size: Int, genres: Set<TonightGenre>): Int = size * 17 / 10 + 1 + if (genres.isEmpty()) 0 else 6

/** What the deck knows of the friend it is dealt for: the movies (or series) they share. */
class PartnerTaste(
    val name: String,
    /** Their "To watch" list, as TMDB ids. */
    val watchlist: List<Int>,
    /** What they have already watched. */
    val watched: Set<Int>,
    /** Every title of their library, watched or not. */
    val library: Set<Int>,
    /** Their best rated titles, best first, as seeds for recommendations. */
    val favourites: List<Pair<Int, String>>,
    /** Series they are in the middle of. */
    val started: Set<Int> = emptySet(),
)

/** The taste of a friend from what they share of their movies, or of their series for [kind]. */
fun partnerTasteOf(name: String, items: List<FriendItem>, kind: TonightKind = TonightKind.MOVIE): PartnerTaste {
    val titles = items.filter { it.kind == kind.key }.mapNotNull { item -> item.itemKey.toIntOrNull()?.let { it to item } }
    // Movies and series name their statuses alike: "watched", "want_to_watch", "watching".
    val watched = titles.filter { it.second.status == MovieStatus.WATCHED.key }
    val rated = if (kind == TonightKind.SERIES) titles.filter { it.second.status == MovieStatus.WATCHED.key || it.second.status == SeriesStatus.WATCHING.key } else watched
    return PartnerTaste(
        name = name,
        watchlist = titles.filter { it.second.status == MovieStatus.WANT_TO_WATCH.key }.map { it.first },
        watched = watched.map { it.first }.toSet(),
        library = titles.map { it.first }.toSet(),
        favourites = rated
            .sortedWith(compareByDescending<Pair<Int, FriendItem>> { it.second.rating ?: 0.0 }.thenByDescending { it.second.finishedAt ?: it.second.createdAt })
            .map { it.first to it.second.title },
        started = titles.filter { it.second.status == SeriesStatus.WATCHING.key }.map { it.first }.toSet(),
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
 * Deals a round of [size] cards from the library: movies of the "To watch" list first, then TMDB
 * recommendations from the movies rated best, then what is popular. Nothing already watched comes
 * up, and nothing in [skip] (the cards of the round before, when dealing again). With a [partner],
 * their list and their favourites count as much as mine; with [genres], only those kinds of movies.
 */
suspend fun dealTonightDeck(
    container: AppContainer,
    services: Set<StreamingService>,
    shortOnly: Boolean,
    skip: Set<Int> = emptySet(),
    partner: PartnerTaste? = null,
    size: Int = DECK_SIZE,
    genres: Set<TonightGenre> = emptySet(),
    kind: TonightKind = TonightKind.MOVIE,
    length: SeriesLength = SeriesLength.ANY,
): List<TonightCard> =
    if (kind == TonightKind.SERIES) dealSeriesDeck(container, services, length, skip, partner, size, genres)
    else dealMovieDeck(container, services, shortOnly, skip, partner, size, genres)

private suspend fun dealMovieDeck(
    container: AppContainer,
    services: Set<StreamingService>,
    shortOnly: Boolean,
    skip: Set<Int>,
    partner: PartnerTaste?,
    size: Int,
    genres: Set<TonightGenre>,
): List<TonightCard> = coroutineScope {
    val wanted = candidatesFor(size, genres)
    val big = size > DECK_SIZE
    val library = container.movies.items.value
    val inLibrary = library.mapNotNull { it.tmdbId }.toSet()
    val myWatchlist = library.filter { it.status == MovieStatus.WANT_TO_WATCH && it.tmdbId != null && it.tmdbId !in skip }.map { it.tmdbId!! }.shuffled()
    val mySeeds = library.filter { it.status == MovieStatus.WATCHED && it.tmdbId != null }
        .sortedWith(compareByDescending<Movie> { it.rating ?: 0.0 }.thenByDescending { it.watchedDate ?: it.createdAt })
        .take(if (big) 10 else 6).shuffled().take(if (partner == null) (if (big) 5 else 3) else (if (big) 3 else 2))
        .map { Triple(it.tmdbId!!, it.title, null as String?) }
    val theirSeeds = partner?.favourites?.take(if (big) 10 else 6)?.shuffled()?.take(if (big) 3 else 2)?.map { Triple(it.first, it.second, partner.name) }.orEmpty()

    val candidates = LinkedHashMap<Int, Pair<TmdbMovie?, CardReason>>()
    val watchlistLimit = if (big) 14 else 8
    if (partner == null) {
        myWatchlist.take(watchlistLimit).forEach { candidates[it] = null to CardReason.Watchlist }
    } else {
        val myWatched = library.filter { it.status == MovieStatus.WATCHED }.mapNotNull { it.tmdbId }.toSet()
        val theirWatchlist = partner.watchlist.filter { it !in skip }.shuffled()
        mergeWatchlists(myWatchlist, theirWatchlist, myWatched, partner.watched, partner.name, watchlistLimit)
            .forEach { (id, reason) -> candidates[id] = null to reason }
    }
    val known = inLibrary + partner?.library.orEmpty() + skip
    fun fits(movie: TmdbMovie) = movie.id !in known && !movie.posterPath.isNullOrBlank() && matchesGenres(movie.genreIds.orEmpty(), genres)

    // Recommendations of each seed, taken in turn so one movie does not fill the deck.
    val recommended = (mySeeds + theirSeeds).map { (id, title, by) ->
        async {
            val reason = if (by == null) CardReason.BecauseYouLiked(title) else CardReason.PartnerLiked(by, title)
            container.tmdb.fetchMovieRecommendations(id).filter(::fits).map { it to reason }
        }
    }.awaitAll()
    var rank = 0
    while (candidates.size < wanted && recommended.any { rank < it.size }) {
        recommended.forEach { list -> list.getOrNull(rank)?.let { (movie, reason) -> if (movie.id !in candidates) candidates[movie.id] = movie to reason } }
        rank++
    }
    // Then what is popular: this week's trends, or the favourites of the picked genres.
    var page = 1
    while (candidates.size < wanted && page <= 3) {
        val more = if (genres.isEmpty()) container.tmdb.fetchTrendingMoviesPaged(page).results
        else container.tmdb.discoverMovies(genres.flatMap { it.tmdbIds }.toSet(), page)
        if (more.isEmpty()) break
        val reason = if (genres.isEmpty()) CardReason.Trending else CardReason.Popular
        more.filter(::fits).forEach { if (candidates.size < wanted && it.id !in candidates) candidates[it.id] = it to reason }
        page++
    }

    val byTmdbId = library.filter { it.tmdbId != null }.associateBy { it.tmdbId!! }
    val cards = candidates.entries.map { (id, entry) ->
        async { withTimeoutOrNull(CARD_TIMEOUT_MS) { cardFor(container, id, entry.first, byTmdbId[id], entry.second, services) } }
    }.awaitAll().filterNotNull()
    pickDeck(cards, services, if (shortOnly) 120 else null, size, genres)
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

/**
 * The same for series: the "To watch" lists first, then what TMDB recommends after the series
 * rated best (or under way), then what is popular, mini-series straight from TMDB when that is
 * what the players want. A series either of them finished stays out; one under way comes last.
 */
private suspend fun dealSeriesDeck(
    container: AppContainer,
    services: Set<StreamingService>,
    length: SeriesLength,
    skip: Set<Int>,
    partner: PartnerTaste?,
    size: Int,
    genres: Set<TonightGenre>,
): List<TonightCard> = coroutineScope {
    // The length is only known once the details are in: deal wider when it filters.
    val wanted = candidatesFor(size, genres) + if (length == SeriesLength.ANY) 0 else size
    val big = size > DECK_SIZE
    val library = container.series.items.value
    val inLibrary = library.mapNotNull { it.tmdbId }.toSet()
    val myWatched = library.filter { it.status == SeriesStatus.WATCHED }.mapNotNull { it.tmdbId }.toSet()
    val myStarted = library.filter { it.status == SeriesStatus.WATCHING }.mapNotNull { it.tmdbId }.toSet()
    val myWatchlist = library.filter { it.status == SeriesStatus.WANT_TO_WATCH && it.tmdbId != null && it.tmdbId !in skip }.map { it.tmdbId!! }.shuffled()
    val mySeeds = library.filter { it.status != SeriesStatus.WANT_TO_WATCH && it.tmdbId != null }
        .sortedWith(compareByDescending<Series> { it.rating ?: 0.0 }.thenByDescending { it.createdAt })
        .take(if (big) 10 else 6).shuffled().take(if (partner == null) (if (big) 5 else 3) else (if (big) 3 else 2))
        .map { Triple(it.tmdbId!!, it.title, null as String?) }
    val theirSeeds = partner?.favourites?.take(if (big) 10 else 6)?.shuffled()?.take(if (big) 3 else 2)?.map { Triple(it.first, it.second, partner.name) }.orEmpty()

    val candidates = LinkedHashMap<Int, Pair<TmdbSeries?, CardReason>>()
    val watchlistLimit = if (big) 14 else 8
    if (partner == null) {
        myWatchlist.take(watchlistLimit).forEach { candidates[it] = null to CardReason.Watchlist }
    } else {
        val theirWatchlist = partner.watchlist.filter { it !in skip }.shuffled()
        mergeWatchlists(myWatchlist, theirWatchlist, myWatched, partner.watched, partner.name, watchlistLimit)
            .forEach { (id, reason) -> candidates[id] = null to reason }
    }
    val known = inLibrary + partner?.library.orEmpty() + skip
    fun fits(series: TmdbSeries) = series.id !in known && !series.posterPath.isNullOrBlank() && matchesGenres(series.genreIds.orEmpty(), genres, TonightKind.SERIES)

    val recommended = (mySeeds + theirSeeds).map { (id, title, by) ->
        async {
            val reason = if (by == null) CardReason.BecauseYouLiked(title) else CardReason.PartnerLiked(by, title)
            container.tmdb.fetchSeriesRecommendations(id).filter(::fits).map { it to reason }
        }
    }.awaitAll()
    var rank = 0
    while (candidates.size < wanted && recommended.any { rank < it.size }) {
        recommended.forEach { list -> list.getOrNull(rank)?.let { (series, reason) -> if (series.id !in candidates) candidates[series.id] = series to reason } }
        rank++
    }
    var page = 1
    val mini = length == SeriesLength.MINI
    while (candidates.size < wanted && page <= 3) {
        val more = if (genres.isEmpty() && !mini) container.tmdb.fetchTrendingSeries(page).results
        else container.tmdb.discoverSeries(genres.flatMap { it.tvIds }.toSet(), page, miniseries = mini)
        if (more.isEmpty()) break
        val reason = if (genres.isEmpty() && !mini) CardReason.Trending else CardReason.Popular
        more.filter(::fits).forEach { if (candidates.size < wanted && it.id !in candidates) candidates[it.id] = it to reason }
        page++
    }

    val byTmdbId = library.filter { it.tmdbId != null }.associateBy { it.tmdbId!! }
    val cards = candidates.entries.map { (id, entry) ->
        async { withTimeoutOrNull(CARD_TIMEOUT_MS) { seriesCardFor(container, id, entry.first, byTmdbId[id], entry.second, services) } }
    }.awaitAll().filterNotNull()
    pickDeck(cards, services, null, size, genres, TonightKind.SERIES, length, later = myStarted + partner?.started.orEmpty())
}

private suspend fun seriesCardFor(
    container: AppContainer,
    tmdbId: Int,
    listed: TmdbSeries?,
    saved: Series?,
    reason: CardReason,
    services: Set<StreamingService>,
): TonightCard? = coroutineScope {
    val details = async { container.tmdb.fetchSeriesDetails(tmdbId) }
    val providers = async { container.tmdb.fetchSeriesWatchProviders(tmdbId) }
    val series = details.await()
        ?: listed
        ?: saved?.let { TmdbSeries(id = tmdbId, name = it.title, overview = it.description.orEmpty(), firstAirDate = it.firstAirDate.orEmpty(), numberOfSeasons = it.seasons) }
        ?: return@coroutineScope null
    seriesCard(series, providers.await(), reason, services, saved)
}

/** A series as a card: its name and first air date in the movie's place, the length of an episode as the runtime. */
internal fun seriesCard(
    series: TmdbSeries,
    watch: fr.paulbr.nookmind.core.model.WatchProvidersResult,
    reason: CardReason,
    services: Set<StreamingService>,
    saved: Series? = null,
): TonightCard {
    val seasons = series.seasons?.count { it.seasonNumber > 0 && it.episodeCount > 0 }?.takeIf { it > 0 }
        ?: series.numberOfSeasons?.takeIf { it > 0 }
        ?: saved?.seasons
    val episode = series.episodeRunTime?.firstOrNull { it > 0 } ?: series.lastEpisodeToAir?.runtime?.takeIf { it > 0 }
    val genreIds = series.genres?.map { it.id }?.takeIf { it.isNotEmpty() } ?: series.genreIds.orEmpty()
    return TonightCard(
        movie = TmdbMovie(
            id = series.id,
            title = series.name.ifBlank { saved?.title.orEmpty() },
            overview = series.overview.ifBlank { saved?.description.orEmpty() },
            posterPath = series.posterPath,
            releaseDate = series.firstAirDate,
            runtime = episode,
            genres = series.genres,
            genreIds = genreIds,
        ),
        posterUrl = TmdbApi.posterUrl(series.posterPath, "w780") ?: saved?.posterUrl,
        year = series.firstAirDate.take(4).takeIf { it.length == 4 } ?: saved?.firstAirDate?.take(4),
        runtime = episode,
        director = series.createdBy?.firstOrNull()?.name ?: saved?.creator?.takeIf { it.isNotBlank() },
        genres = series.genres?.map { it.name }.orEmpty(),
        provider = pickProvider(watch.flatrate, services),
        providersLink = watch.link,
        reason = reason,
        kind = TonightKind.SERIES,
        seasons = seasons,
        ended = series.status?.let { it == "Ended" || it == "Canceled" },
        miniseries = series.type == "Miniseries",
    )
}
