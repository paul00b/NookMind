package fr.paulbr.nookmind.tools

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.App
import fr.paulbr.nookmind.app.AppContainer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.ModeAmbianceBackground
import fr.paulbr.nookmind.core.model.GoogleBookVolume
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.ThemeMode
import fr.paulbr.nookmind.feature.books.AddBookSheet
import fr.paulbr.nookmind.feature.books.BookDetailSheet
import fr.paulbr.nookmind.feature.movies.AddMovieSheet
import fr.paulbr.nookmind.feature.movies.MovieDetailSheet
import fr.paulbr.nookmind.feature.series.AddSeriesSheet
import fr.paulbr.nookmind.feature.series.SeriesDetailSheet
import fr.paulbr.nookmind.feature.series.SeriesStatsSheet
import fr.paulbr.nookmind.feature.settings.NotificationPromptSheet
import fr.paulbr.nookmind.feature.settings.SettingsPanel
import fr.paulbr.nookmind.feature.common.HomeScreenScaffold
import fr.paulbr.nookmind.feature.common.HomeSection
import fr.paulbr.nookmind.feature.common.SearchResultRow
import fr.paulbr.nookmind.feature.common.rememberSearchController
import fr.paulbr.nookmind.feature.shell.MainScaffold
import fr.paulbr.nookmind.feature.shell.MainTab
import fr.paulbr.nookmind.feature.auth.LoginScreen
import fr.paulbr.nookmind.feature.legal.LegalKind
import fr.paulbr.nookmind.feature.legal.LegalScreen
import fr.paulbr.nookmind.feature.library.MoviesLibraryScreen
import fr.paulbr.nookmind.feature.library.BooksLibraryScreen
import fr.paulbr.nookmind.feature.onboarding.OnboardingScreen
import fr.paulbr.nookmind.core.model.TmdbMovie
import fr.paulbr.nookmind.core.model.WatchProvider
import fr.paulbr.nookmind.feature.tonight.CardReason
import fr.paulbr.nookmind.feature.tonight.TonightRemote
import fr.paulbr.nookmind.feature.tonight.MatchVia
import fr.paulbr.nookmind.feature.tonight.Outcome
import fr.paulbr.nookmind.feature.tonight.Player
import fr.paulbr.nookmind.feature.tonight.StreamingService
import fr.paulbr.nookmind.feature.tonight.TonightCard
import fr.paulbr.nookmind.feature.tonight.TonightScreen
import fr.paulbr.nookmind.feature.tonight.TonightState
import fr.paulbr.nookmind.feature.tonight.TonightStep
import fr.paulbr.nookmind.feature.tonight.Verdict

class ScreenshotEntry(
    val name: String,
    val widthDp: Int = Screenshots.WIDTH_DP,
    val heightDp: Int = Screenshots.HEIGHT_DP,
    val content: @Composable () -> Unit,
)

/** Screens rendered by the `screenshots` tool. Extended as features land. */
object ScreenshotCatalog {
    /** Tall enough for a whole detail sheet to show without scrolling. */
    private const val SHEET_HEIGHT_DP = 1500

    internal val container by lazy { AppContainer().also(::seed) }

    /** Re-applied at render time: the repositories reset when the (absent) session resolves to signed-out. */
    private fun seed(c: AppContainer) {
        c.books.seedLocal(FakeData.books)
        c.bookCategories.seedLocal(FakeData.bookCategories)
        c.movies.seedLocal(FakeData.movies)
        c.movieCategories.seedLocal(FakeData.movieCategories)
        c.series.seedLocal(FakeData.series)
        c.seriesCategories.seedLocal(FakeData.seriesCategories)
        c.social.seedLocal(FakeSocial.profile, FakeSocial.friends, FakeSocial.library(), FakeSocial.received(), FakeSocial.sent, FakeSocial.invitations())
    }

    /** The Social tab before any friend: the code to share and nothing else. */
    private fun lonelySocial(): @Composable () -> Unit = {
        remember {
            container.prefs.setMediaMode(MediaMode.MOVIES)
            seed(container)
            container.social.seedLocal(FakeSocial.profile, emptyList(), emptyList())
        }
        NookTheme(ThemeMode.DARK, MediaMode.MOVIES) {
            MainScaffold(container, onOpenLegal = {}, onReplayOnboarding = {}, initialTab = MainTab.SOCIAL)
        }
    }

    /** The main shell on a given mode / tab, with an optional sheet on top. */
    private fun shell(mode: MediaMode, tab: MainTab, dark: Boolean = false, overlay: (@Composable () -> Unit)? = null): @Composable () -> Unit = {
        remember { container.prefs.setMediaMode(mode); seed(container) }
        NookTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT, mode) {
            MainScaffold(container, onOpenLegal = {}, onReplayOnboarding = {}, initialTab = tab)
            overlay?.invoke()
        }
    }

    val entries: List<ScreenshotEntry> = listOf(
        ScreenshotEntry("smoke") { App(container) },
        // The mode icons, large, to check the generated Lucide vectors against lucide.dev.
        ScreenshotEntry("icons", widthDp = 256, heightDp = 120) {
            NookTheme(ThemeMode.LIGHT, MediaMode.BOOKS) {
                Box(Modifier.fillMaxSize().background(NookTheme.colors.background)) {
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxSize(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        MediaMode.entries.forEach { m ->
                            androidx.compose.material3.Icon(
                                fr.paulbr.nookmind.core.designsystem.components.modeIcon(m),
                                contentDescription = m.name,
                                modifier = Modifier.size(64.dp),
                                tint = NookTheme.colors.textStrong,
                            )
                        }
                    }
                }
            }
        },
        // Background layer alone, to compare the grain with the web app's `.mode-bg-*`.
        ScreenshotEntry("ambiance", widthDp = 256, heightDp = 256) {
            NookTheme(ThemeMode.LIGHT, MediaMode.BOOKS) {
                Box(Modifier.fillMaxSize().background(NookTheme.colors.background)) {
                    ModeAmbianceBackground(MediaMode.BOOKS)
                }
            }
        },
        ScreenshotEntry("books-home", content = shell(MediaMode.BOOKS, MainTab.SEARCH)),
        ScreenshotEntry("books-home-dark", content = shell(MediaMode.BOOKS, MainTab.SEARCH, dark = true)),
        ScreenshotEntry("books-home-dropdown") {
            NookTheme(ThemeMode.LIGHT, MediaMode.BOOKS) {
                val search = rememberSearchController<GoogleBookVolume>(timeoutText = "timeout") { FakeData.bookSearchResults }
                LaunchedEffect(Unit) { search.query = "Dune"; search.results = FakeData.bookSearchResults; search.dropdownOpen = true }
                HomeScreenScaffold(
                    contentPadding = PaddingValues(bottom = 144.dp),
                    title = "Reading tracker", subtitle = "Search for a book to track your reading journey (or add one manually)",
                    titleColor = NookTheme.colors.amberText, search = search, searchPlaceholder = "Search by title, author, or ISBN...",
                    noResultsText = "No books found", sections = emptyList<HomeSection>(), resultKey = { it.id },
                ) { vol -> SearchResultRow(null, vol.volumeInfo.title, vol.volumeInfo.authors?.joinToString() ?: "", onClick = {}) }
            }
        },
        ScreenshotEntry("books-library", content = shell(MediaMode.BOOKS, MainTab.LIBRARY)),
        ScreenshotEntry("books-library-dark", content = shell(MediaMode.BOOKS, MainTab.LIBRARY, dark = true)),
        ScreenshotEntry("books-nextup", content = shell(MediaMode.BOOKS, MainTab.NEXT_UP)),
        ScreenshotEntry("books-detail", content = shell(MediaMode.BOOKS, MainTab.LIBRARY) { BookDetailSheet(container, FakeData.books[0], onClose = {}) }),
        ScreenshotEntry("books-add", content = shell(MediaMode.BOOKS, MainTab.SEARCH) { AddBookSheet(container, FakeData.books[3].copy(id = "", rating = null, personalNote = null), onClose = {}) }),
        ScreenshotEntry("movies-home", content = shell(MediaMode.MOVIES, MainTab.SEARCH)),
        ScreenshotEntry("movies-home-dark", content = shell(MediaMode.MOVIES, MainTab.SEARCH, dark = true)),
        ScreenshotEntry("movies-library", content = shell(MediaMode.MOVIES, MainTab.LIBRARY)),
        ScreenshotEntry("movies-nextup", content = shell(MediaMode.MOVIES, MainTab.NEXT_UP)),
        ScreenshotEntry("movies-detail", content = shell(MediaMode.MOVIES, MainTab.LIBRARY) { MovieDetailSheet(container, FakeData.movies[0], onClose = {}) }),
        ScreenshotEntry("movies-add", content = shell(MediaMode.MOVIES, MainTab.SEARCH) { AddMovieSheet(container, FakeData.movies[2].copy(id = ""), onClose = {}) }),
        ScreenshotEntry("series-home", content = shell(MediaMode.SERIES, MainTab.SEARCH)),
        ScreenshotEntry("series-home-dark", content = shell(MediaMode.SERIES, MainTab.SEARCH, dark = true)),
        ScreenshotEntry("series-library", content = shell(MediaMode.SERIES, MainTab.LIBRARY)),
        ScreenshotEntry("series-library-dark", content = shell(MediaMode.SERIES, MainTab.LIBRARY, dark = true)),
        ScreenshotEntry("series-nextup", content = shell(MediaMode.SERIES, MainTab.NEXT_UP)),
        ScreenshotEntry("series-detail", content = shell(MediaMode.SERIES, MainTab.LIBRARY) { SeriesDetailSheet(container, FakeData.series[0], onClose = {}) }),
        ScreenshotEntry("series-add", content = shell(MediaMode.SERIES, MainTab.SEARCH) { AddSeriesSheet(container, FakeData.series[4].copy(id = ""), onClose = {}) }),
        ScreenshotEntry("series-stats", content = shell(MediaMode.SERIES, MainTab.LIBRARY) { SeriesStatsSheet(container, FakeData.series, onClose = {}) }),
        ScreenshotEntry("settings", content = shell(MediaMode.BOOKS, MainTab.SEARCH) { SettingsPanel(container, onClose = {}, onOpenLegal = {}, onReplayOnboarding = {}) }),
        ScreenshotEntry("settings-dark", content = shell(MediaMode.BOOKS, MainTab.SEARCH, dark = true) { SettingsPanel(container, onClose = {}, onOpenLegal = {}, onReplayOnboarding = {}) }),
        ScreenshotEntry("notif-prompt", content = shell(MediaMode.SERIES, MainTab.SEARCH) { NotificationPromptSheet(container, onDismiss = {}) }),
        ScreenshotEntry("onboarding") { NookTheme(ThemeMode.LIGHT, MediaMode.SERIES) { OnboardingScreen(onFinish = {}) } },
        ScreenshotEntry("login") { NookTheme(ThemeMode.LIGHT, MediaMode.BOOKS) { LoginScreen(container) } },
        ScreenshotEntry("login-dark") { NookTheme(ThemeMode.DARK, MediaMode.BOOKS) { LoginScreen(container) } },
        ScreenshotEntry("legal") { NookTheme(ThemeMode.LIGHT, MediaMode.BOOKS) { LegalScreen(LegalKind.PRIVACY, onBack = {}) } },
        ScreenshotEntry("books-library-list", content = shellWithView(MediaMode.BOOKS, MainTab.LIBRARY, list = true)),
        // Dark and tall: a whole sheet in one image, the way the app is mostly used.
        ScreenshotEntry("books-detail-dark", heightDp = SHEET_HEIGHT_DP, content = shell(MediaMode.BOOKS, MainTab.LIBRARY, dark = true) { BookDetailSheet(container, FakeData.books[0], onClose = {}) }),
        ScreenshotEntry("books-add-dark", heightDp = SHEET_HEIGHT_DP, content = shell(MediaMode.BOOKS, MainTab.SEARCH, dark = true) { AddBookSheet(container, FakeData.newBook, onClose = {}) }),
        ScreenshotEntry("movies-detail-dark", heightDp = SHEET_HEIGHT_DP, content = shell(MediaMode.MOVIES, MainTab.LIBRARY, dark = true) { MovieDetailSheet(container, FakeData.movies[0], onClose = {}) }),
        ScreenshotEntry("movies-add-dark", heightDp = SHEET_HEIGHT_DP, content = shell(MediaMode.MOVIES, MainTab.SEARCH, dark = true) { AddMovieSheet(container, FakeData.newMovie, onClose = {}) }),
        ScreenshotEntry("series-detail-dark", heightDp = SHEET_HEIGHT_DP, content = shell(MediaMode.SERIES, MainTab.LIBRARY, dark = true) { SeriesDetailSheet(container, FakeData.series[0], onClose = {}) }),
        ScreenshotEntry("settings-full-dark", heightDp = 2300, content = shell(MediaMode.BOOKS, MainTab.SEARCH, dark = true) { SettingsPanel(container, onClose = {}, onOpenLegal = {}, onReplayOnboarding = {}) }),
        ScreenshotEntry("series-add-dark", heightDp = SHEET_HEIGHT_DP, content = shell(MediaMode.SERIES, MainTab.SEARCH, dark = true) { AddSeriesSheet(container, FakeData.newSeries, onClose = {}) }),
        // Dark versions of every main screen, the mode the app is mostly used in.
        ScreenshotEntry("books-nextup-dark", content = shell(MediaMode.BOOKS, MainTab.NEXT_UP, dark = true)),
        ScreenshotEntry("books-library-list-dark", content = shellWithView(MediaMode.BOOKS, MainTab.LIBRARY, list = true, dark = true)),
        ScreenshotEntry("movies-library-dark", content = shell(MediaMode.MOVIES, MainTab.LIBRARY, dark = true)),
        ScreenshotEntry("movies-nextup-dark", content = shell(MediaMode.MOVIES, MainTab.NEXT_UP, dark = true)),
        ScreenshotEntry("series-nextup-dark", content = shell(MediaMode.SERIES, MainTab.NEXT_UP, dark = true)),
        ScreenshotEntry("series-stats-dark", heightDp = SHEET_HEIGHT_DP, content = shell(MediaMode.SERIES, MainTab.LIBRARY, dark = true) { SeriesStatsSheet(container, FakeData.series, onClose = {}) }),
        ScreenshotEntry("notif-prompt-dark", content = shell(MediaMode.SERIES, MainTab.SEARCH, dark = true) { NotificationPromptSheet(container, onDismiss = {}) }),
        ScreenshotEntry("onboarding-dark") { NookTheme(ThemeMode.DARK, MediaMode.SERIES) { OnboardingScreen(onFinish = {}) } },
        // The library scrolled out: how the grid reads past the first screen.
        ScreenshotEntry("books-library-tall-dark", heightDp = 1600, content = shell(MediaMode.BOOKS, MainTab.LIBRARY, dark = true)),
        ScreenshotEntry("movies-library-tall-dark", heightDp = 1600, content = shell(MediaMode.MOVIES, MainTab.LIBRARY, dark = true)),
        ScreenshotEntry("series-library-tall-dark", heightDp = 1600, content = shell(MediaMode.SERIES, MainTab.LIBRARY, dark = true)),
        // The library tabs a default open does not show.
        ScreenshotEntry("books-library-towatch-dark", heightDp = 1200) { libraryOnly(MediaMode.BOOKS) { BooksLibraryScreen(container, PaddingValues(), initialTab = "want_to_read") } },
        ScreenshotEntry("books-library-read-dark", heightDp = 1200) { libraryOnly(MediaMode.BOOKS) { BooksLibraryScreen(container, PaddingValues(), initialTab = "read") } },
        ScreenshotEntry("movies-library-watched-dark", heightDp = 1200) { libraryOnly(MediaMode.MOVIES) { MoviesLibraryScreen(container, PaddingValues(), initialTab = "watched") } },
        ScreenshotEntry("tablet-books-library", widthDp = 1024, heightDp = 768, content = shell(MediaMode.BOOKS, MainTab.LIBRARY)),
        ScreenshotEntry("social-dark", heightDp = 2600, content = shell(MediaMode.MOVIES, MainTab.SOCIAL, dark = true)),
        ScreenshotEntry("social", heightDp = 2600, content = shell(MediaMode.BOOKS, MainTab.SOCIAL)),
        ScreenshotEntry("social-empty-dark", content = lonelySocial()),
        // A movie of Tom's opened from the salon: the add sheet, with what friends did with it.
        ScreenshotEntry("social-friend-movie-dark", heightDp = SHEET_HEIGHT_DP, content = shell(MediaMode.BOOKS, MainTab.SOCIAL, dark = true) {
            NookTheme(ThemeMode.DARK, MediaMode.MOVIES) { AddMovieSheet(container, FakeData.newMovie, onClose = {}) }
        }),
        // Ce soir ?, one entry per step.
        ScreenshotEntry("tonight-setup-dark", content = tonight(TonightStep.SETUP)),
        ScreenshotEntry("tonight-rules-dark", content = tonight(TonightStep.SWIPE) { showRules = true }),
        ScreenshotEntry("tonight-swipe-dark", content = tonight(TonightStep.SWIPE) { cards ->
            preview(cards, TonightStep.SWIPE, index = 2)
            mine[0] = Verdict.YES
            mine[1] = Verdict.NO
        }),
        ScreenshotEntry("tonight-handoff-dark", content = tonight(TonightStep.HANDOFF)),
        // Ce soir ? online, with Léa on her own phone.
        ScreenshotEntry("tonight-setup-online-dark", content = tonight(TonightStep.SETUP) { friend = FakeSocial.friends[0] }),
        ScreenshotEntry("tonight-online-swipe-dark", content = tonight(TonightStep.SWIPE) { cards ->
            remote = TonightRemote("s-1", isHost = true, partnerId = "u-lea").apply { partnerJoined = true; partnerProgress = 6 }
            preview(cards.mapIndexed { i, c -> if (i == 3) c.copy(reason = CardReason.PartnerWatchlist("Léa")) else c }, TonightStep.SWIPE, index = 3)
        }),
        ScreenshotEntry("tonight-waiting-dark", content = tonight(TonightStep.WAITING) { cards ->
            remote = TonightRemote("s-1", isHost = true, partnerId = "u-lea").apply { partnerJoined = true; partnerProgress = 4 }
            preview(cards, TonightStep.WAITING, index = cards.size)
        }),
        ScreenshotEntry("tonight-waiting-invite-dark", content = tonight(TonightStep.WAITING) { cards ->
            remote = TonightRemote("s-1", isHost = true, partnerId = "u-lea")
            preview(cards, TonightStep.WAITING, index = cards.size)
        }),
        ScreenshotEntry("tonight-online-duel-dark", content = tonight(TonightStep.DUEL) { cards ->
            remote = TonightRemote("s-1", isHost = true, partnerId = "u-lea").apply { partnerJoined = true; partnerProgress = cards.size; myPick = 1 }
            mine[1] = Verdict.YES; theirs[3] = Verdict.LOVE
            preview(cards, TonightStep.DUEL, outcome = Outcome.Maybes(3, 1))
        }),
        ScreenshotEntry("tonight-partner-left-dark", content = tonight(TonightStep.SWIPE) { cards ->
            remote = TonightRemote("s-1", isHost = true, partnerId = "u-lea").apply { partnerJoined = true; partnerLeft = true }
            preview(cards, TonightStep.SWIPE, index = 2)
        }),
        ScreenshotEntry("tonight-duel-dark", content = tonight(TonightStep.DUEL) { cards ->
            mine[0] = Verdict.YES; theirs[0] = Verdict.LOVE
            mine[2] = Verdict.YES; theirs[2] = Verdict.YES
            preview(cards, TonightStep.DUEL, player = Player.PARTNER, outcome = Outcome.Final(0, 2))
        }),
        ScreenshotEntry("tonight-match-dark", content = tonight(TonightStep.MATCH) { cards ->
            preview(cards, TonightStep.MATCH, player = Player.PARTNER, winner = 2, via = MatchVia.MATCH)
        }),
        ScreenshotEntry("tablet-series-home", widthDp = 1024, heightDp = 768, content = shell(MediaMode.SERIES, MainTab.SEARCH)),
    )

    /** Ce soir ? at [step], on cards made from the fake library, with every animation at rest. */
    private fun tonight(step: TonightStep, configure: TonightState.(List<TonightCard>) -> Unit = {}): @Composable () -> Unit = {
        remember { seed(container) }
        val state = remember {
            TonightState("Léa", setOf(StreamingService.NETFLIX, StreamingService.PRIME)).apply {
                val cards = tonightCards()
                preview(cards, step)
                configure(cards)
            }
        }
        TonightScreen(container, onClose = {}, state = state, animate = false)
    }

    private fun tonightCards(): List<TonightCard> = FakeData.movies.mapIndexed { i, movie ->
        TonightCard(
            movie = TmdbMovie(
                id = movie.tmdbId ?: i, title = movie.title, releaseDate = movie.releaseDate.orEmpty(), runtime = movie.runtime,
                overview = movie.description ?: "Cobb vole des secrets dans les rêves des autres. On lui propose l'inverse : y implanter une idée.",
            ),
            posterUrl = movie.posterUrl, year = movie.releaseDate?.take(4), runtime = movie.runtime, director = movie.director,
            genres = listOfNotNull(movie.genre), provider = WatchProvider(8, "Netflix"), providersLink = null,
            reason = if (i % 2 == 0) CardReason.Watchlist else CardReason.BecauseYouLiked("Interstellar"),
        )
    }

    /** One library screen alone, dark, in grid view, without the navigation around it. */
    @Composable
    private fun libraryOnly(mode: MediaMode, content: @Composable () -> Unit) {
        remember {
            container.prefs.setMediaMode(mode)
            container.prefs.setViewMode(mode, fr.paulbr.nookmind.core.data.ViewMode.GRID)
            seed(container)
        }
        NookTheme(ThemeMode.DARK, mode) {
            Box(Modifier.fillMaxSize().background(NookTheme.colors.background)) { content() }
        }
    }

    /** Same as [shell] but forces the list view mode of the library. */
    private fun shellWithView(mode: MediaMode, tab: MainTab, list: Boolean, dark: Boolean = false): @Composable () -> Unit = {
        remember {
            container.prefs.setMediaMode(mode)
            container.prefs.setViewMode(mode, if (list) fr.paulbr.nookmind.core.data.ViewMode.LIST else fr.paulbr.nookmind.core.data.ViewMode.GRID)
            seed(container)
        }
        NookTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT, mode) {
            MainScaffold(container, onOpenLegal = {}, onReplayOnboarding = {}, initialTab = tab)
        }
    }
}
