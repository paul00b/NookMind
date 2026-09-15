package fr.paulbr.nookmind.feature.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.alpha
import fr.paulbr.nookmind.core.designsystem.components.IconGhostButton
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.SearchSectionPreference
import kotlin.math.roundToInt

/** Height of one reorderable row (`py-2.5` + text) plus the 8 dp gap between rows. */
private val ROW_HEIGHT = 44.dp
private val ROW_GAP = 8.dp

/**
 * Index the dragged row would land on if released now: how many whole rows the finger has travelled,
 * added to where the row currently sits, clamped to the list.
 */
fun dropTarget(fromIndex: Int, dragOffset: Float, stepPx: Float, lastIndex: Int): Int {
    if (fromIndex < 0 || lastIndex < 0 || stepPx <= 0f) return fromIndex
    return (fromIndex + (dragOffset / stepPx).roundToInt()).coerceIn(0, lastIndex)
}

/**
 * How many slots a row that is NOT being dragged steps aside, so the dragged row has somewhere to
 * land: up one slot when the drag passes over it going down, down one slot going up, zero otherwise.
 */
fun rowShiftSlots(index: Int, fromIndex: Int, toIndex: Int): Int = when {
    fromIndex < 0 || index == fromIndex -> 0
    index in (fromIndex + 1)..toIndex -> -1
    index in toIndex..<fromIndex -> 1
    else -> 0
}

/** Travel the drag is allowed, so passing the first or last row does not build up a dead zone. */
fun dragBounds(fromIndex: Int, stepPx: Float, lastIndex: Int): ClosedFloatingPointRange<Float> =
    ((0 - fromIndex) * stepPx)..((lastIndex - fromIndex) * stepPx)

/**
 * Port of the section reorder list of SettingsPanel.tsx: drag the grip to move a section, tap the
 * eye to hide it.
 *
 * The list is NOT reordered while the finger is down. The dragged row follows the finger, the rows
 * it passes slide out of its way, and the move is committed once on release. Reordering mid-drag
 * meant the gesture kept working from the list it had captured when it started, so every move after
 * the first computed the wrong destination and the row ended up drawn on top of another one.
 */
@Composable
fun SectionOrderEditor(
    sections: List<SearchSectionPreference>,
    labelOf: @Composable (String) -> String,
    onMove: (sectionId: String, toIndex: Int) -> Unit,
    onToggleVisible: (sectionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val stepPx = with(density) { (ROW_HEIGHT + ROW_GAP).toPx() }
    val latestSections by rememberUpdatedState(sections)
    val latestMove by rememberUpdatedState(onMove)

    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    val fromIndex = sections.indexOfFirst { it.id == draggingId }
    val toIndex = dropTarget(fromIndex, dragOffset, stepPx, sections.lastIndex)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ROW_GAP)) {
        sections.forEachIndexed { index, section ->
            key(section.id) {
                val dragging = section.id == draggingId
                val shift by animateFloatAsState(
                    rowShiftSlots(index, fromIndex, toIndex) * stepPx,
                    tween(150),
                    label = "rowShift",
                )

                SectionRow(
                    label = labelOf(section.id),
                    visible = section.visible,
                    dragging = dragging,
                    offsetY = if (dragging) dragOffset else shift,
                    onToggleVisible = { onToggleVisible(section.id) },
                    dragModifier = Modifier.pointerInput(section.id) {
                        detectDragGestures(
                            onDragStart = { draggingId = section.id; dragOffset = 0f },
                            onDragEnd = {
                                val from = latestSections.indexOfFirst { it.id == section.id }
                                val to = dropTarget(from, dragOffset, stepPx, latestSections.lastIndex)
                                if (from >= 0 && to != from) latestMove(section.id, to)
                                draggingId = null
                                dragOffset = 0f
                            },
                            onDragCancel = { draggingId = null; dragOffset = 0f },
                            onDrag = { change, amount ->
                                change.consume()
                                val from = latestSections.indexOfFirst { it.id == section.id }
                                if (from >= 0) {
                                    dragOffset = (dragOffset + amount.y)
                                        .coerceIn(dragBounds(from, stepPx, latestSections.lastIndex))
                                }
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionRow(
    label: String,
    visible: Boolean,
    dragging: Boolean,
    offsetY: Float,
    onToggleVisible: () -> Unit,
    dragModifier: Modifier,
) {
    val colors = NookTheme.colors
    val scale by animateFloatAsState(if (dragging) 1.015f else 1f, label = "dragScale")
    Row(
        Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer { translationY = offsetY; scaleX = scale; scaleY = scale }
            .then(if (dragging) Modifier.shadow(12.dp, NookShapes.xl, ambientColor = Palette.Amber500.alpha(0.2f), spotColor = Palette.Amber500.alpha(0.2f)) else Modifier)
            .clip(NookShapes.xl)
            .background(if (dragging) colors.surface else Color.Transparent, NookShapes.xl)
            .border(1.dp, if (dragging) Palette.Amber400 else colors.divider, NookShapes.xl)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(dragModifier.padding(end = 8.dp)) {
            Icon(LucideIcons.GripVertical, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.textFaint)
        }
        Text(
            label,
            style = NookTheme.type.sm.copy(textDecoration = if (visible) null else TextDecoration.LineThrough),
            color = if (visible) colors.textBody else colors.textFaint,
            modifier = Modifier.weight(1f),
        )
        IconGhostButton(
            if (visible) LucideIcons.Eye else LucideIcons.EyeOff,
            contentDescription = null,
            onClick = onToggleVisible,
            size = 16.dp,
            padding = 6.dp,
            tint = if (visible) colors.textBody2 else colors.textFaint,
        )
    }
}
