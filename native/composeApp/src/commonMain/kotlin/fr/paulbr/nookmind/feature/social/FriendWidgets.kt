package fr.paulbr.nookmind.feature.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.data.Friend
import fr.paulbr.nookmind.core.data.FriendItem
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.accent
import fr.paulbr.nookmind.core.designsystem.components.Avatar
import fr.paulbr.nookmind.core.designsystem.components.AvatarSize
import fr.paulbr.nookmind.core.designsystem.components.CompactRating
import fr.paulbr.nookmind.core.designsystem.components.IconGhostButton
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.designsystem.onFill
import fr.paulbr.nookmind.core.ui.HapticCue
import fr.paulbr.nookmind.core.ui.LocalNookHaptics
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.bookCard_wantToRead
import fr.paulbr.nookmind.resources.movieCard_wantToWatch
import fr.paulbr.nookmind.resources.nav_books
import fr.paulbr.nookmind.resources.nav_movies
import fr.paulbr.nookmind.resources.nav_series
import fr.paulbr.nookmind.resources.social_addToList
import fr.paulbr.nookmind.resources.social_addToReadList
import fr.paulbr.nookmind.resources.social_affinity
import fr.paulbr.nookmind.resources.social_badgeNow
import fr.paulbr.nookmind.resources.social_filterAll
import fr.paulbr.nookmind.resources.social_filterLabel
import fr.paulbr.nookmind.resources.social_inCommon
import fr.paulbr.nookmind.resources.social_inYourList
import fr.paulbr.nookmind.resources.social_kindBook
import fr.paulbr.nookmind.resources.social_kindMovie
import fr.paulbr.nookmind.resources.social_kindSeries
import fr.paulbr.nookmind.resources.social_news
import fr.paulbr.nookmind.resources.social_openProfile
import fr.paulbr.nookmind.resources.social_percent
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** "Film", "Série", "Livre", in the text colour of its mode. */
@Composable
internal fun kindLabel(kind: String): Pair<String, Color> {
    val colors = NookTheme.colors
    return when (kind) {
        "book" -> stringResource(Res.string.social_kindBook) to colors.amberText
        "series" -> stringResource(Res.string.social_kindSeries) to colors.tealText
        else -> stringResource(Res.string.social_kindMovie) to colors.indigoText
    }
}

/** "84 %", spaced the French way in French. */
@Composable
internal fun percentText(percent: Int): String = stringResource(Res.string.social_percent, percent)

// ── The row of friends ─────────────────────────────────────────────────────

/**
 * One bubble of the row under "Tes amis": a ring when [news], dimmed while [dimmed] (a request still
 * waiting), the name under it.
 */
@Composable
internal fun StoryBubble(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    news: Boolean = false,
    dimmed: Boolean = false,
    labelColor: Color? = null,
    description: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = NookTheme.colors
    val accent = socialAccent()
    val newsText = stringResource(Res.string.social_news)
    Column(
        modifier
            .width(68.dp)
            .clip(NookShapes.xl)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                (description ?: label).let { contentDescription = if (news) "$it, $newsText" else it }
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(62.dp)
                .border(if (news) 2.5.dp else 1.dp, if (news) accent else colors.border, CircleShape)
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
            if (dimmed) Box(Modifier.size(AvatarSize.XL.dp.dp).clip(CircleShape).background(colors.background.copy(alpha = 0.55f)))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = NookTheme.type.sans(12, if (news) FontWeight.SemiBold else FontWeight.Medium, 16),
            color = labelColor ?: if (news) colors.textStrong else colors.textSubtle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** The inside of a bubble that is an action (Ce soir ?, Inviter): an icon on the violet. */
@Composable
internal fun ActionBubbleIcon(icon: ImageVector) {
    val accent = socialAccent()
    Box(Modifier.size(AvatarSize.XL.dp.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(24.dp), tint = accent)
    }
}

// ── Affinities ─────────────────────────────────────────────────────────────

/** A friend, how close their tastes are to mine, and a few of the titles we share. */
@Composable
internal fun AffinityCard(
    affinity: Affinity,
    friend: Friend,
    opening: (FriendItem) -> Boolean,
    onOpenProfile: () -> Unit,
    onOpenItem: (FriendItem) -> Unit,
) {
    val colors = NookTheme.colors
    val accent = socialAccent()
    val profileText = stringResource(Res.string.social_openProfile, friend.firstName)
    Column(
        Modifier
            .width(212.dp)
            .clip(NookShapes.xl2)
            .background(colors.surface)
            .border(1.dp, colors.border, NookShapes.xl2)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clip(NookShapes.lg).clickable(onClick = onOpenProfile).semantics(mergeDescendants = true) { contentDescription = profileText },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(friend.displayName, friend.avatarUrl, AvatarSize.MD)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(friend.firstName.ifBlank { "?" }, style = NookTheme.type.sans(15, FontWeight.SemiBold, 20), color = colors.textStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    pluralStringResource(Res.plurals.social_inCommon, affinity.common.size, affinity.common.size),
                    style = NookTheme.type.xs, color = colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        affinity.percent?.let { percent ->
            Row(verticalAlignment = Alignment.Bottom) {
                Text(percentText(percent), style = NookTheme.type.serif(32, lineHeight = 34), color = accent)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.social_affinity), style = NookTheme.type.sans(13, lineHeight = 22), color = colors.textSubtle)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            affinity.common.take(3).forEach { item ->
                Box(Modifier.width(42.dp).clip(NookShapes.lg).clickable { onOpenItem(item) }) {
                    MediaImage(item.imageUrl, item.title, Modifier.fillMaxWidth(), mode = modeOfKind(item.kind), shape = NookShapes.lg, placeholderIconSize = 16.dp)
                    if (opening(item)) OpeningVeil(Modifier.matchParentSize())
                }
            }
            val more = affinity.common.size - 3
            if (more > 0) {
                Box(
                    Modifier.width(42.dp).height(63.dp).clip(NookShapes.lg).background(colors.surfaceMuted).clickable(onClick = onOpenProfile),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+$more", style = NookTheme.type.sans(12, FontWeight.SemiBold, 16), color = colors.textSubtle)
                }
            }
        }
    }
}

// ── Filters ────────────────────────────────────────────────────────────────

/** Tout, Films, Séries, Livres: the chosen one in the colour of its mode. [kinds] are the ones offered. */
@Composable
internal fun KindChips(kinds: List<String>, selected: String?, onSelect: (String?) -> Unit, modifier: Modifier = Modifier, count: Int? = null) {
    val label = stringResource(Res.string.social_filterLabel)
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KindChip(null, stringResource(Res.string.social_filterAll), selected == null, if (selected == null) count else null, onSelect)
        kinds.forEach { kind ->
            val text = stringResource(
                when (kind) {
                    "book" -> Res.string.nav_books
                    "series" -> Res.string.nav_series
                    else -> Res.string.nav_movies
                },
            )
            KindChip(kind, text, selected == kind, if (selected == kind) count else null, onSelect)
        }
    }
}

@Composable
private fun KindChip(kind: String?, text: String, selected: Boolean, count: Int?, onSelect: (String?) -> Unit) {
    val colors = NookTheme.colors
    val haptics = LocalNookHaptics.current
    val fill = when {
        !selected -> colors.surface
        kind == null -> colors.textStrong
        else -> colors.accent(modeOfKind(kind)).fill
    }
    val ink = when {
        !selected -> colors.textBody2
        kind == null -> colors.background
        else -> onFill(fill)
    }
    Box(
        Modifier
            .height(36.dp)
            .clip(NookShapes.full)
            .background(fill)
            .border(1.dp, if (selected) Color.Transparent else colors.border, NookShapes.full)
            .selectable(selected = selected, role = Role.Tab) {
                if (!selected) {
                    haptics.perform(HapticCue.TICK)
                    onSelect(kind)
                }
            }
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (count != null) "$text · $count" else text,
            style = NookTheme.type.sans(13, if (selected) FontWeight.SemiBold else FontWeight.Medium, 18),
            color = ink,
        )
    }
}

// ── Cette semaine ──────────────────────────────────────────────────────────

/**
 * "Léa a terminé Yellowface": the poster, who did what, the kind, rating and when. The whole line
 * opens the title's sheet; the + adds it to my list straight away, the check says it is there.
 */
@Composable
internal fun WeekRow(
    item: FriendItem,
    friend: Friend?,
    inMyLibrary: Boolean,
    loading: Boolean,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
) {
    val colors = NookTheme.colors
    val (kindText, kindColor) = kindLabel(item.kind)
    val verb = verbOf(item)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).clip(NookShapes.xl).clickable(onClick = onOpen).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Big enough to recognise the poster at a glance; the text takes what is left.
            Box(Modifier.width(64.dp)) {
                MediaImage(item.imageUrl, null, Modifier.fillMaxWidth(), mode = modeOfKind(item.kind), shape = NookShapes.xl, placeholderIconSize = 22.dp)
                if (loading) OpeningVeil(Modifier.matchParentSize())
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    buildAnnotatedString {
                        val strong = SpanStyle(color = colors.textStrong, fontWeight = FontWeight.SemiBold)
                        friend?.firstName?.takeIf { it.isNotBlank() }?.let { withStyle(strong) { append(it) }; append(' ') }
                        append(verb)
                        append(' ')
                        withStyle(strong) { append(item.title) }
                    },
                    style = NookTheme.type.sans(14, lineHeight = 20),
                    color = colors.textBody2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(kindText, style = NookTheme.type.sans(12, FontWeight.Medium, 16), color = kindColor)
                    item.rating?.let {
                        Dot()
                        CompactRating(ratingText(it))
                    }
                    agoLabel(activityOf(item))?.let {
                        Dot()
                        Text(it, style = NookTheme.type.xs, color = colors.textSubtle, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        if (inMyLibrary) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Icon(LucideIcons.Check, stringResource(Res.string.social_inYourList), Modifier.size(20.dp), tint = colors.emeraldText)
            }
        } else {
            IconGhostButton(
                LucideIcons.Plus,
                stringResource(if (item.kind == "book") Res.string.social_addToReadList else Res.string.social_addToList),
                onAdd,
                size = 18.dp,
                tint = colors.textStrong,
                background = colors.surfaceMuted,
                padding = 13.dp,
            )
        }
    }
}

@Composable
private fun Dot() {
    Text(" · ", style = NookTheme.type.xs, color = NookTheme.colors.textFaint)
}

// ── Profile grid ───────────────────────────────────────────────────────────

/** A poster of a friend's library: their rating or where they are, and a check when I have it too. */
@Composable
internal fun PosterTile(item: FriendItem, inMyLibrary: Boolean, loading: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = NookTheme.colors
    Box(modifier.fillMaxWidth().clip(NookShapes.xl).clickable(onClick = onOpen)) {
        MediaImage(item.imageUrl, item.title, Modifier.fillMaxWidth(), mode = modeOfKind(item.kind), placeholderIconSize = 26.dp)
        val rating = item.rating
        val badge: Pair<String, Pair<Color, Color>>? = when {
            rating != null -> "★ ${ratingText(rating)}" to (Palette.Amber300 to Palette.Amber900)
            item.status == "watching" || item.status == "reading" -> stringResource(Res.string.social_badgeNow) to (colors.accent(modeOfKind(item.kind)).fill.let { it to onFill(it) })
            item.status.startsWith("want") -> stringResource(if (item.kind == "book") Res.string.bookCard_wantToRead else Res.string.movieCard_wantToWatch) to (Palette.Gray200 to Palette.Gray900)
            else -> null
        }
        badge?.let { (text, tones) ->
            Text(
                text,
                style = NookTheme.type.sans(11, FontWeight.Bold, 14),
                color = tones.second,
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).clip(NookShapes.md).background(tones.first).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        if (inMyLibrary) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape).background(Palette.Emerald600),
                contentAlignment = Alignment.Center,
            ) {
                Icon(LucideIcons.Check, stringResource(Res.string.social_inYourList), Modifier.size(13.dp), tint = Palette.White)
            }
        }
        if (loading) OpeningVeil(Modifier.matchParentSize())
    }
}
