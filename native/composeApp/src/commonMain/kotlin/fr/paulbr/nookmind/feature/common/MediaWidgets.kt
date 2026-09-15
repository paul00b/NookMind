package fr.paulbr.nookmind.feature.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.ktor.http.encodeURLParameter
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.components.HairlineDivider
import fr.paulbr.nookmind.core.designsystem.components.OutlinedActionButton
import fr.paulbr.nookmind.core.designsystem.components.PersonImage
import fr.paulbr.nookmind.core.designsystem.components.Pill
import fr.paulbr.nookmind.core.designsystem.components.SkeletonBox
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.getRatingStyle
import fr.paulbr.nookmind.core.domain.parseDateOnly
import fr.paulbr.nookmind.core.domain.toFixed1
import fr.paulbr.nookmind.core.domain.todayLocal
import fr.paulbr.nookmind.core.model.TmdbCastMember
import fr.paulbr.nookmind.core.model.WatchProvider
import fr.paulbr.nookmind.core.model.WatchProvidersResult
import fr.paulbr.nookmind.core.network.ImdbApi
import fr.paulbr.nookmind.core.network.TmdbApi
import fr.paulbr.nookmind.core.platform.DateStyle
import fr.paulbr.nookmind.core.platform.formatLocalDate
import fr.paulbr.nookmind.core.platform.openExternalUrl
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.common_cancel
import fr.paulbr.nookmind.resources.common_ok
import fr.paulbr.nookmind.resources.common_trailer
import fr.paulbr.nookmind.resources.common_trailerNotFound
import fr.paulbr.nookmind.resources.watchProviders_availableOn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/** IMDb rating pill (`px-3 py-1 rounded-full font-bold`) with the colour scale of the web; opens IMDb when linked. */
@Composable
fun ImdbRatingPill(rating: Double?, imdbId: String?, loading: Boolean, modifier: Modifier = Modifier) {
    if (loading) {
        SkeletonBox(modifier.width(44.dp).height(28.dp), color = NookTheme.colors.surfaceMuted2)
        return
    }
    if (rating == null) return
    val style = getRatingStyle(rating)
    Pill(
        rating.toFixed1(),
        background = Color(style.background),
        color = Color(style.foreground),
        modifier = modifier.then(if (imdbId != null) Modifier.clip(NookShapes.full).clickable { openExternalUrl(ImdbApi.titleUrl(imdbId)) } else Modifier),
        textStyle = NookTheme.type.sans(14, FontWeight.Bold, 20),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    )
}

/**
 * `<input type="date">` replacement: shows the date in the device format and opens a Material date
 * picker (dates after today are not selectable, like the `max` attribute of the web inputs).
 */
@Composable
fun DateField(
    value: String?,
    onChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "—",
    compact: Boolean = false,
) {
    val colors = NookTheme.colors
    var open by remember { mutableStateOf(false) }
    val date = parseDateOnly(value)
    Row(
        modifier
            .clip(NookShapes.xl)
            .background(colors.surface, NookShapes.xl)
            .border(1.dp, colors.borderStrong, NookShapes.xl)
            .clickable { open = true }
            .padding(horizontal = 16.dp, vertical = if (compact) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(LucideIcons.CalendarDays, null, Modifier.size(15.dp), tint = colors.textFaint)
        Text(
            date?.let { formatLocalDate(it, DateStyle.NUMERIC) } ?: placeholder,
            style = if (compact) NookTheme.type.sm else NookTheme.type.base,
            color = if (date != null) colors.textBody else colors.textFaint,
        )
    }
    if (open) {
        val todayMillis = todayLocal().atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: todayLocal()).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    open = false
                    val picked = state.selectedDateMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date }
                    onChange(picked?.toString())
                }) { Text(stringResource(Res.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(Res.string.common_cancel)) } },
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }
}

/** Cast accordion of the movie / series sheets: "Cast (N)" header, chevron, horizontal cast cards. */
@Composable
fun CastAccordion(cast: List<TmdbCastMember>, label: String, onSelect: (personId: Int) -> Unit, modifier: Modifier = Modifier) {
    if (cast.isEmpty()) return
    val colors = NookTheme.colors
    var open by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (open) 180f else 0f, label = "chevron")
    Column(modifier.fillMaxWidth().clip(NookShapes.xl).border(1.dp, colors.divider, NookShapes.xl)) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("$label (${cast.size})", style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = colors.textBody2, modifier = Modifier.weight(1f))
            Icon(LucideIcons.ChevronDown, null, Modifier.size(16.dp).rotate(rotation), tint = colors.textFaint)
        }
        AnimatedVisibility(open) {
            Column {
                HairlineDivider()
                LazyRow(
                    Modifier.padding(vertical = 16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(cast, key = { it.id }) { person -> CastCard(person, onClick = { onSelect(person.id) }) }
                }
            }
        }
    }
}

@Composable
private fun CastCard(person: TmdbCastMember, onClick: () -> Unit) {
    Column(Modifier.width(96.dp).clip(NookShapes.lg).clickable(onClick = onClick)) {
        PersonImage(TmdbApi.posterUrl(person.profilePath), person.name, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(person.name, style = NookTheme.type.sans(12, FontWeight.SemiBold, 14), color = NookTheme.colors.textBody, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (!person.character.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(person.character, style = NookTheme.type.sans(11, lineHeight = 13), color = NookTheme.colors.textSubtle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

// Fallback search URLs that open the provider's app through universal links (WatchProviders.tsx).
private val PROVIDER_SEARCH_URLS: Map<Int, (String) -> String> = mapOf(
    8 to { t: String -> "https://www.netflix.com/search?q=${encode(t)}" },
    1796 to { t: String -> "https://www.netflix.com/search?q=${encode(t)}" },
    9 to { t: String -> "https://www.primevideo.com/search?phrase=${encode(t)}" },
    119 to { t: String -> "https://www.primevideo.com/search?phrase=${encode(t)}" },
    337 to { t: String -> "https://www.disneyplus.com/search?q=${encode(t)}" },
    350 to { t: String -> "https://tv.apple.com/search?term=${encode(t)}" },
    381 to { t: String -> "https://www.canalplus.com/recherche/${encode(t)}" },
    56 to { t: String -> "https://www.canalplus.com/recherche/${encode(t)}" },
    236 to { t: String -> "https://www.paramountplus.com/search?q=${encode(t)}" },
    283 to { t: String -> "https://www.crunchyroll.com/search?q=${encode(t)}" },
    1899 to { t: String -> "https://www.max.com/search?q=${encode(t)}" },
    1825 to { t: String -> "https://www.max.com/search?q=${encode(t)}" },
)

private fun encode(value: String): String = value.encodeURLParameter()

private fun fallbackUrl(provider: WatchProvider, title: String, tmdbLink: String?): String? =
    PROVIDER_SEARCH_URLS[provider.providerId]?.invoke(title) ?: tmdbLink

/** Port of WatchProviders.tsx: streaming logos (36 dp) that deep-link into the provider apps. */
@Composable
fun WatchProvidersRow(container: AppContainer, providers: WatchProvidersResult?, title: String, loading: Boolean, modifier: Modifier = Modifier) {
    var deepLinks by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(providers?.link, providers?.flatrate?.size) {
        val link = providers?.link ?: return@LaunchedEffect
        if (providers.flatrate.isEmpty()) return@LaunchedEffect
        deepLinks = container.tmdb.fetchWatchProviderDeepLinks(link)
    }
    if (loading) {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { SkeletonBox(Modifier.size(36.dp), shape = NookShapes.lg, color = NookTheme.colors.surfaceMuted2) }
        }
        return
    }
    if (providers == null || providers.flatrate.isEmpty()) return
    Column(modifier) {
        Text(stringResource(Res.string.watchProviders_availableOn), style = NookTheme.type.sm, color = NookTheme.colors.textSubtle)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            providers.flatrate.forEach { provider ->
                val href = deepLinks[provider.providerId.toString()] ?: fallbackUrl(provider, title, providers.link)
                Box(
                    Modifier.size(36.dp).clip(NookShapes.lg).background(NookTheme.colors.surfaceMuted)
                        .then(if (href != null) Modifier.clickable { openExternalUrl(href) } else Modifier),
                ) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w92${provider.logoPath}",
                        contentDescription = provider.providerName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}

/** "Watch trailer" outlined button: fetches the best YouTube key then opens the player. */
@Composable
fun TrailerButton(container: AppContainer, type: String, tmdbId: Int, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var key by remember { mutableStateOf<String?>(null) }
    OutlinedActionButton(
        text = stringResource(Res.string.common_trailer),
        onClick = {
            if (loading) return@OutlinedActionButton
            loading = true
            scope.launch {
                val found = container.tmdb.fetchTrailerKey(type, tmdbId)
                loading = false
                if (found != null) key = found else container.toasts.error(Res.string.common_trailerNotFound)
            }
        },
        modifier = modifier.fillMaxWidth(),
        icon = LucideIcons.Play,
        loading = loading,
    )
    key?.let { TrailerDialog(it, onClose = { key = null }) }
}

/** Runtime-agnostic container of the YouTube trailer player (WebView on Android). */
@Composable
expect fun YouTubeEmbed(videoKey: String, modifier: Modifier)

/** Port of TrailerModal.tsx: black 90 % backdrop, 16:9 player, close cross above it. */
@Composable
fun TrailerDialog(videoKey: String, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true),
    ) {
        Box(Modifier.fillMaxSize().background(Palette.Black.copy(alpha = 0.9f)).clickable(onClick = onClose).padding(16.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxWidth().widthIn(max = 672.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Icon(LucideIcons.X, null, Modifier.size(24.dp).clickable(onClick = onClose), tint = Palette.White.copy(alpha = 0.7f))
                }
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(NookShapes.xl).background(Palette.Black).clickable(enabled = false) {}) {
                    YouTubeEmbed(videoKey, Modifier.fillMaxSize())
                }
            }
        }
    }
}

/** Day part of a `YYYY-MM-DD` value as a LocalDate, or null. */
fun isoToLocalDate(value: String?): LocalDate? = parseDateOnly(value)
