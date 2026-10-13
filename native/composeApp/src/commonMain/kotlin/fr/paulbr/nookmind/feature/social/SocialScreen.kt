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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.config.AppConfig
import fr.paulbr.nookmind.core.data.AddFriendResult
import fr.paulbr.nookmind.core.data.FriendItem
import fr.paulbr.nookmind.core.data.SocialStatus
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.Avatar
import fr.paulbr.nookmind.core.designsystem.components.AvatarSize
import fr.paulbr.nookmind.core.designsystem.components.BannerTone
import fr.paulbr.nookmind.core.designsystem.components.HairlineDivider
import fr.paulbr.nookmind.core.designsystem.components.IconGhostButton
import fr.paulbr.nookmind.core.designsystem.components.InlineBanner
import fr.paulbr.nookmind.core.designsystem.components.Spinner
import fr.paulbr.nookmind.core.designsystem.components.TextLink
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.ShareLinks
import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.BookStatus
import fr.paulbr.nookmind.core.model.Movie
import fr.paulbr.nookmind.core.model.MovieStatus
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesStatus
import fr.paulbr.nookmind.core.network.TmdbApi
import fr.paulbr.nookmind.core.platform.deviceLanguageTag
import fr.paulbr.nookmind.core.platform.shareText
import fr.paulbr.nookmind.feature.common.ItemSheetsHost
import fr.paulbr.nookmind.feature.common.LocalWideLayout
import fr.paulbr.nookmind.feature.common.rememberItemSheets
import fr.paulbr.nookmind.feature.tonight.TonightEntryCard
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.common_retry
import fr.paulbr.nookmind.resources.social_addFriends
import fr.paulbr.nookmind.resources.social_affinities
import fr.paulbr.nookmind.resources.social_codeCopied
import fr.paulbr.nookmind.resources.social_emptyText
import fr.paulbr.nookmind.resources.social_emptyTitle
import fr.paulbr.nookmind.resources.social_error
import fr.paulbr.nookmind.resources.social_friendAccepted
import fr.paulbr.nookmind.resources.social_friendAlready
import fr.paulbr.nookmind.resources.social_friendFailed
import fr.paulbr.nookmind.resources.social_friendNotFound
import fr.paulbr.nookmind.resources.social_friendRequested
import fr.paulbr.nookmind.resources.social_friendSelf
import fr.paulbr.nookmind.resources.social_friends
import fr.paulbr.nookmind.resources.social_inviteBubble
import fr.paulbr.nookmind.resources.social_inviteText
import fr.paulbr.nookmind.resources.social_lately
import fr.paulbr.nookmind.resources.social_notSetUp
import fr.paulbr.nookmind.resources.social_pending
import fr.paulbr.nookmind.resources.social_reactions
import fr.paulbr.nookmind.resources.social_requests
import fr.paulbr.nookmind.resources.social_shareText
import fr.paulbr.nookmind.resources.social_sharing
import fr.paulbr.nookmind.resources.social_sharingNote
import fr.paulbr.nookmind.resources.social_subtitle
import fr.paulbr.nookmind.resources.social_thisWeek
import fr.paulbr.nookmind.resources.social_title
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

/**
 * The Social tab, the same in every mode. Up top the row of friends and On regarde quoi ? under
 * it, then how close each friend's tastes are to mine, and what they did this week, filtered by kind. A
 * friend opens their page; any title opens its sheet, to add it to mine. Requests, invitations,
 * reactions received and what I share stay where they were.
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
    val invitations by social.invitations.collectAsState()
    val pendingCode by container.pendingFriendCode.collectAsState()
    val openProfile by container.socialProfile.collectAsState()
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
    // A ring on the friends who moved since the last visit; leaving the tab marks it all seen.
    val seenAt = remember { container.prefs.socialSeenAt }
    DisposableEffect(Unit) {
        onDispose { container.prefs.socialSeenAt = Clock.System.now().toString() }
    }

    val accepted = friends.filter { it.accepted }
    val incoming = friends.filter { !it.accepted && it.incoming }
    val outgoing = friends.filter { !it.accepted && !it.incoming }
    val byId = friends.associateBy { it.userId }
    val mine = remember(movies, series, books) { myItems(movies, series, books) }
    val ordered = remember(accepted, library) { accepted.sortedByDescending { latestActivityOf(library, it.userId).orEmpty() } }
    val closest = remember(library, accepted, mine) { affinities(library, accepted.map { it.userId }, mine) }
    val week = remember(library) { weekFeed(library) }
    var weekKind by remember { mutableStateOf<String?>(null) }

    // A friend's movie, series or book opens like a search result (to add it on my side), or as
    // my own sheet when I already have it.
    val sheets = rememberItemSheets(container)
    fun openItem(item: FriendItem) = sheets.open(item.kind, item.itemKey, item.title, item.subtitle, item.imageUrl)
    fun addItem(item: FriendItem) {
        scope.launch { addToMine(container, item) }
    }

    // "Add friends": my invitation (QR code, link, code) and the field for a friend's code, opened
    // from the button by the title, or by an invitation link with the friend's code filled in.
    var inviteOpen by remember { mutableStateOf(false) }
    var inviteCode by remember { mutableStateOf("") }
    LaunchedEffect(pendingCode) {
        pendingCode?.let { code ->
            container.socialProfile.value = null
            inviteCode = code
            inviteOpen = true
            container.pendingFriendCode.value = null
        }
    }
    val inviteLink = profile?.friendCode?.let { code ->
        ShareLinks.friendLinkFor(AppConfig.apiBaseUrl, code, container.auth.currentUser?.displayName(""), deviceLanguageTag())
    }
    val copyCode: () -> Unit = {
        profile?.friendCode?.let {
            clipboard.setText(AnnotatedString(it))
            container.toasts.success(Res.string.social_codeCopied)
        }
    }
    val shareInvite: () -> Unit = {
        profile?.friendCode?.let { code ->
            scope.launch {
                shareText(if (inviteLink != null) getString(Res.string.social_inviteText, inviteLink) else getString(Res.string.social_shareText, code))
            }
        }
    }
    val openInvite: () -> Unit = { inviteCode = ""; inviteOpen = true }

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

    val shownProfile = openProfile?.let { id -> byId[id] }
    if (openProfile != null && shownProfile == null && status == SocialStatus.READY) {
        // Removed meanwhile, or no longer a friend: back to the list.
        LaunchedEffect(openProfile) { container.socialProfile.value = null }
    }

    if (shownProfile != null) {
        BackHandler(enabled = true) { container.socialProfile.value = null }
        FriendProfileScreen(
            container = container,
            friend = shownProfile,
            mine = mine,
            sheets = sheets,
            contentPadding = contentPadding,
            onAdd = ::addItem,
            onBack = { container.socialProfile.value = null },
        )
    } else {
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(vertical = if (wide) 32.dp else 16.dp)) {
                val gutter = Modifier.padding(horizontal = if (wide) 32.dp else 16.dp)
                Row(gutter, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(Res.string.social_title), style = NookTheme.type.h1Serif, color = socialAccent())
                        Text(stringResource(Res.string.social_subtitle), style = NookTheme.type.sm, color = colors.textSubtle)
                    }
                    if (status != SocialStatus.NOT_SET_UP) {
                        Spacer(Modifier.width(12.dp))
                        IconGhostButton(
                            LucideIcons.UserPlus,
                            stringResource(Res.string.social_addFriends),
                            onClick = openInvite,
                            size = 20.dp,
                            tint = socialAccent(),
                            background = socialAccent().copy(alpha = 0.12f),
                            padding = 12.dp,
                        )
                    }
                }

                Column(gutter) {
                    invitations.forEach { session ->
                        Spacer(Modifier.height(16.dp))
                        InvitationCard(session, byId[session.host], onJoin = { onOpenTonight(session.id) }, onDecline = { social.decline(session) })
                    }
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
                }

                // Tes amis: friends by their latest move, requests of mine dimmed, then a way to invite more.
                SocialSpacer()
                SocialTitle(stringResource(Res.string.social_friends), gutter)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = if (wide) 28.dp else 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ordered.forEach { friend ->
                        StoryBubble(
                            friend.firstName.ifBlank { "?" },
                            onClick = { container.socialProfile.value = friend.userId },
                            news = hasNews(latestActivityOf(library, friend.userId), seenAt),
                        ) { Avatar(friend.displayName, friend.avatarUrl, AvatarSize.XL) }
                    }
                    outgoing.forEach { friend ->
                        StoryBubble(
                            stringResource(Res.string.social_pending),
                            onClick = { container.socialProfile.value = friend.userId },
                            dimmed = true,
                            description = "${friend.firstName}, ${stringResource(Res.string.social_pending)}",
                        ) { Avatar(friend.displayName, friend.avatarUrl, AvatarSize.XL) }
                    }
                    if (status != SocialStatus.NOT_SET_UP) {
                        StoryBubble(stringResource(Res.string.social_inviteBubble), onClick = openInvite, labelColor = socialAccent()) {
                            ActionBubbleIcon(LucideIcons.UserPlus)
                        }
                    }
                }

                Column(gutter) {
                    // On regarde quoi ?, right under the friends to play it with.
                    Spacer(Modifier.height(18.dp))
                    TonightEntryCard(onClick = { onOpenTonight(null) })
                    if (status == SocialStatus.READY && accepted.isEmpty() && incoming.isEmpty() && outgoing.isEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        EmptyFriends()
                        Spacer(Modifier.height(14.dp))
                        FriendCodeCard(profile, onCopy = copyCode, onShare = shareInvite, onAdd = { code -> addFriend(code) })
                    }
                }

                if (closest.isNotEmpty()) {
                    SocialSpacer()
                    SocialTitle(stringResource(Res.string.social_affinities), gutter)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = if (wide) 32.dp else 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        closest.forEach { affinity ->
                            val friend = byId[affinity.friendId] ?: return@forEach
                            AffinityCard(
                                affinity = affinity,
                                friend = friend,
                                opening = { sheets.isOpening(it.kind, it.itemKey) },
                                onOpenProfile = { container.socialProfile.value = friend.userId },
                                onOpenItem = ::openItem,
                            )
                        }
                    }
                }

                Column(gutter) {
                    if (received.isNotEmpty()) {
                        SocialSpacer()
                        SocialTitle(stringResource(Res.string.social_reactions))
                        received.take(5).forEach { r ->
                            ReceivedReactionRow(r, byId[r.fromUser], onOpen = { sheets.open(r.kind, r.itemKey, r.itemTitle) })
                        }
                    }

                    if (week.items.isNotEmpty()) {
                        val kinds = kindsIn(week.items)
                        val chosen = weekKind?.takeIf { it in kinds }
                        SocialSpacer()
                        SocialTitle(stringResource(if (week.thisWeek) Res.string.social_thisWeek else Res.string.social_lately))
                        if (kinds.size > 1) {
                            KindChips(kinds, chosen, onSelect = { weekKind = it })
                            Spacer(Modifier.height(6.dp))
                        }
                        week.items.ofKind(chosen).forEachIndexed { i, item ->
                            if (i > 0) HairlineDivider()
                            WeekRow(
                                item = item,
                                friend = byId[item.owner],
                                inMyLibrary = (item.kind to item.itemKey) in mine,
                                loading = sheets.isOpening(item.kind, item.itemKey),
                                onOpen = { openItem(item) },
                                onAdd = { addItem(item) },
                            )
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
        }
    }

    ItemSheetsHost(container, sheets)

    if (inviteOpen) {
        FriendInviteSheet(
            profile = profile,
            link = inviteLink,
            initialCode = inviteCode,
            onCopy = copyCode,
            onShare = shareInvite,
            onAdd = { code -> addFriend(code) },
            onClose = { inviteOpen = false },
        )
    }
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

/** What I have, by (kind, key) as a friend's items name them, with my rating. */
internal fun myItems(movies: List<Movie>, series: List<Series>, books: List<Book>): Map<Pair<String, String>, MyItem> =
    (
        movies.mapNotNull { m -> m.tmdbId?.let { MyItem("movie", it.toString(), m.rating) } } +
            series.mapNotNull { s -> s.tmdbId?.let { MyItem("series", it.toString(), s.rating) } } +
            books.mapNotNull { b -> b.googleBooksId?.let { MyItem("book", it, b.rating) } }
        ).associateBy { it.kind to it.key }

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
