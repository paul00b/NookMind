package fr.paulbr.nookmind.core.data

import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.BookStatus
import fr.paulbr.nookmind.core.model.Movie
import fr.paulbr.nookmind.core.model.Series
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `books.author`, `movies.director` and `series.creator` are NOT NULL in Postgres, and `author` has
 * no database default. The insert body must therefore carry them even when they are empty, which is
 * only true if the encoder writes default values.
 */
class InsertPayloadTest {

    @Test
    fun keepsAnEmptyAuthor() {
        val payload = insertPayload(Book.serializer(), Book(title = "Sans auteur"), "user-1")
        assertTrue("author" in payload, "author must be in the payload, the column is NOT NULL without a default")
        assertEquals("", payload["author"]?.jsonPrimitive?.content)
    }

    @Test
    fun keepsAnEmptyDirectorAndCreator() {
        val movie = insertPayload(Movie.serializer(), Movie(title = "Sans realisateur"), "user-1")
        assertEquals("", movie["director"]?.jsonPrimitive?.content)

        val series = insertPayload(Series.serializer(), Series(title = "Sans createur"), "user-1")
        assertEquals("", series["creator"]?.jsonPrimitive?.content)
    }

    @Test
    fun keepsTheDefaultStatus() {
        val payload = insertPayload(Book.serializer(), Book(title = "Un livre"), "user-1")
        assertEquals("want_to_read", payload["status"]?.jsonPrimitive?.content)
    }

    @Test
    fun keepsTheEmptyCollectionsOfASeries() {
        val payload = insertPayload(Series.serializer(), Series(title = "Une serie"), "user-1")
        assertTrue("watched_seasons" in payload)
        assertTrue("watched_episodes" in payload)
    }

    @Test
    fun overwritesTheUserIdAndDropsTheColumnsOwnedByTheDatabase() {
        val payload = insertPayload(
            Book.serializer(),
            Book(id = "ignored", userId = "ignored", createdAt = "ignored", title = "Un livre"),
            "user-1",
        )
        assertFalse("id" in payload)
        assertFalse("created_at" in payload)
        assertEquals(JsonPrimitive("user-1"), payload["user_id"])
    }

    @Test
    fun omitsTheNullablesSoPostgresAppliesItsOwnDefaults() {
        val payload = insertPayload(Book.serializer(), Book(title = "Un livre"), "user-1")
        assertFalse("rating" in payload, "a null rating must be omitted, not sent as null")
        assertFalse("personal_note" in payload)
        assertFalse("current_page" in payload)
    }

    @Test
    fun carriesTheValuesThatWereFilledIn() {
        val payload = insertPayload(
            Book.serializer(),
            Book(title = "Un livre", author = "Une autrice", status = BookStatus.READING, rating = 4.5, pageCount = 320),
            "user-1",
        )
        assertEquals("Un livre", payload["title"]?.jsonPrimitive?.content)
        assertEquals("Une autrice", payload["author"]?.jsonPrimitive?.content)
        assertEquals("reading", payload["status"]?.jsonPrimitive?.content)
        assertEquals("4.5", payload["rating"]?.jsonPrimitive?.content)
        assertEquals("320", payload["page_count"]?.jsonPrimitive?.content)
    }
}
