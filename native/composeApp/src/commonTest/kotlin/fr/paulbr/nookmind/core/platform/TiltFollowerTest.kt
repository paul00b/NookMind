package fr.paulbr.nookmind.core.platform

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TiltFollowerTest {
    @Test
    fun startsLevelWhateverTheAngle() {
        val first = TiltFollower().next(0.3f, 0.8f)
        assertEquals(0f, first.x, 0.001f)
        assertEquals(0f, first.y, 0.001f)
    }

    @Test
    fun followsATiltThenSettles() {
        val follower = TiltFollower()
        follower.next(0f, 0.7f)
        var tilt = follower.next(0.5f, 0.7f)
        repeat(20) { tilt = follower.next(0.5f, 0.7f) }
        assertTrue(tilt.x > 0.3f, "tilt.x = ${tilt.x}")
        repeat(2_000) { tilt = follower.next(0.5f, 0.7f) }
        assertTrue(abs(tilt.x) < 0.05f, "the new angle becomes the rest: ${tilt.x}")
    }
}
