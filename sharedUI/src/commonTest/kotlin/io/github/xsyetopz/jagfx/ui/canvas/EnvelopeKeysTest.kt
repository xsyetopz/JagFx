package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.ui.input.key.Key
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnvelopeKeysTest {
    private val segments = listOf(Segment(0, 1000), Segment(20000, 30000), Segment(40000, 50000))

    private fun run(
        command: EnvelopeKeyCommand,
        selected: Int,
        list: List<Segment> = segments,
        step: Int = KEY_STEP,
    ) = envelopeKeyEdit(command, list, selected, step)

    @Test
    fun arrowsSelectAndShiftedArrowsMove() {
        assertEquals(EnvelopeKeyCommand.SelectNext, envelopeKeyCommand(Key.DirectionRight, false))
        assertEquals(EnvelopeKeyCommand.MoveLater, envelopeKeyCommand(Key.DirectionRight, true))
        assertNull(envelopeKeyCommand(Key.DirectionUp, false))
        assertEquals(EnvelopeKeyCommand.MoveUp, envelopeKeyCommand(Key.DirectionUp, true))
        assertNull(envelopeKeyCommand(Key.A, false))
    }

    @Test
    fun selectingWithNothingSelectedStartsFromTheNearEnd() {
        assertEquals(0, run(EnvelopeKeyCommand.SelectNext, -1)?.selection)
        assertEquals(segments.lastIndex, run(EnvelopeKeyCommand.SelectPrevious, -1)?.selection)
    }

    @Test
    fun selectingPastAnEndIsLeftToTheCellNavigation() {
        assertNull(run(EnvelopeKeyCommand.SelectNext, segments.lastIndex))
        assertNull(run(EnvelopeKeyCommand.SelectPrevious, 0))
    }

    @Test
    fun aMoveStaysBetweenItsNeighbours() {
        val later = assertNotNull(run(EnvelopeKeyCommand.MoveLater, 1, step = Int.MAX_VALUE / 2))
        val earlier =
            assertNotNull(run(EnvelopeKeyCommand.MoveEarlier, 1, step = Int.MAX_VALUE / 2))

        assertEquals(segments[2].duration, later.segments?.get(1)?.duration)
        assertEquals(segments[0].duration, earlier.segments?.get(1)?.duration)
    }

    @Test
    fun aFineMoveIsSmallerThanACoarseOne() {
        val coarse = assertNotNull(run(EnvelopeKeyCommand.MoveLater, 1, step = KEY_STEP))
        val fine = assertNotNull(run(EnvelopeKeyCommand.MoveLater, 1, step = KEY_FINE_STEP))

        assertEquals(segments[1].duration + KEY_STEP, coarse.segments?.get(1)?.duration)
        assertEquals(segments[1].duration + KEY_FINE_STEP, fine.segments?.get(1)?.duration)
        assertTrue(KEY_FINE_STEP < KEY_STEP)
    }

    @Test
    fun theFirstPointKeepsItsTimeButMovesInLevel() {
        assertNull(run(EnvelopeKeyCommand.MoveLater, 0))
        val up = assertNotNull(run(EnvelopeKeyCommand.MoveUp, 0))

        assertEquals(segments[0].targetLevel + KEY_STEP, up.segments?.get(0)?.targetLevel)
        assertEquals(segments[0].duration, up.segments?.get(0)?.duration)
    }

    @Test
    fun aLevelMoveStopsAtTheEndsOfTheRange() {
        val top = listOf(Segment(0, SynthLimits.U16_MAX), Segment(100, 0))

        assertNull(run(EnvelopeKeyCommand.MoveUp, 0, top))
        assertNull(run(EnvelopeKeyCommand.MoveDown, 1, top))
    }

    @Test
    fun addPutsAPointHalfwayAfterTheSelectedOneAndSelectsIt() {
        val edit = assertNotNull(run(EnvelopeKeyCommand.Add, 1))
        val added = edit.segments?.get(2)

        assertEquals(segments.size + 1, edit.segments?.size)
        assertEquals(2, edit.selection)
        assertEquals((segments[1].duration + segments[2].duration) / 2, added?.duration)
        assertEquals((segments[1].targetLevel + segments[2].targetLevel) / 2, added?.targetLevel)
    }

    @Test
    fun addAfterTheLastPointReachesTowardsTheEndOfTheVoice() {
        val edit = assertNotNull(run(EnvelopeKeyCommand.Add, segments.lastIndex))

        assertEquals(
            (segments.last().duration + SynthLimits.U16_MAX) / 2,
            edit.segments?.last()?.duration,
        )
    }

    @Test
    fun addStopsAtTheSegmentLimit() {
        val full = List(SynthLimits.MAX_SEGMENTS) { Segment(it, 0) }

        assertNull(run(EnvelopeKeyCommand.Add, 0, full))
    }

    @Test
    fun deleteRemovesTheSelectedPointAndKeepsTheSelectionInRange() {
        val edit = assertNotNull(run(EnvelopeKeyCommand.Delete, segments.lastIndex))

        assertEquals(segments.dropLast(1), edit.segments)
        assertEquals(edit.segments?.lastIndex, edit.selection)
    }

    @Test
    fun deleteKeepsTheLastRemainingPointAndNeedsASelection() {
        assertNull(run(EnvelopeKeyCommand.Delete, 0, listOf(Segment(0, 0))))
        assertNull(run(EnvelopeKeyCommand.Delete, -1))
    }
}
