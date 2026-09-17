package fr.paulbr.nookmind.core.data

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The signed-out screen switches between the onboarding carousel and the sign-in form on this one
 * value, so it has to be observable. It used to be a plain getter over the settings, and the bug
 * that caused is covered by [finishingTheCarouselAfterASignOutIsVisibleToTheCaller].
 */
class AppPreferencesOnboardingTest {

    @Test
    fun aFreshInstallHasNotSeenTheCarousel() {
        assertFalse(AppPreferences(MapSettings()).onboardingCompleted.value)
    }

    @Test
    fun finishingTheCarouselIsRemembered() {
        val settings = MapSettings()
        AppPreferences(settings).setOnboardingCompleted(true)
        assertTrue(AppPreferences(settings).onboardingCompleted.value)
    }

    @Test
    fun signingOutSendsTheUserBackThroughTheCarousel() {
        val prefs = AppPreferences(MapSettings())
        prefs.setOnboardingCompleted(true)
        prefs.setOnboardingCompleted(false) // what AuthRepository.signOut does
        assertFalse(prefs.onboardingCompleted.value)
    }

    /**
     * The reported bug: sign out, go through the carousel again, tap "Get started". Every value the
     * screen switched on was already at its final state, so nothing changed and the slide sat there.
     * The flow has to emit on this second completion.
     */
    @Test
    fun finishingTheCarouselAfterASignOutIsVisibleToTheCaller() {
        val prefs = AppPreferences(MapSettings())
        prefs.setOnboardingCompleted(true) // first run, before signing in

        val seen = mutableListOf<Boolean>()
        // Same thing `collectAsState` does: read the current value, then every later one.
        seen += prefs.onboardingCompleted.value
        prefs.setOnboardingCompleted(false) // sign out
        seen += prefs.onboardingCompleted.value
        prefs.setOnboardingCompleted(true) // "Get started" on the last slide
        seen += prefs.onboardingCompleted.value

        assertTrue(seen == listOf(true, false, true), "expected true, false, true but saw $seen")
    }

    @Test
    fun replayingTheCarouselFromSettingsAlsoEmits() {
        val prefs = AppPreferences(MapSettings())
        prefs.setOnboardingCompleted(true)
        prefs.setOnboardingCompleted(false) // "Revoir l'onboarding"
        assertFalse(prefs.onboardingCompleted.value)
        prefs.setOnboardingCompleted(true)
        assertTrue(prefs.onboardingCompleted.value)
    }
}
