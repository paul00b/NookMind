package fr.paulbr.nookmind.feature.series

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.alpha
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.components.SheetCloseButton
import fr.paulbr.nookmind.core.designsystem.components.SkeletonBox
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.TmdbSeriesStats
import fr.paulbr.nookmind.core.domain.computeLocalStats
import fr.paulbr.nookmind.core.domain.computeTmdbStats
import fr.paulbr.nookmind.core.domain.relevantSeriesForStats
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.seriesStats_avgRating
import fr.paulbr.nookmind.resources.seriesStats_days
import fr.paulbr.nookmind.resources.seriesStats_episodes
import fr.paulbr.nookmind.resources.seriesStats_hours
import fr.paulbr.nookmind.resources.seriesStats_minutes
import fr.paulbr.nookmind.resources.seriesStats_seasons
import fr.paulbr.nookmind.resources.seriesStats_series
import fr.paulbr.nookmind.resources.seriesStats_subtitle
import fr.paulbr.nookmind.resources.seriesStats_title
import fr.paulbr.nookmind.resources.seriesStats_topCreator
import fr.paulbr.nookmind.resources.seriesStats_topGenre
import fr.paulbr.nookmind.resources.seriesStats_totalWatchTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** `formatHours` of SeriesStatsSheet.tsx with the plural resources. */
@Composable
fun formatWatchTime(minutes: Int): String {
    val totalHours = minutes / 60
    val m = minutes % 60
    if (totalHours < 24) {
        if (totalHours == 0) return pluralStringResource(Res.plurals.seriesStats_minutes, m, m)
        if (m == 0) return pluralStringResource(Res.plurals.seriesStats_hours, totalHours, totalHours)
        return pluralStringResource(Res.plurals.seriesStats_hours, totalHours, totalHours) + " $m min"
    }
    val d = totalHours / 24
    val h = totalHours % 24
    if (h == 0) return pluralStringResource(Res.plurals.seriesStats_days, d, d)
    return pluralStringResource(Res.plurals.seriesStats_days, d, d) + " " + pluralStringResource(Res.plurals.seriesStats_hours, h, h)
}

/** Port of SeriesStatsSheet.tsx: total watch time hero + six fun-fact cards. */
@Composable
fun SeriesStatsSheet(container: AppContainer, series: List<Series>, onClose: () -> Unit) {
    val colors = NookTheme.colors
    val relevant = remember(series) { relevantSeriesForStats(series) }
    val local = remember(relevant) { computeLocalStats(relevant) }
    var tmdbStats by remember { mutableStateOf<TmdbSeriesStats?>(null) }
    var loadingMinutes by remember { mutableStateOf(true) }
    LaunchedEffect(relevant.size) {
        if (relevant.isEmpty()) {
            tmdbStats = TmdbSeriesStats(0, 0)
            loadingMinutes = false
            return@LaunchedEffect
        }
        loadingMinutes = true
        tmdbStats = runCatching { computeTmdbStats(relevant, container.tmdb) }.getOrNull()
        loadingMinutes = false
    }

    NookSheet(onClose = onClose, maxWidth = 448.dp, background = if (colors.isDark) Palette.NightCard else Palette.Cream) { controller ->
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(48.dp).clip(NookShapes.xl2).background(Palette.Teal500.alpha(0.10f)), contentAlignment = Alignment.Center) {
                        Icon(LucideIcons.BarChart2, null, Modifier.size(22.dp), tint = Palette.Teal500)
                    }
                    Column {
                        Text(stringResource(Res.string.seriesStats_title), style = NookTheme.type.titleSerif, color = colors.textStrong)
                        Text(stringResource(Res.string.seriesStats_subtitle), style = NookTheme.type.sans(12, FontWeight.Medium, 16), color = colors.tealText)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    Modifier.fillMaxWidth().clip(NookShapes.xl2).background(Palette.Teal500.alpha(if (colors.isDark) 0.15f else 0.10f)).padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(LucideIcons.Clock, null, Modifier.size(28.dp), tint = Palette.Teal500)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(Res.string.seriesStats_totalWatchTime), style = NookTheme.type.xs, color = colors.textSubtle)
                        Spacer(Modifier.height(4.dp))
                        if (loadingMinutes) {
                            SkeletonBox(Modifier.width(96.dp).height(32.dp), shape = NookShapes.lg, color = Palette.Teal500.alpha(0.2f))
                        } else {
                            val minutes = tmdbStats?.minutes
                            Text(if (minutes != null) formatWatchTime(minutes) else "—", style = NookTheme.type.h1Serif.copy(lineHeight = 34.sp()), color = colors.tealText)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                val episodes = tmdbStats?.episodes?.takeIf { it > 0 }?.toString()
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(LucideIcons.Tv, stringResource(Res.string.seriesStats_series), local.totalSeries.toString(), Modifier.weight(1f))
                        StatCard(LucideIcons.Film, stringResource(Res.string.seriesStats_episodes), episodes, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(LucideIcons.BarChart2, stringResource(Res.string.seriesStats_seasons), local.watchedSeasonsCount.takeIf { it > 0 }?.toString(), Modifier.weight(1f))
                        StatCard(LucideIcons.Star, stringResource(Res.string.seriesStats_avgRating), local.averageRating?.let { "${formatRating(it)}/5" }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(LucideIcons.Tag, stringResource(Res.string.seriesStats_topGenre), local.favoriteGenre, Modifier.weight(1f), small = true)
                        StatCard(LucideIcons.User, stringResource(Res.string.seriesStats_topCreator), local.favoriteCreator, Modifier.weight(1f), small = true)
                    }
                }
            }
            SheetCloseButton(controller, Modifier.align(Alignment.TopEnd).padding(16.dp), size = 18.dp)
        }
    }
}

private fun formatRating(value: Double): String = if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

private fun Int.sp() = androidx.compose.ui.unit.TextUnit(toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String?, modifier: Modifier = Modifier, small: Boolean = false) {
    val colors = NookTheme.colors
    Column(
        modifier.clip(NookShapes.xl2).background(Palette.Amber500.alpha(if (colors.isDark) 0.10f else 0.05f)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(14.dp), tint = colors.amberText)
            Text(label, style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(value ?: "—", style = if (small) NookTheme.type.h3Serif else NookTheme.type.h1Serif, color = colors.textStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
