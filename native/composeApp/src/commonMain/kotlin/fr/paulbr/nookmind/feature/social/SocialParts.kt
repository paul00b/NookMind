package fr.paulbr.nookmind.feature.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.data.Friend
import fr.paulbr.nookmind.core.data.FriendItem
import fr.paulbr.nookmind.core.data.ReceivedReaction
import fr.paulbr.nookmind.core.data.SocialProfile
import fr.paulbr.nookmind.core.data.TonightSessionRow
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.components.Avatar
import fr.paulbr.nookmind.core.designsystem.components.AvatarSize
import fr.paulbr.nookmind.core.designsystem.components.CompactRating
import fr.paulbr.nookmind.core.designsystem.components.GhostButton
import fr.paulbr.nookmind.core.designsystem.components.IconGhostButton
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.NookCard
import fr.paulbr.nookmind.core.designsystem.components.NookPressable
import fr.paulbr.nookmind.core.designsystem.components.NookTextField
import fr.paulbr.nookmind.core.designsystem.components.NookToggle
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.ui.HapticCue
import fr.paulbr.nookmind.core.ui.LocalNookHaptics
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.common_decimalSeparator
import fr.paulbr.nookmind.resources.social_accept
import fr.paulbr.nookmind.resources.social_add
import fr.paulbr.nookmind.resources.social_addToList
import fr.paulbr.nookmind.resources.social_addToReadList
import fr.paulbr.nookmind.resources.social_agoDays
import fr.paulbr.nookmind.resources.social_agoHours
import fr.paulbr.nookmind.resources.social_agoMinutes
import fr.paulbr.nookmind.resources.social_agoNow
import fr.paulbr.nookmind.resources.social_codeHint
import fr.paulbr.nookmind.resources.social_codePlaceholder
import fr.paulbr.nookmind.resources.social_copy
import fr.paulbr.nookmind.resources.social_decline
import fr.paulbr.nookmind.resources.social_friendsTake
import fr.paulbr.nookmind.resources.social_inYourList
import fr.paulbr.nookmind.resources.social_invitation
import fr.paulbr.nookmind.resources.social_join
import fr.paulbr.nookmind.resources.social_myCode
import fr.paulbr.nookmind.resources.social_pending
import fr.paulbr.nookmind.resources.social_reactCulte
import fr.paulbr.nookmind.resources.social_reactEnvie
import fr.paulbr.nookmind.resources.social_reactMerci
import fr.paulbr.nookmind.resources.social_reactedCulte
import fr.paulbr.nookmind.resources.social_reactedEnvie
import fr.paulbr.nookmind.resources.social_reactedMerci
import fr.paulbr.nookmind.resources.social_remove
import fr.paulbr.nookmind.resources.social_removeConfirm
import fr.paulbr.nookmind.resources.social_share
import fr.paulbr.nookmind.resources.social_shareBooks
import fr.paulbr.nookmind.resources.social_shareMovies
import fr.paulbr.nookmind.resources.social_shareRatings
import fr.paulbr.nookmind.resources.social_shareSeries
import fr.paulbr.nookmind.resources.social_verbFinished
import fr.paulbr.nookmind.resources.social_verbRead
import fr.paulbr.nookmind.resources.social_verbReading
import fr.paulbr.nookmind.resources.social_verbWantRead
import fr.paulbr.nookmind.resources.social_verbWantWatch
import fr.paulbr.nookmind.resources.social_verbWatched
import fr.paulbr.nookmind.resources.social_verbWatching
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** The violet of the Social tab, readable on both themes. */
@Composable
internal fun socialAccent(): Color = if (NookTheme.colors.isDark) Palette.Purple400 else Palette.Purple600

/** "2 h", "3 j", "à l'instant", or nothing for an old or unreadable date. */
@Composable
internal fun agoLabel(iso: String): String? {
    val (unit, value) = agoOf(iso) ?: return null
    return when (unit) {
        AgoUnit.NOW -> stringResource(Res.string.social_agoNow)
        AgoUnit.MINUTES -> stringResource(Res.string.social_agoMinutes, value)
        AgoUnit.HOURS -> stringResource(Res.string.social_agoHours, value)
        AgoUnit.DAYS -> stringResource(Res.string.social_agoDays, value)
    }
}

/** What a friend did with an item, as a verb: "a vu", "lit", "veut voir"… */
@Composable
internal fun verbOf(item: FriendItem): String = stringResource(
    when (item.kind) {
        "book" -> when (item.status) {
            "read" -> Res.string.social_verbRead
            "reading" -> Res.string.social_verbReading
            else -> Res.string.social_verbWantRead
        }
        "series" -> when (item.status) {
            "watched" -> Res.string.social_verbFinished
            "watching" -> Res.string.social_verbWatching
            else -> Res.string.social_verbWantWatch
        }
        else -> if (item.status == "watched") Res.string.social_verbWatched else Res.string.social_verbWantWatch
    },
)

/** "4", "4,5" ("4.5" in English). */
@Composable
internal fun ratingText(rating: Double): String {
    val tenths = (rating * 10).roundToInt()
    return if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}${stringResource(Res.string.common_decimalSeparator)}${tenths % 10}"
}

internal fun modeOfKind(kind: String): MediaMode = when (kind) {
    "book" -> MediaMode.BOOKS
    "series" -> MediaMode.SERIES
    else -> MediaMode.MOVIES
}

/** Section title of the Social tab, with an optional count. */
@Composable
internal fun SocialTitle(text: String, modifier: Modifier = Modifier, count: Int? = null) {
    Row(modifier.padding(start = 2.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = NookTheme.type.h3Serif, color = NookTheme.colors.textStrong)
        if (count != null && count > 0) {
            Spacer(Modifier.width(8.dp))
            Text(count.toString(), style = NookTheme.type.sans(14, FontWeight.SemiBold, 20), color = NookTheme.colors.textFaint)
        }
    }
}

// ── Invitations ────────────────────────────────────────────────────────────

/** A friend waits for me in Ce soir ?: in the colours of the game, with the way in. */
@Composable
internal fun InvitationCard(session: TonightSessionRow, host: Friend?, onJoin: () -> Unit, onDecline: () -> Unit) {
    val name = host?.firstName?.ifBlank { null } ?: "?"
    Row(
        Modifier
            .fillMaxWidth()
            .clip(NookShapes.xl2)
            .background(Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF4C1D95), Color(0xFF831843))))
            .clickable(onClick = onJoin)
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Avatar(host?.displayName ?: name, host?.avatarUrl, AvatarSize.LG)
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp).size(22.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(LucideIcons.Popcorn, null, Modifier.size(13.dp), tint = Color(0xFF6D28D9))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.social_invitation, name), style = NookTheme.type.sans(15, FontWeight.SemiBold, 20), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            agoLabel(session.createdAt)?.let {
                Text(it, style = NookTheme.type.sans(13, lineHeight = 18), color = Color.White.copy(alpha = 0.7f))
            }
        }
        Spacer(Modifier.width(8.dp))
        NookPressable(
            onClick = onJoin,
            background = Color.White,
            contentColor = Color(0xFF111111),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            pressScale = 0.95f,
        ) {
            Text(stringResource(Res.string.social_join), style = NookTheme.type.sans(14, FontWeight.SemiBold, 18))
        }
        IconGhostButton(LucideIcons.X, stringResource(Res.string.social_decline), onDecline, size = 18.dp, tint = Color.White.copy(alpha = 0.75f))
    }
}

// ── Friends ────────────────────────────────────────────────────────────────

/** My friend code, to share, and the field to enter someone else's. */
@Composable
internal fun FriendCodeCard(profile: SocialProfile?, onCopy: () -> Unit, onShare: () -> Unit, onAdd: suspend (String) -> Boolean) {
    val colors = NookTheme.colors
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    NookCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Text(stringResource(Res.string.social_myCode), style = NookTheme.type.sm, color = colors.textSubtle)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                profile?.friendCode ?: "······",
                style = NookTheme.type.sans(30, FontWeight.Bold, 36).copy(letterSpacing = 5.sp),
                color = colors.textStrong,
                modifier = Modifier.weight(1f),
            )
            IconGhostButton(LucideIcons.Copy, stringResource(Res.string.social_copy), onCopy, enabled = profile != null, background = colors.surfaceMuted)
            Spacer(Modifier.width(8.dp))
            IconGhostButton(LucideIcons.Share, stringResource(Res.string.social_share), onShare, enabled = profile != null, background = colors.surfaceMuted)
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(Res.string.social_codeHint), style = NookTheme.type.sm, color = colors.textSubtle)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            NookTextField(
                value = draft,
                onValueChange = { value -> draft = value.uppercase().filter { it.isLetterOrDigit() }.take(6) },
                placeholder = stringResource(Res.string.social_codePlaceholder),
                modifier = Modifier.weight(1f),
                textStyle = NookTheme.type.sans(16, FontWeight.SemiBold, 24).copy(letterSpacing = 2.sp),
            )
            Spacer(Modifier.width(10.dp))
            PrimaryButton(
                stringResource(Res.string.social_add),
                onClick = {
                    sending = true
                    scope.launch {
                        if (onAdd(draft)) draft = ""
                        sending = false
                    }
                },
                enabled = draft.length == 6,
                loading = sending,
                icon = LucideIcons.UserPlus,
                color = Palette.Purple500,
            )
        }
    }
}

/** A request someone sent me: accept or decline. */
@Composable
internal fun RequestRow(friend: Friend, onAccept: () -> Unit, onDecline: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(friend.displayName, friend.avatarUrl, AvatarSize.MD)
        Spacer(Modifier.width(12.dp))
        Text(friend.displayName.ifBlank { "?" }, style = NookTheme.type.sans(15, FontWeight.SemiBold, 20), color = NookTheme.colors.textStrong, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        PrimaryButton(stringResource(Res.string.social_accept), onClick = onAccept, icon = LucideIcons.Check, color = Palette.Purple500, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp))
        IconGhostButton(LucideIcons.X, stringResource(Res.string.social_decline), onDecline, size = 18.dp)
    }
}

/** A friend, or a request of mine still waiting; removing asks once more. */
@Composable
internal fun FriendRow(friend: Friend, onRemove: () -> Unit) {
    val colors = NookTheme.colors
    var confirming by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(friend.displayName, friend.avatarUrl, AvatarSize.MD)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(friend.displayName.ifBlank { "?" }, style = NookTheme.type.sans(15, FontWeight.SemiBold, 20), color = colors.textStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!friend.accepted) Text(stringResource(Res.string.social_pending), style = NookTheme.type.xs, color = colors.textSubtle)
        }
        if (confirming) {
            GhostButton(stringResource(Res.string.social_removeConfirm), onClick = { confirming = false; onRemove() }, color = colors.redText, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp))
            IconGhostButton(LucideIcons.X, null, { confirming = false }, size = 16.dp)
        } else {
            IconGhostButton(LucideIcons.Trash2, stringResource(Res.string.social_remove), { confirming = true }, size = 16.dp, tint = colors.textFaint)
        }
    }
}

// ── Salon ──────────────────────────────────────────────────────────────────

/** A poster tile of "En ce moment", with the friend on its corner. */
@Composable
internal fun NowTile(item: FriendItem, friend: Friend?, modifier: Modifier = Modifier) {
    Column(modifier.width(96.dp)) {
        Box {
            MediaImage(item.imageUrl, item.title, Modifier.fillMaxWidth(), mode = modeOfKind(item.kind), shape = NookShapes.xl, placeholderIconSize = 24.dp)
            if (friend != null) {
                Box(Modifier.align(Alignment.BottomStart).padding(6.dp).border(2.dp, NookTheme.colors.background, CircleShape)) {
                    Avatar(friend.displayName, friend.avatarUrl, AvatarSize.SM)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(item.title, style = NookTheme.type.sans(13, FontWeight.Medium, 17), color = NookTheme.colors.textBody, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (friend != null) {
            Text("${friend.firstName} ${verbOf(item)}", style = NookTheme.type.xs, color = NookTheme.colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The three reactions, in order. */
internal val REACTIONS = listOf("culte", "envie", "merci")

@Composable
private fun reactionLabel(key: String): Pair<ImageVector, String> = when (key) {
    "culte" -> LucideIcons.Flame to stringResource(Res.string.social_reactCulte)
    "envie" -> LucideIcons.Heart to stringResource(Res.string.social_reactEnvie)
    else -> LucideIcons.Sparkles to stringResource(Res.string.social_reactMerci)
}

/** One line of the salon: who did what, the item, the reactions, and a way to add it to mine. */
@Composable
internal fun SalonItem(
    item: FriendItem,
    friend: Friend?,
    myReaction: String?,
    inMyLibrary: Boolean,
    onReact: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = NookTheme.colors
    val accent = socialAccent()
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        MediaImage(item.imageUrl, item.title, Modifier.width(76.dp), mode = modeOfKind(item.kind), shape = NookShapes.xl, placeholderIconSize = 22.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (friend != null) Avatar(friend.displayName, friend.avatarUrl, AvatarSize.SM, Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    listOfNotNull("${friend?.firstName.orEmpty()} ${verbOf(item)}".trim(), agoLabel(activityOf(item))).joinToString(" · "),
                    style = NookTheme.type.sans(13, lineHeight = 18), color = colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(item.title, style = NookTheme.type.serif(18, lineHeight = 23), color = colors.textStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = NookTheme.type.sans(13, lineHeight = 18), color = colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(8.dp))
                }
                item.rating?.let { CompactRating(ratingText(it)) }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                REACTIONS.forEach { key ->
                    val (icon, label) = reactionLabel(key)
                    ReactionChip(icon, label, selected = myReaction == key, accent = accent) { onReact(key) }
                }
                Spacer(Modifier.weight(1f))
                if (inMyLibrary) {
                    Icon(LucideIcons.Check, stringResource(Res.string.social_inYourList), Modifier.size(18.dp), tint = colors.emeraldText)
                } else {
                    IconGhostButton(
                        LucideIcons.Plus,
                        stringResource(if (item.kind == "book") Res.string.social_addToReadList else Res.string.social_addToList),
                        onAdd, size = 18.dp, tint = accent, background = accent.copy(alpha = 0.12f), padding = 7.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReactionChip(icon: ImageVector, label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val colors = NookTheme.colors
    val haptics = LocalNookHaptics.current
    Row(
        Modifier
            .clip(NookShapes.full)
            .background(if (selected) accent.copy(alpha = 0.16f) else colors.surfaceMuted)
            .border(1.dp, if (selected) accent.copy(alpha = 0.5f) else Color.Transparent, NookShapes.full)
            .clickable {
                haptics.perform(if (selected) HapticCue.TOGGLE_OFF else HapticCue.CONFIRM)
                onClick()
            }
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(13.dp), tint = if (selected) accent else colors.textSubtle)
        Spacer(Modifier.width(4.dp))
        Text(label, style = NookTheme.type.sans(12, FontWeight.SemiBold, 16), color = if (selected) accent else colors.textSubtle)
    }
}

/** "Tom trouve « Dune » culte · 2 h" */
@Composable
internal fun ReceivedReactionRow(reaction: ReceivedReaction, friend: Friend?) {
    val colors = NookTheme.colors
    val name = friend?.firstName ?: fr.paulbr.nookmind.core.data.firstNameOf(reaction.displayName)
    val text = stringResource(
        when (reaction.reaction) {
            "culte" -> Res.string.social_reactedCulte
            "envie" -> Res.string.social_reactedEnvie
            else -> Res.string.social_reactedMerci
        },
        name, reaction.itemTitle,
    )
    val (icon, _) = reactionLabel(reaction.reaction)
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box {
            Avatar(friend?.displayName ?: reaction.displayName, friend?.avatarUrl, AvatarSize.MD)
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = 3.dp, y = 3.dp).size(20.dp).clip(CircleShape).background(colors.surface).border(1.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, Modifier.size(11.dp), tint = socialAccent()) }
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = NookTheme.type.sans(14, lineHeight = 19), color = colors.textBody, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        agoLabel(reaction.createdAt)?.let {
            Spacer(Modifier.width(8.dp))
            Text(it, style = NookTheme.type.xs, color = colors.textFaint)
        }
    }
}

// ── Sharing ────────────────────────────────────────────────────────────────

@Composable
internal fun SharingCard(profile: SocialProfile, onChange: (movies: Boolean, series: Boolean, books: Boolean, ratings: Boolean) -> Unit) {
    NookCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
        ShareToggle(LucideIcons.Film, stringResource(Res.string.social_shareMovies), profile.shareMovies) { onChange(it, profile.shareSeries, profile.shareBooks, profile.shareRatings) }
        ShareToggle(LucideIcons.Tv, stringResource(Res.string.social_shareSeries), profile.shareSeries) { onChange(profile.shareMovies, it, profile.shareBooks, profile.shareRatings) }
        ShareToggle(LucideIcons.BookOpen, stringResource(Res.string.social_shareBooks), profile.shareBooks) { onChange(profile.shareMovies, profile.shareSeries, it, profile.shareRatings) }
        ShareToggle(LucideIcons.Star, stringResource(Res.string.social_shareRatings), profile.shareRatings) { onChange(profile.shareMovies, profile.shareSeries, profile.shareBooks, it) }
    }
}

@Composable
private fun ShareToggle(icon: ImageVector, label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = NookTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = colors.textSubtle)
        Spacer(Modifier.width(12.dp))
        Text(label, style = NookTheme.type.sans(15, lineHeight = 20), color = colors.textBody, modifier = Modifier.weight(1f))
        NookToggle(checked, onChange)
    }
}

// ── In the detail sheets ───────────────────────────────────────────────────

/**
 * "Tes potes": which friends have this movie, series or book, what they did with it and their
 * rating. Nothing at all when no friend has it.
 */
@Composable
fun FriendsTake(container: AppContainer, kind: String, key: String?) {
    if (key.isNullOrBlank()) return
    val library by container.social.library.collectAsState()
    val friends by container.social.friends.collectAsState()
    val takes = library.filter { it.kind == kind && it.itemKey == key }
        .mapNotNull { item -> friends.firstOrNull { it.userId == item.owner }?.let { item to it } }
    if (takes.isEmpty()) return
    val colors = NookTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.social_friendsTake), style = NookTheme.type.sm, color = colors.textSubtle)
        Spacer(Modifier.height(8.dp))
        takes.forEach { (item, friend) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(friend.displayName, friend.avatarUrl, AvatarSize.SM)
                Spacer(Modifier.width(10.dp))
                Text(friend.firstName, style = NookTheme.type.sans(14, FontWeight.SemiBold, 20), color = colors.textStrong)
                Spacer(Modifier.width(6.dp))
                Text(verbOf(item), style = NookTheme.type.sm, color = colors.textSubtle, modifier = Modifier.weight(1f), maxLines = 1)
                item.rating?.let { CompactRating(ratingText(it)) }
            }
        }
    }
}
