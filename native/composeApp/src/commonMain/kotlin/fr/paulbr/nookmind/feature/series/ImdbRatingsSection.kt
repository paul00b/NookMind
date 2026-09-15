package fr.paulbr.nookmind.feature.series

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.GhostButton
import fr.paulbr.nookmind.core.designsystem.components.HairlineDivider
import fr.paulbr.nookmind.core.designsystem.components.SkeletonBox
import fr.paulbr.nookmind.core.designsystem.components.VerticalHairline
import fr.paulbr.nookmind.core.domain.ImdbStats
import fr.paulbr.nookmind.core.domain.computeImdbStats
import fr.paulbr.nookmind.core.domain.getRatingStyle
import fr.paulbr.nookmind.core.domain.toFixed1
import fr.paulbr.nookmind.core.model.SeasonState
import fr.paulbr.nookmind.core.network.ImdbApi
import fr.paulbr.nookmind.core.platform.openExternalUrl
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.seriesDetail_imdbAverage
import fr.paulbr.nookmind.resources.seriesDetail_imdbBest
import fr.paulbr.nookmind.resources.seriesDetail_imdbLoading
import fr.paulbr.nookmind.resources.seriesDetail_imdbNotAvailable
import fr.paulbr.nookmind.resources.seriesDetail_imdbRetry
import fr.paulbr.nookmind.resources.seriesDetail_imdbWorst
import org.jetbrains.compose.resources.stringResource

/**
 * Port of the "IMDB ratings by episode" section: average / best / worst header, the per-season
 * heat map (one column per season, one cell per episode, coloured by rating) and its legend.
 */
@Composable
fun ImdbRatingsSection(
    seasonRatings: Map<Int, SeasonState>,
    notFound: Boolean,
    loading: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    fallbackSeasons: Int = 3,
) {
    val colors = NookTheme.colors
    val stats = computeImdbStats(seasonRatings)
    val seasons = seasonRatings.keys.sorted()

    Column(modifier.fillMaxWidth()) {
        when {
            notFound -> Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.seriesDetail_imdbNotAvailable), style = NookTheme.type.sm, color = colors.textFaint, textAlign = TextAlign.Center)
                GhostButton(stringResource(Res.string.seriesDetail_imdbRetry), onClick = onRetry)
            }
            loading && seasons.isEmpty() -> Text(
                stringResource(Res.string.seriesDetail_imdbLoading),
                style = NookTheme.type.sm, color = colors.textFaint, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
            else -> {
                if (stats != null) ImdbStatsHeader(stats) else ImdbStatsSkeleton()
                HairlineDivider()
                if (seasons.isEmpty()) ImdbGridSkeleton(fallbackSeasons) else ImdbHeatMap(seasonRatings, seasons)
            }
        }
    }
}

@Composable
private fun ImdbStatsHeader(stats: ImdbStats) {
    val entries = listOf(
        stats.average to stringResource(Res.string.seriesDetail_imdbAverage),
        stats.best to stringResource(Res.string.seriesDetail_imdbBest),
        stats.worst to stringResource(Res.string.seriesDetail_imdbWorst),
    )
    Row(Modifier.fillMaxWidth().height(70.dp)) {
        entries.forEachIndexed { index, (value, label) ->
            val style = getRatingStyle(value.toDoubleOrNull())
            Column(Modifier.weight(1f).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    value,
                    modifier = Modifier.clip(NookShapes.md).background(Color(style.background)).padding(horizontal = 12.dp, vertical = 4.dp),
                    style = NookTheme.type.sans(14, FontWeight.ExtraBold, 20),
                    color = Color(style.foreground),
                )
                Text(label.uppercase(), style = NookTheme.type.sans(10, lineHeight = 14).copy(letterSpacing = 0.5.sp), color = NookTheme.colors.textSubtle)
            }
            if (index < entries.lastIndex) VerticalHairline(Modifier.height(70.dp))
        }
    }
}

@Composable
private fun ImdbStatsSkeleton() {
    Row(Modifier.fillMaxWidth().height(70.dp)) {
        repeat(3) { index ->
            Column(Modifier.weight(1f).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonBox(Modifier.width(56.dp).height(28.dp), shape = NookShapes.md)
                SkeletonBox(Modifier.width(40.dp).height(10.dp), shape = NookShapes.sm)
            }
            if (index < 2) VerticalHairline(Modifier.height(70.dp))
        }
    }
}

@Composable
private fun ImdbGridSkeleton(seasons: Int) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(Modifier.height(24.dp))
            repeat(6) { SkeletonBox(Modifier.size(28.dp), shape = NookShapes.sm) }
        }
        repeat(seasons.coerceAtLeast(1)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                SkeletonBox(Modifier.width(32.dp).height(18.dp), shape = NookShapes.sm)
                Spacer(Modifier.height(0.dp))
                repeat(6) { SkeletonBox(Modifier.width(44.dp).height(28.dp), shape = NookShapes.sm) }
            }
        }
    }
}

@Composable
private fun ImdbHeatMap(seasonRatings: Map<Int, SeasonState>, seasons: List<Int>) {
    val colors = NookTheme.colors
    val maxEpisodes = seasons.maxOfOrNull { (seasonRatings[it] as? SeasonState.Loaded)?.episodes?.size ?: 0 } ?: 0
    val rowCount = if (maxEpisodes > 0) maxEpisodes else 6

    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Spacer(Modifier.height(24.dp))
                repeat(rowCount) { index ->
                    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                        Text(if (maxEpisodes > 0) "E${index + 1}" else "", style = NookTheme.type.sans(10, FontWeight.Medium, 14), color = colors.textFaint)
                    }
                }
            }
            seasons.forEach { season ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                        Text("S$season", style = NookTheme.type.sans(10, FontWeight.SemiBold, 14), color = colors.textSubtle)
                    }
                    when (val state = seasonRatings[season]) {
                        SeasonState.Loading -> repeat(6) { SkeletonBox(Modifier.width(44.dp).height(28.dp), shape = NookShapes.sm) }
                        SeasonState.Error -> Box(Modifier.width(44.dp).height(28.dp).clip(NookShapes.sm).background(colors.surfaceMuted2), contentAlignment = Alignment.Center) {
                            Text("—", style = NookTheme.type.sans(9, lineHeight = 12), color = colors.textFaint)
                        }
                        is SeasonState.Loaded -> state.episodes.forEach { episode ->
                            val style = getRatingStyle(episode.imdbRating)
                            Box(
                                Modifier
                                    .width(44.dp).height(28.dp)
                                    .clip(NookShapes.sm)
                                    .background(Color(style.background), NookShapes.sm)
                                    .then(if (episode.imdbId != null) Modifier.clickable { openExternalUrl(ImdbApi.titleUrl(episode.imdbId)) } else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(episode.imdbRating?.toFixed1() ?: "N/A", style = NookTheme.type.sans(12, FontWeight.Bold, 16), color = Color(style.foreground))
                            }
                        }
                        null -> Unit
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("9–10" to 9.5, "8–9" to 8.5, "7–8" to 7.5, "6–7" to 6.5, "5–6" to 5.5, "<5" to 4.0).forEach { (label, value) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(12.dp).clip(NookShapes.sm).background(Color(getRatingStyle(value).background)))
                    Text(label, style = NookTheme.type.sans(10, lineHeight = 14), color = colors.textSubtle)
                }
            }
        }
    }
}
