package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.ui.graphics.Color
import io.github.xsyetopz.jagfx.ui.canvas.EnvelopeStyle
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot

/**
 * What a cell shows around its plot. A small cell keeps only the title; the enlarged cell, which
 * is the [ScreenViewState.openCell], shows every control.
 */
internal data class CellChrome(
    val enlarged: Boolean,
) {
    val headerControls: Boolean get() = enlarged
    val viewToggles: Boolean get() = enlarged
    val controlsPanel: Boolean get() = enlarged
}

/** The chrome of [slot] while [openCell] fills the rack, or `null` when the whole rack shows. */
internal fun cellChromeOf(
    slot: SignalChainSlot,
    openCell: SignalChainSlot?,
): CellChrome = CellChrome(enlarged = slot == openCell)

/**
 * The envelope style of [slot] under [chrome]. A small cell draws faint grid lines only; the
 * enlarged cell draws what the toggles of [canvas] ask for.
 */
internal fun envelopeStyleOf(
    slot: SignalChainSlot,
    chrome: CellChrome,
    canvas: CanvasView,
    lineColor: Color,
    selected: Boolean,
    durationMs: Int,
): EnvelopeStyle =
    EnvelopeStyle(
        lineColor = lineColor,
        selected = selected,
        durationMs = durationMs,
        slot = slot,
        axisLabels = chrome.viewToggles && canvas.axisValues,
        gridLines = !chrome.viewToggles || canvas.gridLines,
        faintGrid = !chrome.enlarged,
        pointLabels = chrome.viewToggles && canvas.pointReadouts,
    )
