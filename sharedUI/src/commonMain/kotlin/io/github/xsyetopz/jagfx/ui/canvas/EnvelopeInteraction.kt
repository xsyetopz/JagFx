package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot

/** Snap divisions of the time axis across the visible width at zoom 1. */
private const val POSITION_SNAP_DIVISIONS = 8

/** Depth in pixels of the edge zones that scroll a zoomed plot while a breakpoint is dragged. */
private const val AUTO_SCROLL_EDGE = 28f

/** Scroll step in pixels at the outer edge of an auto-scroll zone. */
private const val AUTO_SCROLL_MAX_STEP = 18f

/** Hoisted view state that the envelope input handlers read. */
internal class EnvelopeView(
    val envelope: Envelope?,
    val zoom: Int,
    val scrollOffset: Float,
    val selectedIndex: Int,
    val scaleMode: EnvelopeScaleMode = EnvelopeScaleMode.FullScale,
    val snapEnabled: Boolean = false,
)

/** Callbacks through which the envelope canvas reports edits. */
internal class EnvelopeCallbacks(
    val onZoomChange: (Int) -> Unit = {},
    val onScrollOffsetChange: (Float) -> Unit = {},
    val onSelect: (Int) -> Unit = {},
    val onSegmentsChange: (List<Segment>) -> Unit = {},
    val onGestureStart: () -> Unit = {},
    val onGestureEnd: () -> Unit = {},
    val onTimeViewChange: ((TimeView) -> Unit)? = null,
)

/**
 * How [EnvelopeCanvas] draws: [lineColor] defaults to the marker color.
 * A positive [durationMs] draws the time axis in ms; a [slot] with a value range draws the value
 * axis in its unit and gives the readout its unit.
 * [axisLabels] draws the time and value labels, [gridLines] the grid (in the faint color when
 * [faintGrid]), and [pointLabels] a time and value label at every point.
 */
@Immutable
internal data class EnvelopeStyle(
    val lineColor: Color = Color.Unspecified,
    val selected: Boolean = false,
    val interactive: Boolean = true,
    val durationMs: Int = 0,
    val slot: SignalChainSlot? = null,
    val axisLabels: Boolean = true,
    val gridLines: Boolean = true,
    val faintGrid: Boolean = false,
    val pointLabels: Boolean = false,
)

/** Context menu entries of the envelope canvas. */
internal enum class EnvelopeMenuKind { DeletePoint, AddPointHere, AddPointAtEnd }

/** Localized labels of the envelope context menu (`MenuDeletePoint`, `MenuAddPointHere`, ...). */
internal data class EnvelopeMenuLabels(
    val deletePoint: String,
    val addPointHere: String,
    val addPointAtEnd: String,
)

/**
 * An open context menu at [position] with one [kind] entry; choosing it applies [segments] and
 * selects [selection].
 */
internal class EnvelopeMenu(
    val position: Offset,
    val kind: EnvelopeMenuKind,
    val segments: List<Segment>,
    val selection: Int,
)

/** Applies [menu]: the new segments, then the new selection. */
internal fun EnvelopeCallbacks.applyMenu(menu: EnvelopeMenu) {
    onSegmentsChange(menu.segments)
    onSelect(menu.selection)
}

/**
 * Runs the key command of [event] (see [envelopeKeyCommand]): select, move, fine move, add, or
 * delete. Each edit is one [EnvelopeCallbacks.onSegmentsChange], so one undo step. Shift moves a
 * point by [KEY_STEP]; Ctrl or Cmd by [KEY_FINE_STEP]. Alt is left to the cell navigation. True
 * when [event] was handled.
 */
internal fun handleEnvelopeKey(
    event: KeyEvent,
    view: EnvelopeView,
    callbacks: EnvelopeCallbacks,
): Boolean {
    val fine = event.isCtrlPressed || event.isMetaPressed
    val command = envelopeKeyCommand(event.key, event.isShiftPressed || fine)
    val edit =
        view.envelope?.segments?.takeIf { event.type == KeyEventType.KeyDown }?.let { segments ->
            command?.let {
                envelopeKeyEdit(
                    it,
                    segments,
                    view.selectedIndex,
                    if (fine) KEY_FINE_STEP else KEY_STEP,
                )
            }
        }
    edit?.segments?.let(callbacks.onSegmentsChange)
    edit?.let { callbacks.onSelect(it.selection) }
    return edit != null
}

/**
 * Callbacks to the canvas itself: focus it, open a menu, set the hover cursor, and show or hide
 * the readout.
 */
internal class EnvelopeCanvasHooks(
    val requestFocus: () -> Unit,
    val onMenu: (EnvelopeMenu) -> Unit,
    val onHoverIcon: (PointerIcon) -> Unit,
    val onReadout: (Int?) -> Unit,
)

/** Pointer handling of an interactive envelope canvas; see [EnvelopeGestures]. */
internal fun Modifier.envelopePointerInput(
    view: State<EnvelopeView>,
    callbacks: State<EnvelopeCallbacks>,
    hooks: EnvelopeCanvasHooks,
): Modifier =
    pointerInput(Unit) {
        val gestures = EnvelopeGestures(view, callbacks, hooks)
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                gestures.handle(event, size, viewConfiguration.doubleTapTimeoutMillis)
            }
        }
    }

/**
 * Envelope canvas pointer behaviour: right-click menu, double-click insert, breakpoint
 * drag with snapping and auto-scroll, pan when zoomed, wheel zoom, and hover cursors.
 */
private class EnvelopeGestures(
    private val view: State<EnvelopeView>,
    private val callbacks: State<EnvelopeCallbacks>,
    hooks: EnvelopeCanvasHooks,
) {
    private val requestFocus = hooks.requestFocus
    private val onMenu = hooks.onMenu
    private val onHoverIcon = hooks.onHoverIcon
    private val onReadout = hooks.onReadout
    private var size = IntSize.Zero
    private var dragIndex = -1
    private var dragMin = 0
    private var dragRange = 1
    private var dragSegments: List<Segment> = emptyList()
    private var scroll = 0f
    private var panning = false
    private var panStartX = 0f
    private var panStartOffset = 0f
    private var lastPressTime: Long? = null
    private var lastPressPosition = Offset.Zero

    fun handle(
        event: PointerEvent,
        size: IntSize,
        doubleTapTimeout: Long,
    ) {
        this.size = size
        val change = event.changes.firstOrNull() ?: return
        when (event.type) {
            PointerEventType.Press -> {
                requestFocus()
                if (event.buttons.isSecondaryPressed) {
                    openMenu(change.position)
                } else if (event.buttons.isTertiaryPressed) {
                    startPan(change.position.x)
                } else {
                    press(change, doubleTapTimeout)
                }
                change.consume()
            }

            PointerEventType.Move -> {
                move(change)
            }

            PointerEventType.Release -> {
                release()
            }

            PointerEventType.Exit -> {
                if (dragIndex < 0) onReadout(null)
            }

            PointerEventType.Scroll -> {
                val modifiers = event.keyboardModifiers
                if (modifiers.isCtrlPressed || modifiers.isMetaPressed) {
                    zoomAt(-change.scrollDelta.y, change.position.x)
                    change.consume()
                }
            }
        }
    }

    private fun startPan(x: Float) {
        panning = true
        panStartX = x
        panStartOffset = view.value.scrollOffset
    }

    private fun geometry(
        segments: List<Segment>,
        scrollOffset: Float,
    ): EnvelopeGeometry {
        val current = view.value
        return EnvelopeGeometry(
            segments = segments,
            width = size.width.toFloat(),
            height = size.height.toFloat(),
            viewport = Viewport(current.zoom, scrollOffset),
            scaleMode = current.scaleMode,
        )
    }

    private fun openMenu(position: Offset) {
        val envelope = view.value.envelope ?: return
        val segments = envelope.segments
        val g = geometry(segments, view.value.scrollOffset)
        val point = g.hitTest(position.x, position.y)
        val line = if (point < 0) g.lineHitTest(position.x, position.y) else -1
        if (point >= 0) callbacks.value.onSelect(point)
        val menu =
            when {
                point >= 0 -> {
                    removeSegment(segments, point)?.let {
                        EnvelopeMenu(position, EnvelopeMenuKind.DeletePoint, it, -1)
                    }
                }

                line >= 0 -> {
                    g.insertPointOnLine(segments, line, position.x, position.y)?.let {
                        EnvelopeMenu(position, EnvelopeMenuKind.AddPointHere, it, line)
                    }
                }

                else -> {
                    appendSegment(segments)?.let {
                        EnvelopeMenu(position, EnvelopeMenuKind.AddPointAtEnd, it, it.lastIndex)
                    }
                }
            }
        menu?.let(onMenu)
    }

    private fun press(
        change: PointerInputChange,
        doubleTapTimeout: Long,
    ) {
        val envelope = view.value.envelope ?: return
        val position = change.position
        val previous = lastPressTime
        val isDouble =
            previous != null &&
                change.uptimeMillis - previous <= doubleTapTimeout &&
                (position - lastPressPosition).getDistance() <= ENVELOPE_POINT_HIT_RADIUS
        lastPressTime = if (isDouble) null else change.uptimeMillis
        lastPressPosition = position
        val g = geometry(envelope.segments, view.value.scrollOffset)
        val point = g.hitTest(position.x, position.y)
        val line = if (isDouble && point < 0) g.lineHitTest(position.x, position.y) else -1
        when {
            line >= 0 -> {
                g.insertPointOnLine(envelope.segments, line, position.x, position.y)?.let {
                    callbacks.value.onSegmentsChange(it)
                    callbacks.value.onSelect(line)
                }
            }

            point >= 0 -> {
                startDrag(g, point)
            }

            else -> {
                callbacks.value.onSelect(-1)
                if (view.value.zoom > 1) startPan(position.x)
            }
        }
    }

    private fun startDrag(
        g: EnvelopeGeometry,
        index: Int,
    ) {
        dragIndex = index
        dragMin = g.minValue
        dragRange = g.range
        dragSegments = g.segments
        scroll = g.scrollOffset
        onHoverIcon(PointerIcon.Hand)
        callbacks.value.onSelect(index)
        requestFocus()
        callbacks.value.onGestureStart()
    }

    private fun move(change: PointerInputChange) {
        val position = change.position
        when {
            dragIndex >= 0 -> {
                drag(position)
                change.consume()
            }

            panning -> {
                val offset =
                    (panStartOffset + panStartX - position.x)
                        .coerceIn(0f, maxScrollOffset(size.plotWidth(), view.value.zoom))
                callbacks.value.onScrollOffsetChange(offset)
                change.consume()
            }

            else -> {
                val envelope = view.value.envelope
                val g = envelope?.let { geometry(it.segments, view.value.scrollOffset) }
                onHoverIcon(hoverIcon(g, position))
                onReadout(g?.hitTest(position.x, position.y)?.takeIf { it >= 0 })
            }
        }
    }

    private fun drag(position: Offset) {
        if (view.value.envelope == null) return
        val zoom = view.value.zoom
        val snap = view.value.snapEnabled
        if (zoom > 1) autoScroll(position.x, zoom)
        val g = geometry(dragSegments, scroll)
        val raw = g.yToPeakLevel(position.y, dragMin, dragRange)
        val level = if (snap) snapLevel(raw, dragMin, dragRange, zoom) else raw
        var segments = withTargetLevel(dragSegments, dragIndex, level)
        segments = g.movePoint(segments, dragIndex, position.x)
        if (snap) {
            val step = ENVELOPE_POSITION_SPAN / (POSITION_SNAP_DIVISIONS * zoom * zoom)
            segments = snapPosition(segments, dragIndex, step)
        }
        if (segments != dragSegments) {
            dragSegments = segments
            callbacks.value.onSegmentsChange(segments)
        }
        onReadout(dragIndex)
    }

    private fun autoScroll(
        x: Float,
        zoom: Int,
    ) {
        val width = size.width.toFloat()
        val step =
            when {
                x < AUTO_SCROLL_EDGE -> {
                    -AUTO_SCROLL_MAX_STEP * (AUTO_SCROLL_EDGE - x) / AUTO_SCROLL_EDGE
                }

                x > width - AUTO_SCROLL_EDGE -> {
                    AUTO_SCROLL_MAX_STEP * (x - (width - AUTO_SCROLL_EDGE)) / AUTO_SCROLL_EDGE
                }

                else -> {
                    0f
                }
            }
        val next =
            (scroll + step.coerceIn(-AUTO_SCROLL_MAX_STEP, AUTO_SCROLL_MAX_STEP))
                .coerceIn(0f, maxScrollOffset(size.plotWidth(), zoom))
        if (next != scroll) {
            scroll = next
            callbacks.value.onScrollOffsetChange(next)
        }
    }

    private fun release() {
        if (dragIndex >= 0) callbacks.value.onGestureEnd()
        dragIndex = -1
        panning = false
    }

    /** Steps the zoom by [delta], keeping the time under canvas [x] where it is. */
    private fun zoomAt(
        delta: Float,
        x: Float,
    ) {
        val current = view.value
        val plotWidth = size.plotWidth()
        if (plotWidth <= 0f) return
        val relative = ((x - ENVELOPE_PADDING) / plotWidth).coerceIn(0f, 1f)
        val next =
            Viewport(current.zoom, current.scrollOffset)
                .toTimeView(plotWidth)
                .zoomedStepAt(delta, relative)
        if (next.zoom == current.zoom) return
        val onTimeView = callbacks.value.onTimeViewChange
        if (onTimeView != null) {
            onTimeView(next)
        } else {
            callbacks.value.onZoomChange(next.zoom)
            callbacks.value.onScrollOffsetChange(next.toViewport(plotWidth).scrollOffset)
        }
    }
}

/** The cursor at [position] on a plot with [geometry]: hand on a breakpoint, crosshair on a line. */
private fun hoverIcon(
    geometry: EnvelopeGeometry?,
    position: Offset,
): PointerIcon =
    when {
        geometry == null -> PointerIcon.Default
        geometry.hitTest(position.x, position.y) >= 0 -> PointerIcon.Hand
        geometry.lineHitTest(position.x, position.y) >= 0 -> PointerIcon.Crosshair
        else -> PointerIcon.Default
    }

/** Width in pixels of the plot inside a canvas of this size at zoom 1. */
private fun IntSize.plotWidth(): Float = (width - 2 * ENVELOPE_PADDING).coerceAtLeast(0f)
