package fr.paulbr.nookmind.core.domain

import fr.paulbr.nookmind.core.model.MediaMode
import kotlinx.serialization.Serializable

// Port of src/lib/searchSectionOrder.ts (pure part; persistence lives in AppPreferences).

@Serializable
data class SearchSectionPreference(val id: String, val visible: Boolean = true)

object SearchSections {
    val DEFAULT_ORDER: Map<MediaMode, List<String>> = mapOf(
        MediaMode.BOOKS to listOf("want_to_read", "last_read"),
        MediaMode.MOVIES to listOf("trending", "want_to_watch", "last_watched"),
        MediaMode.SERIES to listOf("trending", "watching", "waiting", "want_to_watch", "last_watched"),
    )

    fun defaults(mode: MediaMode): List<SearchSectionPreference> =
        DEFAULT_ORDER.getValue(mode).map { SearchSectionPreference(it, true) }

    /** Drops unknown ids, de-duplicates, and appends any missing default section (visible). */
    fun sanitize(value: List<SearchSectionPreference>?, mode: MediaMode): List<SearchSectionPreference> {
        val fallback = DEFAULT_ORDER.getValue(mode)
        if (value == null) return defaults(mode)
        val valid = fallback.toSet()
        val deduped = ArrayList<SearchSectionPreference>()
        for (item in value) {
            if (item.id !in valid) continue
            if (deduped.none { it.id == item.id }) deduped += item
        }
        val missing = fallback.filter { id -> deduped.none { it.id == id } }.map { SearchSectionPreference(it, true) }
        return deduped + missing
    }

    fun <T> moveItem(items: List<T>, fromIndex: Int, toIndex: Int): List<T> {
        if (fromIndex == toIndex || fromIndex < 0 || toIndex < 0 || toIndex >= items.size || fromIndex >= items.size) return items
        val next = items.toMutableList()
        val item = next.removeAt(fromIndex)
        next.add(toIndex, item)
        return next
    }

    fun <T> orderSections(sections: List<T>, prefs: List<SearchSectionPreference>, idOf: (T) -> String): List<T> {
        val rank = prefs.withIndex().associate { (index, item) -> item.id to index }
        return sections.sortedBy { rank[idOf(it)] ?: Int.MAX_VALUE }
    }
}
