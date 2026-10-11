package fr.paulbr.nookmind.feature.tonight

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.data.AuthState
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.NookTextField
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.ThemeMode
import fr.paulbr.nookmind.core.ui.HapticCue
import fr.paulbr.nookmind.core.ui.LocalNookHaptics
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.common_defaultDisplayName
import fr.paulbr.nookmind.resources.common_retry
import fr.paulbr.nookmind.resources.tonight_chance
import fr.paulbr.nookmind.resources.tonight_close
import fr.paulbr.nookmind.resources.tonight_deal
import fr.paulbr.nookmind.resources.tonight_dealError
import fr.paulbr.nookmind.resources.tonight_dealing
import fr.paulbr.nookmind.resources.tonight_entrySubtitle
import fr.paulbr.nookmind.resources.tonight_extraTime
import fr.paulbr.nookmind.resources.tonight_final
import fr.paulbr.nookmind.resources.tonight_finalTitle
import fr.paulbr.nookmind.resources.tonight_go
import fr.paulbr.nookmind.resources.tonight_handOver
import fr.paulbr.nookmind.resources.tonight_length
import fr.paulbr.nookmind.resources.tonight_maybesTitle
import fr.paulbr.nookmind.resources.tonight_noneTitle
import fr.paulbr.nookmind.resources.tonight_partnerFallback
import fr.paulbr.nookmind.resources.tonight_partnerPlaceholder
import fr.paulbr.nookmind.resources.tonight_redeal
import fr.paulbr.nookmind.resources.tonight_secret
import fr.paulbr.nookmind.resources.tonight_services
import fr.paulbr.nookmind.resources.tonight_shortOnly
import fr.paulbr.nookmind.resources.tonight_singleTitle
import fr.paulbr.nookmind.resources.tonight_tapPoster
import fr.paulbr.nookmind.resources.tonight_title
import fr.paulbr.nookmind.resources.tonight_turnOf
import fr.paulbr.nookmind.resources.tonight_withWho
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** The fixed night palette of Ce soir ?, whatever the app theme: it plays like a cinema. */
internal object TonightColors {
    val Background = Color(0xFF0D0E14)
    val Surface = Color(0xFF171A24)
    val Surface2 = Color(0xFF232736)
    val Line = Color(0x29FFFFFF)
    val Text = Color(0xFFF4F2EE)
    val Muted = Color(0xFFA3A5B4)
    val Faint = Color(0xFF6F7283)
    val Violet = Color(0xFF6D28D9)
    val VioletLight = Color(0xFFC4B5FD)
    val VioletText = Color(0xFFA78BFA)
    val Track = Color(0xFF333849)
    val Yes = Color(0xFF34D399)
    val No = Color(0xFFF87171)
    val Love = Color(0xFFF472B6)
    val Seen = Color(0xFFCBD5E1)
    val Me = Color(0xFF7C3AED)
    val Partner = Color(0xFFDB2777)
    val Paper = Color(0xFFF3ECDF)
    val Ink = Color(0xFF1C1917)
    val InkMuted = Color(0xFF78716C)
}

/** The players' first names, as the ticket and the hand-off screen show them. */
internal class TonightNames(val me: String, val partner: String)

@Composable
internal fun rememberTonightNames(container: AppContainer, state: TonightState): TonightNames {
    val auth by container.auth.state.collectAsState()
    val fallbackMe = stringResource(Res.string.common_defaultDisplayName)
    val fallbackPartner = stringResource(Res.string.tonight_partnerFallback)
    val me = ((auth as? AuthState.SignedIn)?.user?.displayName(fallbackMe) ?: fallbackMe).trim().substringBefore(' ')
    val partner = state.partnerName.trim().ifBlank { fallbackPartner }
    return TonightNames(me, partner)
}

/**
 * Ce soir ?, on a single phone: each player swipes the same ten cards in turn, then the movie both
 * said yes to wins. Drawn over the whole app; [animate] off renders each step at rest (screenshots).
 */
@Composable
fun TonightScreen(
    container: AppContainer,
    onClose: () -> Unit,
    state: TonightState = rememberTonightState(container),
    animate: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    BackHandler(enabled = true, onBack = onClose)

    fun deal(skip: Set<Int> = emptySet()) {
        container.prefs.tonightPartner = state.partnerName.trim()
        container.prefs.tonightServices = state.services.map { it.key }.toSet()
        state.dealError = false
        state.step = TonightStep.DEALING
        scope.launch {
            val started = kotlin.time.TimeSource.Monotonic.markNow()
            val cards = runCatching { dealTonightDeck(container, state.services, state.shortOnly, skip) }.getOrDefault(emptyList())
            // Let the shuffle be seen, even when everything came from the cache.
            val left = 900L - started.elapsedNow().inWholeMilliseconds
            if (left > 0) delay(left)
            if (cards.size < 4) {
                state.dealError = true
            } else {
                state.showRules = !container.prefs.tonightTutorialSeen
                state.startRound(cards)
            }
        }
    }

    NookTheme(themeMode = ThemeMode.DARK, mediaMode = MediaMode.MOVIES) {
        Box(
            Modifier
                .fillMaxSize()
                .background(TonightColors.Background)
                // Swallows the touches the steps leave, so nothing reaches the app underneath.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        ) {
            AmbientGlow()
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                val names = rememberTonightNames(container, state)
                Crossfade(state.step, animationSpec = tween(if (animate) 260 else 0), label = "tonight-step") { step ->
                    when (step) {
                        TonightStep.SETUP -> SetupStep(state, onDeal = { deal() }, onClose = onClose)
                        TonightStep.DEALING -> DealingStep(state.dealError, onRetry = { deal() }, onClose = onClose)
                        TonightStep.SWIPE -> SwipeStep(container, state, names, onClose = onClose)
                        TonightStep.HANDOFF -> HandoffStep(state, names)
                        TonightStep.DUEL -> DuelStep(state, names, onRedeal = { deal(state.deck.map { it.tmdbId }.toSet()) }, onClose = onClose)
                        TonightStep.MATCH -> MatchStep(container, state, names, animate, onPlayAgain = { state.reset() }, onClose = onClose)
                    }
                }
            }
        }
    }
}

// ── Setup ──────────────────────────────────────────────────────────────────

@Composable
private fun SetupStep(state: TonightState, onDeal: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.End) {
            RoundIconButton(LucideIcons.X, stringResource(Res.string.tonight_close), onClose)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text(stringResource(Res.string.tonight_title), style = NookTheme.type.serif(46, lineHeight = 50), color = TonightColors.Text)
            Spacer(Modifier.height(10.dp))
            SectionLabel(stringResource(Res.string.tonight_withWho))
            NookTextField(
                value = state.partnerName,
                onValueChange = { state.partnerName = it.take(24) },
                placeholder = stringResource(Res.string.tonight_partnerPlaceholder),
                modifier = Modifier.fillMaxWidth(),
                textStyle = NookTheme.type.sans(17, FontWeight.Medium, 24),
                shape = RoundedCornerShape(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            )
            SectionLabel(stringResource(Res.string.tonight_services))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StreamingService.entries.forEach { service ->
                    TonightChip(service.label, selected = service in state.services) {
                        state.services = if (service in state.services) state.services - service else state.services + service
                    }
                }
            }
            SectionLabel(stringResource(Res.string.tonight_length))
            TonightChip(stringResource(Res.string.tonight_shortOnly), selected = state.shortOnly) { state.shortOnly = !state.shortOnly }
            Spacer(Modifier.height(24.dp))
        }
        BigButton(stringResource(Res.string.tonight_deal), onDeal, Modifier.padding(horizontal = 18.dp, vertical = 16.dp))
    }
}

// ── Dealing ────────────────────────────────────────────────────────────────

@Composable
private fun DealingStep(error: Boolean, onRetry: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.End) {
            RoundIconButton(LucideIcons.X, stringResource(Res.string.tonight_close), onClose)
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (error) {
                Text(stringResource(Res.string.tonight_dealError), style = NookTheme.type.sans(16, FontWeight.Medium, 24), color = TonightColors.Text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                BigButton(stringResource(Res.string.common_retry), onRetry, Modifier.width(220.dp))
            } else {
                ShufflingCards()
                Spacer(Modifier.height(36.dp))
                Text(stringResource(Res.string.tonight_dealing), style = NookTheme.type.sans(16, FontWeight.Medium, 24), color = TonightColors.Muted)
            }
        }
    }
}

/** Three card backs fanning in and out while the deck is dealt. */
@Composable
private fun ShufflingCards() {
    val transition = rememberInfiniteTransition(label = "shuffle")
    val fan by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "fan")
    Box(Modifier.size(width = 150.dp, height = 190.dp), contentAlignment = Alignment.Center) {
        listOf(-1, 1, 0).forEach { side ->
            Box(
                Modifier
                    .size(width = 104.dp, height = 156.dp)
                    .graphicsLayer {
                        rotationZ = side * 14f * fan
                        translationX = side * 26.dp.toPx() * fan
                        translationY = if (side == 0) -6.dp.toPx() * fan else 0f
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF312E81), TonightColors.Violet, Color(0xFF9D174D))))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(LucideIcons.Popcorn, null, Modifier.size(30.dp), tint = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

// ── Hand-off ───────────────────────────────────────────────────────────────

@Composable
private fun HandoffStep(state: TonightState, names: TonightNames) {
    Box(Modifier.fillMaxSize()) {
        state.deck.firstOrNull()?.posterUrl?.let { poster ->
            AsyncImage(
                model = poster, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 110.dp).clip(RoundedCornerShape(28.dp)).blur(28.dp).alpha(0.45f),
            )
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Initial(names.partner, TonightColors.Partner, 104.dp, ring = true)
            Spacer(Modifier.height(26.dp))
            Text(stringResource(Res.string.tonight_turnOf, names.partner), style = NookTheme.type.serif(34, lineHeight = 40), color = TonightColors.Text, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(Res.string.tonight_handOver), style = NookTheme.type.sans(16, lineHeight = 23), color = TonightColors.Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.clip(CircleShape).background(Color(0x73000000)).padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(LucideIcons.Lock, null, Modifier.size(15.dp), tint = TonightColors.Text)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.tonight_secret), style = NookTheme.type.sans(14, FontWeight.Medium, 18), color = TonightColors.Text)
            }
            Spacer(Modifier.height(80.dp))
        }
        BigButton(stringResource(Res.string.tonight_go), { state.startPartnerTurn() }, Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp, vertical = 16.dp))
    }
}

// ── Duel ───────────────────────────────────────────────────────────────────

@Composable
private fun DuelStep(state: TonightState, names: TonightNames, onRedeal: () -> Unit, onClose: () -> Unit) {
    val haptics = LocalNookHaptics.current
    val scope = rememberCoroutineScope()
    val outcome = state.outcome ?: Outcome.None
    val sides: List<Int> = when (outcome) {
        is Outcome.Final -> listOf(outcome.a, outcome.b)
        is Outcome.Maybes -> listOf(outcome.a, outcome.b)
        is Outcome.Single -> listOf(outcome.index)
        else -> emptyList()
    }
    val pickVia = if (outcome is Outcome.Final) MatchVia.FINAL else MatchVia.EXTRA_TIME
    var chosen by remember(outcome) { mutableStateOf<Int?>(null) }
    var hot by remember(outcome) { mutableStateOf<Int?>(null) }
    val locked = chosen != null || hot != null

    fun choose(i: Int, via: MatchVia) {
        if (chosen != null) return
        chosen = i
        haptics.perform(HapticCue.CONFIRM)
        scope.launch { delay(650); state.win(i, via) }
    }

    fun roll() {
        if (locked) return
        val pool = sides.ifEmpty { state.deck.indices.filter { state.mine[it] != Verdict.SEEN && state.theirs[it] != Verdict.SEEN }.ifEmpty { state.deck.indices.toList() } }
        val winner = pool.random()
        scope.launch {
            if (sides.size == 2) {
                // The highlight jumps from one poster to the other, slower and slower, and stops on the
                // winner: with an even number of jumps, it starts on the other one.
                val delays = listOf(80, 80, 90, 100, 120, 150, 190, 240, 310, 400)
                var current = if (winner == sides[0]) sides[1] else sides[0]
                for (wait in delays) {
                    hot = current
                    haptics.perform(HapticCue.TICK)
                    delay(wait.toLong())
                    current = if (current == sides[0]) sides[1] else sides[0]
                }
                hot = winner
            }
            choose(winner, MatchVia.CHANCE)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (outcome is Outcome.Final) Res.string.tonight_final else Res.string.tonight_extraTime).uppercase(),
                style = NookTheme.type.sans(12, FontWeight.Bold, 16).copy(letterSpacing = 1.7.sp), color = TonightColors.VioletText,
                modifier = Modifier.weight(1f),
            )
            RoundIconButton(LucideIcons.X, stringResource(Res.string.tonight_close), onClose)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            val title = when (outcome) {
                is Outcome.Final -> Res.string.tonight_finalTitle
                is Outcome.Maybes -> Res.string.tonight_maybesTitle
                is Outcome.Single -> Res.string.tonight_singleTitle
                else -> Res.string.tonight_noneTitle
            }
            Text(stringResource(title), style = NookTheme.type.serif(32, lineHeight = 38), color = TonightColors.Text)
            if (sides.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(Res.string.tonight_tapPoster), style = NookTheme.type.sans(15, lineHeight = 21), color = TonightColors.Muted)
                Spacer(Modifier.height(20.dp))
                Box {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                        sides.forEach { i ->
                            DuelPoster(
                                card = state.deck[i],
                                mine = state.mine[i], theirs = state.theirs[i], names = names,
                                highlight = when { chosen == i -> TonightColors.Yes; hot == i -> TonightColors.Text; else -> null },
                                onClick = { if (!locked) choose(i, pickVia) },
                                modifier = Modifier.weight(1f, fill = false).width(170.dp),
                            )
                        }
                    }
                    if (sides.size == 2) {
                        Box(
                            Modifier.align(Alignment.TopCenter).offset(y = 104.dp).size(46.dp).clip(CircleShape).background(TonightColors.Text),
                            contentAlignment = Alignment.Center,
                        ) { Text("VS", style = NookTheme.type.sans(15, FontWeight.ExtraBold, 18), color = Color(0xFF111111)) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            if (sides.size != 1) AltButton(LucideIcons.Dices, stringResource(Res.string.tonight_chance), enabled = !locked, onClick = ::roll)
            Spacer(Modifier.height(8.dp))
            AltButton(LucideIcons.Repeat, stringResource(Res.string.tonight_redeal), enabled = !locked, onClick = onRedeal)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DuelPoster(card: TonightCard, mine: Verdict?, theirs: Verdict?, names: TonightNames, highlight: Color?, onClick: () -> Unit, modifier: Modifier) {
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .then(if (highlight != null) Modifier.border(3.dp, highlight, RoundedCornerShape(18.dp)).padding(5.dp) else Modifier.padding(5.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(TonightColors.Surface)
                .clickable(onClick = onClick),
        ) {
            AsyncImage(card.posterUrl, card.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            // Who said yes, as avatars: no need for words here.
            Row(Modifier.padding(8.dp)) {
                if (mine?.isYes == true) Initial(names.me, TonightColors.Me, 24.dp, love = mine == Verdict.LOVE)
                if (theirs?.isYes == true) {
                    Box(Modifier.offset(x = if (mine?.isYes == true) (-6).dp else 0.dp)) {
                        Initial(names.partner, TonightColors.Partner, 24.dp, love = theirs == Verdict.LOVE)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(card.title, style = NookTheme.type.serif(16, lineHeight = 20), color = TonightColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// ── Shared pieces ──────────────────────────────────────────────────────────

/** Violet halo at the top of every step. */
@Composable
internal fun AmbientGlow() {
    Box(
        Modifier.fillMaxWidth().height(420.dp).drawBehind {
            drawRect(
                Brush.radialGradient(
                    listOf(TonightColors.Violet.copy(alpha = 0.34f), Color.Transparent),
                    center = Offset(size.width / 2f, -size.height * 0.08f),
                    radius = size.width * 1.05f,
                ),
            )
        },
    )
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = NookTheme.type.sans(11, FontWeight.Bold, 14).copy(letterSpacing = 1.3.sp),
        color = TonightColors.Faint,
        modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
    )
}

@Composable
private fun TonightChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val haptics = LocalNookHaptics.current
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (selected) TonightColors.Violet else TonightColors.Surface2)
            .clickable { haptics.perform(if (selected) HapticCue.TOGGLE_OFF else HapticCue.TOGGLE_ON); onClick() }
            .padding(horizontal = 15.dp, vertical = 10.dp),
    ) {
        Text(text, style = NookTheme.type.sans(15, FontWeight.Medium, 20), color = if (selected) Color.White else Color(0xFFD9DAE2))
    }
}

/** Full-width call to action of every step: the violet of the evening, pressed in when touched. */
@Composable
internal fun BigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, light: Boolean = false, icon: ImageVector? = null) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(60.dp)
            .graphicsLayer { val s = if (pressed) 0.97f else 1f; scaleX = s; scaleY = s }
            .clip(shape)
            .then(if (light) Modifier.background(TonightColors.Text) else Modifier.background(Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF9333EA)))))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val content = if (light) Color(0xFF111111) else Color.White
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp), tint = content)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = NookTheme.type.sans(17, FontWeight.SemiBold, 22), color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun AltButton(icon: ImageVector, text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(16.dp))
            .background(TonightColors.Surface)
            .border(1.dp, TonightColors.Line, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(TonightColors.Violet.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(18.dp), tint = TonightColors.VioletLight)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = NookTheme.type.sans(15, FontWeight.Medium, 20), color = TonightColors.Text)
    }
}

@Composable
internal fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        Modifier
            .size(40.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(RoundedCornerShape(13.dp))
            .background(TonightColors.Surface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, Modifier.size(19.dp), tint = TonightColors.Muted)
    }
}

/** A player as a coloured circle with their initial; [love] adds a small heart. */
@Composable
internal fun Initial(name: String, color: Color, size: Dp, ring: Boolean = false, love: Boolean = false) {
    Box {
        Box(
            Modifier
                .size(size)
                .then(if (ring) Modifier.border(2.dp, TonightColors.VioletText, CircleShape).padding(6.dp) else Modifier)
                .clip(CircleShape)
                .background(color)
                .then(if (ring) Modifier else Modifier.border(2.dp, TonightColors.Background, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            Text(initialOf(name), style = NookTheme.type.sans((size.value * 0.4f).toInt().coerceAtLeast(10), FontWeight.SemiBold), color = Color.White)
        }
        if (love) {
            Icon(LucideIcons.Heart, null, Modifier.align(Alignment.BottomEnd).offset(x = 3.dp, y = 3.dp).size(12.dp), tint = TonightColors.Love)
        }
    }
}

/** The way in, at the top of the movies' Next Up tab. */
@Composable
fun TonightEntryCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(NookShapes.xl2)
            .background(Brush.linearGradient(listOf(Color(0xFF312E81), TonightColors.Violet, Color(0xFF9D174D))))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(LucideIcons.Popcorn, null, Modifier.size(24.dp), tint = Color.White)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.tonight_title), style = NookTheme.type.serif(22, lineHeight = 26), color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(Res.string.tonight_entrySubtitle), style = NookTheme.type.sans(14, lineHeight = 19), color = Color.White.copy(alpha = 0.85f))
        }
        Icon(LucideIcons.ChevronRight, null, Modifier.size(20.dp), tint = Color.White.copy(alpha = 0.8f))
    }
}
