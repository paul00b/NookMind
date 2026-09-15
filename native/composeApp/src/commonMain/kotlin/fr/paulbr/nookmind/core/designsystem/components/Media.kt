package fr.paulbr.nookmind.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.model.MediaMode

/**
 * Poster / cover image with the grey placeholder + mode icon of the web app when there is no URL.
 * Aspect ratio 2:3 by default; pass `aspect = 16f / 9f` for episode stills.
 */
@Composable
fun MediaImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    mode: MediaMode = NookTheme.mode,
    shape: Shape = NookShapes.xl,
    aspect: Float = 2f / 3f,
    placeholderIconSize: Dp = 40.dp,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
) {
    Box(modifier.aspectRatio(aspect).clip(shape).background(NookTheme.colors.surfaceMuted, shape)) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            PosterPlaceholder(Modifier.fillMaxSize(), mode = mode, iconSize = placeholderIconSize, shape = shape)
        }
        overlay?.invoke(this)
    }
}

/** Square-ish image with a text initial fallback (cast members). */
@Composable
fun PersonImage(url: String?, name: String, modifier: Modifier = Modifier, shape: Shape = NookShapes.xl) {
    Box(modifier.aspectRatio(2f / 3f).clip(shape).background(NookTheme.colors.surfaceMuted, shape), contentAlignment = Alignment.Center) {
        if (!url.isNullOrBlank()) {
            AsyncImage(model = url, contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            androidx.compose.material3.Text(
                name.take(1),
                style = NookTheme.type.sans(18, androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = NookTheme.colors.textFaint,
            )
        }
    }
}
