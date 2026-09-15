package fr.paulbr.nookmind.core.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.model.MediaMode

/** `.card` — white / #1a1f2e surface, 1 px hairline border, 16 dp radius. */
@Composable
fun NookCard(
    modifier: Modifier = Modifier,
    shape: Shape = NookShapes.xl2,
    background: Color = NookTheme.colors.surface,
    borderColor: Color = NookTheme.colors.border,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(background, shape)
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/** Hairline divider (`border-black/6 dark:border-white/6`). */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier, color: Color = NookTheme.colors.divider, thickness: Dp = 1.dp) {
    Box(modifier.fillMaxWidth().height(thickness).background(color))
}

@Composable
fun VerticalHairline(modifier: Modifier = Modifier, color: Color = NookTheme.colors.divider) {
    Box(modifier.width(1.dp).background(color))
}

/** `animate-pulse` — opacity 1 → 0.5 → 1 over 2 s. */
@Composable
fun Modifier.pulse(): Modifier {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    return this.alpha(alpha)
}

/** Grey skeleton block (`bg-gray-200 dark:bg-gray-700 animate-pulse`). */
@Composable
fun SkeletonBox(modifier: Modifier, shape: Shape = NookShapes.full, color: Color = NookTheme.colors.surfaceMuted2) {
    Box(modifier.pulse().clip(shape).background(color, shape))
}

/** Rounded-corner media placeholder with the mode icon centered (`aspect-2/3` posters). */
@Composable
fun PosterPlaceholder(
    modifier: Modifier = Modifier,
    mode: MediaMode = NookTheme.mode,
    iconSize: Dp = 40.dp,
    shape: Shape = NookShapes.xl,
    iconTint: Color = NookTheme.colors.textDisabled,
    background: Color = NookTheme.colors.surfaceMuted,
) {
    Box(modifier.clip(shape).background(background, shape), contentAlignment = Alignment.Center) {
        Icon(modeIcon(mode), contentDescription = null, modifier = Modifier.size(iconSize), tint = iconTint)
    }
}

fun modeIcon(mode: MediaMode): ImageVector = when (mode) {
    MediaMode.BOOKS -> LucideIcons.BookOpen
    MediaMode.MOVIES -> LucideIcons.Film
    MediaMode.SERIES -> LucideIcons.Tv
}

/** Section title of the sliders: `text-sm font-semibold uppercase tracking-wider` with optional icon. */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NookTheme.colors.textSubtle,
    icon: ImageVector? = null,
    iconSize: Dp = 14.dp,
) {
    Row(modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (icon != null) Icon(icon, null, Modifier.size(iconSize), tint = color)
        Text(
            text.uppercase(),
            style = NookTheme.type.sans(14, FontWeight.SemiBold, 20).copy(letterSpacing = 0.7.sp),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Small uppercase label with letter spacing (`text-xs font-semibold uppercase tracking-wider`). */
@Composable
fun OverlineLabel(text: String, modifier: Modifier = Modifier, color: Color = NookTheme.colors.textFaint, size: Int = 12, tracking: Double = 0.6) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = NookTheme.type.sans(size, FontWeight.SemiBold).copy(letterSpacing = tracking.sp),
        color = color,
    )
}

/** Centered empty state with a round tinted icon (`w-20 h-20 bg-amber-500/10 rounded-full`). */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    accent: Color = Palette.Amber500,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 80.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(80.dp).clip(NookShapes.full).background(accent.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(36.dp), tint = accent)
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = NookTheme.type.h3Serif.copy(fontWeight = FontWeight.SemiBold), color = NookTheme.colors.textBody, textAlign = TextAlign.Center)
        if (description != null) {
            Spacer(Modifier.height(8.dp))
            Text(description, style = NookTheme.type.sm, color = NookTheme.colors.textSubtle, textAlign = TextAlign.Center, modifier = Modifier.width(320.dp))
        }
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

/** Inline alert banner (`bg-red-50 border-red-200 rounded-xl px-4 py-3 text-sm text-red-600`). */
@Composable
fun InlineBanner(
    text: String,
    modifier: Modifier = Modifier,
    tone: BannerTone = BannerTone.ERROR,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = NookTheme.colors
    val (bg, border, fg) = when (tone) {
        BannerTone.ERROR -> if (colors.isDark) Triple(Palette.Red900.copy(alpha = 0.2f), Palette.Red800, Palette.Red400) else Triple(Palette.Red50, Palette.Red200, Palette.Red600)
        BannerTone.SUCCESS -> if (colors.isDark) Triple(Palette.Emerald900.copy(alpha = 0.2f), Palette.Emerald900, Palette.Emerald400) else Triple(Palette.Emerald50, Palette.Emerald200, Palette.Emerald600)
        BannerTone.WARNING -> if (colors.isDark) Triple(Palette.Amber900.copy(alpha = 0.2f), Palette.Amber700, Palette.Amber300) else Triple(Palette.Amber50, Palette.Amber200, Palette.Amber700)
        BannerTone.NEUTRAL -> Triple(colors.surfaceMuted, colors.borderNeutral, colors.textBody2)
    }
    Row(
        modifier.fillMaxWidth().clip(NookShapes.xl).background(bg, NookShapes.xl).border(1.dp, border, NookShapes.xl).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(15.dp), tint = if (tone == BannerTone.ERROR) Palette.Red500 else fg)
        Text(text, style = NookTheme.type.sm, color = fg, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

enum class BannerTone { ERROR, SUCCESS, WARNING, NEUTRAL }

/** 2:3 poster frame helper. */
@Composable
fun PosterFrame(width: Dp, modifier: Modifier = Modifier, shape: Shape = NookShapes.xl, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.width(width).aspectRatio(2f / 3f).clip(shape), content = content)
}
