package fr.paulbr.nookmind.core.domain

import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesStatus
import kotlinx.datetime.LocalDate

// Port of src/lib/seriesUtils.ts — same rules, same edge cases (covered by SeriesUtilsTest).

private fun getReleasedSeasonCount(series: Series, today: LocalDate): Int? {
    val seasons = series.seasons ?: return null
    val nextSeason = series.nextSeasonNumber
    val hasFutureSeasonAnnounced =
        nextSeason != null && isFutureDate(series.nextAirDate, today) && nextSeason <= seasons
    if (!hasFutureSeasonAnnounced) return seasons
    return maxOf(0, (nextSeason ?: 1) - 1)
}

private fun hasWatchedAllPreviousEpisodesInNextSeason(series: Series): Boolean {
    val nextSeason = series.nextSeasonNumber ?: return false
    val nextEpisode = series.nextEpisodeNumber ?: return false
    if (nextEpisode <= 1) return true
    val watched = series.watchedEpisodes[nextSeason.toString()]?.toSet() ?: emptySet()
    for (episode in 1 until nextEpisode) {
        if (episode !in watched) return false
    }
    return true
}

fun deriveSeriesStatus(
    watchedSeasons: List<Int>,
    totalSeasons: Int?,
    hasUnreleasedEpisodes: Boolean = false,
    watchedEpisodes: Map<String, List<Int>> = emptyMap(),
): SeriesStatus {
    val hasAnyEpisodeWatched = watchedSeasons.isNotEmpty() || watchedEpisodes.values.any { it.isNotEmpty() }
    if (!hasAnyEpisodeWatched) return SeriesStatus.WANT_TO_WATCH
    if (!hasUnreleasedEpisodes && totalSeasons != null && totalSeasons > 0 && watchedSeasons.size >= totalSeasons) {
        return SeriesStatus.WATCHED
    }
    return SeriesStatus.WATCHING
}

fun getEffectiveSeriesStatus(series: Series, today: LocalDate = todayLocal()): SeriesStatus {
    val status = deriveSeriesStatus(series.watchedSeasons, series.seasons, false, series.watchedEpisodes)
    if (status == SeriesStatus.WATCHED && series.nextSeasonNumber != null && isFutureDate(series.nextAirDate, today)) {
        return SeriesStatus.WATCHING
    }
    return status
}

/**
 * A series is "waiting" when the user watched everything available and a new season /
 * episode is announced for a future date.
 */
fun isSeriesWaiting(s: Series, today: LocalDate = todayLocal()): Boolean {
    val effectiveStatus = getEffectiveSeriesStatus(s, today)
    if (effectiveStatus == SeriesStatus.WANT_TO_WATCH) return false
    val nextSeason = s.nextSeasonNumber
    if (!isFutureDate(s.nextAirDate, today) || nextSeason == null) return false
    if (effectiveStatus == SeriesStatus.WATCHED) return true

    val nextEpisode = s.nextEpisodeNumber
    if (nextEpisode != null && nextEpisode > 1) {
        if (nextSeason in s.watchedSeasons) return true
        return hasWatchedAllPreviousEpisodesInNextSeason(s)
    }

    val hasStartedNextSeason =
        (s.watchedEpisodes[nextSeason.toString()]?.size ?: 0) > 0 || nextSeason in s.watchedSeasons
    if (hasStartedNextSeason) return false

    val releasedSeasonCount = getReleasedSeasonCount(s, today)
    if (releasedSeasonCount != null && s.watchedSeasons.size < releasedSeasonCount) return false

    return nextSeason > s.watchedSeasons.size
}

/**
 * Label of the "waiting" badge:
 *  - no date → [fallback]
 *  - tomorrow → [tomorrowLabel]
 *  - < 60 days → [inDaysLabel]
 *  - ≥ 60 days → "15 avr." (same year) or "avr. 2026"
 *  - past date → [fallback]
 */
fun formatWaitingLabel(
    nextAirDate: String?,
    fallback: String,
    tomorrowLabel: String,
    inDaysLabel: (Int) -> String,
    formatDayMonth: (LocalDate) -> String,
    formatMonthYear: (LocalDate) -> String,
    today: LocalDate = todayLocal(),
): String {
    val date = parseDateOnly(nextAirDate) ?: return fallback
    val days = daysUntil(date, today)
    if (days <= 0) return fallback
    if (days == 1) return tomorrowLabel
    if (days < 60) return inDaysLabel(days)
    return if (date.year == today.year) formatDayMonth(date) else formatMonthYear(date)
}
