package io.github.xsyetopz.jagfx.ui.canvas

import io.github.xsyetopz.jagfx.core.model.Segment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// A 108x108 canvas has a 100x100 plot at zoom 1; a quarter of the voice (16384) is 25 pixels.
private const val SIZE = 108f
private const val QUARTER = 16384

private fun geometry(
    segments: List<Segment>,
    zoom: Int = 1,
    scroll: Float = 0f,
    scaleMode: EnvelopeScaleMode = EnvelopeScaleMode.FullScale,
) = EnvelopeGeometry(segments, SIZE, SIZE, Viewport(zoom, scroll), scaleMode)

private fun positions(segments: List<Segment>?) = segments?.map { it.duration }

class EnvelopeGeometryTest {
    private val rise =
        listOf(Segment(0, 0), Segment(QUARTER, 65535), Segment(3 * QUARTER, 0))

    @Test
    fun mapsBreakpointsToTheirStoredPositions() {
        val g = geometry(rise)

        assertEquals(3, g.pointCount)
        assertEquals(listOf(4f, 29f, 79f), List(3) { g.pointX(it) })
        assertEquals(listOf(104f, 4f, 104f), List(3) { g.pointY(it) })
        assertEquals(54f, g.valueToY(32767), 0.01f)
    }

    @Test
    fun firstBreakpointIsAtTimeZeroWhateverItsStoredPosition() {
        // The game client does not read the position of the first segment.
        val g = geometry(listOf(Segment(QUARTER, 0), Segment(2 * QUARTER, 0)))

        assertEquals(listOf(4f, 54f), List(2) { g.pointX(it) })
    }

    @Test
    fun zoomAndScrollShiftPointsLeft() {
        val g = geometry(rise, zoom = 2, scroll = 50f)

        assertEquals(listOf(-46f, 4f, 104f), List(3) { g.pointX(it) })
    }

    @Test
    fun mapsPixelsBackToPositionAndLevel() {
        val g = geometry(rise)
        val zoomed = geometry(rise, zoom = 2, scroll = 50f)

        assertEquals(QUARTER.toFloat(), g.xToPosition(29f))
        assertEquals(QUARTER.toFloat(), zoomed.xToPosition(4f))
        assertEquals(65535, g.yToPeakLevel(4f))
        assertEquals(0, g.yToPeakLevel(104f))
        assertEquals(32767, g.yToPeakLevel(54f))
        assertEquals(0, g.yToPeakLevel(200f))
        assertEquals(65535, g.yToPeakLevel(-50f))
    }

    @Test
    fun xToPositionIsZeroWithoutPlotWidth() {
        assertEquals(0f, EnvelopeGeometry(rise, 8f, SIZE).xToPosition(50f))
    }

    @Test
    fun autoScaleSpansTheEnvelopeLevels() {
        val g =
            geometry(
                listOf(Segment(0, 1000), Segment(1, 3000), Segment(2, 2000)),
                scaleMode = EnvelopeScaleMode.AutoScale,
            )

        assertEquals(1000, g.minValue)
        assertEquals(2000, g.range)
        assertEquals(104f, g.pointY(0))
        assertEquals(4f, g.pointY(1))
        assertEquals(2000, g.yToPeakLevel(54f))
    }

    @Test
    fun autoScaleWithOneLevelUsesRangeOne() {
        val g = geometry(listOf(Segment(10, 500)), scaleMode = EnvelopeScaleMode.AutoScale)

        assertEquals(500, g.minValue)
        assertEquals(1, g.range)
        assertEquals(104f, g.pointY(0))
        assertEquals(501, g.yToPeakLevel(4f))
    }

    @Test
    fun normalizedUsesTheFullScale() {
        val g = geometry(listOf(Segment(1, 3000)), scaleMode = EnvelopeScaleMode.Normalized)

        assertEquals(0, g.minValue)
        assertEquals(65535, g.range)
    }

    @Test
    fun hitTestFindsEveryBreakpoint() {
        val g = geometry(rise)

        assertEquals(0, g.hitTest(4f, 104f))
        assertEquals(1, g.hitTest(29f, 4f))
        assertEquals(1, g.hitTest(35f, 4f))
        assertEquals(-1, g.hitTest(38f, 4f))
        assertEquals(2, g.hitTest(79f, 104f))
    }

    @Test
    fun lineHitTestReturnsTheBreakpointThatEndsTheLine() {
        val g = geometry(rise)

        assertEquals(1, g.lineHitTest(16.5f, 54f))
        assertEquals(2, g.lineHitTest(54f, 54f))
        assertEquals(-1, g.lineHitTest(54f, 20f))
        assertEquals(-1, g.lineHitTest(95f, 104f))
    }

    @Test
    fun movePointKeepsItBetweenItsNeighbours() {
        val g = geometry(rise)

        assertEquals(listOf(0, 2 * QUARTER, 3 * QUARTER), positions(g.movePoint(rise, 1, 54f)))
        assertEquals(listOf(0, 3 * QUARTER, 3 * QUARTER), positions(g.movePoint(rise, 1, 90f)))
        assertEquals(listOf(0, 0, 3 * QUARTER), positions(g.movePoint(rise, 1, -10f)))
    }

    @Test
    fun movePointOfTheLastBreakpointStopsAtTheVoiceEnd() {
        val g = geometry(rise)

        assertEquals(listOf(0, QUARTER, 65535), positions(g.movePoint(rise, 2, 500f)))
        assertEquals(listOf(0, QUARTER, QUARTER), positions(g.movePoint(rise, 2, 4f)))
    }

    @Test
    fun movePointLeavesTheFirstBreakpointAtTimeZero() {
        val g = geometry(rise)

        assertEquals(rise, g.movePoint(rise, 0, 54f))
    }

    @Test
    fun snapLevelRoundsToZoomedSteps() {
        assertEquals(32767, snapLevel(30000, 0, 65535, 1))
        assertEquals(8191, snapLevel(10000, 0, 65535, 2))
        assertEquals(1500, snapLevel(1400, 1000, 2000, 1))
    }

    @Test
    fun snapPositionRoundsWithinItsNeighbours() {
        val segments = listOf(Segment(0, 0), Segment(1000, 0), Segment(1100, 0))

        assertEquals(listOf(0, 1024, 1100), positions(snapPosition(segments, 1, 512f)))
        assertEquals(listOf(0, 0, 1100), positions(snapPosition(segments, 1, 4096f)))
        assertEquals(segments, snapPosition(segments, 0, 512f))
    }

    @Test
    fun insertPointOnLineAddsABreakpointAtTheClick() {
        val g = geometry(rise)

        assertEquals(
            listOf(
                Segment(0, 0),
                Segment(QUARTER, 65535),
                Segment(2 * QUARTER, 32767),
                Segment(3 * QUARTER, 0),
            ),
            g.insertPointOnLine(rise, 2, 54f, 54f),
        )
        assertEquals(
            listOf(0, QUARTER, 3 * QUARTER, 3 * QUARTER),
            positions(g.insertPointOnLine(rise, 2, 100f, 54f)),
        )
    }

    @Test
    fun insertPointOnLineRefusesAtTheSegmentLimit() {
        val full = List(255) { Segment(it, 0) }

        assertNull(geometry(full).insertPointOnLine(full, 1, 4f, 50f))
        assertEquals(255, geometry(full.drop(1)).insertPointOnLine(full.drop(1), 1, 4f, 50f)?.size)
        assertNull(geometry(rise).insertPointOnLine(rise, 0, 4f, 50f))
    }

    @Test
    fun removeAndAppendRespectTheSegmentBounds() {
        assertNull(removeSegment(listOf(Segment(1, 0)), 0))
        assertEquals(listOf(Segment(2, 0)), removeSegment(listOf(Segment(1, 0), Segment(2, 0)), 0))
        assertNull(appendSegment(List(255) { Segment(1, 0) }))
        assertEquals(Segment(65535, 7), appendSegment(listOf(Segment(0, 7)))?.last())
        assertEquals(listOf(Segment(65535, 0)), appendSegment(emptyList()))
    }
}
