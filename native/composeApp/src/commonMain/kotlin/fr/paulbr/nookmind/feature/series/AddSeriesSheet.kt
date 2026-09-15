package fr.paulbr.nookmind.feature.series

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.components.BannerTone
import fr.paulbr.nookmind.core.designsystem.components.GhostButton
import fr.paulbr.nookmind.core.designsystem.components.InlineBanner
import fr.paulbr.nookmind.core.designsystem.components.LabeledField
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.components.NookTextArea
import fr.paulbr.nookmind.core.designsystem.components.NookTextField
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.StarRating
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.deriveSeriesStatus
import fr.paulbr.nookmind.core.domain.normalizeTitle
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesStatus
import fr.paulbr.nookmind.feature.common.SheetHeader
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.addSeries_addAnyway
import fr.paulbr.nookmind.resources.addSeries_addToList
import fr.paulbr.nookmind.resources.addSeries_alreadyInList
import fr.paulbr.nookmind.resources.addSeries_cancel
import fr.paulbr.nookmind.resources.addSeries_creatorLabel
import fr.paulbr.nookmind.resources.addSeries_creatorPlaceholder
import fr.paulbr.nookmind.resources.addSeries_descriptionLabel
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Port of AddSeriesModal.tsx: form + season grid, status derived from the watched seasons. */
@Composable
fun AddSeriesSheet(container: AppContainer, prefill: Series?, onClose: () -> Unit) {
    val allSeries by container.series.items.collectAsState()
    val scope = rememberCoroutineScope()
    var form by remember { mutableStateOf(prefill ?: Series(title = "")) }
    var saving by remember { mutableStateOf(false) }
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
            if (isDuplicate) {
                InlineBanner(stringResource(Res.string.addSeries_alreadyInList), tone = BannerTone.WARNING, icon = LucideIcons.AlertTriangle)
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GhostButton(stringResource(Res.string.addSeries_cancel), onClick = { controller.close() }, modifier = Modifier.weight(1f))
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
                    modifier = Modifier.weight(1f),
                    enabled = form.title.isNotBlank(),
                    loading = saving,
                    icon = LucideIcons.Tv,
                )
            }
        }
    }
}
