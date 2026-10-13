package fr.paulbr.nookmind.feature.tonight

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.ui.HapticCue
import fr.paulbr.nookmind.core.ui.LocalNookHaptics
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.tonight_close
import fr.paulbr.nookmind.resources.tonight_go
import fr.paulbr.nookmind.resources.tonight_love
import fr.paulbr.nookmind.resources.tonight_no
import fr.paulbr.nookmind.resources.tonight_preview
import fr.paulbr.nookmind.resources.tonight_reasonBecause
import fr.paulbr.nookmind.resources.tonight_reasonBoth
import fr.paulbr.nookmind.resources.tonight_reasonPartnerLiked
import fr.paulbr.nookmind.resources.tonight_reasonPartnerWatchlist
import fr.paulbr.nookmind.resources.tonight_reasonPopular
import fr.paulbr.nookmind.resources.tonight_reasonTrending
import fr.paulbr.nookmind.resources.tonight_reasonWatchlist
import fr.paulbr.nookmind.resources.tonight_ruleHold
import fr.paulbr.nookmind.resources.tonight_ruleLove
import fr.paulbr.nookmind.resources.tonight_ruleNo
import fr.paulbr.nookmind.resources.tonight_ruleSeen
import fr.paulbr.nookmind.resources.tonight_ruleYes
import fr.paulbr.nookmind.resources.tonight_rules
import fr.paulbr.nookmind.resources.tonight_rulesTitle
import fr.paulbr.nookmind.resources.tonight_seen
import fr.paulbr.nookmind.resources.tonight_stampLove
import fr.paulbr.nookmind.resources.tonight_stampNo
import fr.paulbr.nookmind.resources.tonight_stampSeen
import fr.paulbr.nookmind.resources.tonight_stampYes
import fr.paulbr.nookmind.resources.tonight_undo
import fr.paulbr.nookmind.resources.tonight_yes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import fr.paulbr.nookmind.resources.tonight_episodeLength
import fr.paulbr.nookmind.resources.tonight_ruleSeenSeries
import fr.paulbr.nookmind.resources.tonight_seasons
import fr.paulbr.nookmind.resources.tonight_seenSeries
import fr.paulbr.nookmind.resources.tonight_seriesEnded
import fr.paulbr.nookmind.resources.tonight_seriesOngoing
import fr.paulbr.nookmind.resources.tonight_stampSeenSeries
import org.jetbrains.compose.resources.pluralStringResource

private val CardShape = RoundedCornerShape(26.dp)

@Composable
internal fun SwipeStep(container: AppContainer, state: TonightState, names: TonightNames, onClose: () -> Unit) {
    val haptics = LocalNookHaptics.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var deckSize by remember { mutableStateOf(IntSize.Zero) }
    var flipped by remember { mutableStateOf(false) }
    var previewing by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val top = state.index
    val card = state.deck.getOrNull(top)
    val threshold = with(density) { 100.dp.toPx() }

    LaunchedEffect(top, state.player) {
        offset.snapTo(Offset.Zero)
        flipped = false
        previewing = false
    }
    // Stills for the long-press preview, fetched as the card comes up.
    LaunchedEffect(card?.tmdbId) {
        val id = card?.tmdbId ?: return@LaunchedEffect
        if (id !in state.stills) {
            state.stills[id] = if (card?.kind == TonightKind.SERIES) container.tmdb.fetchSeriesStills(id) else container.tmdb.fetchMovieStills(id)
        }
    }

    fun fling(verdict: Verdict) {
        if (busy || card == null) return
        busy = true
        haptics.perform(
            when (verdict) {
                Verdict.YES, Verdict.LOVE -> HapticCue.CONFIRM
                Verdict.NO -> HapticCue.REJECT
                Verdict.SEEN -> HapticCue.TICK
            },
        )
        scope.launch {
            val w = deckSize.width.toFloat().coerceAtLeast(1f)
            val h = deckSize.height.toFloat().coerceAtLeast(1f)
            val from = offset.value
            val target = when (verdict) {
                Verdict.YES -> Offset(w * 1.6f, from.y + h * 0.08f)
                Verdict.NO -> Offset(-w * 1.6f, from.y + h * 0.08f)
                Verdict.LOVE -> Offset(from.x, -h * 1.4f)
                Verdict.SEEN -> Offset(from.x, h * 1.4f)
            }
            offset.animateTo(target, tween(280, easing = FastOutLinearInEasing))
            state.decide(verdict)
            offset.snapTo(Offset.Zero)
            busy = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        // The poster of the card in play, blurred, tints the whole screen.
        card?.posterUrl?.let { poster ->
            Crossfade(poster, animationSpec = tween(400), label = "backdrop") { url ->
                AsyncImage(
                    url, null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.3f; scaleX = 1.2f; scaleY = 1.2f }.blur(48.dp),
                )
            }
        }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(LucideIcons.X, stringResource(Res.string.tonight_close), onClose)
                Spacer(Modifier.width(12.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(if (state.deck.size > 12) 2.dp else 4.dp)) {
                    state.deck.indices.forEach { i ->
                        Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(if (i < top) TonightColors.VioletText else TonightColors.Track))
                    }
                }
                Spacer(Modifier.width(12.dp))
                RoundIconButton(LucideIcons.CircleHelp, stringResource(Res.string.tonight_rules), { state.showRules = true })
                Spacer(Modifier.width(8.dp))
                RoundIconButton(LucideIcons.Undo2, stringResource(Res.string.tonight_undo), { if (!busy) state.undo() }, enabled = top > 0)
                Spacer(Modifier.width(10.dp))
                val remote = state.remote
                if (remote != null) {
                    PartnerBadge(names.partner, remote, state.deck.size)
                } else {
                    val me = state.player == Player.ME
                    Initial(if (me) names.me else names.partner, if (me) TonightColors.Me else TonightColors.Partner, 32.dp)
                }
            }

            Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).onSizeChanged { deckSize = it },
            ) {
                // Two cards wait behind the one in play; the next one rises as the top card leaves.
                val progress = (offset.value.getDistance() / threshold).coerceIn(0f, 1f)
                for (position in 2 downTo 0) {
                    val i = top + position
                    val item = state.deck.getOrNull(i) ?: continue
                    key(i) {
                        val scale by animateFloatAsState(1f - 0.05f * position + if (position == 1) 0.04f * progress else 0f, spring(stiffness = 500f), label = "scale")
                        val lift by animateDpAsState(14.dp * position, spring(stiffness = 500f), label = "lift")
                        val layer = Modifier.fillMaxSize().graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationY = lift.toPx()
                            alpha = if (position == 2) 0.45f else 1f
                        }
                        if (position == 0) {
                            DeckCard(
                                card = item,
                                stills = state.stills[item.tmdbId].orEmpty(),
                                flipped = flipped,
                                previewing = previewing,
                                offset = { offset.value },
                                threshold = threshold,
                                modifier = layer
                                    .graphicsLayer {
                                        translationX = offset.value.x
                                        translationY += offset.value.y
                                        rotationZ = (offset.value.x / density.density * 0.06f).coerceIn(-18f, 18f)
                                    }
                                    .pointerInput(top, state.player) {
                                        val minFling = 40.dp.toPx()
                                        val flingVelocity = 900.dp.toPx()
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            if (busy) return@awaitEachGesture
                                            val tracker = VelocityTracker()
                                            tracker.addPosition(down.uptimeMillis, down.position)
                                            var drag = Offset.Zero
                                            var moved = false
                                            var held = false
                                            var crossed: Verdict? = null
                                            val hold = scope.launch {
                                                delay(430)
                                                if (!moved && !flipped) {
                                                    held = true
                                                    previewing = true
                                                    haptics.perform(HapticCue.TICK)
                                                }
                                            }
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                tracker.addPosition(change.uptimeMillis, change.position)
                                                if (!change.pressed) break
                                                drag += change.positionChange()
                                                if (!moved && drag.getDistance() > viewConfiguration.touchSlop) {
                                                    moved = true
                                                    hold.cancel()
                                                }
                                                if (moved && !held) {
                                                    change.consume()
                                                    val now = drag
                                                    scope.launch { offset.snapTo(now) }
                                                    val pointing = verdictOf(drag.x, drag.y, 0f, 0f, threshold, minFling, flingVelocity)
                                                    if (pointing != crossed) {
                                                        if (pointing != null) haptics.perform(HapticCue.TICK)
                                                        crossed = pointing
                                                    }
                                                }
                                            }
                                            hold.cancel()
                                            when {
                                                held -> previewing = false
                                                !moved -> flipped = !flipped
                                                else -> {
                                                    val velocity = tracker.calculateVelocity()
                                                    val verdict = verdictOf(drag.x, drag.y, velocity.x, velocity.y, threshold, minFling, flingVelocity)
                                                    if (verdict != null) fling(verdict)
                                                    else scope.launch { offset.animateTo(Offset.Zero, spring(dampingRatio = 0.55f, stiffness = 380f)) }
                                                }
                                            }
                                        }
                                    },
                            )
                        } else {
                            DeckCard(item, emptyList(), flipped = false, previewing = false, offset = { Offset.Zero }, threshold = threshold, modifier = layer)
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionButton(LucideIcons.X, stringResource(Res.string.tonight_no), TonightColors.No, 64) { fling(Verdict.NO) }
                ActionButton(
                    LucideIcons.Eye,
                    stringResource(if (state.kind == TonightKind.SERIES) Res.string.tonight_seenSeries else Res.string.tonight_seen),
                    TonightColors.Seen, 52,
                ) { fling(Verdict.SEEN) }
                ActionButton(LucideIcons.Heart, stringResource(Res.string.tonight_love), TonightColors.Love, 52) { fling(Verdict.LOVE) }
                ActionButton(LucideIcons.Check, stringResource(Res.string.tonight_yes), TonightColors.Yes, 64, filled = true) { fling(Verdict.YES) }
            }
        }

        state.remote?.let { remote ->
            JoinedBanner(names.partner, remote.partnerJoined, Modifier.align(Alignment.TopCenter).padding(top = 66.dp))
        }

        if (state.showRules) {
            RulesOverlay(series = state.kind == TonightKind.SERIES, onGo = {
                state.showRules = false
                container.prefs.tonightTutorialSeen = true
            })
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, description: String, color: Color, size: Int, filled: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (filled) color.copy(alpha = 0.14f) else TonightColors.Surface)
            .border(1.dp, if (filled) color.copy(alpha = 0.45f) else TonightColors.Line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, Modifier.size((size * 0.42f).dp), tint = color)
    }
}

/** A card of the deck: the poster, its stamps, what it is and why it is there; the synopsis on its back. */
@Composable
private fun DeckCard(
    card: TonightCard,
    stills: List<String>,
    flipped: Boolean,
    previewing: Boolean,
    offset: () -> Offset,
    threshold: Float,
    modifier: Modifier,
) {
    val density = LocalDensity.current.density
    val flip by animateFloatAsState(if (flipped) 180f else 0f, tween(450), label = "flip")
    Box(modifier.graphicsLayer { rotationY = flip; cameraDistance = 14f * density }) {
        if (flip <= 90f) {
            Box(Modifier.fillMaxSize().clip(CardShape).background(Color.Black)) {
                AsyncImage(card.posterUrl, card.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                if (previewing) StillsPreview(stills.ifEmpty { listOfNotNull(card.posterUrl) })
                Box(
                    Modifier.fillMaxWidth().fillMaxHeight(0.58f).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000), Color(0xF0000000)))),
                )
                Stamp(stringResource(Res.string.tonight_stampYes), TonightColors.Yes, -16f, Alignment.TopStart) { stampAlpha(offset(), threshold, Verdict.YES) }
                Stamp(stringResource(Res.string.tonight_stampNo), TonightColors.No, 16f, Alignment.TopEnd) { stampAlpha(offset(), threshold, Verdict.NO) }
                Stamp(stringResource(Res.string.tonight_stampLove), TonightColors.Love, -6f, Alignment.Center) { stampAlpha(offset(), threshold, Verdict.LOVE) }
                Stamp(
                    stringResource(if (card.kind == TonightKind.SERIES) Res.string.tonight_stampSeenSeries else Res.string.tonight_stampSeen),
                    TonightColors.Seen, 4f, Alignment.Center,
                ) { stampAlpha(offset(), threshold, Verdict.SEEN) }
                if (!previewing) CardInfo(card, Modifier.align(Alignment.BottomStart).padding(18.dp))
            }
        } else {
            CardBack(card, Modifier.fillMaxSize().graphicsLayer { rotationY = 180f })
        }
    }
}

private fun stampAlpha(offset: Offset, threshold: Float, verdict: Verdict): Float {
    val vertical = abs(offset.y) > abs(offset.x)
    val value = when (verdict) {
        Verdict.YES -> if (vertical) 0f else offset.x
        Verdict.NO -> if (vertical) 0f else -offset.x
        Verdict.LOVE -> if (vertical) -offset.y else 0f
        Verdict.SEEN -> if (vertical) offset.y / 1.15f else 0f
    }
    return (value / threshold).coerceIn(0f, 1f)
}

@Composable
private fun BoxScope.Stamp(text: String, color: Color, angle: Float, alignment: Alignment, alpha: () -> Float) {
    Box(
        Modifier
            .align(alignment)
            .padding(horizontal = 22.dp, vertical = 34.dp)
            .graphicsLayer { this.alpha = alpha(); rotationZ = angle }
            .border(4.dp, color, RoundedCornerShape(12.dp))
            .background(Color(0x47000000), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        Text(text, style = NookTheme.type.sans(28, FontWeight.ExtraBold, 34).copy(letterSpacing = 2.sp), color = color, maxLines = 1)
    }
}

@Composable
private fun CardInfo(card: TonightCard, modifier: Modifier) {
    Column(modifier) {
        Text(card.title, style = NookTheme.type.serif(30, lineHeight = 33), color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis)
        val meta = cardMeta(card)
        if (meta.isNotEmpty()) {
            Text(meta, style = NookTheme.type.sans(15, FontWeight.Medium, 20), color = Color.White.copy(alpha = 0.82f), modifier = Modifier.padding(top = 8.dp))
        }
        // The platform, and for a series whether it is over: chips that wrap rather than squeeze.
        val ended = card.ended?.takeIf { card.kind == TonightKind.SERIES }
        if (card.provider != null || ended != null) {
            FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                card.provider?.let {
                    Text(
                        providerLabel(it),
                        style = NookTheme.type.sans(13, FontWeight.SemiBold, 16), color = Color.White, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x29FFFFFF)).padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                ended?.let { over ->
                    Text(
                        stringResource(if (over) Res.string.tonight_seriesEnded else Res.string.tonight_seriesOngoing),
                        style = NookTheme.type.sans(13, FontWeight.SemiBold, 16),
                        color = if (over) Color(0xFFD9DAE2) else Color(0xFF6EE7B7), maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(if (over) Color(0x29FFFFFF) else Color(0x2E34D399)).padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
        val (icon, reason) = when (val r = card.reason) {
            CardReason.Watchlist -> LucideIcons.Bookmark to stringResource(Res.string.tonight_reasonWatchlist)
            is CardReason.PartnerWatchlist -> LucideIcons.Bookmark to stringResource(Res.string.tonight_reasonPartnerWatchlist, r.name)
            CardReason.BothWatchlists -> LucideIcons.Heart to stringResource(Res.string.tonight_reasonBoth)
            is CardReason.BecauseYouLiked -> LucideIcons.Sparkles to stringResource(Res.string.tonight_reasonBecause, r.title)
            is CardReason.PartnerLiked -> LucideIcons.Sparkles to stringResource(Res.string.tonight_reasonPartnerLiked, r.name, r.title)
            CardReason.Trending -> LucideIcons.Flame to stringResource(Res.string.tonight_reasonTrending)
            CardReason.Popular -> LucideIcons.Star to stringResource(Res.string.tonight_reasonPopular)
        }
        Row(
            Modifier.padding(top = 12.dp).clip(CircleShape).background(Color(0x8C6D28D9)).border(1.dp, Color(0x59C4B5FD), CircleShape)
                .padding(start = 10.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(15.dp), tint = TonightColors.VioletLight)
            Spacer(Modifier.width(7.dp))
            Text(reason, style = NookTheme.type.sans(14, FontWeight.Medium, 18), color = Color(0xFFF5F3FF), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CardBack(card: TonightCard, modifier: Modifier) {
    Column(modifier.clip(CardShape).background(TonightColors.Surface).padding(22.dp)) {
        Text(card.title, style = NookTheme.type.serif(26, lineHeight = 30), color = TonightColors.Text)
        val by = listOfNotNull(card.director, cardMeta(card).ifEmpty { null }).joinToString(" · ")
        if (by.isNotEmpty()) Text(by, style = NookTheme.type.sans(15, lineHeight = 20), color = TonightColors.Muted, modifier = Modifier.padding(top = 6.dp))
        if (card.genres.isNotEmpty()) {
            FlowRow(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                card.genres.forEach {
                    Text(it, style = NookTheme.type.sans(13, FontWeight.Medium, 16), color = Color(0xFFD9DAE2), modifier = Modifier.clip(CircleShape).background(TonightColors.Surface2).padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
        }
        Text(
            card.movie.overview, style = NookTheme.type.sans(16, lineHeight = 24), color = Color(0xFFD7D8E0),
            modifier = Modifier.padding(top = 16.dp), maxLines = 14, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Long press: the movie's stills drift by in letterbox, like the first seconds of a trailer. */
@Composable
private fun StillsPreview(urls: List<String>) {
    if (urls.isEmpty()) return
    var shown by remember(urls) { mutableIntStateOf(0) }
    LaunchedEffect(urls) {
        while (true) {
            delay(1_700)
            shown = (shown + 1) % urls.size
        }
    }
    val drift = rememberInfiniteTransition(label = "drift")
    val zoom by drift.animateFloat(1.08f, 1.28f, infiniteRepeatable(tween(3_400, easing = LinearEasing), RepeatMode.Reverse), label = "zoom")
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(5_000, easing = LinearEasing)) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Crossfade(urls[shown % urls.size], animationSpec = tween(700), label = "still") { url ->
            AsyncImage(url, null, Modifier.fillMaxSize().graphicsLayer { scaleX = zoom; scaleY = zoom }, contentScale = ContentScale.Crop)
        }
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.13f).align(Alignment.TopCenter).background(Color.Black))
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.13f).align(Alignment.BottomCenter).background(Color.Black))
        Row(Modifier.align(Alignment.TopStart).padding(start = 16.dp).fillMaxHeight(0.2f), verticalAlignment = Alignment.Bottom) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(TonightColors.No))
            Spacer(Modifier.width(7.dp))
            Text(stringResource(Res.string.tonight_preview).uppercase(), style = NookTheme.type.sans(12, FontWeight.Bold, 14).copy(letterSpacing = 1.2.sp), color = Color.White)
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.16f).padding(horizontal = 16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Color(0x40FFFFFF))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(progress.value).background(Color.White))
            }
        }
    }
}

/** The rules, once: the first time Ce soir ? is played on this phone. */
@Composable
private fun RulesOverlay(series: Boolean, onGo: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xD6060709)).clickable(onClick = {}, indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(Modifier.padding(horizontal = 28.dp)) {
            Text(stringResource(Res.string.tonight_rulesTitle), style = NookTheme.type.serif(30, lineHeight = 36), color = TonightColors.Text)
            Spacer(Modifier.height(20.dp))
            Rule(LucideIcons.Check, TonightColors.Yes, stringResource(Res.string.tonight_ruleYes))
            Rule(LucideIcons.X, TonightColors.No, stringResource(Res.string.tonight_ruleNo))
            Rule(LucideIcons.Heart, TonightColors.Love, stringResource(Res.string.tonight_ruleLove))
            Rule(LucideIcons.Eye, TonightColors.Seen, stringResource(if (series) Res.string.tonight_ruleSeenSeries else Res.string.tonight_ruleSeen))
            Rule(LucideIcons.Hand, TonightColors.Text, stringResource(Res.string.tonight_ruleHold))
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.clip(RoundedCornerShape(14.dp)).background(TonightColors.Text).clickable(onClick = onGo).padding(horizontal = 20.dp, vertical = 13.dp),
            ) {
                Text(stringResource(Res.string.tonight_go), style = NookTheme.type.sans(16, FontWeight.SemiBold, 20), color = Color(0xFF111111))
            }
        }
    }
}

@Composable
private fun Rule(icon: ImageVector, color: Color, text: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(TonightColors.Surface2), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = color)
        }
        Spacer(Modifier.width(14.dp))
        Text(text, style = NookTheme.type.sans(16, lineHeight = 22), color = Color(0xFFE3E4EA))
    }
}

/** "2021 · 2 h 35" for a movie, "2022 · 2 saisons · épisodes de 50 min" for a series. */
@Composable
internal fun cardMeta(card: TonightCard): String {
    if (card.kind != TonightKind.SERIES) return listOfNotNull(card.year, formatRuntime(card.runtime)).joinToString(" · ")
    val seasons = card.seasons?.let { pluralStringResource(Res.plurals.tonight_seasons, it, it) }
    val episode = formatRuntime(card.runtime)?.let { stringResource(Res.string.tonight_episodeLength, it) }
    return listOfNotNull(card.year, seasons, episode).joinToString(" · ")
}
