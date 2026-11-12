package fr.paulbr.nookmind.feature.wrapped

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.wrapped_entry
import androidx.compose.foundation.shape.RoundedCornerShape
import org.jetbrains.compose.resources.stringResource

/**
 * The way into "Ton année": on the home page from December to January, for everyone, with a few
 * posters of the year fanned out on its right; in Settings of the personal build, plain.
 * [onClose] hides it for the season.
 */
@Composable
fun WrappedBanner(
    year: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    posters: List<String> = emptyList(),
    onClose: (() -> Unit)? = null,
) {
    val fan = posters.take(3)
    Box(
        modifier.fillMaxWidth().widthIn(max = 576.dp).clip(NookShapes.xl3)
            .background(Brush.linearGradient(listOf(Color(0xFF6D28D9), Color(0xFF2E1065))))
            .clickable(onClick = onClick),
    ) {
        if (fan.isNotEmpty()) {
            // Small posters fanned out behind the right edge.
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 18.dp).width(92.dp).height(84.dp)) {
                fan.forEachIndexed { i, url ->
                    AsyncImage(
                        model = url.replace(Regex("/t/p/w\\d+/"), "/t/p/w185/").replace("http://", "https://"),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (i * 22).dp, y = (if (i == 1) -4 else 4).dp)
                            .rotate(-10f + i * 10f)
                            .width(46.dp).height(68.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33FFFFFF)),
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = if (fan.isNotEmpty()) 120.dp else 18.dp, top = 28.dp, bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(LucideIcons.Sparkles, null, Modifier.size(22.dp), tint = Color(0xFFFCD34D))
            Text(
                stringResource(Res.string.wrapped_entry, year),
                style = NookTheme.type.serif(22, FontWeight.Bold, 28), color = Color.White,
                modifier = Modifier.weight(1f),
            )
            if (fan.isEmpty()) Icon(LucideIcons.ChevronRight, null, Modifier.size(18.dp), tint = Color.White)
        }
        if (onClose != null) {
            Icon(
                LucideIcons.X, null,
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(28.dp).clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onClose).padding(7.dp),
                tint = Color.White.copy(alpha = 0.75f),
            )
        }
    }
}
