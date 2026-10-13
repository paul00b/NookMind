package fr.paulbr.nookmind.feature.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.data.Friend
import fr.paulbr.nookmind.core.data.FriendItem
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.Avatar
import fr.paulbr.nookmind.core.designsystem.components.AvatarSize
import fr.paulbr.nookmind.core.designsystem.components.GhostButton
import fr.paulbr.nookmind.core.designsystem.components.HairlineDivider
import fr.paulbr.nookmind.core.designsystem.components.IconGhostButton
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.feature.common.ItemSheets
import fr.paulbr.nookmind.feature.common.StatusSegmentedControl
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.common_back
import fr.paulbr.nookmind.resources.common_cancel
import fr.paulbr.nookmind.resources.common_itemsCountBooks
import fr.paulbr.nookmind.resources.common_itemsCountMovies
import fr.paulbr.nookmind.resources.common_itemsCountSeries
import fr.paulbr.nookmind.resources.social_affinityWithYou
import fr.paulbr.nookmind.resources.social_cancelRequest
import fr.paulbr.nookmind.resources.social_favorites
import fr.paulbr.nookmind.resources.social_inCommon
import fr.paulbr.nookmind.resources.social_options
import fr.paulbr.nookmind.resources.social_profileNothing
import fr.paulbr.nookmind.resources.social_profileNothingCommon
import fr.paulbr.nookmind.resources.social_profileNothingHere
import fr.paulbr.nookmind.resources.social_reactError
import fr.paulbr.nookmind.resources.social_removeFriend
import fr.paulbr.nookmind.resources.social_removeFriendConfirm
import fr.paulbr.nookmind.resources.social_requestPending
import fr.paulbr.nookmind.resources.social_sameRated
import fr.paulbr.nookmind.resources.social_tabActivity
import fr.paulbr.nookmind.resources.social_tabCommon
import fr.paulbr.nookmind.resources.social_tabLibrary
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private enum class ProfileTab(val key: String) { LIBRARY("library"), COMMON("common"), ACTIVITY("activity") }

/**
 * A friend's page, inside the Social tab: who they are, how close our tastes are, their favourites,
 * then their library, the titles we share or their latest moves, filtered by kind. Any poster opens
 * the title's sheet, to add it to mine. A request still waiting shows only the way to cancel it.
 */
@Composable
internal fun FriendProfileScreen(
    container: AppContainer,
    friend: Friend,
    mine: Map<Pair<String, String>, MyItem>,
    sheets: ItemSheets,
    contentPadding: PaddingValues,
    onAdd: (FriendItem) -> Unit,
    onBack: () -> Unit,
) {
    val colors = NookTheme.colors
    val social = container.social
    val scope = rememberCoroutineScope()
    val library by social.library.collectAsState()
    val sent by social.sent.collectAsState()

    val theirs = remember(library, friend.userId) { library.filter { it.owner == friend.userId }.sortedByDescending(::activityOf).distinctBy { it.kind to it.itemKey } }
    val affinity = remember(theirs, mine) { affinityOf(friend.userId, theirs, mine) }
    val favorites = remember(theirs) { favoritesOf(theirs) }
    var tab by remember { mutableStateOf(ProfileTab.LIBRARY) }
    var kind by remember { mutableStateOf<String?>(null) }
    var optionsOpen by remember { mutableStateOf(false) }

    val source = if (tab == ProfileTab.COMMON) affinity.common else theirs
    val kinds = kindsIn(source)
    val chosen = kind?.takeIf { it in kinds }
    val shown = source.ofKind(chosen)
    fun open(item: FriendItem) = sheets.open(item.kind, item.itemKey, item.title, item.subtitle, item.imageUrl)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 104.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 32.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            full {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconGhostButton(LucideIcons.ChevronLeft, stringResource(Res.string.common_back), onBack, background = colors.surface, padding = 12.dp)
                    Spacer(Modifier.weight(1f))
                    IconGhostButton(LucideIcons.Ellipsis, stringResource(Res.string.social_options), { optionsOpen = true }, background = colors.surface, padding = 12.dp)
                }
            }
            full {
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(friend.displayName, friend.avatarUrl, AvatarSize.XXL)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(friend.displayName.ifBlank { "?" }, style = NookTheme.type.serif(26, lineHeight = 31), color = colors.textStrong, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (friend.accepted && theirs.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            CountsLine(theirs)
                        }
                    }
                }
            }

            if (!friend.accepted) {
                full {
                    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(Res.string.social_requestPending), style = NookTheme.type.sm, color = colors.textSubtle)
                        GhostButton(
                            stringResource(Res.string.social_cancelRequest),
                            onClick = { scope.launch { if (social.remove(friend)) onBack() } },
                            color = colors.redText,
                            borderColor = colors.border,
                        )
                    }
                }
                return@LazyVerticalGrid
            }

            if (affinity.common.isNotEmpty()) {
                full { AffinityStrip(affinity, onClick = { tab = ProfileTab.COMMON }) }
            }

            if (favorites.isNotEmpty()) {
                full {
                    Column(Modifier.padding(top = 4.dp)) {
                        SocialTitle(stringResource(Res.string.social_favorites))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            favorites.forEach { item ->
                                Box(Modifier.weight(1f).clip(NookShapes.xl).clickable { open(item) }) {
                                    MediaImage(item.imageUrl, item.title, Modifier.fillMaxWidth(), mode = modeOfKind(item.kind), placeholderIconSize = 22.dp)
                                    if (sheets.isOpening(item.kind, item.itemKey)) OpeningVeil(Modifier.matchParentSize())
                                }
                            }
                            repeat(4 - favorites.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            if (theirs.isEmpty()) {
                full {
                    Text(
                        stringResource(Res.string.social_profileNothing, friend.firstName.ifBlank { "?" }),
                        style = NookTheme.type.sm,
                        color = colors.textSubtle,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                return@LazyVerticalGrid
            }

            full {
                val tabs = listOf(
                    ProfileTab.LIBRARY.key to stringResource(Res.string.social_tabLibrary),
                    ProfileTab.COMMON.key to stringResource(Res.string.social_tabCommon),
                    ProfileTab.ACTIVITY.key to stringResource(Res.string.social_tabActivity),
                )
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusSegmentedControl(
                        options = tabs,
                        selected = tab.key,
                        onSelect = { key -> tab = ProfileTab.entries.first { it.key == key } },
                        colorOf = { colors.surfaceMuted2 },
                    )
                    if (kinds.size > 1) KindChips(kinds, chosen, onSelect = { kind = it }, count = shown.size)
                }
            }

            if (shown.isEmpty()) {
                full {
                    Text(
                        stringResource(if (tab == ProfileTab.COMMON) Res.string.social_profileNothingCommon else Res.string.social_profileNothingHere),
                        style = NookTheme.type.sm,
                        color = colors.textSubtle,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else if (tab == ProfileTab.ACTIVITY) {
                val rows = shown.take(40)
                rows.forEachIndexed { i, item ->
                    full {
                        Column {
                            if (i > 0) HairlineDivider()
                            SalonItem(
                                item = item,
                                friend = friend,
                                myReaction = sent.firstOrNull { it.toUser == item.owner && it.kind == item.kind && it.itemKey == item.itemKey }?.reaction,
                                inMyLibrary = (item.kind to item.itemKey) in mine,
                                loading = sheets.isOpening(item.kind, item.itemKey),
                                onOpen = { open(item) },
                                onReact = { key ->
                                    scope.launch { if (!social.react(item, key)) container.toasts.error(Res.string.social_reactError) }
                                },
                                onAdd = { onAdd(item) },
                            )
                        }
                    }
                }
            } else {
                items(shown, key = { "${it.kind}:${it.itemKey}" }) { item ->
                    PosterTile(
                        item = item,
                        inMyLibrary = (item.kind to item.itemKey) in mine,
                        loading = sheets.isOpening(item.kind, item.itemKey),
                        onOpen = { open(item) },
                    )
                }
            }
        }
    }

    if (optionsOpen) {
        FriendOptionsSheet(
            friend = friend,
            onRemove = {
                scope.launch {
                    if (social.remove(friend)) onBack()
                }
            },
            onClose = { optionsOpen = false },
        )
    }
}

/** A row of the grid that spans every column. */
private fun LazyGridScope.full(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

/** "142 films · 37 séries · 58 livres", each number in the colour of its mode. */
@Composable
private fun CountsLine(items: List<FriendItem>) {
    val colors = NookTheme.colors
    val parts = listOf("movie", "series", "book").mapNotNull { kind ->
        val count = items.count { it.kind == kind }
        if (count == 0) return@mapNotNull null
        val text = pluralStringResource(
            when (kind) {
                "book" -> Res.plurals.common_itemsCountBooks
                "series" -> Res.plurals.common_itemsCountSeries
                else -> Res.plurals.common_itemsCountMovies
            },
            count, count,
        )
        Triple(text, count.toString(), kindLabel(kind).second)
    }
    Text(
        buildAnnotatedString {
            parts.forEachIndexed { i, (text, number, tone) ->
                if (i > 0) append(" · ")
                if (text.startsWith(number)) {
                    withStyle(SpanStyle(color = tone, fontWeight = FontWeight.SemiBold)) { append(number) }
                    append(text.removePrefix(number))
                } else {
                    append(text)
                }
            }
        },
        style = NookTheme.type.sans(13, lineHeight = 18),
        color = colors.textSubtle,
    )
}

/** "84 % d'affinité avec toi · 12 titres en commun, dont 3 notés pareil"; the count alone under five. */
@Composable
private fun AffinityStrip(affinity: Affinity, onClick: () -> Unit) {
    val colors = NookTheme.colors
    val accent = socialAccent()
    val common = pluralStringResource(Res.plurals.social_inCommon, affinity.common.size, affinity.common.size)
    val detail = if (affinity.sameRated > 0) "$common, ${pluralStringResource(Res.plurals.social_sameRated, affinity.sameRated, affinity.sameRated)}" else common
    Row(
        Modifier.fillMaxWidth().clip(NookShapes.xl2).background(accent.copy(alpha = 0.12f)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val percent = affinity.percent
        if (percent != null) {
            Text(percentText(percent), style = NookTheme.type.serif(24, lineHeight = 28), color = accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.social_affinityWithYou), style = NookTheme.type.sans(13, FontWeight.SemiBold, 18), color = colors.textStrong)
                Text(detail, style = NookTheme.type.sans(13, lineHeight = 18), color = colors.textSubtle)
            }
        } else {
            Text(detail, style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = colors.textStrong, modifier = Modifier.weight(1f))
        }
    }
}

/** The ⋯ of a profile: removing the friend, asked twice. */
@Composable
private fun FriendOptionsSheet(friend: Friend, onRemove: () -> Unit, onClose: () -> Unit) {
    val colors = NookTheme.colors
    var confirming by remember { mutableStateOf(false) }
    NookSheet(onClose = onClose, maxWidth = 480.dp) { controller ->
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(friend.displayName, friend.avatarUrl, AvatarSize.MD)
                Spacer(Modifier.width(12.dp))
                Text(friend.displayName.ifBlank { "?" }, style = NookTheme.type.h3Serif, color = colors.textStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (confirming) {
                Text(stringResource(Res.string.social_removeFriendConfirm, friend.firstName.ifBlank { "?" }), style = NookTheme.type.sm, color = colors.textBody)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GhostButton(stringResource(Res.string.common_cancel), onClick = { confirming = false }, modifier = Modifier.weight(1f), borderColor = colors.border)
                    PrimaryButton(
                        stringResource(Res.string.social_removeFriend),
                        onClick = { onRemove(); controller.close() },
                        modifier = Modifier.weight(1f),
                        color = Palette.Red600,
                    )
                }
            } else {
                GhostButton(
                    stringResource(Res.string.social_removeFriend),
                    onClick = { confirming = true },
                    icon = LucideIcons.Trash2,
                    color = colors.redText,
                    borderColor = colors.border,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
