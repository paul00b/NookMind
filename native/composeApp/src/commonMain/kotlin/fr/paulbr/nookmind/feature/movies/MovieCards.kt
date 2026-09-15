package fr.paulbr.nookmind.feature.movies

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
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.Movie
import fr.paulbr.nookmind.core.model.MovieStatus
import fr.paulbr.nookmind.feature.common.LocalWideLayout
import fr.paulbr.nookmind.feature.library.CardRemoveButton
import fr.paulbr.nookmind.feature.library.LibraryListRow
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.addMovie_alreadyWatched
import fr.paulbr.nookmind.resources.addMovie_wantToWatch
import fr.paulbr.nookmind.resources.movieCard_wantToWatch
import fr.paulbr.nookmind.resources.movieCard_watched
import org.jetbrains.compose.resources.stringResource

fun movieStatusColor(status: MovieStatus): Color = when (status) {
    MovieStatus.WATCHED -> Palette.Emerald500
    MovieStatus.WANT_TO_WATCH -> Palette.Amber500
}

@Composable
fun movieStatusLabel(status: MovieStatus): String = when (status) {
    MovieStatus.WATCHED -> stringResource(Res.string.movieCard_watched)
    MovieStatus.WANT_TO_WATCH -> stringResource(Res.string.movieCard_wantToWatch)
}

/** Options of the status selector of the movie form (`addMovie.wantToWatch` / `addMovie.alreadyWatched`). */
@Composable
fun movieStatusOptions(): List<Pair<String, String>> = listOf(
    MovieStatus.WANT_TO_WATCH.key to stringResource(Res.string.addMovie_wantToWatch),
    MovieStatus.WATCHED.key to stringResource(Res.string.addMovie_alreadyWatched),
)

/** Port of MovieCard.tsx. */
@Composable
fun MovieCard(movie: Movie, onClick: () -> Unit, modifier: Modifier = Modifier, onRemove: (() -> Unit)? = null) {
    NookCard(modifier, onClick = onClick) {
        Box {
            MediaImage(movie.posterUrl, movie.title, Modifier.fillMaxWidth(), mode = MediaMode.MOVIES) {
                StatusBadge(movieStatusLabel(movie.status), movieStatusColor(movie.status), Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
            if (onRemove != null) CardRemoveButton(onRemove, Modifier.align(Alignment.TopStart))
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(movie.title, style = NookTheme.type.cardTitleSerif, color = NookTheme.colors.textStrong, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(movie.director, style = NookTheme.type.xs, color = NookTheme.colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (movie.status == MovieStatus.WATCHED && movie.rating != null) StarRating(movie.rating, size = 13.dp)
        }
    }
}

/** Port of MovieListRow (MovieLibrary.tsx). */
@Composable
fun MovieListRow(movie: Movie, onClick: () -> Unit, onRemove: (() -> Unit)? = null) {
    val wide = LocalWideLayout.current
    LibraryListRow(movie.posterUrl, movie.title, movie.director, onClick, mode = MediaMode.MOVIES, onRemove = onRemove) {
        if (wide && !movie.genre.isNullOrBlank()) GenrePill(movie.genre, small = true)
        if (wide && movie.status == MovieStatus.WATCHED && movie.rating != null) StarRating(movie.rating, size = 12.dp)
        SolidPill(movieStatusLabel(movie.status), movieStatusColor(movie.status), small = true)
    }
}
