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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.draw.drawWithContent
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
import fr.paulbr.nookmind.core.data.TonightSessionRow
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.Avatar
import fr.paulbr.nookmind.core.designsystem.components.AvatarSize
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
import fr.paulbr.nookmind.resources.tonight_gone
import fr.paulbr.nookmind.resources.tonight_handOver
import fr.paulbr.nookmind.resources.tonight_invite
import fr.paulbr.nookmind.resources.tonight_maybesTitle
import fr.paulbr.nookmind.resources.tonight_noneTitle
import fr.paulbr.nookmind.resources.tonight_onlineError
import fr.paulbr.nookmind.resources.tonight_onlineHint
import fr.paulbr.nookmind.resources.tonight_partnerFallback
import fr.paulbr.nookmind.resources.tonight_partnerPicked
import fr.paulbr.nookmind.resources.tonight_partnerPlaceholder
import fr.paulbr.nookmind.resources.tonight_redeal
import fr.paulbr.nookmind.resources.tonight_samePhone
import fr.paulbr.nookmind.resources.tonight_secret
import fr.paulbr.nookmind.resources.tonight_services
import fr.paulbr.nookmind.resources.tonight_shortOnly
import fr.paulbr.nookmind.resources.tonight_singleTitle
import fr.paulbr.nookmind.resources.tonight_tapPoster
import fr.paulbr.nookmind.resources.tonight_tapPosterOnline
import fr.paulbr.nookmind.resources.tonight_title
import fr.paulbr.nookmind.resources.tonight_turnOf
import fr.paulbr.nookmind.resources.tonight_waitingPick
import fr.paulbr.nookmind.resources.tonight_withWho
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import fr.paulbr.nookmind.resources.tonight_mood
import fr.paulbr.nookmind.resources.tonight_genreAll
import fr.paulbr.nookmind.resources.tonight_howMany
import fr.paulbr.nookmind.resources.tonight_cards
import fr.paulbr.nookmind.resources.tonight_genreAction
import fr.paulbr.nookmind.resources.tonight_genreComedy
import fr.paulbr.nookmind.resources.tonight_genreDrama
import fr.paulbr.nookmind.resources.tonight_genreThriller
import fr.paulbr.nookmind.resources.tonight_genreScifi
import fr.paulbr.nookmind.resources.tonight_genreHorror
import fr.paulbr.nookmind.resources.tonight_genreRomance
import fr.paulbr.nookmind.resources.tonight_genreAnimation
import fr.paulbr.nookmind.resources.tonight_genreDocumentary

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
 * Ce soir ?: on a single phone each player swipes the same ten cards in turn; online each one plays
 * on their own phone, and the first movie both say yes to wins at once. Drawn over the whole app;
 * [joinSessionId] opens an invitation; [animate] off renders each step at rest (screenshots).
 */
@Composable
fun TonightScreen(
    container: AppContainer,
    onClose: () -> Unit,
    state: TonightState = rememberTonightState(container),
    animate: Boolean = true,
    joinSessionId: String? = null,
) {
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf<TonightLink?>(null) }
    val fallbackPartner = stringResource(Res.string.tonight_partnerFallback)

    /** Leaving an online evening before the end calls it off for the other phone too. */
    fun leave() {
        val remote = state.remote
        if (remote != null && state.step != TonightStep.MATCH && !remote.partnerLeft) {
            container.scope.launch { container.social.cancelTonight(remote.sessionId) }
        }
        onClose()
    }
    BackHandler(enabled = true, onBack = ::leave)

    fun goOnline(row: TonightSessionRow, myId: String, cards: List<TonightCard>) {
        val isHost = row.host == myId
        val remote = TonightRemote(row.id, isHost, if (isHost) row.guest else row.host).apply { round = row.round }
        val newLink = TonightLink(container.social, state, remote, myId, scope)
        state.remote = remote
        state.sync = newLink
        link = newLink
        state.showRules = !container.prefs.tonightTutorialSeen
        state.startRound(cards)
        newLink.applyRow(row)
    }

    fun deal(skip: Set<Int> = emptySet()) {
        val friend = state.friend
        val remote = state.remote
        container.prefs.tonightServices = state.services.map { it.key }.toSet()
        container.prefs.tonightGenres = state.genres.map { it.key }.toSet()
        container.prefs.tonightDeckSize = state.deckSize
        if (remote == null) {
            container.prefs.tonightFriend = friend?.userId.orEmpty()
            if (friend == null) container.prefs.tonightPartner = state.partnerName.trim() else state.partnerName = friend.firstName
        }
        state.dealError = null
        state.step = TonightStep.DEALING
        scope.launch {
            val started = kotlin.time.TimeSource.Monotonic.markNow()
            val partnerId = remote?.partnerId ?: friend?.userId
            val taste = partnerId?.let { id -> partnerTasteOf(state.partnerName, container.social.library.value.filter { it.owner == id }) }
            val cards = runCatching { dealTonightDeck(container, state.services, state.shortOnly, skip, taste, state.deckSize, state.genres) }.getOrDefault(emptyList())
            // Let the shuffle be seen, even when everything came from the cache.
            val left = 900L - started.elapsedNow().inWholeMilliseconds
            if (left > 0) delay(left)
            val myId = container.social.myId
            val activeLink = link
            when {
                cards.size < 4 -> state.dealError = DealError.NOT_ENOUGH
                remote != null && activeLink != null -> if (!activeLink.redeal(cards)) state.dealError = DealError.ONLINE
                friend != null && myId != null -> {
                    val row = container.social.createTonight(friend.userId, encodeDeck(cards, myId, friend.userId))
                    if (row == null) state.dealError = DealError.ONLINE else goOnline(row, myId, cards)
                }
                friend != null -> state.dealError = DealError.ONLINE
                else -> {
                    state.showRules = !container.prefs.tonightTutorialSeen
                    state.startRound(cards)
                }
            }
        }
    }

    // An invitation: the deck is the host's, the reasons are said from this side.
    LaunchedEffect(joinSessionId) {
        val id = joinSessionId ?: return@LaunchedEffect
        state.dealError = null
        state.step = TonightStep.DEALING
        val started = kotlin.time.TimeSource.Monotonic.markNow()
        val myId = container.social.myId
        val row = container.social.joinTonight(id)
        val left = 900L - started.elapsedNow().inWholeMilliseconds
        if (left > 0) delay(left)
        if (row == null || myId == null || row.status == "cancelled" || row.status == "done") {
            state.dealError = DealError.GONE
            return@LaunchedEffect
        }
        val partnerId = if (row.host == myId) row.guest else row.host
        val partner = container.social.friendById(partnerId) ?: run {
            container.social.refresh()
            container.social.friendById(partnerId)
        }
        state.friend = partner
        state.partnerName = partner?.firstName?.ifBlank { null } ?: fallbackPartner
        goOnline(row, myId, decodeDeck(row.deck, myId, state.partnerName))
    }

    // Where the other phone is: its progress, a match, its picks, a new deal, or its leaving.
    LaunchedEffect(link) {
        val active = link ?: return@LaunchedEffect
        while (true) {
            delay(1_500)
            if (state.step != TonightStep.MATCH) active.poll()
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
                        TonightStep.SETUP -> SetupStep(container, state, onDeal = { deal() }, onClose = ::leave)
                        TonightStep.DEALING -> DealingStep(state.dealError, onRetry = { deal(state.deck.map { it.tmdbId }.toSet()) }, onClose = ::leave)
                        TonightStep.SWIPE -> SwipeStep(container, state, names, onClose = ::leave)
                        TonightStep.HANDOFF -> HandoffStep(state, names)
                        TonightStep.WAITING -> WaitingStep(container, state, names, onClose = ::leave)
                        TonightStep.DUEL -> DuelStep(state, names, link, onRedeal = { deal(state.deck.map { it.tmdbId }.toSet()) }, onClose = ::leave)
                        TonightStep.MATCH -> MatchStep(container, state, names, animate, onPlayAgain = { link = null; state.reset() }, onClose = onClose)
                    }
                }
                if (state.remote?.partnerLeft == true && state.step != TonightStep.MATCH) {
                    PartnerLeftOverlay(names.partner, onClose = onClose)
                }
            }
        }
    }
}

// ── Setup ──────────────────────────────────────────────────────────────────

@Composable
private fun SetupStep(container: AppContainer, state: TonightState, onDeal: () -> Unit, onClose: () -> Unit) {
    val friends by container.social.friends.collectAsState()
    val accepted = friends.filter { it.accepted }
    val friend = state.friend
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.End) {
            RoundIconButton(LucideIcons.X, stringResource(Res.string.tonight_close), onClose)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text(stringResource(Res.string.tonight_title), style = NookTheme.type.serif(46, lineHeight = 50), color = TonightColors.Text)
            Spacer(Modifier.height(10.dp))
            SectionLabel(stringResource(Res.string.tonight_withWho))
            if (accepted.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    WhoChip(stringResource(Res.string.tonight_samePhone), selected = friend == null, onClick = {
                        state.friend = null
                        state.partnerName = container.prefs.tonightPartner
                    }) {
                        Icon(LucideIcons.Smartphone, null, Modifier.size(22.dp), tint = TonightColors.Text)
                    }
                    accepted.forEach { f ->
                        WhoChip(f.firstName, selected = friend?.userId == f.userId, onClick = { state.friend = f }) {
                            Avatar(f.displayName, f.avatarUrl, AvatarSize.LG)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            if (friend == null) {
                NookTextField(
                    value = state.partnerName,
                    onValueChange = { state.partnerName = it.take(24) },
                    placeholder = stringResource(Res.string.tonight_partnerPlaceholder),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = NookTheme.type.sans(17, FontWeight.Medium, 24),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(LucideIcons.Wifi, null, Modifier.size(16.dp), tint = TonightColors.VioletText)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.tonight_onlineHint, friend.firstName), style = NookTheme.type.sans(15, lineHeight = 21), color = TonightColors.Muted)
                }
            }
            SectionLabel(stringResource(Res.string.tonight_mood))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TonightChip(stringResource(Res.string.tonight_genreAll), selected = state.genres.isEmpty()) { state.genres = emptySet() }
                TonightGenre.entries.forEach { genre ->
                    TonightChip(genreLabel(genre), selected = genre in state.genres) {
                        state.genres = if (genre in state.genres) state.genres - genre else state.genres + genre
                    }
                }
            }
            SectionLabel(stringResource(Res.string.tonight_services))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StreamingService.entries.forEach { service ->
                    TonightChip(service.label, selected = service in state.services) {
                        state.services = if (service in state.services) state.services - service else state.services + service
                    }
                }
            }
            SectionLabel(stringResource(Res.string.tonight_howMany))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DECK_SIZES.forEach { size ->
                    TonightChip(stringResource(Res.string.tonight_cards, size), selected = state.deckSize == size) { state.deckSize = size }
                }
                Spacer(Modifier.width(8.dp))
                TonightChip(stringResource(Res.string.tonight_shortOnly), selected = state.shortOnly) { state.shortOnly = !state.shortOnly }
            }
            Spacer(Modifier.height(24.dp))
        }
        BigButton(
            if (friend != null) stringResource(Res.string.tonight_invite, friend.firstName) else stringResource(Res.string.tonight_deal),
            onDeal,
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            icon = if (friend != null) LucideIcons.Send else null,
        )
    }
}

@Composable
private fun genreLabel(genre: TonightGenre): String = stringResource(
    when (genre) {
        TonightGenre.ACTION -> Res.string.tonight_genreAction
        TonightGenre.COMEDY -> Res.string.tonight_genreComedy
        TonightGenre.DRAMA -> Res.string.tonight_genreDrama
        TonightGenre.THRILLER -> Res.string.tonight_genreThriller
        TonightGenre.SCIFI -> Res.string.tonight_genreScifi
        TonightGenre.HORROR -> Res.string.tonight_genreHorror
        TonightGenre.ROMANCE -> Res.string.tonight_genreRomance
        TonightGenre.ANIMATION -> Res.string.tonight_genreAnimation
        TonightGenre.DOCUMENTARY -> Res.string.tonight_genreDocumentary
    },
)

/** Someone to play with: a round picture and a first name, ringed when picked. */
@Composable
private fun WhoChip(label: String, selected: Boolean, onClick: () -> Unit, picture: @Composable () -> Unit) {
    val haptics = LocalNookHaptics.current
    Column(
        Modifier.width(64.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
            haptics.perform(HapticCue.TOGGLE_ON)
            onClick()
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(60.dp)
                .border(2.dp, if (selected) TonightColors.VioletText else Color.Transparent, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(TonightColors.Surface2),
            contentAlignment = Alignment.Center,
        ) { picture() }
        Spacer(Modifier.height(6.dp))
        Text(
            label, style = NookTheme.type.sans(13, if (selected) FontWeight.SemiBold else FontWeight.Medium, 17),
            color = if (selected) TonightColors.Text else TonightColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Dealing ────────────────────────────────────────────────────────────────

@Composable
private fun DealingStep(error: DealError?, onRetry: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.End) {
            RoundIconButton(LucideIcons.X, stringResource(Res.string.tonight_close), onClose)
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (error != null) {
                val message = when (error) {
                    DealError.NOT_ENOUGH -> Res.string.tonight_dealError
                    DealError.ONLINE -> Res.string.tonight_onlineError
                    DealError.GONE -> Res.string.tonight_gone
                }
                Text(stringResource(message), style = NookTheme.type.sans(16, FontWeight.Medium, 24), color = TonightColors.Text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                if (error == DealError.GONE) BigButton(stringResource(Res.string.tonight_close), onClose, Modifier.width(220.dp))
                else BigButton(stringResource(Res.string.common_retry), onRetry, Modifier.width(220.dp))
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
            Text(stringResource(Res.string.tonight_handOver, state.deck.size), style = NookTheme.type.sans(16, lineHeight = 23), color = TonightColors.Muted, textAlign = TextAlign.Center)
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
private fun DuelStep(state: TonightState, names: TonightNames, link: TonightLink?, onRedeal: () -> Unit, onClose: () -> Unit) {
    val haptics = LocalNookHaptics.current
    val scope = rememberCoroutineScope()
    val outcome = state.outcome ?: Outcome.None
    val remote = state.remote
    val online = remote != null
    val sides: List<Int> = when (outcome) {
        is Outcome.Final -> listOf(outcome.a, outcome.b)
        is Outcome.Maybes -> listOf(outcome.a, outcome.b)
        is Outcome.Single -> listOf(outcome.index)
        else -> emptyList()
    }
    val pickVia = if (outcome is Outcome.Final) MatchVia.FINAL else MatchVia.EXTRA_TIME
    var chosen by remember(outcome) { mutableStateOf<Int?>(null) }
    var hot by remember(outcome) { mutableStateOf<Int?>(null) }
    val locked = chosen != null || hot != null || state.pendingChance != null

    fun choose(i: Int, via: MatchVia) {
        if (chosen != null) return
        chosen = i
        haptics.perform(HapticCue.CONFIRM)
        scope.launch { delay(650); state.win(i, via) }
    }

    fun tap(i: Int) {
        if (locked) return
        if (online) {
            // Online each one picks on their own phone: the same poster wins, two different ones go to chance.
            haptics.perform(HapticCue.CONFIRM)
            link?.pick(i, sides.first(), sides.last())
        } else {
            choose(i, pickVia)
        }
    }

    fun roll() {
        if (locked) return
        val pool = sides.ifEmpty { state.deck.indices.filter { state.mine[it] != Verdict.SEEN && state.theirs[it] != Verdict.SEEN }.ifEmpty { state.deck.indices.toList() } }
        if (online) {
            link?.chance(pool)
        } else {
            state.pendingChance = pool.random()
        }
    }

    // Chance has spoken (here, or on the other phone): the highlight jumps from one poster to the
    // other, slower and slower, and stops on the winner. With an even number of jumps, it starts on
    // the other one.
    LaunchedEffect(state.pendingChance) {
        val winner = state.pendingChance ?: return@LaunchedEffect
        if (sides.size == 2 && winner in sides) {
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
        chosen = winner
        haptics.perform(HapticCue.CONFIRM)
        delay(650)
        state.win(winner, MatchVia.CHANCE)
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
                Text(
                    if (online && sides.size == 2) stringResource(Res.string.tonight_tapPosterOnline, names.partner) else stringResource(Res.string.tonight_tapPoster),
                    style = NookTheme.type.sans(15, lineHeight = 21), color = TonightColors.Muted,
                )
                Spacer(Modifier.height(20.dp))
                Box {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                        sides.forEach { i ->
                            DuelPoster(
                                card = state.deck[i],
                                mine = state.mine[i], theirs = state.theirs[i], names = names,
                                highlight = when {
                                    chosen == i -> TonightColors.Yes
                                    hot == i -> TonightColors.Text
                                    remote?.myPick == i -> TonightColors.VioletText
                                    else -> null
                                },
                                onClick = { tap(i) },
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
                if (remote != null && !locked) {
                    val status = when {
                        remote.myPick != null && !remote.partnerPicked -> stringResource(Res.string.tonight_waitingPick, names.partner)
                        remote.myPick == null && remote.partnerPicked -> stringResource(Res.string.tonight_partnerPicked, names.partner)
                        else -> null
                    }
                    if (status != null) {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier.align(Alignment.CenterHorizontally).clip(CircleShape).background(TonightColors.Surface).padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Initial(names.partner, TonightColors.Partner, 22.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(status, style = NookTheme.type.sans(14, FontWeight.Medium, 18), color = TonightColors.Text)
                        }
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

/**
 * The way in, in the Social tab and on the movies' search page. From 7 pm, a light sweeps across it
 * now and then: it is the time to use it.
 */
@Composable
fun TonightEntryCard(onClick: () -> Unit, modifier: Modifier = Modifier, shine: Boolean = rememberEvening()) {
    val sweep = if (shine) {
        rememberInfiniteTransition(label = "evening").animateFloat(
            0f, 1f, infiniteRepeatable(tween(1_500, delayMillis = 2_600, easing = FastOutSlowInEasing)), label = "sweep",
        )
    } else null
    Row(
        modifier
            .fillMaxWidth()
            .clip(NookShapes.xl2)
            .background(Brush.linearGradient(listOf(Color(0xFF312E81), TonightColors.Violet, Color(0xFF9D174D))))
            .drawWithContent {
                drawContent()
                val progress = sweep?.value ?: return@drawWithContent
                val band = size.width * 0.35f
                val x = -band + progress * (size.width + band * 2)
                drawRect(
                    Brush.linearGradient(
                        0f to Color.Transparent,
                        0.5f to Color.White.copy(alpha = 0.32f),
                        1f to Color.Transparent,
                        start = Offset(x - band / 2, 0f),
                        end = Offset(x + band / 2, size.height),
                    ),
                )
            }
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

/** From 7 pm until the small hours: the evening, rechecked every minute. */
@Composable
private fun rememberEvening(): Boolean {
    val evening by produceState(isEvening()) {
        while (true) {
            delay(60_000)
            value = isEvening()
        }
    }
    return evening
}

private fun isEvening(): Boolean {
    val hour = kotlin.time.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).hour
    return hour >= 19 || hour < 4
}
