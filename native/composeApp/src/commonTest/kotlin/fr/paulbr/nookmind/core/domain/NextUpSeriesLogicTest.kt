package fr.paulbr.nookmind.core.domain

import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesStatus
import fr.paulbr.nookmind.core.model.TmdbEpisode
import fr.paulbr.nookmind.core.model.TmdbSeasonSummary
import fr.paulbr.nookmind.core.model.TmdbSeries
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// Port of the logic exercised by src/pages/NextUpSeries.test.tsx

private val TODAY = LocalDate(2026, 4, 15)

private fun tmdb(lastAired: TmdbEpisode?, next: TmdbEpisode?, episodeCount: Int = 3) = TmdbSeries(
    id = 101, name = "Test Show", overview = "Overview", firstAirDate = "2024-01-01",
    seasons = listOf(TmdbSeasonSummary(1, episodeCount, "2024-01-01")),
    lastEpisodeToAir = lastAired, nextEpisodeToAir = next,
)

private fun series(watched: Map<String, List<Int>>, nextEp: Int) = Series(
    id = "series-1", userId = "user-1", tmdbId = 101, title = "Test Show", creator = "Creator",
    firstAirDate = "2024-01-01", seasons = 1, watchedEpisodes = watched, status = SeriesStatus.WATCHING,
    nextAirDate = "2026-04-20", nextSeasonNumber = 1, nextEpisodeNumber = nextEp, createdAt = "2026-04-01T00:00:00.000Z",
)

class NextUpSeriesLogicTest {
    @Test
    fun nextEpisodeIsAvailableWhenBehindLastAired() {
        val t = tmdb(
            lastAired = TmdbEpisode("2024-01-08", 2, 1, "Episode 2"),
            next = TmdbEpisode("2026-04-20", 3, 1, "Episode 3"),
        )
        val state = getEpisodeState(series(mapOf("1" to listOf(1)), 2), t, TODAY)
        assertEquals(EpisodeState.Available(1, 2), state)
    }

    @Test
    fun afterMarkingCurrentEpisodeTheFollowingUpcomingEpisodeIsProposed() {
        val t = tmdb(
            lastAired = TmdbEpisode("2024-01-08", 2, 1, "Episode 2"),
            next = TmdbEpisode("2026-04-20", 3, 1, "Episode 3"),
        )
        val s = series(mapOf("1" to listOf(1)), 2)
        val (watchedEpisodes, watchedSeasons) = markEpisodeWatched(s, t, 1, 2)
        assertEquals(listOf(1, 2), watchedEpisodes["1"])
        assertEquals(emptyList(), watchedSeasons)
        val next = findNextUpcomingEpisode(
            seasons = listOf(1),
            seasonEpisodes = {
                listOf(
                    TmdbEpisode("2024-01-01", 1, 1, "Episode 1"),
                    TmdbEpisode("2024-01-08", 2, 1, "Episode 2"),
                    TmdbEpisode("2026-04-20", 3, 1, "Episode 3"),
                )
            },
            watchedEpisodes = watchedEpisodes,
            today = TODAY,
        )
        assertEquals("Episode 3", next?.name)
        assertEquals(5, daysUntil("2026-04-20", TODAY))
        val updated = s.copy(watchedEpisodes = watchedEpisodes, watchedSeasons = watchedSeasons)
        val newState = getEpisodeState(updated, t.copy(nextEpisodeToAir = next), TODAY)
        assertIs<EpisodeState.ComingSoon>(newState)
    }

    @Test
    fun episodeAiringTodayIsAvailable() {
        val t = tmdb(
            lastAired = TmdbEpisode("2026-04-08", 2, 1, "Episode 2"),
            next = TmdbEpisode("2026-04-15", 3, 1, "Episode 3"),
        )
        val state = getEpisodeState(series(mapOf("1" to listOf(1, 2)), 3), t, TODAY)
        assertEquals(EpisodeState.Available(1, 3), state)
    }

    @Test
    fun markingTodayEpisodeProposesTheNextWeekEpisode() {
        val t = tmdb(
            lastAired = TmdbEpisode("2026-04-08", 2, 1, "Episode 2"),
            next = TmdbEpisode("2026-04-15", 3, 1, "Episode 3"),
            episodeCount = 4,
        )
        val s = series(mapOf("1" to listOf(1, 2)), 3)
        val (watchedEpisodes, _) = markEpisodeWatched(s, t, 1, 3)
        val next = findNextUpcomingEpisode(
            seasons = listOf(1),
            seasonEpisodes = {
                listOf(
                    TmdbEpisode("2026-04-01", 1, 1, "Episode 1"),
                    TmdbEpisode("2026-04-08", 2, 1, "Episode 2"),
                    TmdbEpisode("2026-04-15", 3, 1, "Episode 3"),
                    TmdbEpisode("2026-04-22", 4, 1, "Episode 4"),
                )
            },
            watchedEpisodes = watchedEpisodes,
            today = TODAY,
        )
        assertEquals("Episode 4", next?.name)
        assertEquals(7, daysUntil("2026-04-22", TODAY))
    }

    @Test
    fun completingLastEpisodeOfSeasonMarksSeasonWatched() {
        val t = tmdb(lastAired = TmdbEpisode("2026-04-08", 3, 1, "Episode 3"), next = null, episodeCount = 3)
        val s = series(mapOf("1" to listOf(1, 2)), 3)
        val (_, watchedSeasons) = markEpisodeWatched(s, t, 1, 3)
        assertEquals(listOf(1), watchedSeasons)
        val upToDate = getEpisodeState(s.copy(watchedEpisodes = mapOf("1" to listOf(1, 2, 3)), watchedSeasons = listOf(1)), t, TODAY)
        assertEquals(EpisodeState.UpToDate, upToDate)
    }

    @Test
    fun rollsOverToNextSeasonWhenCurrentSeasonExhausted() {
        val t = TmdbSeries(
            id = 1, name = "S", seasons = listOf(TmdbSeasonSummary(1, 2), TmdbSeasonSummary(2, 2)),
            lastEpisodeToAir = TmdbEpisode("2026-04-01", 1, 2, "S2E1"),
        )
        val s = Series(id = "x", title = "S", seasons = 2, watchedSeasons = listOf(1), status = SeriesStatus.WATCHING)
        assertEquals(EpisodeState.Available(2, 1), getEpisodeState(s, t, TODAY))
    }
}
