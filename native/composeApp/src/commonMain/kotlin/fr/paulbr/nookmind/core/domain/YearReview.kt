package fr.paulbr.nookmind.core.domain

import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.BookStatus
import fr.paulbr.nookmind.core.model.Movie
import fr.paulbr.nookmind.core.model.MovieStatus
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesStatus
import kotlin.random.Random

/** What the year-in-review screens show, from the library alone. */
data class YearReview(
    val year: Int,
    val movies: List<Movie>,
    val series: List<Series>,
    val books: List<Book>,
    val pages: Int,
    /** Movies of the year before, counted the same way, for "n more than last year". */
    val moviesLastYear: Int,
    val movieMinutes: Int,
    val topMovie: Movie?,
    val topSeries: Series?,
    val topBook: Book?,
    /** Every poster and cover of the year, shuffled: the wall must not give the favourites away. */
    val posters: List<String>,
    val profile: ReviewProfile,
) {
    val isEmpty: Boolean get() = movies.isEmpty() && series.isEmpty() && books.isEmpty()
}

/** The title of the share card, after what the year was mostly made of. */
enum class ReviewProfile { BOOKWORM, SERIES_NIGHTS, MOVIE_BUFF, ALL_ROUNDER, FRESH_START }

/**
 * The library has no "finished on" date for books and series, so they count by when they were
 * added: a book read this year, a series added this year. A series still under way counts whenever
 * it was added, since it is being watched now. Movies use their watched date when they have one.
 */
fun yearReview(year: Int, movies: List<Movie>, series: List<Series>, books: List<Book>, random: Random = Random.Default): YearReview {
    fun inYear(date: String?, y: Int = year) = date.isNullOrBlank() || date.take(4) == y.toString()
    fun watchedIn(m: Movie, y: Int) = m.status == MovieStatus.WATCHED && inYear(m.watchedDate ?: m.createdAt, y)

    val yearMovies = movies.filter { watchedIn(it, year) }
    val yearSeries = series.filter {
        it.status == SeriesStatus.WATCHING || (it.status == SeriesStatus.WATCHED && inYear(it.createdAt))
    }
    val yearBooks = books.filter { it.status == BookStatus.READ && inYear(it.createdAt) }
    val lastYear = movies.count { m -> m.status == MovieStatus.WATCHED && (m.watchedDate ?: m.createdAt).take(4) == (year - 1).toString() }

    fun <T> best(items: List<T>, rating: (T) -> Double?, date: (T) -> String): T? =
        items.filter { (rating(it) ?: 0.0) > 0 }.maxWithOrNull(compareBy<T> { rating(it) }.thenBy { date(it) })

    val counts = mapOf(
        ReviewProfile.MOVIE_BUFF to yearMovies.size,
        ReviewProfile.SERIES_NIGHTS to yearSeries.size * 3,
        ReviewProfile.BOOKWORM to yearBooks.size * 2,
    )
    val total = counts.values.sum()
    val lead = counts.maxByOrNull { it.value }
    val profile = when {
        total == 0 -> ReviewProfile.FRESH_START
        lead != null && lead.value * 2 > total -> lead.key
        else -> ReviewProfile.ALL_ROUNDER
    }

    return YearReview(
        year = year,
        movies = yearMovies,
        series = yearSeries,
        books = yearBooks,
        pages = yearBooks.sumOf { it.pageCount ?: 0 },
        moviesLastYear = lastYear,
        movieMinutes = yearMovies.sumOf { it.runtime ?: 0 },
        topMovie = best(yearMovies, { it.rating }, { it.watchedDate ?: it.createdAt }),
        topSeries = best(yearSeries, { it.rating }, { it.createdAt }),
        topBook = best(yearBooks, { it.rating }, { it.createdAt }),
        posters = (yearMovies.mapNotNull { it.posterUrl } + yearSeries.mapNotNull { it.posterUrl } + yearBooks.mapNotNull { it.coverUrl })
            .filter { it.isNotBlank() }.distinct().shuffled(random),
        profile = profile,
    )
}
