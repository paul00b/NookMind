package fr.paulbr.nookmind.core.ui

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

private class FakeHapticFeedback : HapticFeedback {
    val performed = mutableListOf<HapticFeedbackType>()

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        performed += hapticFeedbackType
    }
}

class HapticsTest {

    @Test
    fun everyCueMapsToItsPlatformType() {
        assertEquals(HapticFeedbackType.Confirm, HapticCue.CONFIRM.toFeedbackType())
        assertEquals(HapticFeedbackType.Reject, HapticCue.REJECT.toFeedbackType())
        assertEquals(HapticFeedbackType.SegmentTick, HapticCue.TICK.toFeedbackType())
        assertEquals(HapticFeedbackType.ToggleOn, HapticCue.TOGGLE_ON.toFeedbackType())
        assertEquals(HapticFeedbackType.ToggleOff, HapticCue.TOGGLE_OFF.toFeedbackType())
    }

    @Test
    fun distinctCuesDoNotCollapseOntoOneType() {
        val types = HapticCue.entries.map { it.toFeedbackType() }.toSet()
        assertEquals(HapticCue.entries.size, types.size)
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
