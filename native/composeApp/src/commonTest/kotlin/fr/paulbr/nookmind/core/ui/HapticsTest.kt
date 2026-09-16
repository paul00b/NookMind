package fr.paulbr.nookmind.core.ui

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

private class FakeHapticFeedback : HapticFeedback {
    val performed = mutableListOf<HapticFeedbackType>()

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        performed += hapticFeedbackType
    }
}

class HapticsTest {

    @Test
    fun mapsToModernTypesAtApi34() {
        assertEquals(HapticFeedbackType.Confirm, HapticCue.CONFIRM.toFeedbackType(34))
        assertEquals(HapticFeedbackType.Reject, HapticCue.REJECT.toFeedbackType(34))
        assertEquals(HapticFeedbackType.SegmentTick, HapticCue.TICK.toFeedbackType(34))
        assertEquals(HapticFeedbackType.ToggleOn, HapticCue.TOGGLE_ON.toFeedbackType(34))
        assertEquals(HapticFeedbackType.ToggleOff, HapticCue.TOGGLE_OFF.toFeedbackType(34))
    }

    @Test
    fun degradesToggleAndTickAtApi30() {
        assertEquals(HapticFeedbackType.Confirm, HapticCue.CONFIRM.toFeedbackType(30))
        assertEquals(HapticFeedbackType.Reject, HapticCue.REJECT.toFeedbackType(30))
        assertEquals(HapticFeedbackType.ContextClick, HapticCue.TICK.toFeedbackType(30))
        assertEquals(HapticFeedbackType.ContextClick, HapticCue.TOGGLE_ON.toFeedbackType(30))
        assertEquals(HapticFeedbackType.VirtualKey, HapticCue.TOGGLE_OFF.toFeedbackType(30))
    }

    @Test
    fun degradesEverythingAtTheMinSdkFloorOf24() {
        assertEquals(HapticFeedbackType.ContextClick, HapticCue.CONFIRM.toFeedbackType(24))
        assertEquals(HapticFeedbackType.LongPress, HapticCue.REJECT.toFeedbackType(24))
        assertEquals(HapticFeedbackType.ContextClick, HapticCue.TICK.toFeedbackType(24))
        assertEquals(HapticFeedbackType.ContextClick, HapticCue.TOGGLE_ON.toFeedbackType(24))
        assertEquals(HapticFeedbackType.VirtualKey, HapticCue.TOGGLE_OFF.toFeedbackType(24))
    }

    // NOTE: full 5-way distinctness genuinely only holds at API 34. Below API 30, CONFIRM,
    // TICK and TOGGLE_ON all fall back to ContextClick (3-way collision); at 30-33, TICK and
    // TOGGLE_ON still share ContextClick. This is a direct consequence of the fallback table
    // given in review — minSdk 24 only has so many "safe" distinct constants to draw from.
    @Test
    fun distinctCuesDoNotCollapseOntoOneTypeAtApi34() {
        val types = HapticCue.entries.map { it.toFeedbackType(34) }.toSet()
        assertEquals(HapticCue.entries.size, types.size)
    }

    @Test
    fun toggleOnAndToggleOffStayDistinctBelowApi34() {
        assertNotEquals(HapticCue.TOGGLE_ON.toFeedbackType(24), HapticCue.TOGGLE_OFF.toFeedbackType(24))
        assertNotEquals(HapticCue.TOGGLE_ON.toFeedbackType(30), HapticCue.TOGGLE_OFF.toFeedbackType(30))
    }

    @Test
    fun confirmAndRejectStayDistinctAtEveryTier() {
        listOf(24, 30, 34).forEach { apiLevel ->
            assertNotEquals(
                HapticCue.CONFIRM.toFeedbackType(apiLevel),
                HapticCue.REJECT.toFeedbackType(apiLevel),
                "collapsed at API $apiLevel",
            )
        }
    }

    @Test
    fun disabledForwardsNothingToTheFake() {
        val feedback = FakeHapticFeedback()
        val haptics = nookHaptics(enabled = false, feedback = feedback)

        HapticCue.entries.forEach { haptics.perform(it) }

        assertEquals(emptyList(), feedback.performed)
        assertSame(NookHaptics.None, haptics)
    }

    @Test
    fun enabledForwardsEachCuesMappedTypeInOrder() {
        val feedback = FakeHapticFeedback()
        val haptics = nookHaptics(enabled = true, feedback = feedback)

        HapticCue.entries.forEach { haptics.perform(it) }

        assertEquals(HapticCue.entries.map { it.toFeedbackType() }, feedback.performed)
    }
}
