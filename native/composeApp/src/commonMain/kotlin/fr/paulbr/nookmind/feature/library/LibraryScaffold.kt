package fr.paulbr.nookmind.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.data.ViewMode
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.alpha
import fr.paulbr.nookmind.core.designsystem.components.EmptyState
import fr.paulbr.nookmind.core.designsystem.components.IconGhostButton
import fr.paulbr.nookmind.core.designsystem.components.InlineBanner
import fr.paulbr.nookmind.core.designsystem.components.BannerTone
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.NookCard
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.SkeletonBox
import fr.paulbr.nookmind.core.designsystem.components.TextLink
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.feature.common.LocalWideLayout

/** Colour of a status tab (`amber` default, `blue` watching, `emerald` watched). */
enum class TabTone { AMBER, BLUE, EMERALD }

@Composable
fun TabTone.base(): Color = when (this) {
    TabTone.AMBER -> Palette.Amber500
    TabTone.BLUE -> Palette.Blue500
    TabTone.EMERALD -> Palette.Emerald500
}

@Composable
fun TabTone.text(): Color = when (this) {
    TabTone.AMBER -> NookTheme.colors.amberText
    TabTone.BLUE -> NookTheme.colors.blueText
    TabTone.EMERALD -> NookTheme.colors.emeraldText
}

/** A built-in status tab of a library. */
data class LibraryTab(val id: String, val label: String, val count: Int, val tone: TabTone = TabTone.AMBER, val icon: ImageVector? = null)

/** `grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5`. */
fun libraryColumns(maxWidth: Dp): Int = when {
    maxWidth < 768.dp -> 2
    maxWidth < 1024.dp -> 3
    maxWidth < 1280.dp -> 4
    else -> 5
}

/** `p-4 md:p-8` page padding. */
@Composable
fun libraryPagePadding(): Dp = if (LocalWideLayout.current) 32.dp else 16.dp

/** Max content width of the library pages (`max-w-6xl`). */
val LIBRARY_MAX_WIDTH = 1152.dp

/** Title + count + view toggle (+ extra actions) of the library pages. */
@Composable
fun LibraryHeader(
    title: String,
    titleColor: Color,
    subtitle: String,
    viewMode: ViewMode,
    onViewMode: (ViewMode) -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(title, style = NookTheme.type.h1Serif, color = titleColor)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = NookTheme.type.sm, color = NookTheme.colors.textSubtle)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            actions?.invoke(this)
            ViewModeToggle(viewMode, onViewMode)
        }
    }
}

/** `bg-gray-100 rounded-xl p-1` segmented grid / list toggle. */
@Composable
fun ViewModeToggle(viewMode: ViewMode, onViewMode: (ViewMode) -> Unit, modifier: Modifier = Modifier) {
    val colors = NookTheme.colors
    Row(
        modifier.clip(NookShapes.xl).background(colors.surfaceMuted, NookShapes.xl).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf(ViewMode.GRID to LucideIcons.LayoutGrid, ViewMode.LIST to LucideIcons.List).forEach { (mode, icon) ->
            val active = viewMode == mode
            Box(
                Modifier
                    .then(if (active) Modifier.shadow(1.dp, NookShapes.lg, ambientColor = Palette.Black.alpha(0.1f), spotColor = Palette.Black.alpha(0.1f)) else Modifier)
                    .clip(NookShapes.lg)
                    .background(if (active) colors.surface else Color.Transparent, NookShapes.lg)
                    .clickable { onViewMode(mode) }
                    .padding(8.dp),
            ) {
                Icon(icon, contentDescription = mode.key, modifier = Modifier.size(16.dp), tint = if (active) colors.amberText else colors.textFaint)
            }
        }
    }
}

/** Red confirmation banner shown above the content when deleting a collection. */
@Composable
fun DeleteCategoryBanner(text: String, yesText: String, cancelText: String, onConfirm: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    InlineBanner(
        text,
        modifier = modifier,
        tone = BannerTone.ERROR,
        icon = LucideIcons.Trash2,
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextLink(yesText, onClick = onConfirm, color = Palette.Red600, weight = FontWeight.Medium)
                TextLink(cancelText, onClick = onCancel, color = NookTheme.colors.textSubtle)
            }
        },
    )
}

/** "N items" + "Add …" primary button of a collection tab. */
@Composable
fun CategoryToolbar(countText: String, addLabel: String, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(countText, style = NookTheme.type.sm, color = NookTheme.colors.textSubtle, modifier = Modifier.weight(1f))
        PrimaryButton(addLabel, onClick = onAdd, icon = LucideIcons.Plus, iconSize = 15.dp, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp))
    }
}

/** Pulsing placeholder card of the loading grid. */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    NookCard(modifier) {
        SkeletonBox(Modifier.fillMaxWidth().aspectRatio(2f / 3f), shape = NookShapes.xl)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 4.dp).padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBox(Modifier.fillMaxWidth(0.8f).height(14.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.6f).height(12.dp))
        }
    }
}

fun LazyListScope.skeletonGrid(columns: Int, count: Int = 8) {
    gridRows(List(count) { it }, columns, key = { "skeleton-$it" }) { SkeletonCard() }
}

/**
 * Lays [items] out as grid rows of [columns] cells (`gap-4`). The cells of a row share its height, so a
 * card whose title fits on one line ends level with a neighbour on two.
 */
fun <T> LazyListScope.gridRows(items: List<T>, columns: Int, key: (T) -> Any, spacing: Dp = 16.dp, itemContent: @Composable (T) -> Unit) {
    val rows = items.chunked(columns)
    rows.forEachIndexed { rowIndex, chunk ->
        item(key = "row-" + key(chunk.first()).toString()) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = if (rowIndex < rows.lastIndex) spacing else 0.dp).height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                chunk.forEach { Box(Modifier.weight(1f).fillMaxHeight(), propagateMinConstraints = true) { itemContent(it) } }
                repeat(columns - chunk.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

enum class SegmentPosition { FIRST, MIDDLE, LAST, SINGLE }

/** Lays [items] out as the rows of one `card divide-y` container, lazily. */
fun <T> LazyListScope.listRows(items: List<T>, key: (T) -> Any, rowContent: @Composable (T) -> Unit) {
    items.forEachIndexed { index, item ->
        val position = when {
            items.size == 1 -> SegmentPosition.SINGLE
            index == 0 -> SegmentPosition.FIRST
            index == items.lastIndex -> SegmentPosition.LAST
            else -> SegmentPosition.MIDDLE
        }
        item(key = "list-" + key(item).toString()) {
            ListCardSegment(position) { rowContent(item) }
        }
    }
}

/** One slice of a card: background, side borders, rounded ends, hairline between rows. */
@Composable
fun ListCardSegment(position: SegmentPosition, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = NookTheme.colors
    val radius = 16.dp
    val shape = when (position) {
        SegmentPosition.SINGLE -> RoundedCornerShape(radius)
        SegmentPosition.FIRST -> RoundedCornerShape(topStart = radius, topEnd = radius)
        SegmentPosition.LAST -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
        SegmentPosition.MIDDLE -> RoundedCornerShape(0.dp)
    }
    val border = colors.border
    val divider = colors.divider
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface, shape)
            .drawBehind {
                val stroke = 1.dp.toPx()
                val r = radius.toPx()
                val extend = r * 2
                val top = if (position == SegmentPosition.FIRST || position == SegmentPosition.SINGLE) 0f else -extend
                val bottom = if (position == SegmentPosition.LAST || position == SegmentPosition.SINGLE) size.height else size.height + extend
                drawRoundRect(
                    color = border,
                    topLeft = Offset(stroke / 2f, top + stroke / 2f),
                    size = Size(size.width - stroke, bottom - top - stroke),
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(stroke),
                )
                if (position == SegmentPosition.MIDDLE || position == SegmentPosition.LAST) {
                    drawLine(divider, Offset(0f, stroke / 2f), Offset(size.width, stroke / 2f), stroke)
                }
            },
    ) {
        content()
    }
}

/**
 * Row of the list view: 40 x 56 thumbnail, serif title, subtitle, then [trailing] (genre, stars,
 * status pill) and the optional remove cross of a collection.
 */
@Composable
fun LibraryListRow(
    imageUrl: String?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mode: MediaMode = NookTheme.mode,
    onRemove: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit,
) {
    val colors = NookTheme.colors
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MediaImage(imageUrl, title, Modifier.width(40.dp), mode = mode, shape = NookShapes.lg, placeholderIconSize = 16.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = NookTheme.type.cardTitleSerif, color = colors.textStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = NookTheme.type.xs, color = colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
        if (onRemove != null) {
            IconGhostButton(LucideIcons.X, contentDescription = null, onClick = onRemove, size = 14.dp, padding = 6.dp, tint = colors.textFaint, shape = NookShapes.lg)
        }
    }
}

/** Small "remove from collection" cross over a grid card (`top-2 left-2 bg-black/50 rounded-full`). */
@Composable
fun CardRemoveButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.padding(8.dp).size(20.dp).clip(NookShapes.full).background(Palette.Black.alpha(0.5f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(LucideIcons.X, null, Modifier.size(11.dp), tint = Palette.White)
    }
}

/** Last line of a grid card (stars, progress, "year · length"), at a fixed height. */
@Composable
fun CardFooterLine(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.CenterStart) { content() }
}

/** Empty state of a collection tab, with the "Add …" call to action. */
@Composable
fun CategoryEmptyState(title: String, description: String, addLabel: String, onAdd: () -> Unit) {
    EmptyState(
        icon = LucideIcons.FolderOpen,
        title = title,
        description = description,
        action = { PrimaryButton(addLabel, onClick = onAdd, icon = LucideIcons.Plus, iconSize = 15.dp) },
    )
}
