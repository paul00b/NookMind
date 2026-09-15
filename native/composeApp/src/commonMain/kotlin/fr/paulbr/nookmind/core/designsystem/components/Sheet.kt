package fr.paulbr.nookmind.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import kotlinx.coroutines.launch

/** Gives sheet content a way to close itself with the slide-down animation (`useSheetClose`). */
class SheetController(private val state: SheetState, private val scope: kotlinx.coroutines.CoroutineScope, private val onClosed: () -> Unit) {
    fun close() {
        scope.launch { state.hide() }.invokeOnCompletion { onClosed() }
    }
}

/**
 * Port of SheetModal.tsx on top of Material 3's ModalBottomSheet: card background, 24 dp top
 * corners, drag handle (`h-1.5 w-12 rounded-full bg-gray-300 dark:bg-gray-600`), dark scrim,
 * swipe-to-dismiss, back to dismiss. [header] stays fixed while [content] scrolls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NookSheet(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = NookTheme.colors.surface,
    showHandle: Boolean = true,
    scrollable: Boolean = true,
    maxWidth: Dp = 640.dp,
    shape: Shape = NookShapes.sheetTop,
    header: (@Composable (SheetController) -> Unit)? = null,
    content: @Composable ColumnScope.(SheetController) -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val controller = SheetController(state, scope, onClose)
    ModalBottomSheet(
        onDismissRequest = onClose,
        modifier = modifier,
        sheetState = state,
        sheetMaxWidth = maxWidth,
        shape = shape,
        containerColor = background,
        contentColor = NookTheme.colors.textBody,
        tonalElevation = 0.dp,
        scrimColor = Palette.Black.copy(alpha = 0.5f),
        dragHandle = {
            if (showHandle) {
                Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.width(48.dp).height(6.dp).clip(NookShapes.full)
                            .background(if (NookTheme.colors.isDark) Palette.Gray600 else Palette.Gray300, NookShapes.full),
                    )
                }
            } else {
                Box(Modifier.height(0.dp))
            }
        },
    ) {
        Column(Modifier.fillMaxWidth()) {
            header?.invoke(controller)
            if (scrollable) {
                Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    content(controller)
                }
            } else {
                Column(Modifier.fillMaxWidth().weight(1f, fill = false)) { content(controller) }
            }
        }
    }
}

/** Close cross of the sheets (`absolute top-4 right-4 btn-ghost p-2`). */
@Composable
fun SheetCloseButton(controller: SheetController, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    IconGhostButton(LucideIcons.X, contentDescription = null, onClick = { controller.close() }, modifier = modifier, size = size)
}
