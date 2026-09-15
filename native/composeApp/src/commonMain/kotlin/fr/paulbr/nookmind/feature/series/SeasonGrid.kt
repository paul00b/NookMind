package fr.paulbr.nookmind.feature.series

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.alpha
import fr.paulbr.nookmind.core.designsystem.components.GhostButton
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.Spinner
import fr.paulbr.nookmind.core.designsystem.components.TextLink
import fr.paulbr.nookmind.core.domain.daysUntil
import fr.paulbr.nookmind.core.domain.parseDateOnly
import fr.paulbr.nookmind.core.domain.todayLocal
import fr.paulbr.nookmind.core.platform.DateStyle
import fr.paulbr.nookmind.feature.common.formatIsoDate
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.seriesDetail_addPreviousEpisodes
import fr.paulbr.nookmind.resources.seriesDetail_addPreviousSeasons
import fr.paulbr.nookmind.resources.seriesDetail_availableOn
import fr.paulbr.nookmind.resources.seriesDetail_availableOnSoon
import fr.paulbr.nookmind.resources.seriesDetail_clickSeasonHint
import fr.paulbr.nookmind.resources.seriesDetail_comingSoon
import fr.paulbr.nookmind.resources.seriesDetail_episodes
import fr.paulbr.nookmind.resources.seriesDetail_loadingEpisodes
import fr.paulbr.nookmind.resources.seriesDetail_markAll
import fr.paulbr.nookmind.resources.seriesDetail_no
import fr.paulbr.nookmind.resources.seriesDetail_noEpisodeData
import fr.paulbr.nookmind.resources.seriesDetail_removeFollowingEpisodes
import fr.paulbr.nookmind.resources.seriesDetail_season
import fr.paulbr.nookmind.resources.seriesDetail_seasonsProgress
import fr.paulbr.nookmind.resources.seriesDetail_unmarkAll
import fr.paulbr.nookmind.resources.seriesDetail_yes
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Availability of one episode, from its TMDB air date. */
enum class EpisodeAvailability { AVAILABLE, COMING_SOON, FUTURE }

/** The "fill the gap?" question raised after a toggle (port of `PendingFill`). */
private sealed interface PendingFill {
    val apply: () -> Unit

    class Episodes(val upTo: Int, override val apply: () -> Unit) : PendingFill
    class RemoveFollowing(val count: Int, override val apply: () -> Unit) : PendingFill
    class Seasons(val upTo: Int, override val apply: () -> Unit) : PendingFill
}

/**
 * Port of SeasonGrid.tsx: season chips (green watched, blue partial, grey otherwise), an expandable
 * episode panel with per-episode toggles, "mark all", unreleased-episode messages, and the
 * banners offering to fill previous episodes / seasons or trim the following ones.
 */
@Composable
fun SeasonGrid(
    totalSeasons: Int,
    watchedSeasons: List<Int>,
    watchedEpisodes: Map<String, List<Int>>,
    onChange: ((watchedSeasons: List<Int>, watchedEpisodes: Map<String, List<Int>>) -> Unit)?,
    modifier: Modifier = Modifier,
    readonly: Boolean = onChange == null,
    compact: Boolean = false,
    episodeCounts: Map<String, Int>? = null,
    episodeAirDates: Map<String, Map<Int, String?>>? = null,
    onSeasonExpand: ((Int) -> Unit)? = null,
    loadingEpisodesSeason: Int? = null,
    onSeasonToggle: ((Int?) -> Unit)? = null,
) {
    val colors = NookTheme.colors
    var expandedSeason by remember { mutableStateOf<Int?>(null) }
    var pendingFill by remember { mutableStateOf<PendingFill?>(null) }
    var episodeMessage by remember { mutableStateOf<String?>(null) }
    var messageTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val episodesEnabled = onSeasonExpand != null
    val today = todayLocal()

    fun countOf(season: Int): Int = episodeCounts?.get(season.toString()) ?: 0

    fun resolvedEpisodes(season: Int): List<Int> {
        val key = season.toString()
        val explicit = watchedEpisodes[key] ?: emptyList()
        if (explicit.isNotEmpty()) return explicit
        val count = countOf(season)
        if (season in watchedSeasons && count > 0) return (1..count).toList()
        return explicit
    }

    fun availability(season: Int, episode: Int): EpisodeAvailability {
        val dates = episodeAirDates?.get(season.toString()) ?: return EpisodeAvailability.AVAILABLE
        if (!dates.containsKey(episode)) return EpisodeAvailability.AVAILABLE
        val airDate = dates[episode] ?: return EpisodeAvailability.COMING_SOON
        val date = parseDateOnly(airDate) ?: return EpisodeAvailability.COMING_SOON
        return if (date > today) EpisodeAvailability.FUTURE else EpisodeAvailability.AVAILABLE
    }

    fun availableEpisodes(season: Int): List<Int> =
        (1..countOf(season)).filter { availability(season, it) == EpisodeAvailability.AVAILABLE }

    fun isSeasonWatched(season: Int): Boolean {
        val count = episodeCounts?.get(season.toString())
        if (count != null && count > 0) {
            val watchedCount = watchedEpisodes[season.toString()]?.size ?: 0
            if (watchedCount == 0 && season in watchedSeasons) return true
            return watchedCount >= count
        }
        return season in watchedSeasons
    }

    fun isSeasonPartial(season: Int): Boolean {
        val count = episodeCounts?.get(season.toString())
        val watched = watchedEpisodes[season.toString()]?.size ?: 0
        if (count != null && count > 0) return watched in 1 until count
        return watched > 0 && season !in watchedSeasons
    }

    fun isSeasonEmpty(season: Int): Boolean = episodeCounts?.get(season.toString()) == 0

    val availableOnSoonTemplate = stringResource(Res.string.seriesDetail_availableOnSoon, "%DATE%")
    val availableOnTemplate = stringResource(Res.string.seriesDetail_availableOn, "%DATE%", 0)
    val comingSoonText = stringResource(Res.string.seriesDetail_comingSoon)

    fun futureEpisodeMessage(dateStr: String): String {
        val formatted = formatIsoDate(dateStr, DateStyle.DAY_MONTH_SHORT_YEAR) ?: dateStr
        val days = daysUntil(dateStr, today) ?: 0
        return if (days <= 1) availableOnSoonTemplate.replace("%DATE%", formatted)
        else availableOnTemplate.replace("%DATE%", formatted).replace(Regex("\\b0\\b"), days.toString())
    }

    fun clearTransient() {
        pendingFill = null
        episodeMessage = null
        messageTarget = null
    }

    fun toggleEpisode(season: Int, episode: Int) {
        if (readonly || onChange == null) return
        clearTransient()
        val key = season.toString()
        when (availability(season, episode)) {
            EpisodeAvailability.COMING_SOON -> {
                episodeMessage = comingSoonText
                messageTarget = season to episode
                return
            }
            EpisodeAvailability.FUTURE -> {
                episodeAirDates?.get(key)?.get(episode)?.let {
                    episodeMessage = futureEpisodeMessage(it)
                    messageTarget = season to episode
                }
                return
            }
            EpisodeAvailability.AVAILABLE -> Unit
        }

        val count = countOf(season)
        val current = resolvedEpisodes(season)
        val isMarking = episode !in current
        val newEps = if (isMarking) (current + episode).sorted() else current.filter { it != episode }
        val newWatchedEpisodes = watchedEpisodes + (key to newEps)
        val availableCount = availableEpisodes(season).size
        val effectiveCount = if (availableCount > 0) availableCount else count
        val allWatched = effectiveCount > 0 && newEps.size >= effectiveCount
        val newWatchedSeasons = if (allWatched) (watchedSeasons + season).distinct().sorted() else watchedSeasons.filter { it != season }
        onChange(newWatchedSeasons, newWatchedEpisodes)

        if (isMarking && episode > 1) {
            val hasPrevUnwatched = (1 until episode).any { it !in newEps }
            if (hasPrevUnwatched) {
                val filled = (newEps + (1..episode)).distinct().sorted()
                val filledEpisodes = newWatchedEpisodes + (key to filled)
                val filledAll = count > 0 && filled.size >= count
                val filledSeasons = if (filledAll) (newWatchedSeasons + season).distinct().sorted() else newWatchedSeasons.filter { it != season }
                pendingFill = PendingFill.Episodes(episode - 1) { onChange(filledSeasons, filledEpisodes) }
            }
        }
        if (!isMarking) {
            val nextWatched = newEps.count { it > episode }
            if (nextWatched > 0) {
                val trimmed = newEps.filter { it <= episode }
                val trimmedEpisodes = newWatchedEpisodes + (key to trimmed)
                val trimmedSeasons = newWatchedSeasons.filter { it != season }
                pendingFill = PendingFill.RemoveFollowing(nextWatched) { onChange(trimmedSeasons, trimmedEpisodes) }
            }
        }
    }

    fun toggleAllEpisodes(season: Int) {
        if (readonly || onChange == null) return
        clearTransient()
        val key = season.toString()
        val count = countOf(season)
        if (count == 0) return
        val current = resolvedEpisodes(season)
        val available = availableEpisodes(season)
        val allAvailableWatched = available.isNotEmpty() && available.all { it in current }
        val newEps = if (allAvailableWatched) emptyList() else available
        val newWatchedEpisodes = watchedEpisodes + (key to newEps)
        val availableCount = if (available.isNotEmpty()) available.size else count
        val fullyWatched = newEps.size >= availableCount
        val newWatchedSeasons = when {
            allAvailableWatched -> watchedSeasons.filter { it != season }
            fullyWatched -> (watchedSeasons + season).distinct().sorted()
            else -> watchedSeasons.filter { it != season }
        }
        onChange(newWatchedSeasons, newWatchedEpisodes)

        if (!allAvailableWatched && season > 1) {
            val hasPrevUnwatched = (1 until season).any { it !in watchedSeasons }
            if (hasPrevUnwatched) {
                val filledSeasons = (newWatchedSeasons + (1..season)).distinct().sorted()
                pendingFill = PendingFill.Seasons(season - 1) { onChange(filledSeasons, newWatchedEpisodes) }
            }
        }
    }

    /** "Mark all" for a season whose episode count is unknown: toggles the whole season. */
    fun toggleWholeSeason(season: Int) {
        if (readonly || onChange == null) return
        clearTransient()
        val isMarking = season !in watchedSeasons
        val next = if (isMarking) (watchedSeasons + season).sorted() else watchedSeasons.filter { it != season }
        onChange(next, watchedEpisodes)
        if (isMarking && season > 1) {
            val hasPrevUnwatched = (1 until season).any { it !in watchedSeasons }
            if (hasPrevUnwatched) {
                val filled = (next + (1..season)).distinct().sorted()
                pendingFill = PendingFill.Seasons(season - 1) { onChange(filled, watchedEpisodes) }
            }
        }
    }

    val watchedCount = (1..totalSeasons).count { isSeasonWatched(it) }

    Column(modifier.fillMaxWidth()) {
        if (!compact) {
            Text(
                pluralStringResource(Res.plurals.seriesDetail_seasonsProgress, totalSeasons, watchedCount, totalSeasons),
                style = NookTheme.type.sm, color = colors.textSubtle,
            )
            Spacer(Modifier.height(8.dp))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..totalSeasons).forEach { season ->
                val watched = isSeasonWatched(season)
                val partial = isSeasonPartial(season)
                val empty = isSeasonEmpty(season)
                val expanded = expandedSeason == season
                val background = when {
                    empty -> colors.surfaceMuted
                    watched -> Palette.Emerald500
                    partial -> Palette.Blue400
                    else -> colors.surfaceMuted
                }
                val content = when {
                    empty -> colors.textFaint
                    watched || partial -> Palette.White
                    else -> colors.textSubtle
                }
                val ring = when {
                    empty -> Palette.Gray300
                    watched -> Palette.Emerald300
                    partial -> Palette.Blue400.alpha(0.6f)
                    else -> Palette.Amber300
                }
                Box(
                    Modifier
                        .defaultMinSize(minWidth = if (compact) 48.dp else 56.dp)
                        .clip(NookShapes.xl)
                        .background(background, NookShapes.xl)
                        .then(if (expanded) Modifier.border(2.dp, ring, NookShapes.xl) else Modifier)
                        .clickable(enabled = !readonly) {
                            clearTransient()
                            val next = if (expandedSeason == season) null else season
                            expandedSeason = next
                            if (next != null && episodesEnabled && episodeCounts?.get(season.toString()) == null) onSeasonExpand?.invoke(season)
                            onSeasonToggle?.invoke(next)
                        }
                        .padding(horizontal = if (compact) 12.dp else 16.dp, vertical = if (compact) 4.dp else 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("S$season", style = if (compact) NookTheme.type.sans(12, FontWeight.Medium, 16) else NookTheme.type.sans(14, FontWeight.Medium, 20), color = content, maxLines = 1)
                }
            }
        }

        if (!compact && expandedSeason == null) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(Res.string.seriesDetail_clickSeasonHint), style = NookTheme.type.tiny, color = colors.textFaint)
        }

        val season = expandedSeason
        if (!compact && season != null) {
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.fillMaxWidth().clip(NookShapes.xl).background(colors.surfaceSubtle).border(1.dp, colors.divider, NookShapes.xl).padding(12.dp),
            ) {
                val epCount = episodeCounts?.get(season.toString())
                val isEmpty = epCount == 0
                val canMark = epCount == null || epCount == 0 || availableEpisodes(season).isNotEmpty()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f)) {
                        Text("${stringResource(Res.string.seriesDetail_season)} $season", style = NookTheme.type.sans(12, FontWeight.SemiBold, 16), color = colors.textMuted)
                        if (epCount != null && !isEmpty) {
                            val watchedEps = if (isSeasonWatched(season)) epCount else watchedEpisodes[season.toString()]?.size ?: 0
                            Text(" — $watchedEps/$epCount ${stringResource(Res.string.seriesDetail_episodes)}", style = NookTheme.type.xs, color = colors.textFaint)
                        }
                    }
                    if (!readonly && onChange != null && !isEmpty && canMark) {
                        TextLink(
                            if (isSeasonWatched(season)) stringResource(Res.string.seriesDetail_unmarkAll) else stringResource(Res.string.seriesDetail_markAll),
                            onClick = { if ((epCount ?: 0) > 0) toggleAllEpisodes(season) else toggleWholeSeason(season) },
                            color = colors.amberText,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                when {
                    episodesEnabled && loadingEpisodesSeason == season -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spinner(size = 13.dp, color = colors.textFaint, trackColor = colors.surfaceMuted2)
                        Text(stringResource(Res.string.seriesDetail_loadingEpisodes), style = NookTheme.type.xs, color = colors.textFaint)
                    }
                    isEmpty -> Text(comingSoonText, style = NookTheme.type.xs.copy(fontStyle = FontStyle.Italic), color = colors.textFaint)
                    epCount != null -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val resolved = resolvedEpisodes(season)
                            (1..epCount).forEach { ep ->
                                val isWatched = ep in resolved
                                val unavailable = availability(season, ep) != EpisodeAvailability.AVAILABLE
                                val isTarget = messageTarget == (season to ep)
                                Box(
                                    Modifier
                                        .size(32.dp)
                                        .clip(NookShapes.md)
                                        .background(if (isWatched) Palette.Emerald500 else if (colors.isDark) Palette.Gray700 else Palette.Gray100, NookShapes.md)
                                        .then(if (isTarget) Modifier.border(2.dp, Palette.Amber400, NookShapes.md) else Modifier)
                                        .clickable(enabled = !readonly) { toggleEpisode(season, ep) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        ep.toString(),
                                        style = NookTheme.type.sans(12, FontWeight.Medium, 16),
                                        color = if (isWatched) Palette.White else if (unavailable) colors.textFaint else colors.textSubtle,
                                    )
                                }
                            }
                        }
                        episodeMessage?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                it,
                                style = NookTheme.type.xs,
                                color = colors.textBody2,
                                modifier = Modifier.fillMaxWidth().clip(NookShapes.lg).background(colors.surfaceMuted).border(1.dp, colors.borderNeutral, NookShapes.lg).padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                    !episodesEnabled -> Unit
                    else -> Text(stringResource(Res.string.seriesDetail_noEpisodeData), style = NookTheme.type.xs, color = colors.textFaint)
                }
                pendingFill?.let { fill ->
                    if (!readonly) {
                        Spacer(Modifier.height(8.dp))
                        PendingFillBanner(
                            fill = fill,
                            onYes = { fill.apply(); pendingFill = null },
                            onNo = { pendingFill = null },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingFillBanner(fill: Any, onYes: () -> Unit, onNo: () -> Unit) {
    val colors = NookTheme.colors
    val label = when (fill) {
        is PendingFill.RemoveFollowing -> pluralStringResource(Res.plurals.seriesDetail_removeFollowingEpisodes, fill.count, fill.count)
        is PendingFill.Episodes -> pluralStringResource(Res.plurals.seriesDetail_addPreviousEpisodes, fill.upTo, fill.upTo)
        is PendingFill.Seasons -> pluralStringResource(Res.plurals.seriesDetail_addPreviousSeasons, fill.upTo, fill.upTo)
        else -> return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(NookShapes.xl)
            .background(if (colors.isDark) Palette.Amber900.alpha(0.2f) else Palette.Amber50, NookShapes.xl)
            .border(1.dp, if (colors.isDark) Palette.Amber700.alpha(0.5f) else Palette.Amber200, NookShapes.xl)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = NookTheme.type.sans(14, FontWeight.Medium, 22), color = if (colors.isDark) Palette.Amber200 else Palette.Amber900)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Spacer(Modifier.weight(1f))
            GhostButton(
                stringResource(Res.string.seriesDetail_no),
                onClick = onNo,
                borderColor = colors.borderNeutral,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            )
            PrimaryButton(
                stringResource(Res.string.seriesDetail_yes),
                onClick = onYes,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                textStyle = NookTheme.type.sans(14, FontWeight.SemiBold, 20),
            )
        }
    }
}
