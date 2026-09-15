package fr.paulbr.nookmind.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette

enum class AvatarSize(val dp: Int, val textSize: Int) { SM(32, 12), MD(40, 14), LG(48, 16) }

private val AVATAR_COLORS = listOf(Palette.Violet500, Palette.Blue500, Palette.Emerald500, Palette.Rose500, Palette.Amber500, Palette.Indigo500)

/** Same hash as Avatar.tsx so a given name gets the same colour on web and native. */
fun avatarColor(name: String): Color {
    var hash = 0
    for (ch in name) hash = ch.code + ((hash shl 5) - hash)
    return AVATAR_COLORS[kotlin.math.abs(hash) % AVATAR_COLORS.size]
}

fun avatarInitials(name: String): String =
    name.split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).map { it.first().uppercaseChar() }.joinToString("")

@Composable
fun Avatar(name: String, imageUrl: String?, size: AvatarSize = AvatarSize.MD, modifier: Modifier = Modifier) {
    val safeName = name.ifBlank { "?" }
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = name,
            modifier = modifier.size(size.dp.dp).clip(NookShapes.full),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier.size(size.dp.dp).clip(NookShapes.full).background(avatarColor(safeName), NookShapes.full),
            contentAlignment = Alignment.Center,
        ) {
            Text(avatarInitials(safeName), style = NookTheme.type.sans(size.textSize, FontWeight.SemiBold), color = Palette.White)
        }
    }
}
