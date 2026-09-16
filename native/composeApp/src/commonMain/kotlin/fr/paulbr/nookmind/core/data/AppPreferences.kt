package fr.paulbr.nookmind.core.data

import com.russhwolf.settings.Settings
import fr.paulbr.nookmind.core.domain.SearchSectionPreference
import fr.paulbr.nookmind.core.domain.SearchSections
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.ThemeMode
import fr.paulbr.nookmind.core.network.AppJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.builtins.ListSerializer

enum class ViewMode(val key: String) { GRID("grid"), LIST("list") }

/**
 * The `localStorage` of the web app, with the same keys, backed by multiplatform-settings.
 * Reactive values are exposed as StateFlows so screens update instantly.
 */
class AppPreferences(private val settings: Settings) {

    private val _theme = MutableStateFlow(ThemeMode.fromKey(settings.getStringOrNull(KEY_THEME)))
    val theme: StateFlow<ThemeMode> = _theme

    fun setTheme(mode: ThemeMode) {
        settings.putString(KEY_THEME, mode.key)
        _theme.value = mode
    }

    private val _mediaMode = MutableStateFlow(MediaMode.fromKey(settings.getStringOrNull(KEY_MEDIA_MODE)))
    val mediaMode: StateFlow<MediaMode> = _mediaMode

    fun setMediaMode(mode: MediaMode) {
        settings.putString(KEY_MEDIA_MODE, mode.key)
        _mediaMode.value = mode
    }

    private val _hapticsEnabled = MutableStateFlow(settings.getStringOrNull(KEY_HAPTICS) != "false")
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled

    fun setHapticsEnabled(enabled: Boolean) {
        settings.putString(KEY_HAPTICS, enabled.toString())
        _hapticsEnabled.value = enabled
    }

    var onboardingCompleted: Boolean
        get() = settings.getStringOrNull(KEY_ONBOARDING) == "true"
        set(value) = if (value) settings.putString(KEY_ONBOARDING, "true") else settings.remove(KEY_ONBOARDING)

    var notificationPrompted: Boolean
        get() = settings.hasKey(KEY_NOTIF_PROMPTED)
        set(value) = if (value) settings.putString(KEY_NOTIF_PROMPTED, "1") else settings.remove(KEY_NOTIF_PROMPTED)

    fun viewMode(mode: MediaMode): ViewMode =
        if (settings.getStringOrNull(viewModeKey(mode)) == "list") ViewMode.LIST else ViewMode.GRID

    fun setViewMode(mode: MediaMode, view: ViewMode) = settings.putString(viewModeKey(mode), view.key)

    private fun viewModeKey(mode: MediaMode) = when (mode) {
        MediaMode.BOOKS -> "library-view-mode"
        MediaMode.MOVIES -> "movie-library-view-mode"
        MediaMode.SERIES -> "series-library-view-mode"
    }

    // ── Search section order (per mode) ────────────────────────────────────

    private val sectionFlows: Map<MediaMode, MutableStateFlow<List<SearchSectionPreference>>> =
        MediaMode.entries.associateWith { MutableStateFlow(readSections(it)) }

    fun searchSections(mode: MediaMode): StateFlow<List<SearchSectionPreference>> = sectionFlows.getValue(mode)

    fun setSearchSections(mode: MediaMode, prefs: List<SearchSectionPreference>) {
        val sanitized = SearchSections.sanitize(prefs, mode)
        settings.putString(sectionKey(mode), AppJson.encodeToString(ListSerializer(SearchSectionPreference.serializer()), sanitized))
        sectionFlows.getValue(mode).value = sanitized
    }

    fun moveSearchSection(mode: MediaMode, sectionId: String, toIndex: Int) {
        val current = sectionFlows.getValue(mode).value
        val fromIndex = current.indexOfFirst { it.id == sectionId }
        if (fromIndex == -1) return
        setSearchSections(mode, SearchSections.moveItem(current, fromIndex, toIndex))
    }

    fun toggleSearchSectionVisibility(mode: MediaMode, sectionId: String) {
        setSearchSections(mode, sectionFlows.getValue(mode).value.map { if (it.id == sectionId) it.copy(visible = !it.visible) else it })
    }

    fun resetSearchSections(mode: MediaMode) = setSearchSections(mode, SearchSections.defaults(mode))

    private fun readSections(mode: MediaMode): List<SearchSectionPreference> {
        val raw = settings.getStringOrNull(sectionKey(mode)) ?: return SearchSections.defaults(mode)
        val parsed = runCatching { AppJson.decodeFromString(ListSerializer(SearchSectionPreference.serializer()), raw) }.getOrNull()
        return SearchSections.sanitize(parsed, mode)
    }

    private fun sectionKey(mode: MediaMode) = "nookmind_search_section_prefs_${mode.key}"

    // ── Misc ───────────────────────────────────────────────────────────────

    var seriesTmdbRefreshAt: Long?
        get() = settings.getLongOrNull(KEY_SERIES_REFRESH)
        set(value) = if (value == null) settings.remove(KEY_SERIES_REFRESH) else settings.putLong(KEY_SERIES_REFRESH, value)

    companion object {
        const val KEY_THEME = "bm_theme"
        const val KEY_MEDIA_MODE = "media-mode"
        const val KEY_ONBOARDING = "nookmind_onboarding_completed"
        const val KEY_NOTIF_PROMPTED = "bm-notif-prompted"
        const val KEY_SERIES_REFRESH = "nookmind_series_tmdb_refresh"
        const val KEY_HAPTICS = "nookmind_haptics_enabled"
    }
}
