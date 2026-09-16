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
    fun degradesToggleAtApi30ButTickIsFullyDistinctThere() {
        assertEquals(HapticFeedbackType.Confirm, HapticCue.CONFIRM.toFeedbackType(30))
        assertEquals(HapticFeedbackType.Reject, HapticCue.REJECT.toFeedbackType(30))
        assertEquals(HapticFeedbackType.TextHandleMove, HapticCue.TICK.toFeedbackType(30))
        assertEquals(HapticFeedbackType.ContextClick, HapticCue.TOGGLE_ON.toFeedbackType(30))
        assertEquals(HapticFeedbackType.VirtualKey, HapticCue.TOGGLE_OFF.toFeedbackType(30))
    }

    @Test
    fun tickGetsItsOwnConstantAtApi27ButConfirmAndToggleOnStillCollide() {
        assertEquals(HapticFeedbackType.ContextClick, HapticCue.CONFIRM.toFeedbackType(27))
        assertEquals(HapticFeedbackType.LongPress, HapticCue.REJECT.toFeedbackType(27))
        assertEquals(HapticFeedbackType.TextHandleMove, HapticCue.TICK.toFeedbackType(27))
        assertEquals(HapticFeedbackType.ContextClick, HapticCue.TOGGLE_ON.toFeedbackType(27))
        assertEquals(HapticFeedbackType.VirtualKey, HapticCue.TOGGLE_OFF.toFeedbackType(27))
        // Only CONFIRM/TOGGLE_ON collide at this tier: 4 of 5 cues are distinguishable.
        val types = HapticCue.entries.map { it.toFeedbackType(27) }.toSet()
        assertEquals(4, types.size)
    }

    @Test
    fun degradesEverythingAtTheMinSdkFloorOf24() {
        assertEquals(HapticFeedbackType.ContextClick, HapticCue.CONFIRM.toFeedbackType(24))
        assertEquals(HapticFeedbackType.LongPress, HapticCue.REJECT.toFeedbackType(24))
        assertEquals(HapticFeedbackType.ContextClick, HapticCue.TICK.toFeedbackType(24))
        assertEquals(HapticFeedbackType.ContextClick, HapticCue.TOGGLE_ON.toFeedbackType(24))
        assertEquals(HapticFeedbackType.VirtualKey, HapticCue.TOGGLE_OFF.toFeedbackType(24))
    }

    // NOTE: full 5-way distinctness holds from API 30 up (TextHandleMove(27) gives TICK its own
    // constant, Confirm/Reject(30) do the rest). Below API 30, CONFIRM and TOGGLE_ON share
    // ContextClick; below API 27, TICK joins that collision too (3-way at the minSdk=24 floor).
    // This is a direct consequence of how few "safe" distinct constants exist at each tier.
    @Test
    fun distinctCuesDoNotCollapseOntoOneTypeFromApi30Up() {
        listOf(30, 34).forEach { apiLevel ->
            val types = HapticCue.entries.map { it.toFeedbackType(apiLevel) }.toSet()
            assertEquals(HapticCue.entries.size, types.size, "collapsed at API $apiLevel")
        }
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
