package fr.paulbr.nookmind.feature.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.data.AuthState
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.accent
import fr.paulbr.nookmind.core.designsystem.components.Avatar
import fr.paulbr.nookmind.core.designsystem.components.AvatarSize
import fr.paulbr.nookmind.core.designsystem.components.modeIcon
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.common_defaultDisplayName
import fr.paulbr.nookmind.resources.logo
import fr.paulbr.nookmind.resources.nav_books
import fr.paulbr.nookmind.resources.nav_discover
import fr.paulbr.nookmind.resources.nav_home
import fr.paulbr.nookmind.resources.nav_library
import fr.paulbr.nookmind.resources.nav_movies
import fr.paulbr.nookmind.resources.nav_series
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Order of the mode toggle everywhere: Series, Movies, Books. */
val MODE_TOGGLE_ORDER = listOf(MediaMode.SERIES, MediaMode.MOVIES, MediaMode.BOOKS)

@Composable
fun modeLabel(mode: MediaMode): String = when (mode) {
    MediaMode.BOOKS -> stringResource(Res.string.nav_books)
    MediaMode.MOVIES -> stringResource(Res.string.nav_movies)
    MediaMode.SERIES -> stringResource(Res.string.nav_series)
}

@Composable
fun tabLabel(tab: MainTab): String = when (tab) {
    MainTab.SEARCH -> stringResource(Res.string.nav_home)
    MainTab.LIBRARY -> stringResource(Res.string.nav_library)
    MainTab.NEXT_UP -> stringResource(Res.string.nav_discover)
}

fun tabIcon(tab: MainTab): ImageVector = when (tab) {
    MainTab.SEARCH -> LucideIcons.Search
    MainTab.LIBRARY -> LucideIcons.Library
    MainTab.NEXT_UP -> LucideIcons.Compass
}

/**
 * Frosted glass of the floating navigation: the content behind is blurred and tinted, instead of
 * the flat `bg-white/85` of the web app, which has no equivalent in Compose.
 *
 * `noiseFactor` is zero on purpose. Haze can sprinkle grain over the blur, and that grain is what
 * made the ambiance look sandblasted before it was removed.
 *
 * `fallbackTint` is what Android below API 31 gets: there is no RenderEffect there, so the pill
 * falls back to the opaque-ish tint the app used before. Everywhere else it is a real blur.
 */
@Composable
private fun frostedPill(): HazeStyle {
    val surface = NookTheme.colors.surface
    return HazeStyle(
        backgroundColor = surface,
        tints = listOf(HazeTint(surface.copy(alpha = if (NookTheme.colors.isDark) 0.42f else 0.45f))),
        blurRadius = 32.dp,
        noiseFactor = 0f,
        fallbackTint = HazeTint(surface.copy(alpha = 0.92f)),
    )
}

/** Port of BottomNav.tsx: mode pill on top of the three-tab pill, floating 16 dp above the bottom. */
@Composable
fun BottomNav(
    mode: MediaMode,
    onMode: (MediaMode) -> Unit,
    tab: MainTab,
    onTab: (MainTab) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val colors = NookTheme.colors
    val frosted = frostedPill()
    Column(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Mode toggle pill
        Row(
            Modifier
                .shadow(6.dp, NookShapes.full, ambientColor = Palette.Black.copy(alpha = 0.15f), spotColor = Palette.Black.copy(alpha = 0.15f))
                .clip(NookShapes.full)
                .hazeEffect(hazeState, frosted)
                .border(1.dp, colors.border, NookShapes.full)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MODE_TOGGLE_ORDER.forEach { m ->
                val active = m == mode
                val accent = colors.accent(m)
                val bg by animateColorAsState(if (active) accent.base else Color.Transparent, label = "modeBg")
                Row(
                    Modifier
                        .clip(NookShapes.full)
                        .background(bg, NookShapes.full)
                        .clickable { onMode(m) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(modeIcon(m), null, Modifier.size(12.dp), tint = if (active) Palette.White else colors.textSubtle)
                    Text(
                        modeLabel(m),
                        style = NookTheme.type.sans(12, FontWeight.SemiBold, 16),
                        color = if (active) Palette.White else colors.textSubtle,
                    )
                }
            }
        }

        // Main tabs pill
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(10.dp, NookShapes.full, ambientColor = Palette.Black.copy(alpha = 0.2f), spotColor = Palette.Black.copy(alpha = 0.2f))
                .clip(NookShapes.full)
                .hazeEffect(hazeState, frosted)
                .border(1.dp, colors.border, NookShapes.full),
        ) {
            MainTab.entries.forEach { t ->
                val active = t == tab
                val scale by animateFloatAsState(if (active) 1.1f else 1f, label = "tabScale")
                val tint = if (active) colors.amberText else colors.textSubtle
                Column(
                    Modifier.weight(1f).clickable { onTab(t) }.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(tabIcon(t), null, Modifier.size(22.dp).scale(scale), tint = tint)
                    Text(tabLabel(t), style = NookTheme.type.sans(10, FontWeight.Medium, 12), color = tint, maxLines = 1)
                }
            }
        }
    }
}

/** Port of MobileTopBar.tsx: logo + wordmark on the left, avatar (opens Settings) on the right. */
@Composable
fun MobileTopBar(container: AppContainer, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val authState by container.auth.state.collectAsState()
    val user = (authState as? AuthState.SignedIn)?.user
    val name = user?.displayName(stringResource(Res.string.common_defaultDisplayName)) ?: stringResource(Res.string.common_defaultDisplayName)
    Row(
        // No background: the mode halo drawn behind the whole scaffold shows through the header.
        // The web app keeps an opaque bar here, this is a deliberate difference.
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(painterResource(Res.drawable.logo), contentDescription = null, modifier = Modifier.size(28.dp))
            Text("NookMind", style = NookTheme.type.titleSerif, color = NookTheme.colors.textStrong)
        }
        Box(Modifier.clip(NookShapes.full).clickable(onClick = onOpenSettings)) {
            Avatar(name = name, imageUrl = user?.avatarUrl, size = AvatarSize.SM)
        }
    }
}

/** Port of Sidebar.tsx (tablet / desktop widths). */
@Composable
fun Sidebar(
    container: AppContainer,
    tab: MainTab,
    onTab: (MainTab) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NookTheme.colors
    val mode by container.prefs.mediaMode.collectAsState()
    val authState by container.auth.state.collectAsState()
    val user = (authState as? AuthState.SignedIn)?.user
    val fallback = stringResource(Res.string.common_defaultDisplayName)
    val name = user?.displayName(fallback) ?: fallback
    val accent = colors.accent(mode)
    val borderColor = colors.border

    Column(
        modifier
            .sidebarWidth()
            .fillMaxHeight()
            .background(colors.background)
            .drawBehind {
                // `border-r border-black/8 dark:border-white/8`
                val stroke = 1.dp.toPx()
                drawRect(
                    color = borderColor,
                    topLeft = androidx.compose.ui.geometry.Offset(size.width - stroke, 0f),
                    size = androidx.compose.ui.geometry.Size(stroke, size.height),
                )
            }
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painterResource(Res.drawable.logo), contentDescription = null, modifier = Modifier.size(32.dp))
            Text("NookMind", style = NookTheme.type.serif(20), color = colors.textStrong)
        }
        Spacer(Modifier.height(24.dp))

        // Mode toggle (segmented)
        Row(
            Modifier.fillMaxWidth().clip(NookShapes.xl).background(if (colors.isDark) Palette.Gray800.copy(alpha = 0.6f) else Palette.Gray100, NookShapes.xl).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MODE_TOGGLE_ORDER.forEach { m ->
                val active = m == mode
                val a = colors.accent(m)
                Row(
                    Modifier
                        .weight(1f)
                        .clip(NookShapes.lg)
                        .background(if (active) colors.surface else Color.Transparent, NookShapes.lg)
                        .clickable { container.prefs.setMediaMode(m) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(modeIcon(m), null, Modifier.size(13.dp), tint = if (active) a.text else colors.textSubtle)
                    Spacer(Modifier.size(6.dp))
                    Text(modeLabel(m), style = NookTheme.type.sans(12, FontWeight.SemiBold, 16), color = if (active) a.text else colors.textSubtle)
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        // Nav links
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MainTab.entries.forEach { t ->
                val active = t == tab
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(NookShapes.xl)
                        .background(if (active) accent.subtle else Color.Transparent, NookShapes.xl)
                        .clickable { onTab(t) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(tabIcon(t), null, Modifier.size(18.dp), tint = if (active) accent.text else colors.textMuted)
                    Text(tabLabel(t), style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = if (active) accent.text else colors.textMuted)
                }
            }
        }

        // User + settings
        Row(
            Modifier.fillMaxWidth().clip(NookShapes.xl).clickable(onClick = onOpenSettings).padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(name = name, imageUrl = user?.avatarUrl, size = AvatarSize.SM)
            Column(Modifier.weight(1f)) {
                Text(name, style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = colors.textStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(user?.email ?: "", style = NookTheme.type.xs, color = colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(LucideIcons.Settings, null, Modifier.size(15.dp), tint = colors.textFaint)
        }
    }
}
