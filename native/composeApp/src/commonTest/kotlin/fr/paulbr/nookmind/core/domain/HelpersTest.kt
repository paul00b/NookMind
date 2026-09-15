package fr.paulbr.nookmind.core.domain

import fr.paulbr.nookmind.core.model.EpisodeRating
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.SeasonState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private data class TestItem(val id: String, val relatedIds: List<String>, val label: String)

class HelpersTest {
    @Test
    fun prependDoesNotMutate() {
        val current = listOf(TestItem("1", emptyList(), "first"))
        val next = TestItem("2", emptyList(), "second")
        assertEquals(listOf(next, current[0]), prependItem(current, next))
        assertEquals(listOf(TestItem("1", emptyList(), "first")), current)
    }

    @Test
    fun replaceById() {
        val current = listOf(TestItem("1", emptyList(), "first"), TestItem("2", emptyList(), "second"))
        val replacement = TestItem("2", listOf("3"), "updated")
        assertEquals(listOf(current[0], replacement), replaceItemById(current, replacement) { it.id })
    }

    @Test
    fun removeById() {
        val current = listOf(TestItem("1", emptyList(), "first"), TestItem("2", emptyList(), "second"))
        assertEquals(listOf(current[1]), removeItemById(current, "1") { it.id })
    }

    @Test
    fun mergeIdsWithoutDuplicates() {
        assertEquals(listOf("1", "2", "3"), mergeUniqueIds(listOf("1", "2"), listOf("2", "3")))
    }

    @Test
    fun addMappedIdsOnlyOnTarget() {
        val current = listOf(TestItem("a", listOf("1"), "alpha"), TestItem("b", listOf("2"), "beta"))
        val result = addMappedIds(current, "a", { it.id }, { item, ids -> item.copy(relatedIds = ids) }, listOf("1", "3")) { it.relatedIds }
        assertEquals(listOf(TestItem("a", listOf("1", "3"), "alpha"), current[1]), result)
    }

    @Test
    fun removeMappedIdOnlyOnTarget() {
        val current = listOf(TestItem("a", listOf("1", "3"), "alpha"), TestItem("b", listOf("2"), "beta"))
        val result = removeMappedId(current, "a", { it.id }, { item, ids -> item.copy(relatedIds = ids) }, "3") { it.relatedIds }
        assertEquals(listOf(TestItem("a", listOf("1"), "alpha"), current[1]), result)
    }

    @Test
    fun searchSectionSanitizeKeepsOrderAndFillsMissing() {
        val prefs = listOf(SearchSectionPreference("last_watched", false), SearchSectionPreference("bogus"), SearchSectionPreference("trending"))
        val result = SearchSections.sanitize(prefs, MediaMode.SERIES)
        assertEquals(listOf("last_watched", "trending", "watching", "waiting", "want_to_watch"), result.map { it.id })
        assertEquals(false, result.first().visible)
        assertEquals(SearchSections.defaults(MediaMode.BOOKS), SearchSections.sanitize(null, MediaMode.BOOKS))
    }

    @Test
    fun moveItemBounds() {
        assertEquals(listOf("b", "a", "c"), SearchSections.moveItem(listOf("a", "b", "c"), 0, 1))
        assertEquals(listOf("a", "b", "c"), SearchSections.moveItem(listOf("a", "b", "c"), 0, 5))
    }

    @Test
    fun flatEpisodeLookupFollowsTmdbCounts() {
        val ratings = mapOf(
            1 to SeasonState.Loaded(listOf(EpisodeRating(1, "a", 8.0), EpisodeRating(2, "b", 8.5), EpisodeRating(3, "c", 9.0))),
            2 to SeasonState.Loaded(listOf(EpisodeRating(1, "d", 7.0))),
        )
        val lookup = buildFlatEpisodeLookup(ratings, mapOf(1 to 2, 2 to 2))
        assertEquals("a", lookup(1, 1)?.title)
        assertEquals("b", lookup(1, 2)?.title)
        assertEquals("c", lookup(2, 1)?.title)
        assertEquals("d", lookup(2, 2)?.title)
        assertNull(lookup(3, 1))
        assertEquals(ImdbStats("8.1", "9.0", "7.0"), computeImdbStats(ratings))
    }

    @Test
    fun ratingStyleThresholds() {
        assertEquals(0xFF16A34A, getRatingStyle(9.0).background)
        assertEquals(0xFF4ADE80, getRatingStyle(8.4).background)
        assertEquals(0xFFFACC15, getRatingStyle(7.0).background)
        assertEquals(0xFFF97316, getRatingStyle(6.9).background)
        assertEquals(0xFFEF4444, getRatingStyle(5.0).background)
        assertEquals(0xFF7F1D1D, getRatingStyle(4.9).background)
        assertEquals(0xFF374151, getRatingStyle(null).background)
        assertEquals("7.8", 7.75.toFixed1())
        assertEquals("8.0", 8.0.toFixed1())
    }
}
