package fr.paulbr.nookmind.core.domain

// Port of src/context/helpers.ts — pure list transforms shared by the repositories.

fun <T> prependItem(items: List<T>, item: T): List<T> = listOf(item) + items

fun <T> replaceItemById(items: List<T>, item: T, idOf: (T) -> String): List<T> =
    items.map { if (idOf(it) == idOf(item)) item else it }

fun <T> removeItemById(items: List<T>, id: String, idOf: (T) -> String): List<T> =
    items.filter { idOf(it) != id }

fun mergeUniqueIds(existingIds: List<String>, nextIds: List<String>): List<String> =
    (existingIds + nextIds).distinct()

fun <T> removeMappedId(
    items: List<T>,
    targetId: String,
    getItemId: (T) -> String,
    updateIds: (T, List<String>) -> T,
    mappedId: String,
    getMappedIds: (T) -> List<String>,
): List<T> = items.map { item ->
    if (getItemId(item) == targetId) updateIds(item, getMappedIds(item).filter { it != mappedId }) else item
}

fun <T> addMappedIds(
    items: List<T>,
    targetId: String,
    getItemId: (T) -> String,
    updateIds: (T, List<String>) -> T,
    mappedIds: List<String>,
    getMappedIds: (T) -> List<String>,
): List<T> = items.map { item ->
    if (getItemId(item) == targetId) updateIds(item, mergeUniqueIds(getMappedIds(item), mappedIds)) else item
}

/** `s.toLowerCase().trim().replace(/\s+/g, ' ')` — used for duplicate detection. */
fun normalizeTitle(s: String): String = s.lowercase().trim().replace(Regex("\\s+"), " ")
