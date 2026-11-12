package fr.paulbr.nookmind.feature.tonight

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TonightUpcomingTest {
    private val today = LocalDate(2026, 11, 10)

    @Test
    fun aMovieOutLaterIsUpcoming() {
        assertTrue(isUpcoming("2026-12-17", today))
        assertTrue(isUpcoming("2026-11-11", today))
    }

    @Test
    fun aMovieOutTodayOrBeforeIsNot() {
        assertFalse(isUpcoming("2026-11-10", today))
        assertFalse(isUpcoming("2010-07-16", today))
    }

    @Test
    fun anUnknownDateIsNotUpcoming() {
        assertFalse(isUpcoming(null, today))
        assertFalse(isUpcoming("", today))
    }

    @Test
    fun aRecentMovieOnNoPlatformIsStillACinemaFilm() {
        assertTrue(onlyInCinemas("2026-10-22", digital = false, today = today))
        assertFalse(onlyInCinemas("2026-10-22", digital = true, today = today))
    }

    @Test
    fun anOlderOrUnknownMovieIsNotRuledOutForLackOfPlatform() {
        assertFalse(onlyInCinemas("2024-03-01", digital = false, today = today))
        assertFalse(onlyInCinemas(null, digital = false, today = today))
        assertFalse(onlyInCinemas("2026-12-17", digital = false, today = today))
    }
}
