package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.ui.graphics.Color
import io.github.xsyetopz.jagfx.ui.canvas.EnvelopeStyle
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot

/**
 * What cell shows around its plot. Small cell keeps only title; enlarged cell, which
 * is [ScreenViewState.openCell], shows every control.
 */
internal data class CellChrome(
    val enlarged: Boolean,
) {
    val headerControls: Boolean get() = enlarged
    val viewToggles: Boolean get() = enlarged
    val controlsPanel: Boolean get() = enlarged
}

/** Chrome of [slot] while [openCell] fills rack, or `null` when whole rack shows. */
internal fun cellChromeOf(
    slot: SignalChainSlot,
    openCell: SignalChainSlot?,
): CellChrome = CellChrome(enlarged = slot == openCell)

/**
 * Envelope style of [slot] under [chrome]. Small cell draws faint grid lines only;
 * enlarged cell draws what toggles of [canvas] ask for.
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
