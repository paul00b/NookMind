package fr.paulbr.nookmind.core.domain

import fr.paulbr.nookmind.core.model.EpisodeRating
import fr.paulbr.nookmind.core.model.SeasonState
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun series(
    id: String,
    status: SeriesStatus,
    watchedSeasons: List<Int> = emptyList(),
    rating: Double? = null,
    genre: String? = null,
    creator: String = "",
) = Series(
    id = id,
    title = "S$id",
    creator = creator,
    watchedSeasons = watchedSeasons,
    genre = genre,
    status = status,
    rating = rating,
)

class SeriesStatsTest {

    @Test
    fun onlyWatchedAndWatchingSeriesCount() {
        val all = listOf(
            series("1", SeriesStatus.WATCHED),
            series("2", SeriesStatus.WATCHING),
            series("3", SeriesStatus.WANT_TO_WATCH),
        )
        assertEquals(listOf("1", "2"), relevantSeriesForStats(all).map { it.id })
    }

    @Test
    fun localStatsSumSeasonsAndAverageOnlyRatedSeries() {
        val stats = computeLocalStats(
            listOf(
                series("1", SeriesStatus.WATCHED, watchedSeasons = listOf(1, 2, 3), rating = 4.0, genre = "Drama", creator = "A"),
                series("2", SeriesStatus.WATCHED, watchedSeasons = listOf(1), rating = 5.0, genre = "Drama", creator = "B"),
                series("3", SeriesStatus.WATCHING, watchedSeasons = listOf(1, 2), genre = "Comedy", creator = "A"),
            ),
        )
        assertEquals(3, stats.totalSeries)
        assertEquals(6, stats.watchedSeasonsCount)
        assertEquals(4.5, stats.averageRating)
        assertEquals("Drama", stats.favoriteGenre)
        assertEquals("A", stats.favoriteCreator)
    }

    @Test
    fun localStatsWithoutRatingsOrMetadata() {
        val stats = computeLocalStats(listOf(series("1", SeriesStatus.WATCHING)))
        assertNull(stats.averageRating)
        assertNull(stats.favoriteGenre)
        assertNull(stats.favoriteCreator)
        assertEquals(0, stats.watchedSeasonsCount)
    }

    @Test
    fun imdbStatsIgnoreMissingRatingsAndLoadingSeasons() {
        val ratings = mapOf(
            1 to SeasonState.Loaded(
                listOf(
                    EpisodeRating(episode = 1, imdbRating = 8.0),
                    EpisodeRating(episode = 2, imdbRating = null),
                    EpisodeRating(episode = 3, imdbRating = 9.4),
                ),
            ),
            2 to SeasonState.Loading,
            3 to SeasonState.Error,
        )
        val stats = computeImdbStats(ratings)
        assertEquals("8.7", stats?.average)
        assertEquals("9.4", stats?.best)
        assertEquals("8.0", stats?.worst)
    }

    @Test
    fun imdbStatsAreNullWithoutAnyRating() {
        assertNull(computeImdbStats(mapOf(1 to SeasonState.Loading)))
        assertNull(computeImdbStats(mapOf(1 to SeasonState.Loaded(listOf(EpisodeRating(episode = 1, imdbRating = null))))))
    }
}
