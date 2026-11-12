package io.github.xsyetopz.jagfx.ui.screen

import io.github.xsyetopz.jagfx.ui.canvas.GestureCallbacks
import io.github.xsyetopz.jagfx.ui.canvas.PanZoom
import io.github.xsyetopz.jagfx.ui.canvas.PoleRef
import io.github.xsyetopz.jagfx.ui.canvas.TimeView
import io.github.xsyetopz.jagfx.ui.canvas.Viewport
import io.github.xsyetopz.jagfx.ui.canvas.withScrollOffset
import io.github.xsyetopz.jagfx.ui.canvas.zoomedAroundCenter
import io.github.xsyetopz.jagfx.ui.editor.FilterIntent
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot

/** Gesture callbacks that record drag as one undo step. */
internal fun EditorActions.gestureCallbacks(): GestureCallbacks =
    GestureCallbacks(::beginGesture, ::endGesture)

/** Viewport of [view], reporting zoom and scroll changes of canvas of [slot]. */
internal fun EditorActions.panZoom(
    slot: SignalChainSlot,
    view: CanvasView,
): PanZoom =
    PanZoom(
        viewport = Viewport(view.zoom, view.scroll),
        onZoomChange = { zoom -> setZoom(slot, zoom) },
        onScrollOffsetChange = { scroll -> updateCanvas(slot) { it.copy(scroll = scroll) } },
    )

/** Changes view of canvas of [slot]. */
internal fun EditorActions.updateCanvas(
    slot: SignalChainSlot,
    change: (CanvasView) -> CanvasView,
) = updateView { it.withCanvas(slot, change(it.canvas(slot))) }

/** Sets zoom of [slot]; returning to 1x also resets scroll. */
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

/** Sets coefficient that [ref] points at. */
internal fun EditorActions.setCoefficient(
    ref: PoleRef,
    phase: Int,
    magnitude: Int,
) = dispatch(FilterIntent.SetCoefficient(ref.channel, ref.pole, ref.phaseIndex, phase, magnitude))

/** Changes time view that [slot] shows: shared one when linked, else its own. */
internal fun EditorActions.updateTimeView(
    slot: SignalChainSlot,
    change: (TimeView) -> TimeView,
) = updateView { it.withTimeView(slot, change(it.timeViewOf(slot))) }

/**
 * Viewport of time view of [slot] on plot [plotWidth] px wide. Zoom and scroll changes
 * of canvas go to time view, shared by every linked cell.
 */
internal fun EditorActions.timePanZoom(
    slot: SignalChainSlot,
    view: ScreenViewState,
    plotWidth: Float,
): PanZoom =
    PanZoom(
        viewport = view.timeViewOf(slot).toViewport(plotWidth),
        onZoomChange = { zoom -> updateTimeView(slot) { it.zoomedTo(zoom) } },
        onScrollOffsetChange = { offset ->
            updateTimeView(slot) { it.withScrollOffset(offset, plotWidth) }
        },
    )

/** Sets zoom of [slot] to [zoom], around middle of what it shows. */
internal fun EditorActions.zoomTo(
    slot: SignalChainSlot,
    zoom: Int,
) = if (slot.isTimeDomain) {
    updateTimeView(slot) { it.zoomedAroundCenter(zoom) }
} else {
    setZoom(slot, zoom)
}

/** Fits [slot] to whole voice or whole plane: zoom 1, scroll 0. */
internal fun EditorActions.fit(slot: SignalChainSlot) =
    if (slot.isTimeDomain) {
        updateTimeView(slot) { TimeView.Fit }
    } else {
        setZoom(slot, 1)
    }
