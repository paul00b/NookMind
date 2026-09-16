package fr.paulbr.nookmind.feature.settings

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.data.AuthState
import fr.paulbr.nookmind.core.data.PushEnableResult
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.alpha
import fr.paulbr.nookmind.core.designsystem.components.Avatar
import fr.paulbr.nookmind.core.designsystem.components.AvatarSize
import fr.paulbr.nookmind.core.designsystem.components.GhostButton
import fr.paulbr.nookmind.core.designsystem.components.HairlineDivider
import fr.paulbr.nookmind.core.designsystem.components.IconGhostButton
import fr.paulbr.nookmind.core.designsystem.components.NookCard
import fr.paulbr.nookmind.core.designsystem.components.NookTextField
import fr.paulbr.nookmind.core.designsystem.components.NookToggle
import fr.paulbr.nookmind.core.designsystem.components.OverlineLabel
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.TextLink
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.ThemeMode
import fr.paulbr.nookmind.feature.legal.LegalKind
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.common_defaultDisplayName
import fr.paulbr.nookmind.resources.nav_books
import fr.paulbr.nookmind.resources.nav_movies
import fr.paulbr.nookmind.resources.nav_series
import fr.paulbr.nookmind.resources.settings_about
import fr.paulbr.nookmind.resources.settings_app
import fr.paulbr.nookmind.resources.settings_appearance
import fr.paulbr.nookmind.resources.settings_cacheCleared
import fr.paulbr.nookmind.resources.settings_cacheRefreshing
import fr.paulbr.nookmind.resources.settings_cancel
import fr.paulbr.nookmind.resources.settings_clearCache
import fr.paulbr.nookmind.resources.settings_dark
import fr.paulbr.nookmind.resources.settings_deleteAccount
import fr.paulbr.nookmind.resources.settings_displayNameSaved
import fr.paulbr.nookmind.resources.settings_enableNotifications
import fr.paulbr.nookmind.resources.settings_haptics
import fr.paulbr.nookmind.resources.settings_hapticsHelp
import fr.paulbr.nookmind.resources.settings_light
import fr.paulbr.nookmind.resources.settings_notifActivate
import fr.paulbr.nookmind.resources.settings_notifActive
import fr.paulbr.nookmind.resources.settings_notifDeactivate
import fr.paulbr.nookmind.resources.settings_notifDisabled
import fr.paulbr.nookmind.resources.settings_notifEnabled
import fr.paulbr.nookmind.resources.settings_notifEpisodes
import fr.paulbr.nookmind.resources.settings_notifMovies
import fr.paulbr.nookmind.resources.settings_notifNotSupported
import fr.paulbr.nookmind.resources.settings_notifPermissionDenied
import fr.paulbr.nookmind.resources.settings_notifSeasons
import fr.paulbr.nookmind.resources.settings_notifTestNobody
import fr.paulbr.nookmind.resources.settings_notifTestRejected
import fr.paulbr.nookmind.resources.settings_notifTestSent
import fr.paulbr.nookmind.resources.settings_notifTestUnavailable
import fr.paulbr.nookmind.resources.settings_notifTokenError
import fr.paulbr.nookmind.resources.settings_notifications
import fr.paulbr.nookmind.resources.settings_privacyPolicy
import fr.paulbr.nookmind.resources.settings_profile
import fr.paulbr.nookmind.resources.settings_replayOnboarding
import fr.paulbr.nookmind.resources.settings_save
import fr.paulbr.nookmind.resources.settings_searchSections
import fr.paulbr.nookmind.resources.settings_searchSectionsHelp
import fr.paulbr.nookmind.resources.settings_searchSectionsReset
import fr.paulbr.nookmind.resources.settings_searchSectionsTitle
import fr.paulbr.nookmind.resources.settings_sections_books_lastRead
import fr.paulbr.nookmind.resources.settings_sections_books_wantToRead
import fr.paulbr.nookmind.resources.settings_sections_movies_lastWatched
import fr.paulbr.nookmind.resources.settings_sections_movies_trending
import fr.paulbr.nookmind.resources.settings_sections_movies_wantToWatch
import fr.paulbr.nookmind.resources.settings_sections_series_lastWatched
import fr.paulbr.nookmind.resources.settings_sections_series_trending
import fr.paulbr.nookmind.resources.settings_sections_series_waiting
import fr.paulbr.nookmind.resources.settings_sections_series_wantToWatch
import fr.paulbr.nookmind.resources.settings_sections_series_watching
import fr.paulbr.nookmind.resources.settings_signOut
import fr.paulbr.nookmind.resources.settings_system
import fr.paulbr.nookmind.resources.settings_tagline
import fr.paulbr.nookmind.resources.settings_terms
import fr.paulbr.nookmind.resources.settings_testNotifications
import fr.paulbr.nookmind.resources.settings_testNotificationsNoToken
import fr.paulbr.nookmind.resources.settings_testNotificationsRetry
import fr.paulbr.nookmind.resources.settings_testNotificationsSending
import fr.paulbr.nookmind.resources.settings_testNotificationsSent
import fr.paulbr.nookmind.resources.settings_theme
import fr.paulbr.nookmind.resources.settings_title
import fr.paulbr.nookmind.resources.settings_version
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Version shown in the About section (`APP_VERSION` of SettingsPanel.tsx, bumped for the native app). */
const val APP_VERSION = "2.0.0"

private enum class TestState { IDLE, LOADING, SUCCESS, ERROR }

/** Port of SettingsPanel.tsx: right slide-in panel with profile, theme, sections, notifications and about. */
@Composable
fun SettingsPanel(
    container: AppContainer,
    onClose: () -> Unit,
    onOpenLegal: (LegalKind) -> Unit,
    onReplayOnboarding: () -> Unit,
) {
    val colors = NookTheme.colors
    val scope = rememberCoroutineScope()
    val rawHaptics = LocalHapticFeedback.current
    val authState by container.auth.state.collectAsState()
    val user = (authState as? AuthState.SignedIn)?.user
    val fallbackName = stringResource(Res.string.common_defaultDisplayName)
    val theme by container.prefs.theme.collectAsState()
    val hapticsEnabled by container.prefs.hapticsEnabled.collectAsState()
    val currentMode by container.prefs.mediaMode.collectAsState()

    var editingName by remember { mutableStateOf(false) }
    var displayName by remember(user?.id) { mutableStateOf(user?.displayName(fallbackName) ?: fallbackName) }
    var deleteOpen by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var sectionMode by remember { mutableStateOf(currentMode) }
    var testState by remember { mutableStateOf(TestState.IDLE) }
    var testMessage by remember { mutableStateOf("") }

    val subscribed by container.push.subscribed.collectAsState()
    val pushLoading by container.push.loading.collectAsState()
    val preferences by container.push.preferences.collectAsState()
    LaunchedEffect(Unit) { container.push.refresh() }

    val cacheClearedText = stringResource(Res.string.settings_cacheCleared)
    val nameSavedText = stringResource(Res.string.settings_displayNameSaved)
    val notifEnabledText = stringResource(Res.string.settings_notifEnabled)
    val notifDisabledText = stringResource(Res.string.settings_notifDisabled)
    val permissionDeniedText = stringResource(Res.string.settings_notifPermissionDenied)
    val tokenErrorText = stringResource(Res.string.settings_notifTokenError)
    val sendingText = stringResource(Res.string.settings_testNotificationsSending)
    val testSentText = stringResource(Res.string.settings_notifTestSent)
    val testRejectedText = stringResource(Res.string.settings_notifTestRejected)
    val testNobodyText = stringResource(Res.string.settings_notifTestNobody)
    val testUnavailableText = stringResource(Res.string.settings_notifTestUnavailable)
    val testNoTokenText = stringResource(Res.string.settings_testNotificationsNoToken)

    BackHandler(enabled = true, onBack = onClose)

    Box(Modifier.fillMaxSize()) {
        // Backdrop
        Box(Modifier.fillMaxSize().background(Palette.Black.alpha(0.4f)).clickable(onClick = onClose))
        // Panel
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth()
                .widthIn(max = 384.dp)
                .background(if (colors.isDark) Palette.NightCard else Palette.Cream)
                .clickable(enabled = false) {}
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 24.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.settings_title), style = NookTheme.type.h3Serif, color = colors.textStrong, modifier = Modifier.weight(1f))
                IconGhostButton(LucideIcons.X, contentDescription = null, onClick = onClose, size = 18.dp)
            }
            HairlineDivider()

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                // ── Profile ──────────────────────────────────────────────
                SettingsSection(stringResource(Res.string.settings_profile)) {
                    NookCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Avatar(name = displayName, imageUrl = user?.avatarUrl, size = AvatarSize.LG)
                            if (editingName) {
                                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    NookTextField(
                                        displayName, { displayName = it },
                                        textStyle = NookTheme.type.sm, textAlign = TextAlign.Center,
                                        imeAction = ImeAction.Done,
                                        keyboardActions = KeyboardActions(onDone = {
                                            editingName = false
                                            scope.launch { container.auth.updateDisplayName(displayName.trim()) }
                                            container.toasts.success(nameSavedText)
                                        }),
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        PrimaryButton(
                                            stringResource(Res.string.settings_save),
                                            onClick = {
                                                editingName = false
                                                scope.launch { container.auth.updateDisplayName(displayName.trim()) }
                                                container.toasts.success(nameSavedText)
                                            },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                        )
                                        GhostButton(
                                            stringResource(Res.string.settings_cancel),
                                            onClick = { editingName = false; displayName = user?.displayName(fallbackName) ?: fallbackName },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        displayName,
                                        style = NookTheme.type.sans(16, FontWeight.SemiBold, 24),
                                        color = colors.textStrong,
                                        modifier = Modifier.clip(NookShapes.sm).clickable { editingName = true }.padding(horizontal = 4.dp),
                                    )
                                    user?.email?.let {
                                        Spacer(Modifier.height(2.dp))
                                        Text(it, style = NookTheme.type.sm, color = colors.textSubtle)
                                    }
                                }
                            }
                            GhostButton(
                                stringResource(Res.string.settings_signOut),
                                onClick = { scope.launch { container.auth.signOut(); onClose() } },
                                modifier = Modifier.fillMaxWidth(),
                                color = Palette.Red500,
                            )
                            GhostButton(
                                stringResource(Res.string.settings_deleteAccount),
                                onClick = { deleteOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                                color = colors.redText,
                                borderColor = Palette.Red500.alpha(0.3f),
                            )
                        }
                    }
                }

                // ── Appearance ───────────────────────────────────────────
                SettingsSection(stringResource(Res.string.settings_appearance)) {
                    NookCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        Text(stringResource(Res.string.settings_theme), style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = colors.textBody2)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                Triple(ThemeMode.LIGHT, stringResource(Res.string.settings_light), LucideIcons.Sun),
                                Triple(ThemeMode.DARK, stringResource(Res.string.settings_dark), LucideIcons.Moon),
                                Triple(ThemeMode.SYSTEM, stringResource(Res.string.settings_system), LucideIcons.Monitor),
                            ).forEach { (value, label, icon) ->
                                OptionTile(label, icon, selected = theme == value, onClick = { container.prefs.setTheme(value) }, modifier = Modifier.weight(1f), stacked = true)
                            }
                        }
                    }
                }

                // ── Vibrations ───────────────────────────────────────────
                SettingsSection(stringResource(Res.string.settings_haptics)) {
                    NookCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        SettingToggleRow(
                            icon = null,
                            label = stringResource(Res.string.settings_hapticsHelp),
                            checked = hapticsEnabled,
                            onChange = { enabled ->
                                container.prefs.setHapticsEnabled(enabled)
                                // LocalNookHaptics still holds the no-op this frame (it only picks up
                                // the new pref on the next composition), so confirm directly via the
                                // raw platform API — enabling is exactly the moment proof is wanted,
                                // and this is the one place that should reach past the NookHaptics
                                // vocabulary to do it.
                                if (enabled) rawHaptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            },
                        )
                    }
                }

                // ── Search sections ──────────────────────────────────────
                SettingsSection(stringResource(Res.string.settings_searchSections)) {
                    NookCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                Text(stringResource(Res.string.settings_searchSectionsTitle), style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = colors.textBody2)
                                Spacer(Modifier.height(4.dp))
                                Text(stringResource(Res.string.settings_searchSectionsHelp), style = NookTheme.type.xs, color = colors.textSubtle)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    Triple(MediaMode.BOOKS, stringResource(Res.string.nav_books), LucideIcons.BookOpen),
                                    Triple(MediaMode.MOVIES, stringResource(Res.string.nav_movies), LucideIcons.Film),
                                    Triple(MediaMode.SERIES, stringResource(Res.string.nav_series), LucideIcons.Tv),
                                ).forEach { (value, label, icon) ->
                                    OptionTile(label, icon, selected = sectionMode == value, onClick = { sectionMode = value }, modifier = Modifier.weight(1f))
                                }
                            }
                            val sections by container.prefs.searchSections(sectionMode).collectAsState()
                            SectionOrderEditor(
                                sections = sections,
                                labelOf = { id -> sectionLabel(sectionMode, id) },
                                onMove = { id, index -> container.prefs.moveSearchSection(sectionMode, id, index) },
                                onToggleVisible = { id -> container.prefs.toggleSearchSectionVisibility(sectionMode, id) },
                            )
                            GhostButton(
                                stringResource(Res.string.settings_searchSectionsReset),
                                onClick = { container.prefs.resetSearchSections(sectionMode) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                // ── Notifications ────────────────────────────────────────
                SettingsSection(stringResource(Res.string.settings_notifications)) {
                    NookCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        if (!container.push.isSupported) {
                            Text(
                                stringResource(Res.string.settings_notifNotSupported),
                                style = NookTheme.type.sm, color = colors.textSubtle,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = TextAlign.Center,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(LucideIcons.Bell, null, Modifier.size(15.dp), tint = if (subscribed) Palette.Teal500 else colors.textFaint)
                                    Spacer(Modifier.size(10.dp))
                                    Text(
                                        if (subscribed) stringResource(Res.string.settings_notifActive) else stringResource(Res.string.settings_enableNotifications),
                                        style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = colors.textBody, modifier = Modifier.weight(1f),
                                    )
                                    val label = when {
                                        pushLoading -> "…"
                                        subscribed -> stringResource(Res.string.settings_notifDeactivate)
                                        else -> stringResource(Res.string.settings_notifActivate)
                                    }
                                    Box(
                                        Modifier
                                            .clip(NookShapes.full)
                                            .background(if (subscribed) Palette.Teal500.alpha(0.10f) else Palette.Teal500, NookShapes.full)
                                            .clickable(enabled = !pushLoading) {
                                                scope.launch {
                                                    if (subscribed) {
                                                        if (container.push.disable()) container.toasts.success(notifDisabledText)
                                                    } else {
                                                        when (val result = container.push.enable()) {
                                                            PushEnableResult.Enabled -> container.toasts.success(notifEnabledText)
                                                            PushEnableResult.PermissionDenied -> container.toasts.error(permissionDeniedText)
                                                            PushEnableResult.TokenUnavailable -> container.toasts.error(tokenErrorText)
                                                            is PushEnableResult.Failed -> container.toasts.error(result.message ?: tokenErrorText)
                                                        }
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text(label, style = NookTheme.type.sans(12, FontWeight.SemiBold, 16), color = if (subscribed) colors.tealText else Palette.White)
                                    }
                                }
                                if (subscribed) {
                                    HairlineDivider()
                                    SettingToggleRow(LucideIcons.Tv, stringResource(Res.string.settings_notifEpisodes), preferences.notifyEpisodes) { value ->
                                        scope.launch { container.push.updatePreferences { it.copy(notifyEpisodes = value) } }
                                    }
                                    SettingToggleRow(LucideIcons.Clapperboard, stringResource(Res.string.settings_notifSeasons), preferences.notifySeasons) { value ->
                                        scope.launch { container.push.updatePreferences { it.copy(notifySeasons = value) } }
                                    }
                                    SettingToggleRow(LucideIcons.Film, stringResource(Res.string.settings_notifMovies), preferences.notifyMovies) { value ->
                                        scope.launch { container.push.updatePreferences { it.copy(notifyMovies = value) } }
                                    }
                                }
                            }
                        }
                    }
                    if (subscribed) {
                        Spacer(Modifier.height(12.dp))
                        val (bg, border, fg) = when (testState) {
                            TestState.SUCCESS -> Triple(Palette.Teal500.alpha(0.10f), Palette.Teal500.alpha(0.3f), if (colors.isDark) Palette.Teal300 else Palette.Teal700)
                            TestState.ERROR -> Triple(Palette.Red500.alpha(0.10f), Palette.Red500.alpha(0.3f), if (colors.isDark) Palette.Red300 else Palette.Red700)
                            else -> Triple(if (colors.isDark) Palette.Night else Palette.White, colors.border, colors.textBody)
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(NookShapes.xl)
                                .background(bg, NookShapes.xl)
                                .border(1.dp, border, NookShapes.xl)
                                .clickable(enabled = testState != TestState.LOADING && !pushLoading) {
                                    testState = TestState.LOADING
                                    testMessage = sendingText
                                    scope.launch {
                                        val result = container.push.sendTest(testSentText, testRejectedText, testNobodyText, testUnavailableText, testNoTokenText)
                                        testState = if (result.ok) TestState.SUCCESS else TestState.ERROR
                                        testMessage = result.message
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        ) {
                            Icon(LucideIcons.Send, null, Modifier.size(14.dp), tint = fg)
                            Text(
                                when (testState) {
                                    TestState.LOADING -> sendingText
                                    TestState.SUCCESS -> stringResource(Res.string.settings_testNotificationsSent)
                                    TestState.ERROR -> stringResource(Res.string.settings_testNotificationsRetry)
                                    TestState.IDLE -> stringResource(Res.string.settings_testNotifications)
                                },
                                style = NookTheme.type.sans(14, FontWeight.SemiBold, 20), color = fg,
                            )
                        }
                        if (testState != TestState.IDLE && testMessage.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(testMessage, style = NookTheme.type.xs, color = fg)
                        }
                    }
                }

                // ── About ────────────────────────────────────────────────
                SettingsSection(stringResource(Res.string.settings_about)) {
                    NookCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AboutRow(stringResource(Res.string.settings_version), APP_VERSION)
                            AboutRow(stringResource(Res.string.settings_app), "NookMind")
                            Text(
                                stringResource(Res.string.settings_tagline),
                                style = NookTheme.type.xs.copy(fontStyle = FontStyle.Italic),
                                color = colors.textFaint,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center,
                            )
                            HairlineDivider(Modifier.padding(top = 8.dp))
                            val spin = rememberInfiniteTransition(label = "refresh")
                            val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(1000)), label = "angle")
                            ActionRow(
                                icon = LucideIcons.RefreshCw,
                                text = if (refreshing) stringResource(Res.string.settings_cacheRefreshing) else stringResource(Res.string.settings_clearCache),
                                iconModifier = if (refreshing) Modifier.graphicsLayer { rotationZ = angle } else Modifier,
                                enabled = !refreshing,
                                onClick = {
                                    refreshing = true
                                    container.clearCaches()
                                    scope.launch {
                                        delay(800)
                                        refreshing = false
                                        container.toasts.success(cacheClearedText)
                                    }
                                },
                            )
                            HairlineDivider()
                            ActionRow(LucideIcons.RotateCcw, stringResource(Res.string.settings_replayOnboarding), onClick = onReplayOnboarding)
                        }
                    }
                }

                // ── Legal ────────────────────────────────────────────────
                NookCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                        TextLink(stringResource(Res.string.settings_privacyPolicy), onClick = { onOpenLegal(LegalKind.PRIVACY) }, color = colors.textFaint)
                        Text("·", style = NookTheme.type.xs, color = colors.textFaint)
                        TextLink(stringResource(Res.string.settings_terms), onClick = { onOpenLegal(LegalKind.TERMS) }, color = colors.textFaint)
                    }
                }
            }
        }
    }

    if (deleteOpen) DeleteAccountDialog(container, onClose = { deleteOpen = false }, onDeleted = onClose)
}

/** Localized label of one search section (`settings.sections.<mode>.<id>`). */
@Composable
fun sectionLabel(mode: MediaMode, id: String): String = when (mode) {
    MediaMode.BOOKS -> when (id) {
        "want_to_read" -> stringResource(Res.string.settings_sections_books_wantToRead)
        else -> stringResource(Res.string.settings_sections_books_lastRead)
    }
    MediaMode.MOVIES -> when (id) {
        "trending" -> stringResource(Res.string.settings_sections_movies_trending)
        "want_to_watch" -> stringResource(Res.string.settings_sections_movies_wantToWatch)
        else -> stringResource(Res.string.settings_sections_movies_lastWatched)
    }
    MediaMode.SERIES -> when (id) {
        "trending" -> stringResource(Res.string.settings_sections_series_trending)
        "watching" -> stringResource(Res.string.settings_sections_series_watching)
        "waiting" -> stringResource(Res.string.settings_sections_series_waiting)
        "want_to_watch" -> stringResource(Res.string.settings_sections_series_wantToWatch)
        else -> stringResource(Res.string.settings_sections_series_lastWatched)
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        OverlineLabel(title, color = NookTheme.colors.textFaint)
        Spacer(Modifier.height(16.dp))
        content()
    }
}

/** Selectable tile of the theme / mode rows (`rounded-xl border` with an amber active state). */
@Composable
private fun OptionTile(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, stacked: Boolean = false) {
    val colors = NookTheme.colors
    val content = if (selected) colors.amberText else colors.textMuted
    val shape = NookShapes.xl
    val base = modifier
        .clip(shape)
        .background(if (selected) Palette.Amber500.alpha(0.10f) else Color.Transparent, shape)
        .border(1.dp, if (selected) Palette.Amber500 else colors.border, shape)
        .clickable(onClick = onClick)
    if (stacked) {
        Column(base.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, Modifier.size(14.dp), tint = content)
            Text(label, style = NookTheme.type.sans(12, FontWeight.Medium, 16), color = content, maxLines = 1)
        }
    } else {
        Row(base.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(14.dp), tint = content)
            Text(label, style = NookTheme.type.sans(12, FontWeight.Medium, 16), color = content, maxLines = 1)
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector?,
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = NookTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(14.dp), tint = Palette.Teal500)
            Spacer(Modifier.size(10.dp))
        }
        Text(label, style = NookTheme.type.sm, color = colors.textBody2, modifier = Modifier.weight(1f))
        NookToggle(checked, onChange)
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = NookTheme.type.sm, color = NookTheme.colors.textMuted, modifier = Modifier.weight(1f))
        Text(value, style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = NookTheme.colors.textStrong)
    }
}

@Composable
private fun ActionRow(icon: ImageVector, text: String, onClick: () -> Unit, enabled: Boolean = true, iconModifier: Modifier = Modifier) {
    val colors = NookTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(NookShapes.xl).clickable(enabled = enabled, onClick = onClick).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, iconModifier.size(14.dp), tint = colors.textMuted)
        Text(text, style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = colors.textMuted)
    }
}
