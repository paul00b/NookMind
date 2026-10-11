package fr.paulbr.nookmind.feature.social

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.data.AddFriendResult
import fr.paulbr.nookmind.core.data.FriendItem
import fr.paulbr.nookmind.core.data.SocialStatus
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.BannerTone
import fr.paulbr.nookmind.core.designsystem.components.HairlineDivider
import fr.paulbr.nookmind.core.designsystem.components.InlineBanner
import fr.paulbr.nookmind.core.designsystem.components.Spinner
import fr.paulbr.nookmind.core.designsystem.components.TextLink
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.BookStatus
import fr.paulbr.nookmind.core.model.Movie
import fr.paulbr.nookmind.core.model.MovieStatus
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesStatus
import fr.paulbr.nookmind.core.network.TmdbApi
import fr.paulbr.nookmind.core.platform.shareText
import fr.paulbr.nookmind.feature.common.LocalWideLayout
import fr.paulbr.nookmind.feature.tonight.TonightEntryCard
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.common_retry
import fr.paulbr.nookmind.resources.social_codeCopied
import fr.paulbr.nookmind.resources.social_emptyText
import fr.paulbr.nookmind.resources.social_emptyTitle
import fr.paulbr.nookmind.resources.social_error
import fr.paulbr.nookmind.resources.social_friends
import fr.paulbr.nookmind.resources.social_friendAccepted
import fr.paulbr.nookmind.resources.social_friendAlready
import fr.paulbr.nookmind.resources.social_friendFailed
import fr.paulbr.nookmind.resources.social_friendNotFound
import fr.paulbr.nookmind.resources.social_friendRequested
import fr.paulbr.nookmind.resources.social_friendSelf
import fr.paulbr.nookmind.resources.social_now
import fr.paulbr.nookmind.resources.social_notSetUp
import fr.paulbr.nookmind.resources.social_reactError
import fr.paulbr.nookmind.resources.social_reactions
import fr.paulbr.nookmind.resources.social_requests
import fr.paulbr.nookmind.resources.social_salon
import fr.paulbr.nookmind.resources.social_shareText
import fr.paulbr.nookmind.resources.social_sharing
import fr.paulbr.nookmind.resources.social_sharingNote
import fr.paulbr.nookmind.resources.social_subtitle
import fr.paulbr.nookmind.resources.social_title
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import fr.paulbr.nookmind.core.network.GoogleBooksApi
import fr.paulbr.nookmind.feature.movies.AddMovieSheet
import fr.paulbr.nookmind.feature.movies.MovieDetailSheet
import fr.paulbr.nookmind.feature.series.AddSeriesSheet
import fr.paulbr.nookmind.feature.series.SeriesDetailSheet
import fr.paulbr.nookmind.feature.books.AddBookSheet
import fr.paulbr.nookmind.feature.books.BookDetailSheet

/**
 * The Social tab, the same in every mode: invitations to Ce soir ?, what friends are watching and
 * reading, the salon of what they added or finished (with reactions and a way to add it to mine),
 * the friends themselves and what I share with them.
 */
@Composable
fun SocialScreen(container: AppContainer, contentPadding: PaddingValues, onOpenTonight: (String?) -> Unit) {
    val colors = NookTheme.colors
    val wide = LocalWideLayout.current
    val social = container.social
    val status by social.status.collectAsState()
    val profile by social.profile.collectAsState()
    val friends by social.friends.collectAsState()
    val library by social.library.collectAsState()
    val received by social.received.collectAsState()
    val sent by social.sent.collectAsState()
    val invitations by social.invitations.collectAsState()
    val movies by container.movies.items.collectAsState()
    val series by container.series.items.collectAsState()
    val books by container.books.items.collectAsState()
    val scope = rememberCoroutineScope()
    @Suppress("DEPRECATION") val clipboard = LocalClipboardManager.current

    // Fresh on every visit; the invitations keep coming while the tab is open.
    LaunchedEffect(Unit) {
        social.refresh()
        while (true) {
            delay(15_000)
            social.refreshInvitations()
        }
    }

    val accepted = friends.filter { it.accepted }
    val incoming = friends.filter { !it.accepted && it.incoming }
    val outgoing = friends.filter { !it.accepted && !it.incoming }
    val byId = friends.associateBy { it.userId }
    val mine = ownedKeys(movies, series, books)
    val now = nowPlaying(library)
    val salon = salonFeed(library)

    // A friend's movie, series or book opens like a search result (to add it on my side), or as
    // my own sheet when I already have it.
    var opening by remember { mutableStateOf<String?>(null) }
    var moviePrefill by remember { mutableStateOf<Movie?>(null) }
    var seriesPrefill by remember { mutableStateOf<Series?>(null) }
    var bookPrefill by remember { mutableStateOf<Book?>(null) }
    var movieSelected by remember { mutableStateOf<Movie?>(null) }
    var seriesSelected by remember { mutableStateOf<Series?>(null) }
    var bookSelected by remember { mutableStateOf<Book?>(null) }

    fun open(kind: String, key: String, title: String, subtitle: String?, imageUrl: String?) {
        if (opening != null) return
        when (kind) {
            "movie" -> movies.firstOrNull { it.tmdbId?.toString() == key }?.let { movieSelected = it; return }
            "series" -> series.firstOrNull { it.tmdbId?.toString() == key }?.let { seriesSelected = it; return }
            else -> books.firstOrNull { it.googleBooksId == key }?.let { bookSelected = it; return }
        }
        opening = "$kind:$key"
        scope.launch {
            when (kind) {
                "movie" -> {
                    val id = key.toIntOrNull()
                    moviePrefill = id?.let { container.tmdb.fetchMovieDetails(it) }?.let { TmdbApi.extractMovieData(it) }
                        ?: Movie(tmdbId = id, title = title, director = subtitle.orEmpty(), posterUrl = imageUrl)
                }
                "series" -> {
                    val id = key.toIntOrNull()
                    seriesPrefill = id?.let { container.tmdb.fetchSeriesDetails(it) }?.let { TmdbApi.extractSeriesData(it) }
                        ?: Series(tmdbId = id, title = title, creator = subtitle.orEmpty(), posterUrl = imageUrl)
                }
                else -> bookPrefill = container.googleBooks.fetchVolume(key)?.let { GoogleBooksApi.extractBookData(it) }
                    ?: Book(googleBooksId = key, title = title, author = subtitle.orEmpty(), coverUrl = imageUrl)
            }
            opening = null
        }
    }
    fun openItem(item: FriendItem) = open(item.kind, item.itemKey, item.title, item.subtitle, item.imageUrl)

    suspend fun addFriend(code: String): Boolean {
        val result = social.addFriend(code)
        val message = when (result) {
            AddFriendResult.REQUESTED -> Res.string.social_friendRequested
            AddFriendResult.ACCEPTED -> Res.string.social_friendAccepted
            AddFriendResult.ALREADY -> Res.string.social_friendAlready
            AddFriendResult.SELF -> Res.string.social_friendSelf
            AddFriendResult.NOT_FOUND -> Res.string.social_friendNotFound
            AddFriendResult.FAILED -> Res.string.social_friendFailed
        }
        val ok = result == AddFriendResult.REQUESTED || result == AddFriendResult.ACCEPTED
        if (ok) container.toasts.success(message) else container.toasts.error(message)
        return ok
    }

    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(horizontal = if (wide) 32.dp else 16.dp, vertical = if (wide) 32.dp else 16.dp)) {
            Text(stringResource(Res.string.social_title), style = NookTheme.type.h1Serif, color = socialAccent())
            Text(stringResource(Res.string.social_subtitle), style = NookTheme.type.sm, color = colors.textSubtle)
            Spacer(Modifier.height(20.dp))

            invitations.forEach { session ->
                val hostId = session.host
                InvitationCard(session, byId[hostId], onJoin = { onOpenTonight(session.id) }, onDecline = { social.decline(session) })
                Spacer(Modifier.height(10.dp))
            }
            TonightEntryCard(onClick = { onOpenTonight(null) })

            when (status) {
                SocialStatus.NOT_SET_UP -> {
                    Spacer(Modifier.height(16.dp))
                    InlineBanner(stringResource(Res.string.social_notSetUp), tone = BannerTone.WARNING, icon = LucideIcons.AlertTriangle)
                }
                SocialStatus.ERROR -> {
                    Spacer(Modifier.height(16.dp))
                    InlineBanner(stringResource(Res.string.social_error), icon = LucideIcons.AlertTriangle, trailing = {
                        TextLink(stringResource(Res.string.common_retry), onClick = { scope.launch { social.refresh() } })
                    })
                }
                SocialStatus.LOADING -> if (profile == null) {
                    Spacer(Modifier.height(28.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Spinner(size = 24.dp, color = socialAccent()) }
                }
                else -> Unit
            }

            if (incoming.isNotEmpty()) {
                SocialSpacer()
                SocialTitle(stringResource(Res.string.social_requests), count = incoming.size)
                incoming.forEach { friend ->
                    RequestRow(
                        friend,
                        onAccept = { scope.launch { social.accept(friend) } },
                        onDecline = { scope.launch { social.remove(friend) } },
                    )
                }
            }

            if (received.isNotEmpty()) {
                SocialSpacer()
                SocialTitle(stringResource(Res.string.social_reactions))
                received.take(5).forEach { r ->
                    ReceivedReactionRow(r, byId[r.fromUser], onOpen = { open(r.kind, r.itemKey, r.itemTitle, null, null) })
                }
            }

            if (now.isNotEmpty()) {
                SocialSpacer()
                SocialTitle(stringResource(Res.string.social_now))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    now.forEach { item -> NowTile(item, byId[item.owner], loading = opening == "${item.kind}:${item.itemKey}", onOpen = { openItem(item) }) }
                }
            }

            if (salon.isNotEmpty()) {
                SocialSpacer()
                SocialTitle(stringResource(Res.string.social_salon))
                salon.forEachIndexed { i, item ->
                    if (i > 0) HairlineDivider()
                    SalonItem(
                        item = item,
                        friend = byId[item.owner],
                        myReaction = sent.firstOrNull { it.toUser == item.owner && it.kind == item.kind && it.itemKey == item.itemKey }?.reaction,
                        inMyLibrary = (item.kind to item.itemKey) in mine,
                        loading = opening == "${item.kind}:${item.itemKey}",
                        onOpen = { openItem(item) },
                        onReact = { key ->
                            scope.launch { if (!social.react(item, key)) container.toasts.error(Res.string.social_reactError) }
                        },
                        onAdd = { scope.launch { addToMine(container, item) } },
                    )
                }
            }

            SocialSpacer()
            if (status == SocialStatus.READY && accepted.isEmpty() && incoming.isEmpty()) {
                EmptyFriends()
                Spacer(Modifier.height(14.dp))
            } else {
                SocialTitle(stringResource(Res.string.social_friends), count = accepted.size)
            }
            if (status != SocialStatus.NOT_SET_UP) {
                FriendCodeCard(
                    profile,
                    onCopy = {
                        profile?.friendCode?.let {
                            clipboard.setText(AnnotatedString(it))
                            container.toasts.success(Res.string.social_codeCopied)
                        }
                    },
                    onShare = {
                        profile?.friendCode?.let { code -> scope.launch { shareText(getString(Res.string.social_shareText, code)) } }
                    },
                    onAdd = { code -> addFriend(code) },
                )
                Spacer(Modifier.height(6.dp))
                (accepted + outgoing).forEach { friend ->
                    FriendRow(friend, onRemove = { scope.launch { social.remove(friend) } })
                }
            }

            profile?.let { me ->
                SocialSpacer()
                SocialTitle(stringResource(Res.string.social_sharing))
                SharingCard(me) { m, s, b, r -> scope.launch { social.updateSharing(m, s, b, r) } }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(Res.string.social_sharingNote), style = NookTheme.type.xs, color = colors.textFaint, modifier = Modifier.padding(horizontal = 4.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // The Social tab is the same in every mode: each sheet takes the colours of its own kind.
    val themeMode by container.prefs.theme.collectAsState()
    moviePrefill?.let { NookTheme(themeMode, MediaMode.MOVIES) { AddMovieSheet(container, it, onClose = { moviePrefill = null }) } }
    seriesPrefill?.let { NookTheme(themeMode, MediaMode.SERIES) { AddSeriesSheet(container, it, onClose = { seriesPrefill = null }) } }
    bookPrefill?.let { NookTheme(themeMode, MediaMode.BOOKS) { AddBookSheet(container, it, onClose = { bookPrefill = null }) } }
    movieSelected?.let { NookTheme(themeMode, MediaMode.MOVIES) { MovieDetailSheet(container, it, onClose = { movieSelected = null }) } }
    seriesSelected?.let { NookTheme(themeMode, MediaMode.SERIES) { SeriesDetailSheet(container, it, onClose = { seriesSelected = null }) } }
    bookSelected?.let { NookTheme(themeMode, MediaMode.BOOKS) { BookDetailSheet(container, it, onClose = { bookSelected = null }) } }
}

@Composable
private fun SocialSpacer() = Spacer(Modifier.height(30.dp))

/** No friend yet: what they bring, above the code to share. */
@Composable
private fun EmptyFriends() {
    val accent = socialAccent()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).clip(NookShapes.full).background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(LucideIcons.Users, null, Modifier.size(24.dp), tint = accent)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.social_emptyTitle), style = NookTheme.type.h3Serif, color = NookTheme.colors.textStrong)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(Res.string.social_emptyText), style = NookTheme.type.sm, color = NookTheme.colors.textSubtle)
        }
    }
}

/** What I already have, as (kind, key) pairs comparable to a friend's items. */
private fun ownedKeys(movies: List<Movie>, series: List<Series>, books: List<Book>): Set<Pair<String, String>> =
    movies.mapNotNull { m -> m.tmdbId?.let { "movie" to it.toString() } }.toSet() +
        series.mapNotNull { s -> s.tmdbId?.let { "series" to it.toString() } } +
        books.mapNotNull { b -> b.googleBooksId?.let { "book" to it } }

/** Adds a friend's item to my "To watch" / "To read" list, with the full details when they load. */
private suspend fun addToMine(container: AppContainer, item: FriendItem) {
    when (item.kind) {
        "movie" -> {
            val id = item.itemKey.toIntOrNull() ?: return
            val base = container.tmdb.fetchMovieDetails(id)?.let { TmdbApi.extractMovieData(it) }
                ?: Movie(tmdbId = id, title = item.title, director = item.subtitle.orEmpty(), posterUrl = item.imageUrl)
            container.movies.add(base.copy(status = MovieStatus.WANT_TO_WATCH, rating = null, personalNote = null, watchedDate = null))
        }
        "series" -> {
            val id = item.itemKey.toIntOrNull() ?: return
            val base = container.tmdb.fetchSeriesDetails(id)?.let { TmdbApi.extractSeriesData(it) }
                ?: Series(tmdbId = id, title = item.title, creator = item.subtitle.orEmpty(), posterUrl = item.imageUrl)
            container.series.add(base.copy(status = SeriesStatus.WANT_TO_WATCH, rating = null, personalNote = null, watchedSeasons = emptyList(), watchedEpisodes = emptyMap()))
        }
        else -> container.books.add(
            Book(googleBooksId = item.itemKey, title = item.title, author = item.subtitle.orEmpty(), coverUrl = item.imageUrl, status = BookStatus.WANT_TO_READ),
        )
    }
}
