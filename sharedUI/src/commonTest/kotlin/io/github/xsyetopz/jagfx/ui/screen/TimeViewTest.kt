package io.github.xsyetopz.jagfx.ui.screen

import io.github.xsyetopz.jagfx.ui.canvas.TimeView
import io.github.xsyetopz.jagfx.ui.canvas.Viewport
import io.github.xsyetopz.jagfx.ui.canvas.stepZoom
import io.github.xsyetopz.jagfx.ui.canvas.toTimeView
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimeViewTest {
    @Test
    fun presetFourShowsAQuarterOfTheVoice() {
        val view = TimeView().zoomedTo(4)

        assertEquals(0.25f, view.span)
        assertEquals(0f..0.25f, visibleWindow(view.toViewport(400f), 400f))
    }

    @Test
    fun wheelZoomKeepsTheTimeUnderThePointer() {
        val start = TimeView(2, 0.25f)
        val relative = 0.4f
        val anchor = start.timeAt(relative)

        val zoomed = start.zoomedTo(stepZoom(start.zoom, 1f), anchor, relative)

        assertEquals(4, zoomed.zoom)
        assertEquals(anchor, zoomed.timeAt(relative), 1e-6f)
    }

    @Test
    fun wheelZoomAtTheEdgeClampsScrollButKeepsTheZoom() {
        val zoomed = TimeView().zoomedTo(4, anchor = 0.05f, relative = 0.5f)

        assertEquals(TimeView(4, 0f), zoomed)
    }

    @Test
    fun fitReturnsZoomOneScrollZero() {
        assertEquals(TimeView(1, 0f), TimeView.Fit)
        assertEquals(TimeView(1, 0f), TimeView(8, 0.5f).zoomedTo(1))
    }

    @Test
    fun viewportRoundTripsAtAnyPlotWidth() {
        val view = TimeView(4, 0.5f)

        assertEquals(view, view.toViewport(300f).toTimeView(300f))
        assertEquals(Viewport(4, 1200f), view.toViewport(600f))
        assertEquals(view, view.toViewport(600f).toTimeView(600f))
        assertEquals(TimeView(4), Viewport(4, 10f).toTimeView(0f))
    }

    @Test
    fun linkedCellsShareTheViewAndWritesGoToIt() {
        val state = ScreenViewState().withTimeView(SignalChainSlot.Pitch, TimeView(2, 0.5f))

        assertEquals(TimeView(2, 0.5f), state.timeView)
        assertEquals(TimeView(2, 0.5f), state.timeViewOf(SignalChainSlot.Output))
    }

    @Test
    fun unlinkedCellKeepsItsViewWhenTheSharedOneChanges() {
        val own = TimeView(4, 0.25f)
        val state =
            ScreenViewState()
                .withCanvas(SignalChainSlot.GapOff, CanvasView(linked = false))
                .withTimeView(SignalChainSlot.GapOff, own)
                .withTimeView(SignalChainSlot.Pitch, TimeView(2, 0.5f))

        assertEquals(own, state.timeViewOf(SignalChainSlot.GapOff))
        assertEquals(TimeView(2, 0.5f), state.timeViewOf(SignalChainSlot.Pitch))
    }

    @Test
    fun onlyTimeDomainSlotsLink() {
        val linked = SignalChainSlot.entries.filter { ScreenViewState().isLinked(it) }

        assertEquals(SignalChainSlot.envelopeChain + SignalChainSlot.Output, linked)
        assertFalse(ScreenViewState().isLinked(SignalChainSlot.PoleZero))
        assertFalse(ScreenViewState().isLinked(SignalChainSlot.Bode))
        assertTrue(ScreenViewState().isLinked(SignalChainSlot.Filter))
    }

    @Test
    fun modeDefaultsToMain() {
        assertEquals(RackMode.Main, ScreenViewState().mode)
    }
}
