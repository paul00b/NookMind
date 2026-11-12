package fr.paulbr.nookmind.core.network

import fr.paulbr.nookmind.core.network.JellyseerrApi.State
import kotlin.test.Test
import kotlin.test.assertEquals

class JellyseerrApiTest {
    @Test
    fun addressGetsASchemeAndLosesItsTrailingSlash() {
        assertEquals("http://nas:5055", JellyseerrApi.normalize(" nas:5055/ "))
        assertEquals("https://seerr.example.org", JellyseerrApi.normalize("https://seerr.example.org/"))
    }

    @Test
    fun mediaStatusMapsToWhereTheRequestStands() {
        assertEquals(State.NOT_REQUESTED, JellyseerrApi.stateOf(null))
        assertEquals(State.NOT_REQUESTED, JellyseerrApi.stateOf(1))
        assertEquals(State.PENDING, JellyseerrApi.stateOf(2))
        assertEquals(State.PROCESSING, JellyseerrApi.stateOf(3))
        assertEquals(State.PARTIAL, JellyseerrApi.stateOf(4))
        assertEquals(State.AVAILABLE, JellyseerrApi.stateOf(5))
        assertEquals(State.NOT_REQUESTED, JellyseerrApi.stateOf(7))
    }
}
