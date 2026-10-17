package io.github.xsyetopz.jagfx.ui.screen

import io.github.xsyetopz.jagfx.ui.canvas.GestureCallbacks
import io.github.xsyetopz.jagfx.ui.canvas.PanZoom
import io.github.xsyetopz.jagfx.ui.canvas.PoleRef
import io.github.xsyetopz.jagfx.ui.canvas.Viewport
import io.github.xsyetopz.jagfx.ui.editor.FilterIntent
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot

/** The gesture callbacks that record a drag as one undo step. */
internal fun EditorActions.gestureCallbacks(): GestureCallbacks =
    GestureCallbacks(::beginGesture, ::endGesture)

/** The viewport of [view], reporting zoom and scroll changes of the canvas of [slot]. */
internal fun EditorActions.panZoom(
    slot: SignalChainSlot,
    view: CanvasView,
): PanZoom =
    PanZoom(
        viewport = Viewport(view.zoom, view.scroll),
        onZoomChange = { zoom -> setZoom(slot, zoom) },
        onScrollOffsetChange = { scroll -> updateCanvas(slot) { it.copy(scroll = scroll) } },
    )

/** Changes the view of the canvas of [slot]. */
internal fun EditorActions.updateCanvas(
    slot: SignalChainSlot,
    change: (CanvasView) -> CanvasView,
) = updateView { it.withCanvas(slot, change(it.canvas(slot))) }

/** Sets the zoom of [slot]; returning to 1x also resets the scroll. */
internal fun EditorActions.setZoom(
    slot: SignalChainSlot,
    zoom: Int,
) = updateCanvas(slot) {
    if (zoom <=
        1
    ) {
        it.copy(zoom = zoom, scroll = 0f)
    } else {
        it.copy(zoom = zoom)
    }
}

/** Sets the coefficient that [ref] points at. */
internal fun EditorActions.setCoefficient(
    ref: PoleRef,
    phase: Int,
    magnitude: Int,
) = dispatch(FilterIntent.SetCoefficient(ref.channel, ref.pole, ref.phaseIndex, phase, magnitude))
