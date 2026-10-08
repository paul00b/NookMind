package fr.paulbr.nookmind.tools

import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.network.GoogleBooksApi
import fr.paulbr.nookmind.core.network.TmdbApi
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Gives [FakeData] real posters and covers, looked up on TMDB (by id) and Google Books (by title and
 * author), so library grids can be judged with real artwork. Needs the build's TMDB and Google Books
 * keys and a network, which CI has; any lookup that fails or throws keeps its placeholder.
 */
object FakeCovers {
    private const val TIMEOUT_MS = 15_000L

    suspend fun fill(container: AppContainer) {
        FakeData.movies = FakeData.movies.map { it.copy(posterUrl = moviePoster(container, it.tmdbId) ?: it.posterUrl) }
        FakeData.newMovie = FakeData.newMovie.let { it.copy(posterUrl = moviePoster(container, it.tmdbId) ?: it.posterUrl) }
        FakeData.series = FakeData.series.map { it.copy(posterUrl = seriesPoster(container, it.tmdbId) ?: it.posterUrl) }
        FakeData.newSeries = FakeData.newSeries.let { it.copy(posterUrl = seriesPoster(container, it.tmdbId) ?: it.posterUrl) }
        FakeData.books = FakeData.books.map { it.copy(coverUrl = bookCover(container, it.title, it.author) ?: it.coverUrl) }
        FakeData.newBook = FakeData.newBook.let { it.copy(coverUrl = bookCover(container, it.title, it.author) ?: it.coverUrl) }
        val found = FakeData.movies.count { it.posterUrl != null } + FakeData.series.count { it.posterUrl != null } +
            FakeData.books.count { it.coverUrl != null }
        println("covers: $found found")
    }

    private suspend fun moviePoster(container: AppContainer, tmdbId: Int?): String? {
        val id = tmdbId ?: return null
        return runCatching { withTimeoutOrNull(TIMEOUT_MS) { TmdbApi.posterUrl(container.tmdb.fetchMovieDetails(id)?.posterPath, "w342") } }.getOrNull()
    }

    private suspend fun seriesPoster(container: AppContainer, tmdbId: Int?): String? {
        val id = tmdbId ?: return null
        return runCatching { withTimeoutOrNull(TIMEOUT_MS) { TmdbApi.posterUrl(container.tmdb.fetchSeriesDetails(id)?.posterPath, "w342") } }.getOrNull()
    }

    // searchBooks throws on an HTTP error (Google Books rate-limits unauthenticated bursts).
    private suspend fun bookCover(container: AppContainer, title: String, author: String): String? = runCatching {
        withTimeoutOrNull(TIMEOUT_MS) {
            container.googleBooks.searchBooks("$title $author", maxResults = 3)
                .firstNotNullOfOrNull { GoogleBooksApi.extractBookData(it).coverUrl }
        }
    }.getOrNull()
}
