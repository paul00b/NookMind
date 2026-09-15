package fr.paulbr.nookmind.feature.movies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.data.patchOf
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.EditableNote
import fr.paulbr.nookmind.core.designsystem.components.ExpandableDescription
import fr.paulbr.nookmind.core.designsystem.components.GenrePill
import fr.paulbr.nookmind.core.designsystem.components.GhostButton
import fr.paulbr.nookmind.core.designsystem.components.IconGhostButton
import fr.paulbr.nookmind.core.designsystem.components.LabeledBlock
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.MetaPill
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.SheetCloseButton
import fr.paulbr.nookmind.core.designsystem.components.SolidPill
import fr.paulbr.nookmind.core.designsystem.components.StarRating
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.todayIso
import fr.paulbr.nookmind.core.domain.yearInt
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.Movie
import fr.paulbr.nookmind.core.model.MovieStatus
import fr.paulbr.nookmind.core.model.TmdbMovie
import fr.paulbr.nookmind.core.model.WatchProvidersResult
import fr.paulbr.nookmind.core.platform.DateStyle
import fr.paulbr.nookmind.feature.books.CollectionChip
import fr.paulbr.nookmind.feature.common.CastAccordion
import fr.paulbr.nookmind.feature.common.ConfirmDeleteRow
import fr.paulbr.nookmind.feature.common.DateField
import fr.paulbr.nookmind.feature.common.DeleteButton
import fr.paulbr.nookmind.feature.common.ImdbRatingPill
import fr.paulbr.nookmind.feature.common.TrailerButton
import fr.paulbr.nookmind.feature.common.WatchProvidersRow
import fr.paulbr.nookmind.feature.common.formatIsoDate
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.movieDetail_areYouSure
import fr.paulbr.nookmind.resources.movieDetail_cancel
import fr.paulbr.nookmind.resources.movieDetail_cast
import fr.paulbr.nookmind.resources.movieDetail_collections
import fr.paulbr.nookmind.resources.movieDetail_dateSaved
import fr.paulbr.nookmind.resources.movieDetail_delete
import fr.paulbr.nookmind.resources.movieDetail_description
import fr.paulbr.nookmind.resources.movieDetail_moveToWantToWatch
import fr.paulbr.nookmind.resources.movieDetail_moveToWatched
import fr.paulbr.nookmind.resources.movieDetail_movedToWantToWatch
import fr.paulbr.nookmind.resources.movieDetail_movedToWatched
import fr.paulbr.nookmind.resources.movieDetail_noDate
import fr.paulbr.nookmind.resources.movieDetail_noNotes
import fr.paulbr.nookmind.resources.movieDetail_notePlaceholder
import fr.paulbr.nookmind.resources.movieDetail_noteSaved
import fr.paulbr.nookmind.resources.movieDetail_personalNote
import fr.paulbr.nookmind.resources.movieDetail_ratingUpdated
import fr.paulbr.nookmind.resources.movieDetail_runtime
import fr.paulbr.nookmind.resources.movieDetail_save
import fr.paulbr.nookmind.resources.movieDetail_seeLess
import fr.paulbr.nookmind.resources.movieDetail_seeMore
import fr.paulbr.nookmind.resources.movieDetail_wantToWatch
import fr.paulbr.nookmind.resources.movieDetail_watched
import fr.paulbr.nookmind.resources.movieDetail_watchedOnLabel
import fr.paulbr.nookmind.resources.movieDetail_yesDelete
import fr.paulbr.nookmind.resources.movieDetail_yourRating
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Port of MovieDetailModal.tsx. */
@Composable
fun MovieDetailSheet(container: AppContainer, movie: Movie, onClose: () -> Unit) {
    val colors = NookTheme.colors
    val scope = rememberCoroutineScope()
    val categories by container.movieCategories.items.collectAsState()
    var local by remember(movie.id) { mutableStateOf(movie) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editingDate by remember { mutableStateOf(false) }
    var dateDraft by remember { mutableStateOf(movie.watchedDate) }
    var tmdbMovie by remember { mutableStateOf<TmdbMovie?>(null) }
    var providers by remember { mutableStateOf<WatchProvidersResult?>(null) }
    var loadingProviders by remember { mutableStateOf(movie.tmdbId != null) }
    var selectedActorId by remember { mutableStateOf<Int?>(null) }
    var imdbRating by remember { mutableStateOf<Double?>(null) }
    var imdbId by remember { mutableStateOf<String?>(null) }
    var imdbLoading by remember { mutableStateOf(true) }

    LaunchedEffect(movie.tmdbId) {
        val id = movie.tmdbId ?: return@LaunchedEffect
        scope.launch { tmdbMovie = container.tmdb.fetchMovieDetails(id) }
        providers = container.tmdb.fetchMovieWatchProviders(id)
        loadingProviders = false
    }
    LaunchedEffect(movie.tmdbId, tmdbMovie) {
        if (movie.tmdbId != null && tmdbMovie == null) return@LaunchedEffect
        val result = container.imdb.fetchMovieImdbRating(tmdbMovie?.originalTitle?.ifBlank { null } ?: movie.title, yearInt(movie.releaseDate))
        imdbRating = result?.rating
        imdbId = result?.imdbId
        imdbLoading = false
    }

    val cast = (tmdbMovie?.credits?.cast ?: emptyList()).take(12)

    fun apply(patch: Map<String, Any?>, optimistic: Movie, onDone: (() -> Unit)? = null) {
        val previous = local
        local = optimistic
        scope.launch {
            val stored = container.movies.update(movie.id, patchOf(*patch.entries.map { it.key to it.value }.toTypedArray()))
            if (stored == null) local = previous else { local = stored; onDone?.invoke() }
        }
    }

    val movedToWatched = stringResource(Res.string.movieDetail_movedToWatched)
    val movedToWant = stringResource(Res.string.movieDetail_movedToWantToWatch)

    NookSheet(onClose = onClose, maxWidth = 672.dp) { controller ->
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val sideBySide = maxWidth >= 560.dp
                    val poster: @Composable () -> Unit = { MediaImage(local.posterUrl, local.title, Modifier.width(if (sideBySide) 160.dp else 128.dp), mode = MediaMode.MOVIES) }
                    val details: @Composable () -> Unit = {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                Text(local.title, style = NookTheme.type.h2Serif.copy(lineHeight = 28.sp()), color = colors.textStrong, modifier = Modifier.padding(end = 32.dp))
                                Spacer(Modifier.height(4.dp))
                                Text(local.director, style = NookTheme.type.sans(16, FontWeight.Medium, 24), color = colors.textMuted)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                local.genre?.takeIf { it.isNotBlank() }?.let { GenrePill(it) }
                                SolidPill(
                                    if (local.status == MovieStatus.WATCHED) stringResource(Res.string.movieDetail_watched) else stringResource(Res.string.movieDetail_wantToWatch),
                                    movieStatusColor(local.status),
                                )
                                ImdbRatingPill(imdbRating, imdbId, imdbLoading)
                                formatIsoDate(local.releaseDate, DateStyle.DAY_MONTH_LONG_YEAR)?.let { MetaPill(it) }
                                local.runtime?.let { MetaPill(stringResource(Res.string.movieDetail_runtime, it)) }
                            }
                            local.description?.takeIf { it.isNotBlank() }?.let {
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

                WatchProvidersRow(container, providers, local.title, loading = loadingProviders)

                movie.tmdbId?.let { TrailerButton(container, "movie", it) }

                if (local.status == MovieStatus.WATCHED) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        LabeledBlock(stringResource(Res.string.movieDetail_yourRating)) {
                            StarRating(local.rating, onChange = { rating ->
                                apply(mapOf("rating" to rating), local.copy(rating = rating)) { container.toasts.success(Res.string.movieDetail_ratingUpdated) }
                            }, size = 26.dp)
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(Res.string.movieDetail_watchedOnLabel), style = NookTheme.type.sm, color = colors.textSubtle)
                                if (!editingDate) {
                                    IconGhostButton(LucideIcons.Pencil, null, onClick = { dateDraft = local.watchedDate; editingDate = true }, size = 13.dp, padding = 2.dp, tint = colors.textFaint)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            if (editingDate) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DateField(dateDraft, onChange = { dateDraft = it }, compact = true)
                                    PrimaryButton(
                                        stringResource(Res.string.movieDetail_save),
                                        onClick = {
                                            val value = dateDraft
                                            apply(mapOf("watched_date" to value), local.copy(watchedDate = value)) { container.toasts.success(Res.string.movieDetail_dateSaved) }
                                            editingDate = false
                                        },
                                        icon = LucideIcons.Check, iconSize = 14.dp,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                    GhostButton(stringResource(Res.string.movieDetail_cancel), onClick = { dateDraft = local.watchedDate; editingDate = false }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp))
                                }
                            } else {
                                val formatted = formatIsoDate(local.watchedDate, DateStyle.NUMERIC)
                                if (formatted != null) {
                                    Text(formatted, style = NookTheme.type.sm, color = colors.textBody2)
                                } else {
                                    Text(stringResource(Res.string.movieDetail_noDate), style = NookTheme.type.sm.copy(fontStyle = FontStyle.Italic), color = colors.textFaint)
                                }
                            }
                        }
                    }
                }

                CastAccordion(cast, stringResource(Res.string.movieDetail_cast), onSelect = { selectedActorId = it })

                EditableNote(
                    note = local.personalNote,
                    labelText = stringResource(Res.string.movieDetail_personalNote),
                    placeholderText = stringResource(Res.string.movieDetail_notePlaceholder),
                    saveText = stringResource(Res.string.movieDetail_save),
                    cancelText = stringResource(Res.string.movieDetail_cancel),
                    noNotesText = stringResource(Res.string.movieDetail_noNotes),
                    onSave = { note -> apply(mapOf("personal_note" to note), local.copy(personalNote = note)) { container.toasts.success(Res.string.movieDetail_noteSaved) } },
                )

                if (categories.isNotEmpty()) {
                    Column {
                        Text(stringResource(Res.string.movieDetail_collections), style = NookTheme.type.sm, color = colors.textSubtle)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.forEach { cat ->
                                val isIn = local.id in cat.itemIds
                                CollectionChip(cat.title, isIn) {
                                    scope.launch {
                                        if (isIn) container.movieCategories.removeMappedItem(cat.id, local.id)
                                        else container.movieCategories.addMappedItems(cat.id, listOf(local.id))
                                    }
                                }
                            }
                        }
                    }
                }

                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton(
                        if (local.status == MovieStatus.WATCHED) stringResource(Res.string.movieDetail_moveToWantToWatch) else stringResource(Res.string.movieDetail_moveToWatched),
                        onClick = {
                            val next = if (local.status == MovieStatus.WATCHED) MovieStatus.WANT_TO_WATCH else MovieStatus.WATCHED
                            val patch = mutableMapOf<String, Any?>("status" to next.key)
                            var optimistic = local.copy(status = next)
                            if (next == MovieStatus.WANT_TO_WATCH) {
                                patch["rating"] = null; patch["watched_date"] = null
                                optimistic = optimistic.copy(rating = null, watchedDate = null)
                            } else if (local.watchedDate == null) {
                                val today = todayIso()
                                patch["watched_date"] = today
                                optimistic = optimistic.copy(watchedDate = today)
                            }
                            apply(patch, optimistic) { container.toasts.success(if (next == MovieStatus.WATCHED) movedToWatched else movedToWant) }
                        },
                        icon = LucideIcons.ArrowLeftRight,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    )
                    if (!confirmDelete) {
                        DeleteButton(stringResource(Res.string.movieDetail_delete), onClick = { confirmDelete = true })
                    } else {
                        ConfirmDeleteRow(
                            question = stringResource(Res.string.movieDetail_areYouSure),
                            yesText = stringResource(Res.string.movieDetail_yesDelete),
                            cancelText = stringResource(Res.string.movieDetail_cancel),
                            onConfirm = { scope.launch { if (container.movies.delete(movie.id)) onClose() } },
                            onCancel = { confirmDelete = false },
                        )
                    }
                }
            }
            SheetCloseButton(controller, Modifier.align(Alignment.TopEnd).padding(16.dp))
        }
    }

    selectedActorId?.let { ActorSheet(container, it, onClose = { selectedActorId = null }) }
}

private fun Int.sp() = androidx.compose.ui.unit.TextUnit(toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
