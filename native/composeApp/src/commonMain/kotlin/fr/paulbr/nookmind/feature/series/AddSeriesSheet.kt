package fr.paulbr.nookmind.feature.series

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.components.BannerTone
import fr.paulbr.nookmind.core.designsystem.components.EditableNote
import fr.paulbr.nookmind.core.designsystem.components.ExpandableDescription
import fr.paulbr.nookmind.core.designsystem.components.GenrePill
import fr.paulbr.nookmind.core.designsystem.components.GhostButton
import fr.paulbr.nookmind.core.designsystem.components.InlineBanner
import fr.paulbr.nookmind.core.designsystem.components.LabeledBlock
import fr.paulbr.nookmind.core.designsystem.components.LabeledField
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.MetaPill
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.components.NookTextArea
import fr.paulbr.nookmind.core.designsystem.components.NookTextField
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.SheetCloseButton
import fr.paulbr.nookmind.core.designsystem.components.SheetController
import fr.paulbr.nookmind.core.designsystem.components.SolidPill
import fr.paulbr.nookmind.core.designsystem.components.StarRating
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.deriveSeriesStatus
import fr.paulbr.nookmind.core.domain.normalizeTitle
import fr.paulbr.nookmind.core.domain.yearOf
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesStatus
import fr.paulbr.nookmind.feature.common.SheetHeader
import fr.paulbr.nookmind.feature.common.TrailerButton
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.addSeries_addAnyway
import fr.paulbr.nookmind.resources.addSeries_addToList
import fr.paulbr.nookmind.resources.addSeries_alreadyInList
import fr.paulbr.nookmind.resources.addSeries_cancel
import fr.paulbr.nookmind.resources.addSeries_creatorLabel
import fr.paulbr.nookmind.resources.addSeries_creatorPlaceholder
import fr.paulbr.nookmind.resources.addSeries_descriptionLabel
import fr.paulbr.nookmind.resources.addSeries_edit
import fr.paulbr.nookmind.resources.addSeries_firstAirDateLabel
import fr.paulbr.nookmind.resources.addSeries_firstAirDatePlaceholder
import fr.paulbr.nookmind.resources.addSeries_genreLabel
import fr.paulbr.nookmind.resources.addSeries_genrePlaceholder
import fr.paulbr.nookmind.resources.addSeries_noteLabel
import fr.paulbr.nookmind.resources.addSeries_notePlaceholder
import fr.paulbr.nookmind.resources.addSeries_posterUrlLabel
import fr.paulbr.nookmind.resources.addSeries_posterUrlPlaceholder
import fr.paulbr.nookmind.resources.addSeries_ratingLabel
import fr.paulbr.nookmind.resources.addSeries_saving
import fr.paulbr.nookmind.resources.addSeries_seasonsLabel
import fr.paulbr.nookmind.resources.addSeries_seasonsPlaceholder
import fr.paulbr.nookmind.resources.addSeries_title
import fr.paulbr.nookmind.resources.addSeries_titleLabel
import fr.paulbr.nookmind.resources.addSeries_titlePlaceholder
import fr.paulbr.nookmind.resources.addSeries_watchedSeasonsLabel
import fr.paulbr.nookmind.resources.seriesDetail_cancel
import fr.paulbr.nookmind.resources.seriesDetail_episodesSection
import fr.paulbr.nookmind.resources.seriesDetail_noNotes
import fr.paulbr.nookmind.resources.seriesDetail_notePlaceholder
import fr.paulbr.nookmind.resources.seriesDetail_personalNote
import fr.paulbr.nookmind.resources.seriesDetail_save
import fr.paulbr.nookmind.resources.seriesDetail_seasons
import fr.paulbr.nookmind.resources.seriesDetail_seeLess
import fr.paulbr.nookmind.resources.seriesDetail_seeMore
import fr.paulbr.nookmind.resources.seriesDetail_wantToWatch
import fr.paulbr.nookmind.resources.seriesDetail_watched
import fr.paulbr.nookmind.resources.seriesDetail_watching
import fr.paulbr.nookmind.resources.seriesDetail_yourRating
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Port of AddSeriesModal.tsx: form + season grid, status derived from the watched seasons.
 *
 * A search result opens as a preview laid out like [SeriesDetailSheet] (poster, pills, trailer,
 * description, seasons, rating, note), so adding a series reads like looking at one already in the
 * list. "Edit" switches to the full form; a manual add opens the form directly.
 */
@Composable
fun AddSeriesSheet(container: AppContainer, prefill: Series?, onClose: () -> Unit) {
    val allSeries by container.series.items.collectAsState()
    val scope = rememberCoroutineScope()
    var form by remember { mutableStateOf(prefill ?: Series(title = "")) }
    var saving by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var episodeCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var episodeAirDates by remember { mutableStateOf<Map<String, Map<Int, String?>>>(emptyMap()) }
    var loadingEpisodesSeason by remember { mutableStateOf<Int?>(null) }
    val fromSearch = prefill != null
    val isDuplicate = form.title.isNotBlank() && allSeries.any { normalizeTitle(it.title) == normalizeTitle(form.title) }
    val derivedStatus = deriveSeriesStatus(form.watchedSeasons, form.seasons, false, form.watchedEpisodes)
    val showRating = derivedStatus == SeriesStatus.WATCHED

    fun loadSeason(seasonNumber: Int) {
        val tmdbId = form.tmdbId ?: return
        if (episodeCounts.containsKey(seasonNumber.toString())) return
        loadingEpisodesSeason = seasonNumber
        scope.launch {
            val details = container.tmdb.fetchSeasonDetails(tmdbId, seasonNumber)
            if (details != null) {
                episodeCounts = episodeCounts + (seasonNumber.toString() to details.episodes.size)
                episodeAirDates = episodeAirDates + (seasonNumber.toString() to details.episodes.associate { it.episodeNumber to it.airDate })
            }
            loadingEpisodesSeason = null
        }
    }

    val seasonGrid: @Composable (Int) -> Unit = { totalSeasons ->
        SeasonGrid(
            totalSeasons = totalSeasons,
            watchedSeasons = form.watchedSeasons,
            watchedEpisodes = form.watchedEpisodes,
            onChange = { seasons, episodes ->
                val status = deriveSeriesStatus(seasons, form.seasons, false, episodes)
                form = form.copy(
                    watchedSeasons = seasons,
                    watchedEpisodes = episodes,
                    status = status,
                    rating = if (status == SeriesStatus.WANT_TO_WATCH) null else form.rating,
                )
            },
            episodeCounts = if (form.tmdbId != null) episodeCounts else null,
            episodeAirDates = if (form.tmdbId != null) episodeAirDates else null,
            onSeasonExpand = if (form.tmdbId != null) ({ season: Int -> loadSeason(season) }) else null,
            loadingEpisodesSeason = loadingEpisodesSeason,
        )
    }
    val addButton: @Composable (SheetController, Modifier) -> Unit = { controller, modifier ->
        PrimaryButton(
            text = when {
                saving -> stringResource(Res.string.addSeries_saving)
                isDuplicate -> stringResource(Res.string.addSeries_addAnyway)
                else -> stringResource(Res.string.addSeries_addToList)
            },
            onClick = {
                if (form.title.isBlank() || saving) return@PrimaryButton
                saving = true
                scope.launch {
                    val stored = container.series.add(form.copy(status = derivedStatus))
                    saving = false
                    if (stored != null) controller.close()
                }
            },
            modifier = modifier,
            enabled = form.title.isNotBlank(),
            loading = saving,
            icon = LucideIcons.Tv,
        )
    }
    val duplicateWarning: @Composable () -> Unit = {
        if (isDuplicate) InlineBanner(stringResource(Res.string.addSeries_alreadyInList), tone = BannerTone.WARNING, icon = LucideIcons.AlertTriangle)
    }

    if (fromSearch && !editing) {
        NookSheet(onClose = onClose, maxWidth = 672.dp) { controller ->
            val colors = NookTheme.colors
            Box(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 56.dp, top = 24.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MediaImage(form.posterUrl, form.title, Modifier.width(104.dp), mode = MediaMode.SERIES, placeholderIconSize = 32.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column {
                                Text(form.title, style = NookTheme.type.h3Serif.copy(lineHeight = 24.sp), color = colors.textStrong)
                                if (form.creator.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(form.creator, style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = colors.textMuted)
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                form.genre?.takeIf { it.isNotBlank() }?.let { GenrePill(it, small = true) }
                                SolidPill(
                                    when (derivedStatus) {
                                        SeriesStatus.WATCHED -> stringResource(Res.string.seriesDetail_watched)
                                        SeriesStatus.WATCHING -> stringResource(Res.string.seriesDetail_watching)
                                        SeriesStatus.WANT_TO_WATCH -> stringResource(Res.string.seriesDetail_wantToWatch)
                                    },
                                    when (derivedStatus) {
                                        SeriesStatus.WATCHED -> Palette.Emerald500
                                        SeriesStatus.WATCHING -> Palette.Blue500
                                        SeriesStatus.WANT_TO_WATCH -> Palette.Amber500
                                    },
                                    small = true,
                                )
                                yearOf(form.firstAirDate)?.let { MetaPill(it, small = true) }
                                form.seasons?.let { MetaPill(pluralStringResource(Res.plurals.seriesDetail_seasons, it, it), small = true) }
                            }
                        }
                    }

                    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        form.tmdbId?.let { TrailerButton(container, "tv", it) }
                        form.description?.takeIf { it.isNotBlank() }?.let {
                            ExpandableDescription(
                                it,
                                seeMoreText = stringResource(Res.string.seriesDetail_seeMore),
                                seeLessText = stringResource(Res.string.seriesDetail_seeLess),
                                clampLines = 3,
                            )
                        }
                    }

                    val totalSeasons = form.seasons
                    if (totalSeasons != null && totalSeasons > 0) {
                        Spacer(Modifier.height(12.dp))
                        AccordionSection(
                            stringResource(Res.string.seriesDetail_episodesSection),
                            Modifier.padding(horizontal = 16.dp),
                            initiallyOpen = true,
                        ) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) { seasonGrid(totalSeasons) }
                        }
                    }

                    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (showRating) {
                            LabeledBlock(stringResource(Res.string.seriesDetail_yourRating)) {
                                StarRating(form.rating, onChange = { form = form.copy(rating = it) }, size = 26.dp)
                            }
                        }
                        EditableNote(
                            note = form.personalNote,
                            labelText = stringResource(Res.string.seriesDetail_personalNote),
                            placeholderText = stringResource(Res.string.seriesDetail_notePlaceholder),
                            saveText = stringResource(Res.string.seriesDetail_save),
                            cancelText = stringResource(Res.string.seriesDetail_cancel),
                            noNotesText = stringResource(Res.string.seriesDetail_noNotes),
                            onSave = { form = form.copy(personalNote = it.ifBlank { null }) },
                        )
                        duplicateWarning()
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GhostButton(stringResource(Res.string.addSeries_edit), onClick = { editing = true }, modifier = Modifier.weight(1f), icon = LucideIcons.Pencil)
                            addButton(controller, Modifier.weight(1f))
                        }
                    }
                }
                SheetCloseButton(controller, Modifier.align(Alignment.TopEnd).padding(16.dp))
            }
        }
    } else {
        NookSheet(
            onClose = onClose,
            maxWidth = 512.dp,
            header = { controller -> SheetHeader(stringResource(Res.string.addSeries_title), controller) },
        ) { controller ->
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!form.posterUrl.isNullOrBlank()) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        MediaImage(form.posterUrl, form.title, Modifier.width(80.dp), mode = MediaMode.SERIES)
                    }
                }
                LabeledField(stringResource(Res.string.addSeries_titleLabel)) {
                    NookTextField(form.title, { form = form.copy(title = it) }, placeholder = stringResource(Res.string.addSeries_titlePlaceholder))
                }
                LabeledField(stringResource(Res.string.addSeries_creatorLabel)) {
                    NookTextField(form.creator, { form = form.copy(creator = it) }, placeholder = stringResource(Res.string.addSeries_creatorPlaceholder), readOnly = fromSearch)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledField(stringResource(Res.string.addSeries_genreLabel), Modifier.weight(1f)) {
                        NookTextField(form.genre ?: "", { form = form.copy(genre = it.ifBlank { null }) }, placeholder = stringResource(Res.string.addSeries_genrePlaceholder))
                    }
                    LabeledField(stringResource(Res.string.addSeries_firstAirDateLabel), Modifier.weight(1f)) {
                        NookTextField(form.firstAirDate ?: "", { form = form.copy(firstAirDate = it.ifBlank { null }) }, placeholder = stringResource(Res.string.addSeries_firstAirDatePlaceholder), readOnly = fromSearch)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledField(stringResource(Res.string.addSeries_seasonsLabel), Modifier.weight(1f)) {
                        NookTextField(
                            form.seasons?.toString() ?: "",
                            { value ->
                                if (value.isEmpty() || value.all(Char::isDigit)) {
                                    val total = value.toIntOrNull()
                                    val newWatched = if (total != null) form.watchedSeasons.filter { it <= total } else form.watchedSeasons
                                    form = form.copy(seasons = total, watchedSeasons = newWatched, status = deriveSeriesStatus(newWatched, total, false, form.watchedEpisodes))
                                }
                            },
                            placeholder = stringResource(Res.string.addSeries_seasonsPlaceholder),
                            readOnly = fromSearch,
                            keyboardType = KeyboardType.Number,
                        )
                    }
                    LabeledField(stringResource(Res.string.addSeries_posterUrlLabel), Modifier.weight(1f)) {
                        NookTextField(form.posterUrl ?: "", { form = form.copy(posterUrl = it.ifBlank { null }) }, placeholder = stringResource(Res.string.addSeries_posterUrlPlaceholder), keyboardType = KeyboardType.Uri)
                    }
                }

                val totalSeasons = form.seasons
                if (totalSeasons != null && totalSeasons > 0) {
                    LabeledField(stringResource(Res.string.addSeries_watchedSeasonsLabel)) {
                        Spacer(Modifier.height(4.dp))
                        seasonGrid(totalSeasons)
                    }
                }

                if (showRating) {
                    LabeledField(stringResource(Res.string.addSeries_ratingLabel)) {
                        Spacer(Modifier.height(4.dp))
                        StarRating(form.rating, onChange = { form = form.copy(rating = it) }, size = 28.dp)
                    }
                }

                LabeledField(stringResource(Res.string.addSeries_noteLabel)) {
                    NookTextArea(form.personalNote ?: "", { form = form.copy(personalNote = it.ifBlank { null }) }, placeholder = stringResource(Res.string.addSeries_notePlaceholder))
                }
                if (!form.description.isNullOrBlank()) {
                    LabeledField(stringResource(Res.string.addSeries_descriptionLabel)) {
                        NookTextArea(form.description ?: "", {}, readOnly = true)
                    }
                }
                duplicateWarning()
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // From a search result, Cancel goes back to the preview rather than losing the series.
                    GhostButton(stringResource(Res.string.addSeries_cancel), onClick = { if (fromSearch) editing = false else controller.close() }, modifier = Modifier.weight(1f))
                    addButton(controller, Modifier.weight(1f))
                }
            }
        }
    }
}
