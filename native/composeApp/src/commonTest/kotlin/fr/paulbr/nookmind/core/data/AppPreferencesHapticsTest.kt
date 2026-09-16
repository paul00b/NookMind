package fr.paulbr.nookmind.core.data

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppPreferencesHapticsTest {

    @Test
    fun defaultsToOnForAFreshInstall() {
        assertTrue(AppPreferences(MapSettings()).hapticsEnabled.value)
    }

    @Test
    fun turningItOffIsRemembered() {
        val settings = MapSettings()
        AppPreferences(settings).setHapticsEnabled(false)

        assertFalse(AppPreferences(settings).hapticsEnabled.value)
    }

    @Test
    fun turningItBackOnIsRemembered() {
        val settings = MapSettings()
        AppPreferences(settings).setHapticsEnabled(false)
        AppPreferences(settings).setHapticsEnabled(true)

        assertTrue(AppPreferences(settings).hapticsEnabled.value)
    }

    @Test
    fun theFlowEmitsTheNewValue() {
        val prefs = AppPreferences(MapSettings())

        prefs.setHapticsEnabled(false)

        assertEquals(false, prefs.hapticsEnabled.value)
    }
}
