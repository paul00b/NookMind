package fr.paulbr.nookmind.core.domain

import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesStatus
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Port of src/lib/seriesUtils.test.ts and src/components/SeasonGrid.test.ts

private val TODAY = LocalDate(2026, 4, 10)

private fun makeSeries(
    status: SeriesStatus = SeriesStatus.WATCHING,
    seasons: Int? = 3,
    watchedSeasons: List<Int> = emptyList(),
    watchedEpisodes: Map<String, List<Int>> = emptyMap(),
    nextAirDate: String? = "2026-04-15",
    nextSeasonNumber: Int? = 2,
    nextEpisodeNumber: Int? = 1,
) = Series(
    id = "series-1",
    userId = "user-1",
    tmdbId = 101,
    title = "Test Show",
    creator = "Creator",
    firstAirDate = "2024-01-01",
    seasons = seasons,
    watchedSeasons = watchedSeasons,
    watchedEpisodes = watchedEpisodes,
    status = status,
    nextAirDate = nextAirDate,
    nextSeasonNumber = nextSeasonNumber,
    nextEpisodeNumber = nextEpisodeNumber,
    createdAt = "2026-04-01T00:00:00.000Z",
)

class SeriesUtilsTest {
    @Test
    fun derivesWatchingWhenEpisodesWatchedEvenIfStoredStatusSaysWantToWatch() {
        val s = makeSeries(status = SeriesStatus.WANT_TO_WATCH, seasons = 1, watchedEpisodes = mapOf("1" to listOf(1, 2, 3, 4)))
        assertEquals(SeriesStatus.WATCHING, getEffectiveSeriesStatus(s, TODAY))
    }

    @Test
    fun waitingWhenNextSeasonHasNotStarted() {
        assertTrue(isSeriesWaiting(makeSeries(seasons = 1, watchedSeasons = listOf(1), nextAirDate = "2099-04-22", nextSeasonNumber = 2), TODAY))
    }

    @Test
    fun waitingWhenTmdbAlreadyCountsFutureSeason() {
        assertTrue(isSeriesWaiting(makeSeries(seasons = 3, watchedSeasons = listOf(1, 2), nextAirDate = "2099-07-02", nextSeasonNumber = 3), TODAY))
    }

    @Test
    fun notWaitingWhenNextSeasonAlreadyStarted() {
        assertFalse(
            isSeriesWaiting(
                makeSeries(watchedSeasons = listOf(1), seasons = 2, nextAirDate = "2099-04-22", nextSeasonNumber = 2, nextEpisodeNumber = 4),
                TODAY,
            ),
        )
    }

    @Test
    fun waitingWhenCaughtUpInCurrentSeasonAndNextEpisodeIsFuture() {
        assertTrue(
            isSeriesWaiting(
                makeSeries(
                    watchedSeasons = listOf(1, 2, 3, 4), seasons = 5, watchedEpisodes = mapOf("5" to listOf(1, 2, 3, 4)),
                    nextAirDate = "2099-04-29", nextSeasonNumber = 5, nextEpisodeNumber = 5,
                ),
                TODAY,
            ),
        )
    }

    @Test
    fun keepsWatchingWhenNextEpisodeHasNotAired() {
        val s = makeSeries(seasons = 1, watchedSeasons = listOf(1), watchedEpisodes = mapOf("1" to listOf(1)), nextAirDate = "2099-05-08", nextSeasonNumber = 1, nextEpisodeNumber = 2)
        assertEquals(SeriesStatus.WATCHING, getEffectiveSeriesStatus(s, TODAY))
        assertTrue(isSeriesWaiting(s, TODAY))
    }

    @Test
    fun waitsWhenSeasonMarkedCompleteAndNextEpisodeFuture() {
        val s = makeSeries(seasons = 1, watchedSeasons = listOf(1), nextAirDate = "2099-05-08", nextSeasonNumber = 1, nextEpisodeNumber = 2)
        assertTrue(isSeriesWaiting(s, TODAY))
    }

    @Test
    fun notWaitingWhenNewSeasonAlreadyAiringEvenWithoutMarkedEpisode() {
        assertFalse(
            isSeriesWaiting(
                makeSeries(watchedSeasons = listOf(1), seasons = 2, watchedEpisodes = mapOf("2" to listOf(1, 2)), nextAirDate = "2099-04-22", nextSeasonNumber = 2),
                TODAY,
            ),
        )
    }

    @Test
    fun notWaitingWhileStillWatchingCurrentSeason() {
        assertFalse(
            isSeriesWaiting(
                makeSeries(watchedEpisodes = mapOf("1" to listOf(1, 2)), nextAirDate = "2026-04-15", nextSeasonNumber = 1, nextEpisodeNumber = 4),
                TODAY,
            ),
        )
    }

    @Test
    fun waitingForWatchedSeriesWhenFutureSeasonAnnounced() {
        assertTrue(
            isSeriesWaiting(
                makeSeries(status = SeriesStatus.WATCHED, watchedSeasons = listOf(1, 2, 3), seasons = 3, nextAirDate = "2099-04-22", nextSeasonNumber = 4),
                TODAY,
            ),
        )
    }

    // SeasonGrid.test.ts
    @Test
    fun deriveStatusKeepsWatchingWhenEpisodesWatchedButNoSeasonComplete() {
        assertEquals(SeriesStatus.WATCHING, deriveSeriesStatus(emptyList(), 3, false, mapOf("1" to listOf(1))))
    }

    @Test
    fun deriveStatusWantToWatchWhenNothingWatched() {
        assertEquals(SeriesStatus.WANT_TO_WATCH, deriveSeriesStatus(emptyList(), 3, false, emptyMap()))
    }

    @Test
    fun deriveStatusPartiallyWatchedSingleSeasonIsWatching() {
        assertEquals(SeriesStatus.WATCHING, deriveSeriesStatus(emptyList(), 1, false, mapOf("1" to listOf(1, 2, 3, 4))))
    }

    @Test
    fun deriveStatusAllSeasonsWatchedIsWatched() {
        assertEquals(SeriesStatus.WATCHED, deriveSeriesStatus(listOf(1, 2), 2, false, emptyMap()))
        assertEquals(SeriesStatus.WATCHING, deriveSeriesStatus(listOf(1, 2), 2, true, emptyMap()))
    }

    @Test
    fun waitingLabelFollowsTheWebRules() {
        val fmt: (LocalDate) -> String = { "${it.day} ${it.month.name.take(3).lowercase()}." }
        val fmtYear: (LocalDate) -> String = { "${it.month.name.take(3).lowercase()}. ${it.year}" }
        fun label(date: String?) = formatWaitingLabel(date, "Waiting", "tomorrow", { "in ${it}d" }, fmt, fmtYear, TODAY)
        assertEquals("Waiting", label(null))
        assertEquals("Waiting", label("2026-04-10"))
        assertEquals("tomorrow", label("2026-04-11"))
        assertEquals("in 30d", label("2026-05-10"))
        assertEquals("15 jul.", label("2026-07-15"))
        assertEquals("jan. 2027", label("2027-01-15"))
    }
}
