package fr.paulbr.nookmind.feature.movies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
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
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.BannerTone
import fr.paulbr.nookmind.core.designsystem.components.ExpandableDescription
import fr.paulbr.nookmind.core.designsystem.components.GenrePill
import fr.paulbr.nookmind.core.designsystem.components.GhostButton
import fr.paulbr.nookmind.core.designsystem.components.InlineBanner
import fr.paulbr.nookmind.core.designsystem.components.LabeledField
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.MetaPill
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.components.NookTextArea
import fr.paulbr.nookmind.core.designsystem.components.NookTextField
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.SheetCloseButton
import fr.paulbr.nookmind.core.designsystem.components.SolidPill
import fr.paulbr.nookmind.core.designsystem.components.StarRating
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.normalizeTitle
import fr.paulbr.nookmind.core.domain.todayIso
import fr.paulbr.nookmind.core.domain.yearInt
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.Movie
import fr.paulbr.nookmind.core.model.MovieStatus
import fr.paulbr.nookmind.core.model.TmdbMovie
import fr.paulbr.nookmind.core.platform.DateStyle
import fr.paulbr.nookmind.feature.common.CastAccordion
import fr.paulbr.nookmind.feature.common.DateField
import fr.paulbr.nookmind.feature.common.ImdbRatingPill
import fr.paulbr.nookmind.feature.common.PillStatusRow
import fr.paulbr.nookmind.feature.common.SheetHeader
import fr.paulbr.nookmind.feature.common.formatIsoDate
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.addMovie_addAnyway
import fr.paulbr.nookmind.resources.addMovie_addToWatchlist
import fr.paulbr.nookmind.resources.addMovie_alreadyInWatchlist
import fr.paulbr.nookmind.resources.addMovie_cancel
import fr.paulbr.nookmind.resources.addMovie_descriptionLabel
import fr.paulbr.nookmind.resources.addMovie_descriptionPlaceholder
import fr.paulbr.nookmind.resources.addMovie_directorLabel
import fr.paulbr.nookmind.resources.addMovie_directorPlaceholder
import fr.paulbr.nookmind.resources.addMovie_edit
import fr.paulbr.nookmind.resources.addMovie_genreLabel
import fr.paulbr.nookmind.resources.addMovie_genrePlaceholder
import fr.paulbr.nookmind.resources.addMovie_noteLabel
import fr.paulbr.nookmind.resources.addMovie_notePlaceholder
import fr.paulbr.nookmind.resources.addMovie_posterUrlLabel
import fr.paulbr.nookmind.resources.addMovie_posterUrlPlaceholder
import fr.paulbr.nookmind.resources.addMovie_ratingLabel
import fr.paulbr.nookmind.resources.addMovie_releasedLabel
import fr.paulbr.nookmind.resources.addMovie_releasedPlaceholder
import fr.paulbr.nookmind.resources.addMovie_runtimeLabel
import fr.paulbr.nookmind.resources.addMovie_runtimePlaceholder
import fr.paulbr.nookmind.resources.addMovie_saving
import fr.paulbr.nookmind.resources.addMovie_statusLabel
import fr.paulbr.nookmind.resources.addMovie_title
import fr.paulbr.nookmind.resources.addMovie_titleLabel
import fr.paulbr.nookmind.resources.addMovie_titlePlaceholder
import fr.paulbr.nookmind.resources.addMovie_watchedDateLabel
import fr.paulbr.nookmind.resources.movieDetail_cast
import fr.paulbr.nookmind.resources.movieDetail_description
import fr.paulbr.nookmind.resources.movieDetail_runtime
import fr.paulbr.nookmind.resources.movieDetail_seeLess
import fr.paulbr.nookmind.resources.movieDetail_seeMore
import fr.paulbr.nookmind.resources.movieDetail_wantToWatch
import fr.paulbr.nookmind.resources.movieDetail_watched
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Port of AddMovieModal.tsx: preview mode when [prefill] comes from TMDB (poster, meta pills,
 * IMDb rating, cast, status, date, rating, note), switchable to the full form with "Edit";
 * form mode directly for a manual add.
 */
@Composable
fun AddMovieSheet(container: AppContainer, prefill: Movie?, onClose: () -> Unit) {
    val movies by container.movies.items.collectAsState()
    val scope = rememberCoroutineScope()
    var form by remember { mutableStateOf(prefill ?: Movie(title = "")) }
    var saving by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var tmdbMovie by remember { mutableStateOf<TmdbMovie?>(null) }
    var selectedActorId by remember { mutableStateOf<Int?>(null) }
    var imdbRating by remember { mutableStateOf<Double?>(null) }
    var imdbId by remember { mutableStateOf<String?>(null) }
    var imdbLoading by remember { mutableStateOf(true) }
    val fromSearch = prefill != null
    val isDuplicate = form.title.isNotBlank() && movies.any { normalizeTitle(it.title) == normalizeTitle(form.title) }

    LaunchedEffect(fromSearch, prefill?.tmdbId) {
        if (!fromSearch || prefill?.tmdbId == null) return@LaunchedEffect
        tmdbMovie = container.tmdb.fetchMovieDetails(prefill.tmdbId)
    }
    // IMDb indexes original titles: wait for the TMDB details (original_title) before searching.
    LaunchedEffect(fromSearch, tmdbMovie, prefill?.tmdbId) {
        if (!fromSearch) return@LaunchedEffect
        if (prefill?.tmdbId != null && tmdbMovie == null) return@LaunchedEffect
        val result = container.imdb.fetchMovieImdbRating(tmdbMovie?.originalTitle?.ifBlank { null } ?: form.title, yearInt(form.releaseDate))
        imdbRating = result?.rating
        imdbId = result?.imdbId
        imdbLoading = false
    }

    val cast = (tmdbMovie?.credits?.cast ?: emptyList()).take(12)

    fun submit() {
        if (form.title.isBlank() || saving) return
        saving = true
        scope.launch {
            val stored = container.movies.add(form)
            saving = false
            if (stored != null) onClose()
        }
    }

    fun selectStatus(key: String) {
        val status = MovieStatus.fromKey(key)
        form = form.copy(
            status = status,
            rating = if (status == MovieStatus.WANT_TO_WATCH) null else form.rating,
            watchedDate = when {
                status == MovieStatus.WANT_TO_WATCH -> null
                form.watchedDate == null -> todayIso()
                else -> form.watchedDate
            },
        )
    }

    val addButton: @Composable (Modifier) -> Unit = { modifier ->
        PrimaryButton(
            text = when {
                saving -> stringResource(Res.string.addMovie_saving)
                isDuplicate -> stringResource(Res.string.addMovie_addAnyway)
                else -> stringResource(Res.string.addMovie_addToWatchlist)
            },
            onClick = ::submit,
            modifier = modifier,
            enabled = form.title.isNotBlank(),
            loading = saving,
            icon = LucideIcons.Film,
        )
    }
    val duplicateWarning: @Composable () -> Unit = {
        if (isDuplicate) InlineBanner(stringResource(Res.string.addMovie_alreadyInWatchlist), tone = BannerTone.WARNING, icon = LucideIcons.AlertTriangle)
    }
    val statusSelector: @Composable () -> Unit = {
        LabeledField(stringResource(Res.string.addMovie_statusLabel), muted = true) {
            Spacer(Modifier.height(4.dp))
            PillStatusRow(movieStatusOptions(), selected = form.status.key, onSelect = ::selectStatus)
        }
    }
    val watchedFields: @Composable () -> Unit = {
        if (form.status == MovieStatus.WATCHED) {
            LabeledField(stringResource(Res.string.addMovie_watchedDateLabel), muted = true) {
                DateField(form.watchedDate, onChange = { form = form.copy(watchedDate = it) })
            }
            LabeledField(stringResource(Res.string.addMovie_ratingLabel), muted = true) {
                StarRating(form.rating, onChange = { form = form.copy(rating = it) }, size = 26.dp)
            }
        }
    }
    val noteField: @Composable () -> Unit = {
        LabeledField(stringResource(Res.string.addMovie_noteLabel), muted = true) {
            NookTextArea(form.personalNote ?: "", { form = form.copy(personalNote = it.ifBlank { null }) }, placeholder = stringResource(Res.string.addMovie_notePlaceholder))
        }
    }

    if (fromSearch && !editing) {
        NookSheet(onClose = onClose, maxWidth = 672.dp) { controller ->
            Box(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val sideBySide = maxWidth >= 560.dp
                        val poster: @Composable () -> Unit = { MediaImage(form.posterUrl, form.title, Modifier.width(if (sideBySide) 160.dp else 128.dp), mode = MediaMode.MOVIES) }
                        val details: @Composable () -> Unit = {
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column {
                                    Text(form.title, style = NookTheme.type.h2Serif.copy(lineHeight = 28.sp()), color = NookTheme.colors.textStrong, modifier = Modifier.padding(end = 32.dp))
                                    if (form.director.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(form.director, style = NookTheme.type.sans(16, FontWeight.Medium, 24), color = NookTheme.colors.textMuted)
                                    }
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    form.genre?.takeIf { it.isNotBlank() }?.let { GenrePill(it) }
                                    SolidPill(
                                        if (form.status == MovieStatus.WATCHED) stringResource(Res.string.movieDetail_watched) else stringResource(Res.string.movieDetail_wantToWatch),
                                        movieStatusColor(form.status),
                                    )
                                    ImdbRatingPill(imdbRating, imdbId, imdbLoading)
                                    formatIsoDate(form.releaseDate, DateStyle.DAY_MONTH_LONG_YEAR)?.let { MetaPill(it) }
                                    form.runtime?.let { MetaPill(stringResource(Res.string.movieDetail_runtime, it)) }
                                }
                                form.description?.takeIf { it.isNotBlank() }?.let {
                                    ExpandableDescription(it, label = stringResource(Res.string.movieDetail_description), seeMoreText = stringResource(Res.string.movieDetail_seeMore), seeLessText = stringResource(Res.string.movieDetail_seeLess))
                                }
                            }
                        }
                        if (sideBySide) {
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) { poster(); Box(Modifier.weight(1f)) { details() } }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { poster(); Spacer(Modifier.height(24.dp)); details() }
                        }
                    }
                    CastAccordion(cast, stringResource(Res.string.movieDetail_cast), onSelect = { selectedActorId = it })
                    statusSelector()
                    watchedFields()
                    noteField()
                    duplicateWarning()
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GhostButton(stringResource(Res.string.addMovie_edit), onClick = { editing = true }, modifier = Modifier.weight(1f), icon = LucideIcons.Pencil)
                        addButton(Modifier.weight(1f))
                    }
                }
                SheetCloseButton(controller, Modifier.align(Alignment.TopEnd).padding(16.dp))
            }
        }
    } else {
        NookSheet(onClose = onClose, maxWidth = 512.dp, header = { controller -> SheetHeader(stringResource(Res.string.addMovie_title), controller) }) { controller ->
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!form.posterUrl.isNullOrBlank()) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MediaImage(form.posterUrl, form.title, Modifier.width(80.dp), mode = MediaMode.MOVIES) }
                }
                LabeledField(stringResource(Res.string.addMovie_titleLabel)) {
                    NookTextField(form.title, { form = form.copy(title = it) }, placeholder = stringResource(Res.string.addMovie_titlePlaceholder))
                }
                LabeledField(stringResource(Res.string.addMovie_directorLabel)) {
                    NookTextField(form.director, { form = form.copy(director = it) }, placeholder = stringResource(Res.string.addMovie_directorPlaceholder))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledField(stringResource(Res.string.addMovie_genreLabel), Modifier.weight(1f)) {
                        NookTextField(form.genre ?: "", { form = form.copy(genre = it.ifBlank { null }) }, placeholder = stringResource(Res.string.addMovie_genrePlaceholder))
                    }
                    LabeledField(stringResource(Res.string.addMovie_releasedLabel), Modifier.weight(1f)) {
                        NookTextField(form.releaseDate ?: "", { form = form.copy(releaseDate = it.ifBlank { null }) }, placeholder = stringResource(Res.string.addMovie_releasedPlaceholder))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledField(stringResource(Res.string.addMovie_runtimeLabel), Modifier.weight(1f)) {
                        NookTextField(form.runtime?.toString() ?: "", { if (it.isEmpty() || it.all(Char::isDigit)) form = form.copy(runtime = it.toIntOrNull()) }, placeholder = stringResource(Res.string.addMovie_runtimePlaceholder), keyboardType = KeyboardType.Number)
                    }
                    LabeledField(stringResource(Res.string.addMovie_posterUrlLabel), Modifier.weight(1f)) {
                        NookTextField(form.posterUrl ?: "", { form = form.copy(posterUrl = it.ifBlank { null }) }, placeholder = stringResource(Res.string.addMovie_posterUrlPlaceholder), keyboardType = KeyboardType.Uri)
                    }
                }
                LabeledField(stringResource(Res.string.addMovie_descriptionLabel)) {
                    NookTextArea(form.description ?: "", { form = form.copy(description = it.ifBlank { null }) }, placeholder = stringResource(Res.string.addMovie_descriptionPlaceholder), height = 96.dp)
                }
                statusSelector()
                watchedFields()
                noteField()
                duplicateWarning()
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GhostButton(stringResource(Res.string.addMovie_cancel), onClick = { if (fromSearch) editing = false else controller.close() }, modifier = Modifier.weight(1f))
                    addButton(Modifier.weight(1f))
                }
            }
        }
    }

    selectedActorId?.let { ActorSheet(container, it, onClose = { selectedActorId = null }) }
}

private fun Int.sp() = androidx.compose.ui.unit.TextUnit(toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
