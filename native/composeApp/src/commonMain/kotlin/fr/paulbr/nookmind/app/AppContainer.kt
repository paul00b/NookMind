package fr.paulbr.nookmind.app

import fr.paulbr.nookmind.core.config.AppConfig
import fr.paulbr.nookmind.core.data.AppPreferences
import fr.paulbr.nookmind.core.data.AuthRepository
import fr.paulbr.nookmind.core.data.CollectionMessages
import fr.paulbr.nookmind.core.data.CollectionRepository
import fr.paulbr.nookmind.core.data.LibraryMessages
import fr.paulbr.nookmind.core.data.LibraryRepository
import fr.paulbr.nookmind.core.data.PushRepository
import fr.paulbr.nookmind.core.data.SeriesRefreshJob
import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.BookCategory
import fr.paulbr.nookmind.core.model.Movie
import fr.paulbr.nookmind.core.model.MovieCategory
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesCategory
import fr.paulbr.nookmind.core.network.GoogleBooksApi
import fr.paulbr.nookmind.core.network.ImdbApi
import fr.paulbr.nookmind.core.network.JsonCache
import fr.paulbr.nookmind.core.network.NookMindApi
import fr.paulbr.nookmind.core.network.TmdbApi
import fr.paulbr.nookmind.core.network.createHttpClient
import fr.paulbr.nookmind.core.platform.AppleSignInProvider
import fr.paulbr.nookmind.core.platform.GoogleSignInProvider
import fr.paulbr.nookmind.core.platform.PushPlatform
import fr.paulbr.nookmind.core.platform.UnavailableAppleSignIn
import fr.paulbr.nookmind.core.platform.UnavailableGoogleSignIn
import fr.paulbr.nookmind.core.platform.UnsupportedPushPlatform
import fr.paulbr.nookmind.core.platform.createPlatformSettings
import fr.paulbr.nookmind.core.ui.ToastController
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.toast_books_addError
import fr.paulbr.nookmind.resources.toast_books_added
import fr.paulbr.nookmind.resources.toast_books_deleteError
import fr.paulbr.nookmind.resources.toast_books_deleted
import fr.paulbr.nookmind.resources.toast_books_fetchError
import fr.paulbr.nookmind.resources.toast_books_updateError
import fr.paulbr.nookmind.resources.toast_collections_addError
import fr.paulbr.nookmind.resources.toast_collections_createError
import fr.paulbr.nookmind.resources.toast_collections_deleteError
import fr.paulbr.nookmind.resources.toast_collections_fetchError
import fr.paulbr.nookmind.resources.toast_collections_removeError
import fr.paulbr.nookmind.resources.toast_movies_addError
import fr.paulbr.nookmind.resources.toast_movies_added
import fr.paulbr.nookmind.resources.toast_movies_deleteError
import fr.paulbr.nookmind.resources.toast_movies_deleted
import fr.paulbr.nookmind.resources.toast_movies_fetchError
import fr.paulbr.nookmind.resources.toast_movies_updateError
import fr.paulbr.nookmind.resources.toast_series_addError
import fr.paulbr.nookmind.resources.toast_series_added
import fr.paulbr.nookmind.resources.toast_series_deleteError
import fr.paulbr.nookmind.resources.toast_series_deleted
import fr.paulbr.nookmind.resources.toast_series_fetchError
import fr.paulbr.nookmind.resources.toast_series_updateError
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Composition root ("AppProviders" of the web app). Built once per process by the platform
 * entry point, then handed to [App].
 */
class AppContainer(
    googleSignIn: GoogleSignInProvider = UnavailableGoogleSignIn,
    appleSignIn: AppleSignInProvider = UnavailableAppleSignIn,
    pushPlatform: PushPlatform = UnsupportedPushPlatform,
) {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val settings = createPlatformSettings("prefs")
    private val cacheSettings = createPlatformSettings("cache")
    private val sessionSettings = createPlatformSettings("session")

    val prefs = AppPreferences(settings)
    val cache = JsonCache(cacheSettings)
    val toasts = ToastController(scope)

    val httpClient = createHttpClient()
    val nookMindApi = NookMindApi(httpClient)
    val tmdb = TmdbApi(httpClient, cache, nookMindApi)
    val googleBooks = GoogleBooksApi(httpClient)
    val imdb = ImdbApi(nookMindApi, cache)

    val supabase: SupabaseClient = createSupabaseClient(AppConfig.supabaseUrl, AppConfig.supabaseAnonKey) {
        install(Auth) {
            sessionManager = SettingsSessionManager(sessionSettings)
            alwaysAutoRefresh = true
            autoLoadFromStorage = true
        }
        install(Postgrest)
    }

    val auth = AuthRepository(supabase, nookMindApi, prefs, googleSignIn, appleSignIn, scope)

    val books = LibraryRepository(
        table = "books", serializer = Book.serializer(), client = supabase, auth = auth, toasts = toasts,
        messages = LibraryMessages(
            fetchError = Res.string.toast_books_fetchError, addError = Res.string.toast_books_addError,
            updateError = Res.string.toast_books_updateError, deleteError = Res.string.toast_books_deleteError,
            addSuccess = Res.string.toast_books_added, deleteSuccess = Res.string.toast_books_deleted,
        ),
        scope = scope,
    )

    val movies = LibraryRepository(
        table = "movies", serializer = Movie.serializer(), client = supabase, auth = auth, toasts = toasts,
        messages = LibraryMessages(
            fetchError = Res.string.toast_movies_fetchError, addError = Res.string.toast_movies_addError,
            updateError = Res.string.toast_movies_updateError, deleteError = Res.string.toast_movies_deleteError,
            addSuccess = Res.string.toast_movies_added, deleteSuccess = Res.string.toast_movies_deleted,
        ),
        scope = scope,
    )

    val series = LibraryRepository(
        table = "series", serializer = Series.serializer(), client = supabase, auth = auth, toasts = toasts,
        messages = LibraryMessages(
            fetchError = Res.string.toast_series_fetchError, addError = Res.string.toast_series_addError,
            updateError = Res.string.toast_series_updateError, deleteError = Res.string.toast_series_deleteError,
            addSuccess = Res.string.toast_series_added, deleteSuccess = Res.string.toast_series_deleted,
        ),
        scope = scope,
    )

    private val collectionMessages = CollectionMessages(
        fetchError = Res.string.toast_collections_fetchError,
        createError = Res.string.toast_collections_createError,
        deleteError = Res.string.toast_collections_deleteError,
        addError = Res.string.toast_collections_addError,
        removeError = Res.string.toast_collections_removeError,
    )

    val bookCategories = CollectionRepository(
        collectionTable = "book_categories", joinTable = "book_category_items", mappedIdKey = "book_id",
        client = supabase, auth = auth, toasts = toasts, messages = collectionMessages,
        build = { id, userId, title, createdAt, ids -> BookCategory(id, userId, title, createdAt, ids) },
        scope = scope,
    )

    val movieCategories = CollectionRepository(
        collectionTable = "movie_categories", joinTable = "movie_category_items", mappedIdKey = "movie_id",
        client = supabase, auth = auth, toasts = toasts, messages = collectionMessages,
        build = { id, userId, title, createdAt, ids -> MovieCategory(id, userId, title, createdAt, ids) },
        scope = scope,
    )

    val seriesCategories = CollectionRepository(
        collectionTable = "series_categories", joinTable = "series_category_items", mappedIdKey = "series_id",
        client = supabase, auth = auth, toasts = toasts, messages = collectionMessages,
        build = { id, userId, title, createdAt, ids -> SeriesCategory(id, userId, title, createdAt, ids) },
        scope = scope,
    )

    val push = PushRepository(supabase, auth, nookMindApi, pushPlatform)

    val seriesRefresh = SeriesRefreshJob(series, tmdb, prefs, scope)

    /** Route requested from outside the UI (notification tap), consumed by the main scaffold. */
    val pendingRoute = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    /** "Update application" in Settings: drop every network cache (`sessionStorage.clear()`). */
    fun clearCaches() = cache.clear()
}
