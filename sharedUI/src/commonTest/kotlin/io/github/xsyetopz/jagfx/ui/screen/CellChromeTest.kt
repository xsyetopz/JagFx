package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.ui.graphics.Color
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CellChromeTest {
    private val slot = SignalChainSlot.Pitch

    private fun style(
        chrome: CellChrome,
        canvas: CanvasView = CanvasView(),
    ) = envelopeStyleOf(
        slot,
        chrome,
        canvas,
        Color.Unspecified,
        selected = false,
        durationMs = 1000,
    )

    @Test
    fun aCellIsEnlargedOnlyWhenItIsTheOpenCell() {
        assertTrue(cellChromeOf(slot, slot).enlarged)
        assertFalse(cellChromeOf(slot, null).enlarged)
        assertFalse(cellChromeOf(slot, SignalChainSlot.Volume).enlarged)
    }

    @Test
    fun aSmallCellShowsOnlyTheTitle() {
        val chrome = cellChromeOf(slot, null)
        assertFalse(chrome.headerControls)
        assertFalse(chrome.viewToggles)
        assertFalse(chrome.controlsPanel)
    }

    @Test
    fun anEnlargedCellShowsEverything() {
        val chrome = cellChromeOf(slot, slot)
        assertTrue(chrome.headerControls)
        assertTrue(chrome.viewToggles)
        assertTrue(chrome.controlsPanel)
    }

    @Test
    fun canvasTogglesDefaultToAxisValuesAndGridOnReadoutsOff() {
        val view = CanvasView()
        assertTrue(view.axisValues)
        assertTrue(view.gridLines)
        assertFalse(view.pointReadouts)
    }

    @Test
    fun canvasTogglesAreStoredPerSlot() {
        val state =
            ScreenViewState().withCanvas(slot, CanvasView(axisValues = false, pointReadouts = true))
        assertFalse(state.canvas(slot).axisValues)
        assertTrue(state.canvas(slot).pointReadouts)
        assertTrue(state.canvas(SignalChainSlot.Volume).axisValues)
        assertFalse(state.canvas(SignalChainSlot.Volume).pointReadouts)
    }

    @Test
    fun aSmallCellStyleDrawsFaintGridWithoutLabelsOrReadouts() {
        val style = style(cellChromeOf(slot, null), CanvasView(pointReadouts = true))
        assertFalse(style.axisLabels)
        assertTrue(style.gridLines)
        assertTrue(style.faintGrid)
        assertFalse(style.pointLabels)
    }

    @Test
    fun anEnlargedCellStyleFollowsTheToggles() {
        val defaults = style(cellChromeOf(slot, slot))
        assertTrue(defaults.axisLabels)
        assertTrue(defaults.gridLines)
        assertFalse(defaults.faintGrid)
        assertFalse(defaults.pointLabels)

        val toggled =
            style(
                cellChromeOf(slot, slot),
                CanvasView(axisValues = false, gridLines = false, pointReadouts = true),
            )
        assertFalse(toggled.axisLabels)
        assertFalse(toggled.gridLines)
        assertTrue(toggled.pointLabels)
    }

    @Test
    fun theStyleKeepsSlotDurationAndSelection() {
        val built =
            envelopeStyleOf(
                slot,
                cellChromeOf(slot, null),
                CanvasView(),
                Color.Red,
                selected = true,
                durationMs = 750,
            )
        assertEquals(slot, built.slot)
        assertEquals(750, built.durationMs)
        assertTrue(built.selected)
        assertEquals(Color.Red, built.lineColor)
    }
}
