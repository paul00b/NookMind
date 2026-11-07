package fr.paulbr.nookmind.feature.wrapped

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.data.AuthState
import fr.paulbr.nookmind.core.data.FriendItem
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.Avatar
import fr.paulbr.nookmind.core.designsystem.components.AvatarSize
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.ReviewProfile
import fr.paulbr.nookmind.core.domain.YearReview
import fr.paulbr.nookmind.core.domain.computeTmdbStats
import fr.paulbr.nookmind.core.domain.todayLocal
import fr.paulbr.nookmind.core.domain.yearReview
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.SeriesStatus
import fr.paulbr.nookmind.core.platform.shareText
import fr.paulbr.nookmind.core.platform.shareImage
import fr.paulbr.nookmind.core.platform.encodePng
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.draw.drawWithContent
import fr.paulbr.nookmind.feature.social.Affinity
import fr.paulbr.nookmind.feature.social.affinities
import fr.paulbr.nookmind.feature.social.myItems
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.common_defaultDisplayName
import fr.paulbr.nookmind.resources.wrapped_books
import fr.paulbr.nookmind.resources.wrapped_close
import fr.paulbr.nookmind.resources.wrapped_days
import fr.paulbr.nookmind.resources.wrapped_empty
import fr.paulbr.nookmind.resources.wrapped_episodes
import fr.paulbr.nookmind.resources.wrapped_episodesSub
import fr.paulbr.nookmind.resources.wrapped_favBook
import fr.paulbr.nookmind.resources.wrapped_favMovie
import fr.paulbr.nookmind.resources.wrapped_favSeries
import fr.paulbr.nookmind.resources.wrapped_favs
import fr.paulbr.nookmind.resources.wrapped_header
import fr.paulbr.nookmind.resources.wrapped_hours
import fr.paulbr.nookmind.resources.wrapped_introSub
import fr.paulbr.nookmind.resources.wrapped_moreThanLast
import fr.paulbr.nookmind.resources.wrapped_movies
import fr.paulbr.nookmind.resources.wrapped_next
import fr.paulbr.nookmind.resources.wrapped_numbers
import fr.paulbr.nookmind.resources.wrapped_pages
import fr.paulbr.nookmind.resources.wrapped_profileAllRounder
import fr.paulbr.nookmind.resources.wrapped_profileBookworm
import fr.paulbr.nookmind.resources.wrapped_profileFresh
import fr.paulbr.nookmind.resources.wrapped_profileMovieBuff
import fr.paulbr.nookmind.resources.wrapped_profileSeries
import fr.paulbr.nookmind.resources.wrapped_screen
import fr.paulbr.nookmind.resources.wrapped_see
import fr.paulbr.nookmind.resources.wrapped_series
import fr.paulbr.nookmind.resources.wrapped_seriesSub
import fr.paulbr.nookmind.resources.wrapped_share
import fr.paulbr.nookmind.resources.wrapped_shareText
import fr.paulbr.nookmind.resources.wrapped_shareTitle
import fr.paulbr.nookmind.resources.wrapped_soulmate
import fr.paulbr.nookmind.resources.wrapped_soulmateLine
import fr.paulbr.nookmind.resources.wrapped_soulmatePct
import fr.paulbr.nookmind.resources.wrapped_soulmateSub
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private object WrappedColors {
    val Background = Color(0xFF1D2130)
    val Violet = Color(0xFF4C1D95)
    val VioletDeep = Color(0xFF2E1065)
    val Lilac = Color(0xFFDDD6FE)
    val Text = Color(0xFFF3F4F6)
    val Muted = Color(0xFF9CA3AF)
    val Card = Color(0x10FFFFFF)
    val Line = Color(0x1AFFFFFF)
    val Movies = Color(0xFFA5B4FC)
    val Series = Color(0xFF5EEAD4)
    val Books = Color(0xFFFCD34D)
    val Episodes = Color(0xFFF9A8D4)
}

private enum class WrappedPage { INTRO, NUMBERS, FAVOURITES, SOULMATE, SHARE }

/** A friend I match best, the titles we both loved, and who comes next. */
private data class Soulmate(val name: String, val avatarUrl: String?, val percent: Int, val loved: List<FriendItem>, val others: List<Pair<String, Int>>)

/**
 * "Ton année": a few full-screen stories of the year in the library, ending on a card to share.
 * A tap on the right goes on, on the left goes back; the system back closes.
 */
@Composable
fun WrappedScreen(container: AppContainer, onClose: () -> Unit, year: Int = todayLocal().year, startPage: Int = 0) {
    val movies by container.movies.items.collectAsState()
    val series by container.series.items.collectAsState()
    val books by container.books.items.collectAsState()
    val friends by container.social.friends.collectAsState()
    val library by container.social.library.collectAsState()
    val authState by container.auth.state.collectAsState()
    val me = (authState as? AuthState.SignedIn)?.user
    val myName = me?.displayName(stringResource(Res.string.common_defaultDisplayName)).orEmpty()

    val review = remember(year, movies, series, books) { yearReview(year, movies, series, books) }
    var seriesMinutes by remember { mutableIntStateOf(0) }
    var episodes by remember { mutableIntStateOf(0) }
    LaunchedEffect(review.series) {
        val stats = runCatching { computeTmdbStats(review.series, container.tmdb) }.getOrNull() ?: return@LaunchedEffect
        seriesMinutes = stats.minutes
        episodes = stats.episodes
    }

    val soulmate = remember(friends, library, movies, series, books) {
        val accepted = friends.filter { it.accepted }
        val ranked = affinities(library, accepted.map { it.userId }, myItems(movies, series, books)).filter { it.percent != null }
        ranked.firstOrNull()?.let { best -> soulmateOf(best, ranked.drop(1).take(3), accepted.associate { it.userId to it.firstName }, accepted.firstOrNull { it.userId == best.friendId }?.avatarUrl) }
    }

    val pages = remember(review, soulmate) {
        buildList {
            add(WrappedPage.INTRO)
            add(WrappedPage.NUMBERS)
            if (review.topMovie != null || review.topSeries != null || review.topBook != null) add(WrappedPage.FAVOURITES)
            if (soulmate != null) add(WrappedPage.SOULMATE)
            add(WrappedPage.SHARE)
        }
    }
    var index by remember { mutableIntStateOf(startPage.coerceIn(0, 4)) }
    val current = pages[index.coerceIn(0, pages.lastIndex)]
    fun next() { if (index < pages.lastIndex) index++ }
    fun previous() { if (index > 0) index-- }

    BackHandler(enabled = true, onBack = onClose)

    Box(
        Modifier
            .fillMaxSize()
            .background(WrappedColors.Background)
            .pointerInput(pages.size) {
                detectTapGestures { offset -> if (offset.x < size.width / 3f) previous() else next() }
            },
    ) {
        AnimatedContent(current, transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(200)) }, label = "wrapped") { page ->
            Box(Modifier.fillMaxSize()) {
                when (page) {
                    WrappedPage.INTRO -> IntroPage(review, onSee = ::next)
                    WrappedPage.NUMBERS -> NumbersPage(review, (review.movieMinutes + seriesMinutes) / 60, episodes)
                    WrappedPage.FAVOURITES -> FavouritesPage(review)
                    WrappedPage.SOULMATE -> soulmate?.let { SoulmatePage(myName, me?.avatarUrl, it) }
                    WrappedPage.SHARE -> SharePage(review, soulmate)
                }
            }
        }
        StoryHeader(year, pages.size, index, onClose, Modifier.windowInsetsPadding(WindowInsets.safeDrawing))
    }
}

private fun soulmateOf(best: Affinity, others: List<Affinity>, names: Map<String, String>, avatarUrl: String?): Soulmate {
    // What we both loved, from their side; the best rated first, so the examples say something.
    val loved = best.common.sortedByDescending { it.rating ?: 0.0 }.take(3)
    return Soulmate(
        name = names[best.friendId].orEmpty(),
        avatarUrl = avatarUrl,
        percent = best.percent ?: 0,
        loved = loved,
        others = others.mapNotNull { a -> a.percent?.let { names[a.friendId].orEmpty() to it } },
    )
}

@Composable
private fun StoryHeader(year: Int, count: Int, index: Int, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(count) { i ->
                Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (i <= index) Color.White else Color.White.copy(alpha = 0.28f)))
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.wrapped_header, year), style = NookTheme.type.sans(13, FontWeight.SemiBold), color = Color.White.copy(alpha = 0.75f), modifier = Modifier.weight(1f))
            Box(Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Icon(LucideIcons.X, stringResource(Res.string.wrapped_close), Modifier.size(20.dp), tint = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

// ── 1. The wall of the year ─────────────────────────────────────────────────

@Composable
private fun IntroPage(review: YearReview, onSee: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(WrappedColors.Violet, WrappedColors.Background), center = Offset(0f, 0f), radius = 1400f))) {
        if (review.posters.isNotEmpty()) PosterWall(review.posters)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to WrappedColors.Background.copy(alpha = 0.5f),
                    0.25f to WrappedColors.Background.copy(alpha = 0.1f),
                    0.45f to WrappedColors.Background.copy(alpha = 0.35f),
                    0.6f to WrappedColors.Background.copy(alpha = 0.92f),
                    0.7f to WrappedColors.Background,
                ),
            ),
        )
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 28.dp, end = 28.dp, bottom = 48.dp)) {
            Text(review.year.toString(), style = NookTheme.type.serif(96, FontWeight.ExtraBold, 92), color = WrappedColors.Text)
            Spacer(Modifier.height(10.dp))
            Text(
                if (review.isEmpty) stringResource(Res.string.wrapped_empty) else stringResource(Res.string.wrapped_introSub),
                style = NookTheme.type.sans(19, FontWeight.SemiBold, 26),
                color = WrappedColors.Lilac,
            )
            Spacer(Modifier.height(72.dp))
            PillButton(stringResource(Res.string.wrapped_see), onSee)
        }
    }
}

/** Every poster of the year in tilted columns that drift up and down, a column each way. */
@Composable
private fun PosterWall(posters: List<String>) {
    val columns = 8
    val perColumn = 8
    val itemWidth = 66.dp
    val itemHeight = 99.dp
    val gap = 8.dp
    val loop = (itemHeight + gap) * perColumn
    val loopPx = with(LocalDensity.current) { loop.toPx() }
    val transition = rememberInfiniteTransition(label = "wall")
    val progress by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(42_000, easing = LinearEasing), RepeatMode.Restart), label = "drift")
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Row(
            Modifier.requiredSize(width = maxWidth * 1.7f, height = maxHeight * 1.4f).offset(y = (-120).dp).rotate(-14f),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            repeat(columns) { c ->
                val column = List(perColumn) { r -> posters[(c * perColumn + r * 3 + c) % posters.size] }
                val speed = 1f + (c % 3) * 0.25f
                val shift = ((progress * speed + c * 0.17f) % 1f) * loopPx
                Column(
                    Modifier.width(itemWidth).graphicsLayer { translationY = if (c % 2 == 0) -shift else shift - loopPx },
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    (column + column).forEach { url ->
                        MediaImage(url, null, Modifier.width(itemWidth).height(itemHeight), mode = MediaMode.MOVIES, shape = RoundedCornerShape(8.dp), placeholderIconSize = 16.dp)
                    }
                }
            }
        }
    }
}

// ── 2. Numbers ──────────────────────────────────────────────────────────────

@Composable
private fun NumbersPage(review: YearReview, hours: Int, episodes: Int) {
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF172033), WrappedColors.Background))).windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 24.dp, end = 24.dp, top = 84.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Title(stringResource(Res.string.wrapped_numbers))
        Spacer(Modifier.height(10.dp))
        val moreThanLast = review.movies.size - review.moviesLastYear
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(review.movies.size.toString(), stringResource(Res.string.wrapped_movies), if (review.moviesLastYear > 0 && moreThanLast > 0) stringResource(Res.string.wrapped_moreThanLast, moreThanLast, review.year - 1) else null, WrappedColors.Movies, Modifier.weight(1f))
            val finished = review.series.count { it.status == SeriesStatus.WATCHED }
            StatTile(review.series.size.toString(), stringResource(Res.string.wrapped_series), if (finished > 0) stringResource(Res.string.wrapped_seriesSub, finished) else null, WrappedColors.Series, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(review.books.size.toString(), stringResource(Res.string.wrapped_books), if (review.pages > 0) stringResource(Res.string.wrapped_pages, groupThousands(review.pages)) else null, WrappedColors.Books, Modifier.weight(1f))
            StatTile(if (episodes > 0) episodes.toString() else "…", stringResource(Res.string.wrapped_episodes), if (review.series.isNotEmpty()) stringResource(Res.string.wrapped_episodesSub, review.series.size) else null, WrappedColors.Episodes, Modifier.weight(1f))
        }
        if (hours > 0) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(0x598B5CF6), Color(0x40EC4899))))
                    .border(1.dp, WrappedColors.Line, RoundedCornerShape(22.dp)).padding(20.dp),
            ) {
                Text(stringResource(Res.string.wrapped_screen), style = NookTheme.type.sans(13, FontWeight.SemiBold), color = WrappedColors.Lilac)
                Text(stringResource(Res.string.wrapped_hours, hours), style = NookTheme.type.serif(40, FontWeight.ExtraBold, 46), color = WrappedColors.Text)
                if (hours >= 24) Text(stringResource(Res.string.wrapped_days, hours / 24), style = NookTheme.type.sans(14), color = WrappedColors.Text)
            }
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, sub: String?, color: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(WrappedColors.Card).border(1.dp, WrappedColors.Line, RoundedCornerShape(20.dp)).padding(horizontal = 18.dp, vertical = 16.dp)) {
        Text(value, style = NookTheme.type.serif(44, FontWeight.ExtraBold, 46), color = color)
        Text(label, style = NookTheme.type.sans(14, FontWeight.SemiBold), color = WrappedColors.Text)
        if (sub != null) Text(sub, style = NookTheme.type.sans(12), color = WrappedColors.Muted)
    }
}

// ── 3. Favourites ───────────────────────────────────────────────────────────

@Composable
private fun FavouritesPage(review: YearReview) {
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0xFF2A1F14), 0.75f to WrappedColors.Background)).windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 24.dp, end = 24.dp, top = 84.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Title(stringResource(Res.string.wrapped_favs))
        Spacer(Modifier.height(10.dp))
        review.topMovie?.let { FavouriteRow(it.posterUrl, stringResource(Res.string.wrapped_favMovie), it.title, it.rating, WrappedColors.Movies, MediaMode.MOVIES) }
        review.topSeries?.let { FavouriteRow(it.posterUrl, stringResource(Res.string.wrapped_favSeries), it.title, it.rating, WrappedColors.Series, MediaMode.SERIES) }
        review.topBook?.let { FavouriteRow(it.coverUrl, stringResource(Res.string.wrapped_favBook), it.title, it.rating, WrappedColors.Books, MediaMode.BOOKS) }
    }
}

@Composable
private fun FavouriteRow(image: String?, label: String, title: String, rating: Double?, color: Color, mode: MediaMode) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0x0DFFFFFF)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MediaImage(image, title, Modifier.width(72.dp), mode = mode, shape = RoundedCornerShape(12.dp), placeholderIconSize = 22.dp)
        Column(Modifier.weight(1f)) {
            Text(label.uppercase(), style = NookTheme.type.sans(12, FontWeight.Bold), color = color)
            Text(title, style = NookTheme.type.serif(20, FontWeight.Bold, 24), color = WrappedColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (rating != null && rating > 0) Text("★".repeat(rating.toInt().coerceIn(1, 5)), style = NookTheme.type.sans(14), color = WrappedColors.Books)
        }
    }
}

// ── 4. Screen soulmate ──────────────────────────────────────────────────────

@Composable
private fun SoulmatePage(myName: String, myAvatar: String?, soulmate: Soulmate) {
    Column(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF6D28D9), WrappedColors.Background), radius = 1300f, center = Offset(540f, 500f))).windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 24.dp, end = 24.dp, top = 84.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.wrapped_soulmate).uppercase(), style = NookTheme.type.sans(13, FontWeight.Bold), color = WrappedColors.Lilac)
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(myName, myAvatar, AvatarSize.XXL)
            Spacer(Modifier.width(4.dp))
            Avatar(soulmate.name, soulmate.avatarUrl, AvatarSize.XXL)
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.wrapped_soulmatePct, soulmate.name, soulmate.percent), style = NookTheme.type.serif(38, FontWeight.ExtraBold, 42), color = WrappedColors.Text)
        Text(stringResource(Res.string.wrapped_soulmateSub), style = NookTheme.type.sans(15), color = WrappedColors.Text.copy(alpha = 0.9f))
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            soulmate.loved.forEachIndexed { i, item ->
                MediaImage(
                    item.imageUrl, item.title,
                    Modifier.width(74.dp).rotate(listOf(-5f, 0f, 5f)[i % 3]),
                    mode = if (item.kind == "book") MediaMode.BOOKS else if (item.kind == "series") MediaMode.SERIES else MediaMode.MOVIES,
                    shape = RoundedCornerShape(10.dp), placeholderIconSize = 18.dp,
                )
            }
        }
        if (soulmate.others.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0x40000000)).border(1.dp, WrappedColors.Line, RoundedCornerShape(22.dp)).padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(Res.string.wrapped_next).uppercase(), style = NookTheme.type.sans(12, FontWeight.Bold), color = WrappedColors.Lilac)
                soulmate.others.forEach { (name, pct) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(name, null, AvatarSize.SM)
                        Text(name, style = NookTheme.type.sans(15, FontWeight.SemiBold), color = WrappedColors.Text, modifier = Modifier.weight(1f))
                        Box(Modifier.width(90.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x1FFFFFFF))) {
                            Box(Modifier.fillMaxWidth(pct / 100f).height(6.dp).background(WrappedColors.Lilac))
                        }
                        Text("$pct %", style = NookTheme.type.sans(14, FontWeight.Bold), color = WrappedColors.Lilac)
                    }
                }
            }
        }
    }
}

// ── 5. The card to share ────────────────────────────────────────────────────

@Composable
private fun SharePage(review: YearReview, soulmate: Soulmate?) {
    val shareMessage = stringResource(Res.string.wrapped_shareText, review.year, review.movies.size, review.series.size, review.books.size)
    val covers = listOfNotNull(review.topMovie?.posterUrl, review.topSeries?.posterUrl, review.topBook?.coverUrl).ifEmpty { review.posters.take(3) }
    // The card is recorded as it draws, so the picture shared is the one on screen, posters included.
    val card = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val shareCaption = "$shareMessage\nhttps://nookmind.paulbr.fr"
    Box(Modifier.fillMaxSize().background(Color(0xFF111318))) {
        Confetti()
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 30.dp, end = 30.dp, top = 92.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.weight(1f, fill = false).fillMaxWidth()
                    .drawWithContent {
                        card.record { this@drawWithContent.drawContent() }
                        drawLayer(card)
                    }
                    .background(Color(0xFF111318)).padding(14.dp),
            ) {
                Column(
                    Modifier.fillMaxWidth().rotate(-2f).clip(RoundedCornerShape(28.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF6D28D9), WrappedColors.VioletDeep, WrappedColors.Background), start = Offset.Zero, end = Offset(600f, 1400f)))
                        .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(28.dp)).padding(24.dp),
                ) {
                    Text(stringResource(Res.string.wrapped_shareTitle, review.year).uppercase(), style = NookTheme.type.sans(12, FontWeight.Bold), color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(profileTitle(review.profile)), style = NookTheme.type.serif(30, FontWeight.ExtraBold, 32), color = WrappedColors.Text)
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                        covers.forEachIndexed { i, url ->
                            val rot = if (covers.size == 3) listOf(-10f, 0f, 10f)[i] else 0f
                            MediaImage(url, null, Modifier.width(88.dp).padding(horizontal = 2.dp).offset(y = if (i == 1) 0.dp else 8.dp).rotate(rot), shape = RoundedCornerShape(12.dp), placeholderIconSize = 18.dp)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth()) {
                        ShareStat(review.movies.size, stringResource(Res.string.wrapped_movies), WrappedColors.Movies, Modifier.weight(1f))
                        ShareStat(review.series.size, stringResource(Res.string.wrapped_series), WrappedColors.Series, Modifier.weight(1f))
                        ShareStat(review.books.size, stringResource(Res.string.wrapped_books), WrappedColors.Books, Modifier.weight(1f))
                    }
                    if (soulmate != null) {
                        Spacer(Modifier.height(18.dp))
                        Text(
                            stringResource(Res.string.wrapped_soulmateLine, soulmate.name, soulmate.percent),
                            style = NookTheme.type.sans(14, FontWeight.SemiBold),
                            color = WrappedColors.Text,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x14FFFFFF)).padding(horizontal = 14.dp, vertical = 12.dp),
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Text("NookMind", style = NookTheme.type.serif(16, FontWeight.Bold), color = WrappedColors.Text, modifier = Modifier.weight(1f))
                        Text("nookmind.paulbr.fr", style = NookTheme.type.sans(11), color = Color.White.copy(alpha = 0.55f))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            PillButton(stringResource(Res.string.wrapped_share)) {
                scope.launch {
                    val png = runCatching { card.toImageBitmap().encodePng() }.getOrNull()
                    if (png != null) shareImage(png, shareCaption) else shareText(shareCaption)
                }
            }
        }
    }
}

@Composable
private fun ShareStat(value: Int, label: String, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = NookTheme.type.serif(34, FontWeight.ExtraBold, 36), color = color)
        Text(label, style = NookTheme.type.sans(12), color = WrappedColors.Lilac)
    }
}

/** Little paper bits floating up and down around the card. */
@Composable
private fun Confetti() {
    val colors = listOf(WrappedColors.Books, WrappedColors.Episodes, WrappedColors.Movies, WrappedColors.Series, Color.White)
    val bits = remember { List(26) { i -> Triple((i * 37 % 100) / 100f, (i * 53 % 100) / 100f, colors[i % colors.size]) } }
    val transition = rememberInfiniteTransition(label = "confetti")
    val lift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "lift")
    BoxWithConstraints(Modifier.fillMaxSize()) {
        bits.forEachIndexed { i, (x, y, color) ->
            Box(
                Modifier
                    .offset(x = maxWidth * x, y = maxHeight * (0.08f + y * 0.82f))
                    .graphicsLayer { translationY = -lift * (8 + i % 4 * 4).dp.toPx(); rotationZ = (i * 47 % 180).toFloat() }
                    .size(width = if (i % 3 == 0) 8.dp else 5.dp, height = if (i % 2 == 0) 12.dp else 5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color.copy(alpha = 0.85f)),
            )
        }
    }
}

private fun profileTitle(profile: ReviewProfile): StringResource = when (profile) {
    ReviewProfile.BOOKWORM -> Res.string.wrapped_profileBookworm
    ReviewProfile.SERIES_NIGHTS -> Res.string.wrapped_profileSeries
    ReviewProfile.MOVIE_BUFF -> Res.string.wrapped_profileMovieBuff
    ReviewProfile.ALL_ROUNDER -> Res.string.wrapped_profileAllRounder
    ReviewProfile.FRESH_START -> Res.string.wrapped_profileFresh
}

// ── Shared pieces ───────────────────────────────────────────────────────────

@Composable
private fun Title(text: String) {
    Text(text, style = NookTheme.type.serif(34, FontWeight.ExtraBold, 38), color = WrappedColors.Text)
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(Color.White).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = NookTheme.type.sans(16, FontWeight.Bold), color = WrappedColors.Background)
    }
}

/** 5840 -> "5 840", with the narrow no-break space French uses. */
internal fun groupThousands(n: Int): String = n.toString().reversed().chunked(3).joinToString(" ").reversed()
