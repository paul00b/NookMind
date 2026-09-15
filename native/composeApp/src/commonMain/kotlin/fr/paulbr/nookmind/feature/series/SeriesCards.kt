package fr.paulbr.nookmind.feature.series

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.components.GenrePill
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.NookCard
import fr.paulbr.nookmind.core.designsystem.components.SolidPill
import fr.paulbr.nookmind.core.designsystem.components.StarRating
import fr.paulbr.nookmind.core.designsystem.components.StatusBadge
import fr.paulbr.nookmind.core.domain.formatWaitingLabel
import fr.paulbr.nookmind.core.domain.getEffectiveSeriesStatus
import fr.paulbr.nookmind.core.domain.isSeriesWaiting
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesStatus
import fr.paulbr.nookmind.core.platform.DateStyle
import fr.paulbr.nookmind.core.platform.formatLocalDate
import fr.paulbr.nookmind.feature.common.LocalWideLayout
import fr.paulbr.nookmind.feature.library.CardRemoveButton
import fr.paulbr.nookmind.feature.library.LibraryListRow
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.seriesCard_waitingDays
import fr.paulbr.nookmind.resources.seriesCard_waitingNextSeason
import fr.paulbr.nookmind.resources.seriesCard_waitingTomorrow
import fr.paulbr.nookmind.resources.seriesCard_wantToWatch
import fr.paulbr.nookmind.resources.seriesCard_watched
import fr.paulbr.nookmind.resources.seriesCard_watching
import org.jetbrains.compose.resources.stringResource

/** `formatWaitingLabel` with the localized pieces of `seriesCard.*` (empty [fallback] for the first-air variant). */
@Composable
fun waitingLabel(date: String?, fallback: String = stringResource(Res.string.seriesCard_waitingNextSeason)): String {
    val tomorrow = stringResource(Res.string.seriesCard_waitingTomorrow)
    val inDaysTemplate = stringResource(Res.string.seriesCard_waitingDays, 0)
    return formatWaitingLabel(
        nextAirDate = date,
        fallback = fallback,
        tomorrowLabel = tomorrow,
        inDaysLabel = { days -> inDaysTemplate.replace("0", days.toString()) },
        formatDayMonth = { formatLocalDate(it, DateStyle.DAY_MONTH_SHORT) },
        formatMonthYear = { formatLocalDate(it, DateStyle.MONTH_SHORT_YEAR) },
    )
}

/** Label + colour of the status badge of a series (SeriesCard.tsx rules). */
class SeriesBadge(val label: String, val color: Color)

@Composable
fun seriesBadge(series: Series): SeriesBadge {
    val effective = getEffectiveSeriesStatus(series)
    val waiting = isSeriesWaiting(series)
    val futureFirstEpisode = if (effective == SeriesStatus.WANT_TO_WATCH) waitingLabel(series.firstAirDate, fallback = "") else ""
    return when {
        effective == SeriesStatus.WATCHED -> SeriesBadge(stringResource(Res.string.seriesCard_watched), Palette.Emerald500)
        waiting -> SeriesBadge(waitingLabel(series.nextAirDate), Palette.Purple500)
        effective == SeriesStatus.WATCHING -> SeriesBadge("S${series.watchedSeasons.size}/${series.seasons ?: "?"}", Palette.Blue500)
        futureFirstEpisode.isNotEmpty() -> SeriesBadge(futureFirstEpisode, Palette.Sky500)
        else -> SeriesBadge(stringResource(Res.string.seriesCard_wantToWatch), Palette.Amber500)
    }
}

/** Badge of the list rows (SeriesListRow of SeriesLibrary.tsx: text labels instead of S x/y). */
@Composable
fun seriesRowBadge(series: Series): SeriesBadge {
    val effective = getEffectiveSeriesStatus(series)
    return when {
        isSeriesWaiting(series) -> SeriesBadge(waitingLabel(series.nextAirDate), Palette.Purple500)
        effective == SeriesStatus.WATCHED -> SeriesBadge(stringResource(Res.string.seriesCard_watched), Palette.Emerald500)
        effective == SeriesStatus.WATCHING -> SeriesBadge(stringResource(Res.string.seriesCard_watching), Palette.Blue500)
        else -> SeriesBadge(stringResource(Res.string.seriesCard_wantToWatch), Palette.Amber500)
    }
}

/** Port of SeriesCard.tsx. */
@Composable
fun SeriesCard(series: Series, onClick: () -> Unit, modifier: Modifier = Modifier, onRemove: (() -> Unit)? = null) {
    val badge = seriesBadge(series)
    val effective = getEffectiveSeriesStatus(series)
    NookCard(modifier, onClick = onClick) {
        Box {
            MediaImage(series.posterUrl, series.title, Modifier.fillMaxWidth(), mode = MediaMode.SERIES) {
                StatusBadge(badge.label, badge.color, Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
            if (onRemove != null) CardRemoveButton(onRemove, Modifier.align(Alignment.TopStart))
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(series.title, style = NookTheme.type.cardTitleSerif, color = NookTheme.colors.textStrong, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(series.creator, style = NookTheme.type.xs, color = NookTheme.colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (effective == SeriesStatus.WATCHED && series.rating != null) StarRating(series.rating, size = 13.dp)
        }
    }
}

/** Port of SeriesListRow (SeriesLibrary.tsx). */
@Composable
fun SeriesListRow(series: Series, onClick: () -> Unit, onRemove: (() -> Unit)? = null) {
    val wide = LocalWideLayout.current
    val effective = getEffectiveSeriesStatus(series)
    val badge = seriesRowBadge(series)
    LibraryListRow(series.posterUrl, series.title, series.creator, onClick, mode = MediaMode.SERIES, onRemove = onRemove) {
        if (wide && !series.genre.isNullOrBlank()) GenrePill(series.genre, small = true)
        if (wide && effective == SeriesStatus.WATCHED && series.rating != null) StarRating(series.rating, size = 12.dp)
        SolidPill(badge.label, badge.color, small = true)
    }
}
