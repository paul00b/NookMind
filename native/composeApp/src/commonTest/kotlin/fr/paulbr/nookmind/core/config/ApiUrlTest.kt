package fr.paulbr.nookmind.core.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// Port of src/lib/api.test.ts (native branch only: there is no relative fallback in the app).
class ApiUrlTest {
    @Test
    fun usesBaseUrl() {
        assertEquals("https://nookmind.paulbr.fr/api/push/test", ApiUrl.build("https://nookmind.paulbr.fr", "/api/push/test"))
    }

    @Test
    fun prependsLeadingSlash() {
        assertEquals("https://nookmind.paulbr.fr/api/push/test", ApiUrl.build("https://nookmind.paulbr.fr", "api/push/test"))
    }

    @Test
    fun stripsTrailingSlashAndQuotes() {
        assertEquals("https://nookmind.paulbr.fr/api/push/test", ApiUrl.build("https://nookmind.paulbr.fr/", "/api/push/test"))
        assertEquals("https://nookmind.paulbr.fr/api/push/test", ApiUrl.build("https://nookmind.paulbr.fr\"", "/api/push/test"))
        assertEquals("https://nookmind.paulbr.fr/api/push/test", ApiUrl.build("\"https://nookmind.paulbr.fr\"", "/api/push/test"))
        assertEquals("https://nookmind.paulbr.fr/api/push/test", ApiUrl.build("'https://nookmind.paulbr.fr'", "/api/push/test"))
    }

    @Test
    fun throwsWhenMissing() {
        assertFailsWith<IllegalStateException> { ApiUrl.build(null, "/api/push/test") }
        assertFailsWith<IllegalStateException> { ApiUrl.build("  ", "/api/push/test") }
    }
}
