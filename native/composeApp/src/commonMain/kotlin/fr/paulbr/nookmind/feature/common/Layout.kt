package fr.paulbr.nookmind.feature.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** True above Tailwind's `md:` breakpoint (768 dp): sidebar layout, wider posters, inline dialogs. */
val LocalWideLayout = compositionLocalOf { false }

/** `w-20 md:w-28` of the poster sliders. */
@Composable
fun posterSlideWidth(): Dp = if (LocalWideLayout.current) 112.dp else 80.dp

/**
 * `-mx-4 px-4`: lets a horizontally scrolling row bleed through the page padding on phones so its
 * bottom border and its scrolled content reach the screen edges.
 */
fun Modifier.horizontalBleed(amount: Dp): Modifier = layout { measurable, constraints ->
    val extra = amount.roundToPx() * 2
    val widened = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth
    val placeable = measurable.measure(constraints.copy(minWidth = widened, maxWidth = widened))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-amount.roundToPx(), 0) }
}
