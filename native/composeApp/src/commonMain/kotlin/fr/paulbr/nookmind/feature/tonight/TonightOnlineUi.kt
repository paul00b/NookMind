package fr.paulbr.nookmind.feature.tonight

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.ui.HapticCue
import fr.paulbr.nookmind.core.ui.LocalNookHaptics
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.tonight_close
import fr.paulbr.nookmind.resources.tonight_joined
import fr.paulbr.nookmind.resources.tonight_nudge
import fr.paulbr.nookmind.resources.tonight_nudged
import fr.paulbr.nookmind.resources.tonight_partnerLeft
import fr.paulbr.nookmind.resources.tonight_undo
import fr.paulbr.nookmind.resources.tonight_waitingHint
import fr.paulbr.nookmind.resources.tonight_waitingNotJoined
import fr.paulbr.nookmind.resources.tonight_waitingProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** A ring around a player, filled as far as [progress] (0 to 1). */
private fun DrawScope.progressRing(progress: Float, color: Color, stroke: Float) {
    val inset = stroke / 2f
    drawCircle(TonightColors.Track, radius = size.minDimension / 2f - inset, style = Stroke(stroke))
    if (progress > 0f) {
        drawArc(
            color, startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false,
            topLeft = Offset(inset, inset), size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

/** The other player in the corner of the deck: their initial, ringed by how far they got. */
@Composable
internal fun PartnerBadge(name: String, remote: TonightRemote, total: Int, size: Dp = 32.dp) {
    val progress by animateFloatAsState(if (total == 0) 0f else remote.partnerProgress.toFloat() / total, tween(450), label = "partner-badge")
    Box(
        Modifier
            .size(size + 10.dp)
            .alpha(if (remote.partnerJoined) 1f else 0.45f)
            .drawBehind { progressRing(progress, TonightColors.Partner, 3.dp.toPx()) },
        contentAlignment = Alignment.Center,
    ) {
        Initial(name, TonightColors.Partner, size)
    }
}

/** "Léa is here", for a moment, when the other phone opens the invitation. */
@Composable
internal fun JoinedBanner(name: String, joined: Boolean, modifier: Modifier = Modifier) {
    val haptics = LocalNookHaptics.current
    var seen by remember { mutableStateOf(joined) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(joined) {
        if (joined && !seen) {
            seen = true
            haptics.perform(HapticCue.TICK)
            visible = true
            delay(2_600)
            visible = false
        }
    }
    AnimatedVisibility(visible, modifier = modifier, enter = fadeIn() + slideInVertically { -it }, exit = fadeOut()) {
        Row(
            Modifier.clip(CircleShape).background(TonightColors.Surface2).padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Initial(name, TonightColors.Partner, 24.dp)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.tonight_joined, name), style = NookTheme.type.sans(14, FontWeight.SemiBold, 18), color = TonightColors.Text)
        }
    }
}

/** Online, done with the deck: the other one is still swiping, and a match can still come. */
@Composable
internal fun WaitingStep(container: AppContainer, state: TonightState, names: TonightNames, onClose: () -> Unit) {
    val remote = state.remote ?: return
    val scope = rememberCoroutineScope()
    val total = state.deck.size
    val progress by animateFloatAsState(if (total == 0) 0f else remote.partnerProgress.toFloat() / total, tween(500), label = "partner-progress")
    val breath by rememberInfiniteTransition(label = "waiting").animateFloat(
        0.94f, 1.04f, infiniteRepeatable(tween(1_200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath",
    )
    Box(Modifier.fillMaxSize()) {
        state.deck.lastOrNull()?.posterUrl?.let { poster ->
            AsyncImage(
                poster, null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 110.dp).clip(RoundedCornerShape(28.dp)).blur(32.dp).alpha(0.35f),
            )
        }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                RoundIconButton(LucideIcons.Undo2, stringResource(Res.string.tonight_undo), { state.undo() })
                RoundIconButton(LucideIcons.X, stringResource(Res.string.tonight_close), onClose)
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(150.dp)
                        .graphicsLayer { scaleX = breath; scaleY = breath }
                        .drawBehind { progressRing(progress, TonightColors.Partner, 6.dp.toPx()) },
                    contentAlignment = Alignment.Center,
                ) {
                    Initial(names.partner, TonightColors.Partner, 104.dp)
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    if (remote.partnerJoined) stringResource(Res.string.tonight_waitingProgress, names.partner, remote.partnerProgress, total)
                    else stringResource(Res.string.tonight_waitingNotJoined, names.partner),
                    style = NookTheme.type.serif(30, lineHeight = 36), color = TonightColors.Text, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(stringResource(Res.string.tonight_waitingHint), style = NookTheme.type.sans(16, lineHeight = 23), color = TonightColors.Muted, textAlign = TextAlign.Center)
                if (!remote.partnerJoined) {
                    Spacer(Modifier.height(26.dp))
                    AltButton(
                        LucideIcons.Send,
                        if (remote.nudged) stringResource(Res.string.tonight_nudged) else stringResource(Res.string.tonight_nudge, names.partner),
                        enabled = !remote.nudged,
                    ) {
                        remote.nudged = true
                        scope.launch { container.social.nudge(remote.sessionId) }
                    }
                }
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

/** The other phone left: the evening stops here. */
@Composable
internal fun PartnerLeftOverlay(name: String, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(TonightColors.Background.copy(alpha = 0.94f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Initial(name, TonightColors.Partner, 72.dp)
            Spacer(Modifier.height(20.dp))
            Text(stringResource(Res.string.tonight_partnerLeft, name), style = NookTheme.type.serif(28, lineHeight = 34), color = TonightColors.Text, textAlign = TextAlign.Center)
            Spacer(Modifier.height(26.dp))
            BigButton(stringResource(Res.string.tonight_close), onClose, Modifier.width(220.dp))
        }
    }
}
