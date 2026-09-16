package fr.paulbr.nookmind.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.ui.HapticCue
import fr.paulbr.nookmind.core.ui.LocalNookHaptics
import fr.paulbr.nookmind.core.ui.ToastController
import fr.paulbr.nookmind.core.ui.ToastKind
import fr.paulbr.nookmind.core.ui.ToastMessage
import org.jetbrains.compose.resources.stringResource

@Composable
fun ToastMessage.resolve(): String = when (this) {
    is ToastMessage.Text -> text
    is ToastMessage.Resource -> if (args.isEmpty()) stringResource(res) else stringResource(res, *args.toTypedArray())
}

/** Bottom-centred toast stack (`position="bottom-center"`, 96 px above the bottom on mobile). */
@Composable
fun ToastHost(controller: ToastController, modifier: Modifier = Modifier, bottomOffset: Dp = 96.dp) {
    val toasts by controller.toasts.collectAsState()
    val haptics = LocalNookHaptics.current
    var lastCued by remember { mutableStateOf(-1L) }
    LaunchedEffect(toasts) {
        // Ids increase monotonically, so this fires once per toast and never on the
        // recompositions caused by the 3-second dismissal timer.
        val newest = toasts.lastOrNull() ?: return@LaunchedEffect
        if (newest.id <= lastCued) return@LaunchedEffect
        lastCued = newest.id
        when (newest.kind) {
            ToastKind.SUCCESS -> haptics.perform(HapticCue.CONFIRM)
            ToastKind.ERROR -> haptics.perform(HapticCue.REJECT)
            ToastKind.INFO -> Unit
        }
    }
    Box(modifier.fillMaxWidth().padding(bottom = bottomOffset), contentAlignment = Alignment.BottomCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            toasts.forEach { toast ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                ) {
                    Row(
                        Modifier
                            .widthIn(max = 360.dp)
                            .shadow(12.dp, NookShapes.xl, ambientColor = Palette.Black.copy(alpha = 0.3f), spotColor = Palette.Black.copy(alpha = 0.3f))
                            .clip(NookShapes.xl)
                            .background(NookTheme.colors.toastBackground, NookShapes.xl)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        when (toast.kind) {
                            ToastKind.SUCCESS -> Icon(LucideIcons.CheckCircle2, null, Modifier.size(18.dp), tint = Palette.Amber500)
                            ToastKind.ERROR -> Icon(LucideIcons.AlertTriangle, null, Modifier.size(18.dp), tint = Palette.Red400)
                            ToastKind.INFO -> Unit
                        }
                        Text(toast.message.resolve(), style = NookTheme.type.sans(14, FontWeight.Normal, 20), color = NookTheme.colors.toastText)
                    }
                }
            }
        }
    }
}
