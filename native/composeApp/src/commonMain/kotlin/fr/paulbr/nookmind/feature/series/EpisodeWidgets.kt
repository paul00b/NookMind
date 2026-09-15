package fr.paulbr.nookmind.feature.series

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.alpha
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.components.Pill
import fr.paulbr.nookmind.core.designsystem.components.SheetCloseButton
import fr.paulbr.nookmind.core.designsystem.components.SkeletonBox
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.getRatingStyle
import fr.paulbr.nookmind.core.domain.toFixed1
import fr.paulbr.nookmind.core.model.EpisodeRating
import fr.paulbr.nookmind.core.model.TmdbEpisode
import fr.paulbr.nookmind.core.network.ImdbApi
import fr.paulbr.nookmind.core.platform.DateStyle
import fr.paulbr.nookmind.core.platform.openExternalUrl
import fr.paulbr.nookmind.feature.common.formatIsoDate
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.common_episodeNumber
import fr.paulbr.nookmind.resources.nextUp_episodeSynopsis
import fr.paulbr.nookmind.resources.nextUp_noEpisodeOverview
import fr.paulbr.nookmind.resources.seriesDetail_noEpisodeData
import fr.paulbr.nookmind.resources.seriesDetail_viewOnImdb
import org.jetbrains.compose.resources.stringResource

/** One episode tile: TMDB data when available, otherwise the IMDb row alone. */
class EpisodeInfo(val episodeNum: Int, val tmdb: TmdbEpisode?, val imdb: EpisodeRating?)

/** Port of EpisodeRatingBadge.tsx: IMDb rating on the colour scale, else TMDB vote in amber. */
@Composable
fun EpisodeRatingBadge(imdb: EpisodeRating?, tmdb: TmdbEpisode?, medium: Boolean = false, modifier: Modifier = Modifier) {
    val imdbRating = imdb?.imdbRating
    if (imdbRating != null) {
        val style = getRatingStyle(imdbRating)
        Text(
            imdbRating.toFixed1(),
            modifier = modifier
                .clip(if (medium) NookShapes.lg else NookShapes.md)
                .background(Color(style.background))
                .padding(horizontal = if (medium) 12.dp else 6.dp, vertical = if (medium) 6.dp else 2.dp),
            style = if (medium) NookTheme.type.sans(14, FontWeight.ExtraBold, 20) else NookTheme.type.sans(10, FontWeight.ExtraBold, 14),
            color = Color(style.foreground),
        )
        return
    }
    val vote = tmdb?.voteAverage
    if (vote != null && vote > 0) {
        if (medium) {
            Pill(
                vote.toFixed1(),
                background = Palette.Amber500.alpha(0.10f),
                color = NookTheme.colors.amberTextStrong,
                modifier = modifier,
                icon = LucideIcons.Star, iconSize = 11.dp,
                textStyle = NookTheme.type.sans(12, FontWeight.SemiBold, 16),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            )
        } else {
            Row(
                modifier.clip(NookShapes.md).background(Palette.Amber500).padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(LucideIcons.Star, null, Modifier.size(9.dp), tint = Palette.White)
                Text(vote.toFixed1(), style = NookTheme.type.sans(10, FontWeight.ExtraBold, 14), color = Palette.White)
            }
        }
    }
}

/** The 2-column episode tiles of the detail / preview sheets (`grid grid-cols-2 gap-2 p-4`). */
@Composable
fun EpisodeTilesGrid(episodes: List<EpisodeInfo>, loading: Boolean, onSelect: (EpisodeInfo) -> Unit, modifier: Modifier = Modifier) {
    val colors = NookTheme.colors
    when {
        loading -> Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(2) {
                        Column(Modifier.weight(1f).clip(NookShapes.xl).background(colors.surfaceMuted)) {
                            SkeletonBox(Modifier.fillMaxWidth().aspectRatio(16f / 9f), shape = NookShapes.sm, color = colors.surfaceMuted)
                            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                SkeletonBox(Modifier.width(32.dp).height(8.dp), shape = NookShapes.sm)
                                SkeletonBox(Modifier.fillMaxWidth().height(12.dp), shape = NookShapes.sm)
                            }
                        }
                    }
                }
            }
        }
        episodes.isEmpty() -> Text(
            stringResource(Res.string.seriesDetail_noEpisodeData),
            style = NookTheme.type.sm, color = colors.textFaint,
            modifier = modifier.fillMaxWidth().padding(16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        else -> Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            episodes.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { info -> EpisodeTile(info, Modifier.weight(1f)) { onSelect(info) } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EpisodeTile(info: EpisodeInfo, modifier: Modifier, onClick: () -> Unit) {
    val colors = NookTheme.colors
    val name = info.tmdb?.name?.ifBlank { null } ?: info.imdb?.title?.ifBlank { null } ?: stringResource(Res.string.common_episodeNumber, info.episodeNum)
    Column(modifier.clip(NookShapes.xl).background(colors.surfaceSubtle).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(colors.surfaceMuted2)) {
            val still = info.tmdb?.stillPath
            if (!still.isNullOrBlank()) {
                AsyncImage(model = "https://image.tmdb.org/t/p/w185$still", contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(LucideIcons.Tv, null, Modifier.size(18.dp).align(Alignment.Center), tint = colors.textFaint)
            }
            EpisodeRatingBadge(info.imdb, info.tmdb, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
        Column(Modifier.padding(8.dp)) {
            Text("E${info.episodeNum}", style = NookTheme.type.micro, color = colors.textFaint)
            Spacer(Modifier.height(2.dp))
            Text(name, style = NookTheme.type.sans(12, FontWeight.Medium, 16), color = colors.textBody, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The compact episode sheet of the detail / preview sheets (still, SxEy, name, rating, synopsis, IMDb link). */
@Composable
fun EpisodeDetailSheet(info: EpisodeInfo, seasonNum: Int, onClose: () -> Unit) {
    val colors = NookTheme.colors
    val name = info.tmdb?.name?.ifBlank { null } ?: info.imdb?.title?.ifBlank { null } ?: stringResource(Res.string.common_episodeNumber, info.episodeNum)
    val still = info.tmdb?.stillPath?.let { "https://image.tmdb.org/t/p/w400$it" }
    NookSheet(onClose = onClose, maxWidth = 512.dp) { controller ->
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp)) {
                if (still != null) {
                    Box(Modifier.padding(horizontal = 12.dp).fillMaxWidth().aspectRatio(16f / 9f).clip(NookShapes.xl).background(colors.surfaceMuted)) {
                        AsyncImage(model = still, contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
                Column(Modifier.padding(start = 24.dp, end = 24.dp, top = if (still != null) 16.dp else 24.dp, bottom = 24.dp)) {
                    Text("S${seasonNum}E${info.episodeNum}", style = NookTheme.type.xs, color = colors.textFaint)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(name, style = NookTheme.type.titleSerif.copy(lineHeight = 22.sp), color = colors.textStrong, modifier = Modifier.weight(1f))
                        EpisodeRatingBadge(info.imdb, info.tmdb, medium = true)
                    }
                    val airDate = formatIsoDate(info.tmdb?.airDate, DateStyle.DAY_MONTH_LONG_YEAR)
                    val runtime = info.tmdb?.runtime?.takeIf { it > 0 }
                    if (airDate != null || runtime != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            airDate?.let { Text(it, style = NookTheme.type.xs, color = colors.textFaint) }
                            runtime?.let { Text("$it min", style = NookTheme.type.xs, color = colors.textFaint) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    val overview = info.tmdb?.overview?.trim()
                    if (!overview.isNullOrBlank()) {
                        Text(stringResource(Res.string.nextUp_episodeSynopsis).uppercase(), style = NookTheme.type.sans(12, FontWeight.SemiBold, 16).copy(letterSpacing = 0.6.sp), color = colors.textFaint)
                        Spacer(Modifier.height(8.dp))
                        Text(overview, style = NookTheme.type.sans(14, lineHeight = 23), color = colors.textBody2)
                    } else {
                        Text(stringResource(Res.string.nextUp_noEpisodeOverview), style = NookTheme.type.sm.copy(fontStyle = FontStyle.Italic), color = colors.textFaint)
                    }
                    info.imdb?.imdbId?.let { id ->
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(Res.string.seriesDetail_viewOnImdb),
                            style = NookTheme.type.xs, color = colors.amberText,
                            modifier = Modifier.clickable { openExternalUrl(ImdbApi.titleUrl(id)) },
                        )
                    }
                }
            }
            SheetCloseButton(controller, Modifier.align(Alignment.TopEnd).padding(16.dp))
        }
    }
}
