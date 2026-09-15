package fr.paulbr.nookmind.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The reorder list used to move rows while the finger was still down, working from the list it had
 * captured when the drag started. Every move after the first aimed at a stale index and the row was
 * drawn on top of another one. The drag now only computes a destination; these cover that maths.
 */
class SectionOrderEditorTest {

    private val step = 52f // ROW_HEIGHT + ROW_GAP, in pixels

    @Test
    fun staysPutBelowHalfARow() {
        assertEquals(2, dropTarget(fromIndex = 2, dragOffset = 25f, stepPx = step, lastIndex = 3))
        assertEquals(2, dropTarget(fromIndex = 2, dragOffset = -25f, stepPx = step, lastIndex = 3))
    }

    @Test
    fun movesOneSlotPastHalfARow() {
        assertEquals(3, dropTarget(fromIndex = 2, dragOffset = 27f, stepPx = step, lastIndex = 3))
        assertEquals(1, dropTarget(fromIndex = 2, dragOffset = -27f, stepPx = step, lastIndex = 3))
    }

    @Test
    fun dragsFromTheBottomAllTheWayToTheTop() {
        assertEquals(0, dropTarget(fromIndex = 3, dragOffset = -3 * step, stepPx = step, lastIndex = 3))
    }

    @Test
    fun clampsToTheEndsOfTheList() {
        assertEquals(0, dropTarget(fromIndex = 1, dragOffset = -10 * step, stepPx = step, lastIndex = 3))
        assertEquals(3, dropTarget(fromIndex = 1, dragOffset = 10 * step, stepPx = step, lastIndex = 3))
    }

    @Test
    fun theDragCannotTravelBeyondTheList() {
        // From the second row of four: one row of travel upwards, two downwards.
        val bounds = dragBounds(fromIndex = 1, stepPx = step, lastIndex = 3)
        assertEquals(-step, bounds.start)
        assertEquals(2 * step, bounds.endInclusive)
    }

    @Test
    fun rowsBetweenTheSourceAndTheDestinationStepAside() {
        // Row 3 dragged up to slot 0: rows 0, 1 and 2 each move down one slot.
        assertEquals(1, rowShiftSlots(index = 0, fromIndex = 3, toIndex = 0))
        assertEquals(1, rowShiftSlots(index = 1, fromIndex = 3, toIndex = 0))
        assertEquals(1, rowShiftSlots(index = 2, fromIndex = 3, toIndex = 0))
        assertEquals(0, rowShiftSlots(index = 3, fromIndex = 3, toIndex = 0))
    }

    @Test
    fun rowsBelowTheDestinationDoNotMove() {
        // Row 0 dragged down to slot 1: only row 1 steps up, rows 2 and 3 stay.
        assertEquals(0, rowShiftSlots(index = 0, fromIndex = 0, toIndex = 1))
        assertEquals(-1, rowShiftSlots(index = 1, fromIndex = 0, toIndex = 1))
        assertEquals(0, rowShiftSlots(index = 2, fromIndex = 0, toIndex = 1))
        assertEquals(0, rowShiftSlots(index = 3, fromIndex = 0, toIndex = 1))
    }

    @Test
    fun nothingMovesWhileNoRowIsBeingDragged() {
        repeat(4) { assertEquals(0, rowShiftSlots(index = it, fromIndex = -1, toIndex = -1)) }
    }

    @Test
    fun nothingMovesWhenTheRowIsDroppedWhereItStarted() {
        repeat(4) { assertEquals(0, rowShiftSlots(index = it, fromIndex = 2, toIndex = 2)) }
    }
}
