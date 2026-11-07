package fr.paulbr.nookmind.core.domain

import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.BookStatus
import fr.paulbr.nookmind.core.model.Movie
import fr.paulbr.nookmind.core.model.MovieStatus
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesStatus
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YearReviewTest {
    private val movies = listOf(
        Movie(id = "1", title = "Dune", status = MovieStatus.WATCHED, watchedDate = "2026-03-02", runtime = 166, rating = 5.0, posterUrl = "d"),
        Movie(id = "2", title = "Seen without a date", status = MovieStatus.WATCHED, createdAt = "2026-10-01T10:00:00Z", runtime = 100, rating = 3.0),
        Movie(id = "3", title = "Last year", status = MovieStatus.WATCHED, watchedDate = "2025-12-30", rating = 5.0),
        Movie(id = "4", title = "To watch", status = MovieStatus.WANT_TO_WATCH, createdAt = "2026-01-01"),
    )
    private val series = listOf(
        Series(id = "a", title = "Severance", status = SeriesStatus.WATCHING, createdAt = "2024-05-01", rating = 4.5),
        Series(id = "b", title = "Done this year", status = SeriesStatus.WATCHED, createdAt = "2026-02-01"),
        Series(id = "c", title = "Done long ago", status = SeriesStatus.WATCHED, createdAt = "2023-02-01", rating = 5.0),
    )
    private val books = listOf(
        Book(id = "x", title = "Piranesi", status = BookStatus.READ, createdAt = "2026-04-01", pageCount = 272, rating = 4.0),
        Book(id = "y", title = "Reading", status = BookStatus.READING, createdAt = "2026-04-01", pageCount = 500),
    )

    @Test
    fun countsWhatHappenedThisYear() {
        val r = yearReview(2026, movies, series, books, Random(1))
        assertEquals(listOf("1", "2"), r.movies.map { it.id }, "watched date first, the day it was added otherwise")
        assertEquals(setOf("a", "b"), r.series.map { it.id }.toSet(), "a series under way counts, one finished in an earlier year does not")
        assertEquals(listOf("x"), r.books.map { it.id })
        assertEquals(272, r.pages)
        assertEquals(266, r.movieMinutes)
        assertEquals(1, r.moviesLastYear)
    }

    @Test
    fun favouritesAreTheBestRatedOfTheYear() {
        val r = yearReview(2026, movies, series, books, Random(1))
        assertEquals("Dune", r.topMovie?.title, "the five-star movie of 2025 stays out")
        assertEquals("Severance", r.topSeries?.title)
        assertEquals("Piranesi", r.topBook?.title)
    }

    @Test
    fun theProfileFollowsWhatTheYearWasMadeOf() {
        val manyMovies = List(8) { Movie(id = "m$it", status = MovieStatus.WATCHED, watchedDate = "2026-01-0${it + 1}") }
        assertEquals(ReviewProfile.MOVIE_BUFF, yearReview(2026, manyMovies, emptyList(), emptyList()).profile)
        assertEquals(ReviewProfile.FRESH_START, yearReview(2026, emptyList(), emptyList(), emptyList()).profile)
        assertTrue(yearReview(2026, emptyList(), emptyList(), emptyList()).isEmpty)
    }
}
