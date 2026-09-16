package fr.paulbr.nookmind.core.ui

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.test.Test
import kotlin.test.assertEquals

class HapticsTest {

    @Test
    fun everyCueMapsToItsPlatformType() {
        assertEquals(HapticFeedbackType.Confirm, NookHaptic.Confirm.toFeedbackType())
        assertEquals(HapticFeedbackType.Reject, NookHaptic.Reject.toFeedbackType())
        assertEquals(HapticFeedbackType.SegmentTick, NookHaptic.Tick.toFeedbackType())
        assertEquals(HapticFeedbackType.ToggleOn, NookHaptic.ToggleOn.toFeedbackType())
        assertEquals(HapticFeedbackType.ToggleOff, NookHaptic.ToggleOff.toFeedbackType())
    }

    @Test
    fun distinctCuesDoNotCollapseOntoOneType() {
        val types = NookHaptic.entries.map { it.toFeedbackType() }.toSet()
        assertEquals(NookHaptic.entries.size, types.size)
    }

    @Test
    fun theNoneInstanceSwallowsEveryCue() {
        NookHaptic.entries.forEach { NookHaptics.None.perform(it) }
    }

    @Test
    fun aRecordingInstanceReceivesWhatItIsGiven() {
        val received = mutableListOf<NookHaptic>()
        val haptics = NookHaptics { received += it }

        haptics.perform(NookHaptic.Confirm)
        haptics.perform(NookHaptic.Tick)

        assertEquals(listOf(NookHaptic.Confirm, NookHaptic.Tick), received)
    }
}
