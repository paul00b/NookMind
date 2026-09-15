package fr.paulbr.nookmind.feature.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.alpha
import fr.paulbr.nookmind.core.designsystem.components.ExpandableDescription
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.components.SheetCloseButton
import fr.paulbr.nookmind.core.designsystem.components.Spinner
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.model.Movie
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.TmdbCredit
import fr.paulbr.nookmind.core.model.TmdbPerson
import fr.paulbr.nookmind.core.network.TmdbApi
import fr.paulbr.nookmind.core.platform.DateStyle
import fr.paulbr.nookmind.feature.common.formatIsoDate
import fr.paulbr.nookmind.feature.series.AddSeriesSheet
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.actorSheet_birthplace
import fr.paulbr.nookmind.resources.actorSheet_born
import fr.paulbr.nookmind.resources.actorSheet_diedOn
import fr.paulbr.nookmind.resources.actorSheet_filmography
import fr.paulbr.nookmind.resources.actorSheet_noBiography
import fr.paulbr.nookmind.resources.movieDetail_seeLess
import fr.paulbr.nookmind.resources.movieDetail_seeMore
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Port of ActorSheet.tsx: photo, name, birth line, biography, filmography. Tapping a credit opens
 * the add sheet of that movie / series on top (stacked sheets, like the web z-index tiers).
 */
@Composable
fun ActorSheet(container: AppContainer, personId: Int, onClose: () -> Unit) {
    val colors = NookTheme.colors
    val scope = rememberCoroutineScope()
    var person by remember(personId) { mutableStateOf<TmdbPerson?>(null) }
    var selectingCreditId by remember { mutableStateOf<Int?>(null) }
    var moviePrefill by remember { mutableStateOf<Movie?>(null) }
    var seriesPrefill by remember { mutableStateOf<Series?>(null) }

    LaunchedEffect(personId) { person = container.tmdb.fetchPersonDetails(personId) }

    val filmography = (person?.combinedCredits?.cast ?: emptyList())
        .filter { !it.posterPath.isNullOrBlank() }
        .sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title ?: it.name ?: "" })
        .take(20)

    fun select(credit: TmdbCredit) {
        if (selectingCreditId != null) return
        selectingCreditId = credit.id
        scope.launch {
            if (credit.mediaType == "movie") {
                container.tmdb.fetchMovieDetails(credit.id)?.let { moviePrefill = TmdbApi.extractMovieData(it) }
            } else {
                container.tmdb.fetchSeriesDetails(credit.id)?.let { seriesPrefill = TmdbApi.extractSeriesData(it) }
            }
            selectingCreditId = null
        }
    }

    NookSheet(onClose = onClose, maxWidth = 512.dp) { controller ->
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.width(80.dp).aspectRatio(2f / 3f).clip(NookShapes.lg).background(colors.surfaceMuted), contentAlignment = Alignment.Center) {
                        val photo = TmdbApi.posterUrl(person?.profilePath)
                        if (photo != null) {
                            AsyncImage(model = photo, contentDescription = person?.name, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f), contentScale = ContentScale.Crop)
                        } else {
                            Icon(LucideIcons.User, null, Modifier.size(24.dp), tint = colors.textFaint)
                        }
                    }
                    Column(Modifier.weight(1f).padding(top = 4.dp, end = 32.dp)) {
                        Text(person?.name ?: "", style = NookTheme.type.titleSerif.copy(lineHeight = 22.sp()), color = colors.textStrong)
                        val p = person
                        if (p?.birthday != null) {
                            Spacer(Modifier.height(4.dp))
                            val dateText = formatIsoDate(p.deathday ?: p.birthday, DateStyle.DAY_MONTH_LONG_YEAR) ?: (p.deathday ?: p.birthday)
                            val line = if (p.deathday != null) stringResource(Res.string.actorSheet_diedOn, dateText) else stringResource(Res.string.actorSheet_born, dateText)
                            val place = p.placeOfBirth?.takeIf { it.isNotBlank() }?.let { " " + stringResource(Res.string.actorSheet_birthplace, it) } ?: ""
                            Text(line + place, style = NookTheme.type.xs, color = colors.textSubtle)
                        }
                    }
                }
                person?.let { p ->
                    Box(Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp)) {
                        ExpandableDescription(
                            description = p.biography.ifBlank { stringResource(Res.string.actorSheet_noBiography) },
                            seeMoreText = stringResource(Res.string.movieDetail_seeMore),
                            seeLessText = stringResource(Res.string.movieDetail_seeLess),
                        )
                    }
                }
                if (filmography.isNotEmpty()) {
                    Column(Modifier.padding(bottom = 24.dp)) {
                        Text(stringResource(Res.string.actorSheet_filmography), style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = colors.textBody2, modifier = Modifier.padding(horizontal = 24.dp))
                        Spacer(Modifier.height(12.dp))
                        LazyRow(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filmography, key = { "${it.mediaType}-${it.id}" }) { credit ->
                                val busy = selectingCreditId != null
                                Column(
                                    Modifier.width(80.dp).alpha(if (busy && selectingCreditId != credit.id) 0.6f else 1f)
                                        .clip(NookShapes.lg).clickable(enabled = !busy) { select(credit) },
                                ) {
                                    Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(NookShapes.lg).background(colors.surfaceMuted)) {
                                        AsyncImage(model = TmdbApi.posterUrl(credit.posterPath), contentDescription = credit.title ?: credit.name, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f), contentScale = ContentScale.Crop)
                                        if (selectingCreditId == credit.id) {
                                            Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).background(Palette.Black.alpha(0.4f)), contentAlignment = Alignment.Center) {
                                                Spinner(size = 16.dp, color = Palette.White, trackColor = Palette.White.alpha(0.4f))
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(credit.title ?: credit.name ?: "", style = NookTheme.type.sans(11, FontWeight.Medium, 13), color = colors.textBody2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
            SheetCloseButton(controller, Modifier.align(Alignment.TopEnd).padding(16.dp))
        }
    }

    moviePrefill?.let { AddMovieSheet(container, it, onClose = { moviePrefill = null }) }
    seriesPrefill?.let { AddSeriesSheet(container, it, onClose = { seriesPrefill = null }) }
}

private fun Int.sp() = androidx.compose.ui.unit.TextUnit(toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
