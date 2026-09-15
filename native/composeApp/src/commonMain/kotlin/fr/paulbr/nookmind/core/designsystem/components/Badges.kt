package fr.paulbr.nookmind.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette

/** Generic pill: `px-2 py-0.5 rounded-full text-xs font-medium`. */
@Composable
fun Pill(
    text: String,
    background: Color,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconSize: Dp = 12.dp,
    textStyle: TextStyle = NookTheme.type.sans(12, FontWeight.Medium, 16),
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
    shape: Shape = NookShapes.full,
    borderColor: Color? = null,
) {
    Row(
        modifier
            .clip(shape)
            .background(background, shape)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(iconSize), tint = color)
        Text(text, style = textStyle, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Status badge on posters: white text on a 90 % coloured pill. */
@Composable
fun StatusBadge(text: String, color: Color, modifier: Modifier = Modifier, alpha: Float = 0.9f) {
    Pill(text, background = color.copy(alpha = alpha), color = Palette.White, modifier = modifier)
}

/** Meta pill of the detail sheets: `bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 px-3 py-1 rounded-full`. */
@Composable
fun MetaPill(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, small: Boolean = false) {
    Pill(
        text,
        background = NookTheme.colors.surfaceMuted,
        color = NookTheme.colors.textMuted,
        modifier = modifier,
        icon = icon,
        textStyle = if (small) NookTheme.type.xs else NookTheme.type.sm,
        contentPadding = if (small) PaddingValues(horizontal = 10.dp, vertical = 2.dp) else PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    )
}

/** Genre pill: `bg-amber-500/10 text-amber-700 dark:text-amber-400 px-3 py-1 rounded-full font-medium`. */
@Composable
fun GenrePill(text: String, modifier: Modifier = Modifier, small: Boolean = false) {
    Pill(
        text,
        background = Palette.Amber500.copy(alpha = 0.10f),
        color = NookTheme.colors.amberTextStrong,
        modifier = modifier,
        textStyle = if (small) NookTheme.type.sans(12, FontWeight.Medium, 16) else NookTheme.type.sans(14, FontWeight.Medium, 20),
        contentPadding = if (small) PaddingValues(horizontal = 10.dp, vertical = 2.dp) else PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    )
}

/** Solid coloured pill of the detail sheets (`px-3 py-1 rounded-full font-medium text-white`). */
@Composable
fun SolidPill(text: String, color: Color, modifier: Modifier = Modifier, small: Boolean = false) {
    Pill(
        text,
        background = color,
        color = Palette.White,
        modifier = modifier,
        textStyle = if (small) NookTheme.type.sans(12, FontWeight.Medium, 16) else NookTheme.type.sans(14, FontWeight.Medium, 20),
        contentPadding = if (small) PaddingValues(horizontal = 10.dp, vertical = 2.dp) else PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    )
}

/** Count badge next to a tab label (`ml-1.5 text-xs px-1.5 py-0.5 rounded-full`). */
@Composable
fun CountBadge(count: Int, active: Boolean, activeColor: Color = Palette.Amber500, activeText: Color = NookTheme.colors.amberText) {
    val colors = NookTheme.colors
    Pill(
        count.toString(),
        background = if (active) activeColor.copy(alpha = 0.15f) else colors.surfaceMuted,
        color = if (active) activeText else colors.textSubtle,
        textStyle = NookTheme.type.xs,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
    )
}
