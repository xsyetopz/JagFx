package io.github.xsyetopz.jagfx.ui.screen

import io.github.xsyetopz.jagfx.ui.canvas.Viewport
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import kotlin.test.Test
import kotlin.test.assertEquals

class GridModelTest {
    @Test
    fun rackShowsEveryCellOnce() {
        val cells = RackCells

        assertEquals(SignalChainSlot.entries.size, cells.size)
        assertEquals(SignalChainSlot.entries.toSet(), cells.toSet())
    }

    @Test
    fun rackColumnsHoldPitchVolumeAndFilterWithTheirPairsBelow() {
        val columns = RackRows.first().indices.map { column -> RackRows.map { it[column] } }

        assertEquals(
            listOf(
                listOf(
                    SignalChainSlot.Pitch,
                    SignalChainSlot.VibratoRate,
                    SignalChainSlot.VibratoDepth,
                ),
                listOf(
                    SignalChainSlot.Volume,
                    SignalChainSlot.TremoloRate,
                    SignalChainSlot.TremoloDepth,
                ),
                listOf(SignalChainSlot.Filter, SignalChainSlot.GapOff, SignalChainSlot.GapOn),
            ),
            columns,
        )
    }

    @Test
    fun magnitudeMapsToDecibelsAsTheFilterDoes() {
        assertEquals(0.0, magnitudeToDb(0))
        assertEquals(100.0, magnitudeToDb(65535), 0.01)
    }

    @Test
    fun positionMapsToMillisecondsOfTheVoice() {
        assertEquals(0f, positionToMs(0f, 1000))
        assertEquals(250f, positionToMs(16384f, 1000))
        assertEquals(1000f, positionToMs(65536f, 1000))
    }

    @Test
    fun visibleWindowFollowsZoomAndScroll() {
        assertEquals(0f..1f, visibleWindow(Viewport(), 100f))
        assertEquals(0.25f..0.75f, visibleWindow(Viewport(2, 50f), 100f))
        assertEquals(0.75f..1f, visibleWindow(Viewport(4, 300f), 100f))
        assertEquals(0f..1f, visibleWindow(Viewport(2, 50f), 0f))
    }

    @Test
    fun pairedEnvelopesShareTheirFirstSlot() {
        val firsts = SignalChainSlot.entries.associateWith(::pairFirstOf)

        assertEquals(SignalChainSlot.VibratoRate, firsts[SignalChainSlot.VibratoDepth])
        assertEquals(SignalChainSlot.TremoloRate, firsts[SignalChainSlot.TremoloRate])
        assertEquals(SignalChainSlot.GapOff, firsts[SignalChainSlot.GapOn])
        assertEquals(null, firsts[SignalChainSlot.Pitch])
        assertEquals(null, firsts[SignalChainSlot.Filter])
    }
}
