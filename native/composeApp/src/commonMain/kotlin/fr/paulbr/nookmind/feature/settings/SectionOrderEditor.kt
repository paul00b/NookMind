package fr.paulbr.nookmind.feature.settings

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
 * Port of the section reorder list of SettingsPanel.tsx: drag the grip to move a section, tap the
 * eye to hide it. Rows swap live while dragging, like the web pointer-move handler.
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
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ROW_GAP)) {
        sections.forEachIndexed { index, section ->
            val dragging = draggingId == section.id
            SectionRow(
                label = labelOf(section.id),
                visible = section.visible,
                dragging = dragging,
                offsetY = if (dragging) dragOffset else 0f,
                onToggleVisible = { onToggleVisible(section.id) },
                dragModifier = Modifier.pointerInput(section.id, sections.size) {
                    detectDragGestures(
                        onDragStart = { draggingId = section.id; dragOffset = 0f },
                        onDragEnd = { draggingId = null; dragOffset = 0f },
                        onDragCancel = { draggingId = null; dragOffset = 0f },
                        onDrag = { change, amount ->
                            change.consume()
                            dragOffset += amount.y
                            val steps = (dragOffset / stepPx).roundToInt()
                            if (steps != 0) {
                                val current = sections.indexOfFirst { it.id == section.id }
                                val target = (current + steps).coerceIn(0, sections.lastIndex)
                                if (target != current) {
                                    onMove(section.id, target)
                                    dragOffset -= (target - current) * stepPx
                                }
                            }
                        },
                    )
                },
            )
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
            .background(if (dragging) Palette.Amber500.alpha(0.10f) else Color.Transparent, NookShapes.xl)
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
