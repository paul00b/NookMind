package fr.paulbr.nookmind.feature.tonight

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.model.MovieStatus
import fr.paulbr.nookmind.core.network.TmdbApi
import fr.paulbr.nookmind.core.platform.openExternalUrl
import fr.paulbr.nookmind.core.platform.rememberDeviceTilt
import fr.paulbr.nookmind.core.ui.HapticCue
import fr.paulbr.nookmind.core.ui.LocalNookHaptics
import fr.paulbr.nookmind.feature.common.TrailerDialog
import fr.paulbr.nookmind.feature.common.watchProviderUrl
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.common_trailerNotFound
import fr.paulbr.nookmind.resources.tonight_addToList
import fr.paulbr.nookmind.resources.tonight_close
import fr.paulbr.nookmind.resources.tonight_inList
import fr.paulbr.nookmind.resources.tonight_match
import fr.paulbr.nookmind.resources.tonight_matchChance
import fr.paulbr.nookmind.resources.tonight_matchExtraTime
import fr.paulbr.nookmind.resources.tonight_matchFinal
import fr.paulbr.nookmind.resources.tonight_matchLove
import fr.paulbr.nookmind.resources.tonight_matchTitle
import fr.paulbr.nookmind.resources.tonight_names
import fr.paulbr.nookmind.resources.tonight_playAgain
import fr.paulbr.nookmind.resources.tonight_ticketAdmit
import fr.paulbr.nookmind.resources.tonight_ticketAudience
import fr.paulbr.nookmind.resources.tonight_ticketHeader
import fr.paulbr.nookmind.resources.tonight_ticketLength
import fr.paulbr.nookmind.resources.tonight_ticketOn
import fr.paulbr.nookmind.resources.tonight_ticketSeat
import fr.paulbr.nookmind.resources.tonight_ticketShowtime
import fr.paulbr.nookmind.resources.tonight_trailer
import fr.paulbr.nookmind.resources.tonight_watchOn
import fr.paulbr.nookmind.resources.tonight_whereToWatch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random
import kotlin.time.Clock

private val MatchBackground = Color(0xFF0B0B11)
private val TicketShape = RoundedCornerShape(16.dp)
private val TicketHeight = 228.dp

@Composable
internal fun MatchStep(
    container: AppContainer,
    state: TonightState,
    names: TonightNames,
    animate: Boolean,
    onPlayAgain: () -> Unit,
    onClose: () -> Unit,
) {
    val card = state.winningCard ?: return
    val haptics = LocalNookHaptics.current
    val scope = rememberCoroutineScope()
    val library by container.movies.items.collectAsState()
    val inList = library.any { it.tmdbId == card.tmdbId }
    var trailerKey by remember { mutableStateOf<String?>(null) }
    var loadingTrailer by remember { mutableStateOf(false) }
    val trailerMissing = stringResource(Res.string.common_trailerNotFound)

    // One clock for the whole entrance: lights down, beam, avatars, title, ticket, buttons.
    val clock = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!animate) return@LaunchedEffect
        launch { clock.animateTo(1f, tween(2_300, easing = LinearEasing)) }
        delay(450)
        haptics.perform(HapticCue.CONFIRM)
        delay(160)
        haptics.perform(HapticCue.CONFIRM)
        delay(900)
        haptics.perform(HapticCue.TICK)
    }
    fun phase(from: Float, to: Float) = ((clock.value - from) / (to - from)).coerceIn(0f, 1f)

    Box(Modifier.fillMaxSize().background(MatchBackground)) {
        // The projector beam, from the top of the screen.
        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = FastOutSlowInEasing.transform(phase(0.2f, 0.75f)) }) {
            val w = size.width
            val beam = Path().apply {
                moveTo(w / 2f - 20f, 0f)
                lineTo(w / 2f + 20f, 0f)
                lineTo(w * 1.15f, size.height * 0.8f)
                lineTo(-w * 0.15f, size.height * 0.8f)
                close()
            }
            drawPath(beam, Brush.verticalGradient(listOf(TonightColors.VioletLight.copy(alpha = 0.22f), Color.Transparent), endY = size.height * 0.8f))
        }
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), horizontalArrangement = Arrangement.End) {
                RoundIconButton(LucideIcons.X, stringResource(Res.string.tonight_close), onClose)
            }
            // The two players meet.
            val meet = backOut(phase(0.18f, 0.42f))
            Row(Modifier.height(58.dp)) {
                Box(Modifier.graphicsLayer { translationX = (1f - meet) * -90.dp.toPx(); alpha = phase(0.18f, 0.3f) }) {
                    Initial(names.me, TonightColors.Me, 58.dp)
                }
                Box(Modifier.offset(x = (-12).dp).graphicsLayer { translationX = (1f - meet) * 90.dp.toPx(); alpha = phase(0.18f, 0.3f) }) {
                    Initial(names.partner, TonightColors.Partner, 58.dp)
                }
            }
            Column(
                Modifier.padding(horizontal = 24.dp).padding(top = 14.dp).graphicsLayer {
                    val p = phase(0.4f, 0.58f)
                    alpha = p
                    translationY = (1f - p) * 12.dp.toPx()
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val kicker = when (state.via) {
                    MatchVia.MATCH -> Res.string.tonight_match
                    MatchVia.SHARED_LOVE -> Res.string.tonight_matchLove
                    MatchVia.FINAL -> Res.string.tonight_matchFinal
                    MatchVia.EXTRA_TIME -> Res.string.tonight_matchExtraTime
                    MatchVia.CHANCE -> Res.string.tonight_matchChance
                }
                Text(stringResource(kicker).uppercase(), style = NookTheme.type.sans(12, FontWeight.Bold, 16).copy(letterSpacing = 1.7.sp), color = TonightColors.VioletText)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(Res.string.tonight_matchTitle, card.title),
                    style = NookTheme.type.serif(30, lineHeight = 35), color = TonightColors.Text, textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(26.dp))
            Ticket(
                card = card,
                names = stringResource(Res.string.tonight_names, names.me, names.partner),
                showtime = showtime(),
                enter = { phase(0.52f, 0.95f) },
                modifier = Modifier.padding(horizontal = 22.dp),
            )
            Spacer(Modifier.weight(1f))
            Column(
                Modifier.padding(horizontal = 18.dp, vertical = 16.dp).graphicsLayer {
                    val p = phase(0.85f, 1f)
                    alpha = p
                    translationY = (1f - p) * 12.dp.toPx()
                },
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val provider = card.provider
                val watchLink = card.providersLink
                if (provider != null) {
                    BigButton(stringResource(Res.string.tonight_watchOn, providerLabel(provider)), light = true, icon = LucideIcons.Play, onClick = {
                        scope.launch {
                            val deepLinks = watchLink?.let { container.tmdb.fetchWatchProviderDeepLinks(it) }.orEmpty()
                            watchProviderUrl(provider, card.title, watchLink, deepLinks)?.let(::openExternalUrl)
                        }
                    })
                } else if (watchLink != null) {
                    BigButton(stringResource(Res.string.tonight_whereToWatch), light = true, onClick = { openExternalUrl(watchLink) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostAction(
                        if (inList) stringResource(Res.string.tonight_inList) else stringResource(Res.string.tonight_addToList),
                        if (inList) LucideIcons.Check else LucideIcons.Bookmark,
                        enabled = !inList,
                        modifier = Modifier.weight(1f),
                    ) {
                        scope.launch {
                            container.movies.add(
                                TmdbApi.extractMovieData(card.movie).copy(status = MovieStatus.WANT_TO_WATCH, rating = null, personalNote = null, watchedDate = null),
                            )
                        }
                    }
                    GhostAction(stringResource(Res.string.tonight_trailer), LucideIcons.Film, enabled = !loadingTrailer, modifier = Modifier.weight(1f)) {
                        loadingTrailer = true
                        scope.launch {
                            val key = container.tmdb.fetchTrailerKey("movie", card.tmdbId)
                            loadingTrailer = false
                            if (key != null) trailerKey = key else container.toasts.error(trailerMissing)
                        }
                    }
                    Box(
                        Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, TonightColors.Line, RoundedCornerShape(14.dp)).clickable(onClick = onPlayAgain),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(LucideIcons.Repeat, stringResource(Res.string.tonight_playAgain), Modifier.size(19.dp), tint = TonightColors.Text)
                    }
                }
            }
        }
        // Lights down: the screen starts black and the room appears.
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = 0.92f * (1f - FastOutSlowInEasing.transform(phase(0f, 0.45f))) }.background(Color.Black))
    }
    trailerKey?.let { TrailerDialog(it, onClose = { trailerKey = null }) }
}

@Composable
private fun GhostAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, TonightColors.Line, RoundedCornerShape(14.dp))
            .background(if (enabled) Color.Transparent else TonightColors.Violet.copy(alpha = 0.18f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(17.dp), tint = TonightColors.Text)
        Spacer(Modifier.width(7.dp))
        Text(text, style = NookTheme.type.sans(14, FontWeight.Medium, 18), color = TonightColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * The ticket of the evening. It follows the finger in 3D and swings back like card stock, leans
 * with the phone, catches the light as it turns, and flips over when tapped.
 */
@Composable
private fun Ticket(card: TonightCard, names: String, showtime: String, enter: () -> Float, modifier: Modifier) {
    val density = LocalDensity.current.density
    val haptics = LocalNookHaptics.current
    val scope = rememberCoroutineScope()
    val tilt by rememberDeviceTilt()
    val drag = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var flipped by remember { mutableStateOf(false) }
    val flip by animateFloatAsState(if (flipped) 180f else 0f, spring(dampingRatio = 0.62f, stiffness = 140f), label = "ticket-flip")
    val maxDrag = 130.dp.value * density
    fun turnY() = (drag.value.x / density * 0.32f - tilt.x * 18f).coerceIn(-38f, 38f)
    fun turnX() = (-drag.value.y / density * 0.28f + tilt.y * 12f).coerceIn(-32f, 32f)

    Box(
        modifier
            .fillMaxWidth()
            .height(TicketHeight)
            .graphicsLayer {
                val e = enter()
                val settle = backOut(e)
                alpha = (e * 2.5f).coerceIn(0f, 1f)
                translationY = (1f - settle) * -70.dp.toPx()
                rotationZ = -3f - (1f - settle) * 8f
                rotationY = turnY() + flip
                rotationX = turnX()
                cameraDistance = 18f * density
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    flipped = !flipped
                    haptics.perform(HapticCue.TICK)
                })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { scope.launch { drag.animateTo(Offset.Zero, spring(dampingRatio = 0.32f, stiffness = 170f)) } },
                    onDragCancel = { scope.launch { drag.animateTo(Offset.Zero, spring(dampingRatio = 0.32f, stiffness = 170f)) } },
                ) { change, amount ->
                    change.consume()
                    val next = drag.value + amount
                    scope.launch { drag.snapTo(Offset(next.x.coerceIn(-maxDrag, maxDrag), next.y.coerceIn(-maxDrag, maxDrag))) }
                }
            },
    ) {
        val showingBack = flip % 360f in 90f..270f
        if (!showingBack) {
            TicketFront(card, names, showtime, turn = { turnY() + turnX() * 0.4f })
        } else {
            TicketBack(card, names, turn = { -(turnY() + turnX() * 0.4f) }, modifier = Modifier.graphicsLayer { rotationY = 180f })
        }
    }
}

/** Light sweeping across the card as it turns, with a faint holographic edge. */
private fun Modifier.sheen(turn: () -> Float): Modifier = drawWithContent {
    drawContent()
    val w = size.width
    val center = w * (0.5f + turn() / 38f * 0.75f)
    drawRect(
        Brush.linearGradient(
            0f to Color.Transparent,
            0.42f to Color.White.copy(alpha = 0.0f),
            0.5f to Color.White.copy(alpha = 0.26f),
            0.56f to TonightColors.VioletLight.copy(alpha = 0.18f),
            0.62f to Color(0x2222D3EE),
            0.72f to Color.Transparent,
            start = Offset(center - w * 0.7f, 0f),
            end = Offset(center + w * 0.7f, size.height),
        ),
    )
}

/** The two half-circles punched where the stub tears off, in the colour of the room. */
private fun Modifier.notches(atFraction: Float): Modifier = drawWithContent {
    drawContent()
    val x = size.width * atFraction
    val r = 11.dp.toPx()
    drawCircle(MatchBackground, r, Offset(x, 0f))
    drawCircle(MatchBackground, r, Offset(x, size.height))
}

@Composable
private fun TicketFront(card: TonightCard, names: String, showtime: String, turn: () -> Float) {
    val stub = 0.34f
    Row(
        Modifier.fillMaxWidth().height(TicketHeight).clip(TicketShape).background(TonightColors.Paper).sheen(turn).notches(stub),
    ) {
        Box(Modifier.fillMaxWidth(stub).fillMaxHeight()) {
            AsyncImage(card.posterUrl, card.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Canvas(Modifier.width(2.dp).fillMaxHeight()) {
            drawLine(
                TonightColors.Ink.copy(alpha = 0.28f), Offset(size.width / 2f, 14.dp.toPx()), Offset(size.width / 2f, size.height - 14.dp.toPx()),
                strokeWidth = size.width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
            )
        }
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 16.dp, top = 18.dp, bottom = 14.dp)) {
            Text(
                stringResource(Res.string.tonight_ticketHeader).uppercase(),
                style = NookTheme.type.sans(9, FontWeight.Bold, 12).copy(letterSpacing = 1.4.sp), color = TonightColors.InkMuted, maxLines = 1,
            )
            Text(
                card.title, style = NookTheme.type.serif(24, lineHeight = 26), color = TonightColors.Ink,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp, bottom = 12.dp),
            )
            Row {
                TicketField(stringResource(Res.string.tonight_ticketAudience), names, Modifier.weight(1f))
                TicketField(stringResource(Res.string.tonight_ticketShowtime), showtime, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row {
                TicketField(stringResource(Res.string.tonight_ticketLength), formatRuntime(card.runtime) ?: "–", Modifier.weight(1f))
                TicketField(stringResource(Res.string.tonight_ticketOn), card.provider?.let(::providerLabel) ?: "–", Modifier.weight(1f))
            }
            Barcode(card.tmdbId, Modifier.padding(top = 12.dp).fillMaxWidth().height(24.dp))
        }
    }
}

@Composable
private fun TicketBack(card: TonightCard, names: String, turn: () -> Float, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().height(TicketHeight).clip(TicketShape).background(TonightColors.Paper).sheen(turn).padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(LucideIcons.Popcorn, null, Modifier.size(30.dp), tint = TonightColors.Ink)
        Text(
            stringResource(Res.string.tonight_ticketAdmit), style = NookTheme.type.serif(24, lineHeight = 28), color = TonightColors.Ink,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
        )
        Text(names, style = NookTheme.type.sans(15, FontWeight.SemiBold, 20), color = TonightColors.Ink, modifier = Modifier.padding(top = 4.dp))
        Text(stringResource(Res.string.tonight_ticketSeat), style = NookTheme.type.sans(13, lineHeight = 18), color = TonightColors.InkMuted)
        Barcode(card.tmdbId * 31 + 7, Modifier.padding(top = 14.dp).fillMaxWidth(0.8f).height(30.dp))
    }
}

@Composable
private fun TicketField(label: String, value: String, modifier: Modifier) {
    Column(modifier.padding(end = 6.dp)) {
        Text(label.uppercase(), style = NookTheme.type.sans(9, FontWeight.SemiBold, 12).copy(letterSpacing = 1.2.sp), color = Color(0xFFA8A29E), maxLines = 1)
        Text(value, style = NookTheme.type.sans(13, FontWeight.SemiBold, 17), color = TonightColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** A barcode that is the same for the same movie, for the look of it. */
@Composable
private fun Barcode(seed: Int, modifier: Modifier) {
    Canvas(modifier) {
        val random = Random(seed)
        val unit = 1.dp.toPx()
        var x = 0f
        while (x < size.width) {
            val bar = unit * (1 + random.nextInt(3))
            drawRect(TonightColors.Ink.copy(alpha = 0.88f), Offset(x, 0f), androidx.compose.ui.geometry.Size(bar, size.height))
            x += bar + unit * (1 + random.nextInt(2))
        }
    }
}

/** Tonight's showtime: 21:00 before 6 pm, else the next half hour. */
private fun showtime(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val (hour, minute) = when {
        now.hour < 18 -> 21 to 0
        now.minute < 30 -> now.hour to 30
        else -> (now.hour + 1) % 24 to 0
    }
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

/** Ease-out with a small overshoot, for things that land. */
private fun backOut(t: Float): Float {
    val c = 1.70158f
    val x = t - 1f
    return 1f + (c + 1f) * x * x * x + c * x * x
}
