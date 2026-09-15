package fr.paulbr.nookmind.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette

/** A generic tappable surface: shape, background, border, disabled alpha, optional press scale. */
@Composable
fun NookPressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = NookShapes.full,
    background: Color = Color.Transparent,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pressScale: Float = 1f,
    contentColor: Color = LocalContentColor.current,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && pressScale != 1f) pressScale else 1f, tween(150), label = "press")
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .background(background, shape)
            .then(if (borderColor != null) Modifier.border(borderWidth, borderColor, shape) else Modifier)
            .clickable(interactionSource = interaction, indication = ripple(), enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

/** `.btn-primary` — amber pill, white medium text, `active:scale-95`. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    iconSize: Dp = 16.dp,
    color: Color = Palette.Amber500,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
    textStyle: androidx.compose.ui.text.TextStyle = NookTheme.type.sans(14, FontWeight.Medium, 20),
) {
    NookPressable(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !loading,
        background = color,
        contentColor = Palette.White,
        contentPadding = contentPadding,
        pressScale = 0.95f,
    ) {
        if (loading) {
            Spinner(size = 16.dp, color = Palette.White, trackColor = Palette.White.copy(alpha = 0.3f))
            Spacer8()
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize))
            Spacer8()
        }
        Text(text, style = textStyle, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

/** `.btn-ghost` — transparent pill, gray-700/gray-300 medium text. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    iconSize: Dp = 14.dp,
    color: Color = NookTheme.colors.textBody2,
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    textStyle: androidx.compose.ui.text.TextStyle = NookTheme.type.sans(14, FontWeight.Medium, 20),
) {
    NookPressable(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentColor = color,
        borderColor = borderColor,
        contentPadding = contentPadding,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize))
            Spacer8()
        }
        Text(text, style = textStyle, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

/** Icon-only ghost button (`btn-ghost p-2`). */
@Composable
fun IconGhostButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = NookTheme.colors.textBody2,
    enabled: Boolean = true,
    shape: Shape = NookShapes.full,
    background: Color = Color.Transparent,
    padding: Dp = 8.dp,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(background, shape)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(size), tint = tint)
    }
}

/** Outlined action button (`py-2.5 rounded-xl border border-gray-200 dark:border-gray-700 text-sm font-medium`). */
@Composable
fun OutlinedActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    background: Color = Color.Transparent,
    contentColor: Color = NookTheme.colors.textBody2,
    borderColor: Color = NookTheme.colors.borderNeutral,
    shape: Shape = NookShapes.xl,
) {
    NookPressable(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 42.dp),
        enabled = enabled && !loading,
        shape = shape,
        background = background,
        borderColor = borderColor,
        contentColor = contentColor,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        when {
            loading -> { Spinner(size = 16.dp, color = NookTheme.colors.textMuted, trackColor = Palette.Gray300); Spacer8() }
            leading != null -> { leading(); Spacer12() }
            icon != null -> { Icon(icon, null, Modifier.size(15.dp)); Spacer8() }
        }
        Text(text, style = NookTheme.type.sans(14, FontWeight.Medium, 20), maxLines = 1)
    }
}

/** Small toggle chip used for status selectors (`flex-1 py-1.5 rounded-lg text-xs font-medium border`). */
@Composable
fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = NookShapes.lg,
    selectedColor: Color = Palette.Amber500,
    textStyle: androidx.compose.ui.text.TextStyle = NookTheme.type.sans(12, FontWeight.Medium, 16),
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
) {
    val colors = NookTheme.colors
    NookPressable(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        background = if (selected) selectedColor else Color.Transparent,
        borderColor = if (selected) selectedColor else colors.borderNeutral,
        contentColor = if (selected) Palette.White else colors.textMuted,
        contentPadding = contentPadding,
    ) {
        Text(text, style = textStyle, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** Compact pill tab (`px-3 py-1 rounded-full text-xs font-semibold`) for slider categories. */
@Composable
fun PillTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Palette.Amber500) {
    val colors = NookTheme.colors
    NookPressable(
        onClick = onClick,
        modifier = modifier,
        background = if (selected) color else colors.surfaceMuted,
        contentColor = if (selected) Palette.White else colors.textSubtle,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(text, style = NookTheme.type.sans(12, FontWeight.SemiBold, 16), maxLines = 1)
    }
}

@Composable
fun Spacer8() = androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))

@Composable
fun Spacer12() = androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))

/** The `border-2 ... rounded-full animate-spin` spinner of the web app. */
@Composable
fun Spinner(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = Palette.Amber500,
    trackColor: Color = color.copy(alpha = 0.3f),
    strokeWidth: Dp = 2.dp,
) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = modifier.size(size),
        color = color,
        trackColor = trackColor,
        strokeWidth = strokeWidth,
    )
}

/** Text link (`hover:underline`) used for inline confirmations. */
@Composable
fun TextLink(text: String, onClick: () -> Unit, color: Color = NookTheme.colors.textSubtle, weight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onClick).padding(horizontal = 2.dp, vertical = 2.dp),
        style = NookTheme.type.sans(14, weight, 20),
        color = color,
    )
}

@Composable
fun ProvideTextStyle(style: androidx.compose.ui.text.TextStyle, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTextStyle provides style, content = content)
}
