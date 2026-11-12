package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import io.github.xsyetopz.jagfx.core.model.Coefficient
import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Pole
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.theme.SynthPalette
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.round

/**
 * One marker of pole-zero plot.
 *
 * @property channel 0 for feedforward coefficient (zero, circle marker), 1 for feedback
 *   coefficient (pole, cross marker).
 * @property pole Index of pole in its direction.
 * @property phaseIndex 0 for [io.github.xsyetopz.jagfx.core.model.Pole.start], 1 for `end`.
 */
internal data class PoleRef(
    val channel: Int,
    val pole: Int,
    val phaseIndex: Int,
)

/** Feedforward poles for channel 0, feedback poles otherwise. */
internal fun Filter.poles(channel: Int): List<Pole> = if (channel == 0) feedforward else feedback

/** Coefficient that [ref] points at. */
internal fun Filter.coefficient(ref: PoleRef): Coefficient {
    val pole = poles(ref.channel)[ref.pole]
    return if (ref.phaseIndex == 0) pole.start else pole.end
}

/**
 * Geometry of pole-zero map, in density-independent units.
 * x is phase on C1 to C9 frequency axis, stretched by zoom level and shifted left by
 * scroll offset; y is resonance, 0 dB at bottom.
 */
internal object PoleZeroGeometry {
    const val HIT_RADIUS: Double = 10.0
    const val MAGNITUDE_SNAP_DIVISIONS: Int = 32

    private const val MAX_RAW = SynthLimits.U16_MAX
    private const val CHANNELS = 2
    private const val PHASES = 2
    private const val HALF = 0.5

    fun rawPhaseToX(
        rawPhase: Int,
        width: Double,
        zoom: Int,
        scroll: Double = 0.0,
    ): Double = width * max(1, zoom) * FilterFrequencyScale.rawPhaseToNormalizedX(rawPhase) - scroll

    fun xToRawPhase(
        x: Double,
        width: Double,
        zoom: Int,
        scroll: Double = 0.0,
    ): Int {
        val normalized = ((x + scroll) / (width * max(1, zoom))).coerceIn(0.0, 1.0)
        return FilterFrequencyScale.normalizedXToRawPhase(normalized)
    }

    /** y of raw magnitude or gain, snapped and kept one unit inside plot. */
    fun rawToY(
        raw: Int,
        scale: Double,
        height: Double,
    ): Double {
        val normalized = (raw.coerceIn(0, MAX_RAW) * scale / (MAX_RAW * scale)).coerceIn(0.0, 1.0)
        val y = height - height * normalized
        return snap(y.coerceIn(1.0, max(1.0, height - 1.0)))
    }

    fun yToRawMagnitude(
        y: Double,
        height: Double,
    ): Int {
        val normalized = ((height - y) / height).coerceIn(0.0, 1.0)
        val maxDb = MAX_RAW * FilterResponse.MAGNITUDE_DB_SCALE
        return round(normalized * maxDb / FilterResponse.MAGNITUDE_DB_SCALE).toInt()
    }

    /** [rawMagnitude] rounded to multiple of `65535 / 32`. */
    fun snapRawMagnitude(rawMagnitude: Int): Int {
        val step = MAX_RAW / MAGNITUDE_SNAP_DIVISIONS.toDouble()
        return round(round(rawMagnitude / step) * step).toInt().coerceIn(0, MAX_RAW)
    }

    /** Snapped marker centre of [coefficient]. */
    fun point(
        coefficient: Coefficient,
        width: Double,
        height: Double,
        zoom: Int,
        scroll: Double = 0.0,
    ): Offset =
        Offset(
            snap(rawPhaseToX(coefficient.phase, width, zoom, scroll)).toFloat(),
            rawToY(coefficient.magnitude, FilterResponse.MAGNITUDE_DB_SCALE, height).toFloat(),
        )

    /**
     * Every marker of [filter] for phase indexes in [phases], channel 0 before channel 1,
     * poles in order.
     */
    fun refs(
        filter: Filter,
        phases: IntProgression,
    ): List<PoleRef> =
        phases.flatMap { phase ->
            (0 until CHANNELS).flatMap { channel ->
                filter.poles(channel).indices.map { PoleRef(channel, it, phase) }
            }
        }

    /**
     * Nearest marker strictly closer than [HIT_RADIUS] to ([x], [y]); earlier markers win ties.
     * [position] maps coefficient to its marker centre.
     */
    fun hitTest(
        filter: Filter,
        x: Double,
        y: Double,
        position: (Coefficient) -> Offset,
    ): PoleRef? {
        var best: PoleRef? = null
        var bestDistance = HIT_RADIUS
        for (ref in refs(filter, 0 until PHASES)) {
            val p = position(filter.coefficient(ref))
            val distance = hypot(x - p.x, y - p.y)
            if (distance < bestDistance) {
                bestDistance = distance
                best = ref
            }
        }
        return best
    }

    private fun snap(value: Double): Double = floor(value) + HALF
}

private const val MARKER_HALF_SIZE = 3f
private const val SELECTION_RADIUS = 5f
private const val LINE_WIDTH = 1f
internal const val RAIL_ALPHA = 0.7f

/** Drag snapping and [selected] marker of [PoleZeroCanvas]. */
@Immutable
internal data class PoleView(
    val snapEnabled: Boolean = false,
    val selected: PoleRef? = null,
)

/** Event callbacks of [PoleZeroCanvas]. */
internal class PoleCallbacks(
    val onSelect: (PoleRef?) -> Unit = {},
    val onCoefficientChange: (PoleRef, phase: Int, magnitude: Int) -> Unit = { _, _, _ -> },
    val gestures: GestureCallbacks = GestureCallbacks(),
)

/**
 * Pole-zero map of [filter] over frequency (C1 to C9) and resonance (0 to 100 dB): unity-gain rails,
 * line from start to end of each coefficient pair, zeros as circles, and poles as
 * crosses.
 * Scroll offset of [panZoom] is in pixels; wheel zooms, and dragging empty space pans when
 * zoomed.
 *
 * Pressing within 10 units of marker selects it and drags it; [PoleCallbacks.onCoefficientChange]
 * of [callbacks] receives new raw phase and magnitude, snapped to semitones and 1/32 magnitude
 * steps when [PoleView.snapEnabled] of [view]. Pressing elsewhere clears selection. Canvas
 * keeps no state.
 */
@Composable
internal fun PoleZeroCanvas(
    filter: Filter?,
    modifier: Modifier = Modifier,
    view: PoleView = PoleView(),
    callbacks: PoleCallbacks = PoleCallbacks(),
    panZoom: PanZoom = PanZoom(),
) {
    val palette = JagFxTheme.palette
    val viewport = rememberUpdatedState(panZoom.viewport)
    val zoom = rememberUpdatedState(panZoom.viewport.zoom)
    val scroll = rememberUpdatedState(panZoom.viewport.scrollOffset)
    val onZoom = rememberUpdatedState(panZoom.onZoomChange)
    val onScroll = rememberUpdatedState(panZoom.onScrollOffsetChange)
    val input =
        PoleInputState(
            filter = rememberUpdatedState(filter),
            view = rememberUpdatedState(view),
            callbacks = rememberUpdatedState(callbacks),
            viewport = viewport,
        )
    val focusRequester = remember { FocusRequester() }
    Spacer(
        modifier
            .focusablePlot(focusRequester) { handlePoleKey(it, input) }
            .zoomOnWheel(zoom, scroll, onZoom, onScroll)
            .panWhenZoomed(zoom, scroll, onScroll)
            .pointerInput(Unit) { detectPoleDrags(input, focusRequester) }
            .drawWithCache {
                val stroke = Stroke(width = LINE_WIDTH * density)
                val current = panZoom.viewport
                onDrawBehind {
                    drawRect(palette.scope)
                    drawAnalysisGrid(palette, current.zoom, current.scrollOffset)
                    if (filter != null) drawPoles(filter, current, view.selected, stroke, palette)
                }
            },
    )
}

/** Latest parameters of [PoleZeroCanvas], read by long-lived pointer input. */
private class PoleInputState(
    val filter: State<Filter?>,
    val view: State<PoleView>,
    val callbacks: State<PoleCallbacks>,
    val viewport: State<Viewport>,
)

private fun handlePoleKey(
    event: KeyEvent,
    input: PoleInputState,
): Boolean {
    val filter = input.filter.value
    val callbacks = input.callbacks.value
    val selected = input.view.value.selected
    val edit =
        filter?.takeIf { event.type == KeyEventType.KeyDown }?.let {
            poleKeyEdit(
                event.key,
                event.isShiftPressed,
                PoleZeroGeometry.refs(it, 0 until 2),
                selected,
                it::coefficient,
            )
        }
    edit?.let { callbacks.onSelect(it.selection) }
    edit?.coefficient?.let {
        callbacks.gestures.onStart()
        callbacks.onCoefficientChange(edit.selection, it.phase, it.magnitude)
        callbacks.gestures.onEnd()
    }
    return edit != null
}

private suspend fun PointerInputScope.detectPoleDrags(
    input: PoleInputState,
    focusRequester: FocusRequester,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        focusRequester.requestFocus()
        val filter = input.filter.value ?: return@awaitEachGesture
        val width = size.width / density.toDouble()
        val height = size.height / density.toDouble()
        if (width <= 0.0 || height <= 0.0) return@awaitEachGesture
        val zoom = input.viewport.value.zoom
        val scroll = input.viewport.value.scrollOffset / density.toDouble()
        val x = down.position.x / density.toDouble()
        val y = down.position.y / density.toDouble()
        val hit =
            PoleZeroGeometry.hitTest(filter, x, y) {
                PoleZeroGeometry.point(it, width, height, zoom, scroll)
            }
        input.callbacks.value.onSelect(hit)
        if (hit == null) return@awaitEachGesture
        down.consume()
        input.callbacks.value.gestures
            .onStart()
        try {
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                if (change == null || !change.pressed) break
                var phase =
                    PoleZeroGeometry.xToRawPhase(
                        change.position.x / density.toDouble(),
                        width,
                        zoom,
                        scroll,
                    )
                var magnitude =
                    PoleZeroGeometry.yToRawMagnitude(change.position.y / density.toDouble(), height)
                if (input.view.value.snapEnabled) {
                    phase = SemitoneScale.snapRawPhaseToSemitone(phase)
                    magnitude = PoleZeroGeometry.snapRawMagnitude(magnitude)
                }
                input.callbacks.value.onCoefficientChange(hit, phase, magnitude)
                change.consume()
            }
        } finally {
            input.callbacks.value.gestures
                .onEnd()
        }
    }
}

private fun DrawScope.drawPoles(
    filter: Filter,
    viewport: Viewport,
    selected: PoleRef?,
    stroke: Stroke,
    palette: SynthPalette,
) {
    val width = size.width / density.toDouble()
    val height = size.height / density.toDouble()
    val zoom = viewport.zoom
    val scroll = viewport.scrollOffset / density.toDouble()

    fun toPx(point: Offset): Offset = point * density

    val gainScale = FilterResponse.GAIN_DB_SCALE
    val y0 = (PoleZeroGeometry.rawToY(filter.unityGain0, gainScale, height) * density).toFloat()
    val y1 = (PoleZeroGeometry.rawToY(filter.unityGain1, gainScale, height) * density).toFloat()
    drawLine(
        palette.zero.copy(alpha = RAIL_ALPHA),
        Offset(0f, y0),
        Offset(size.width, y0),
        stroke.width,
    )
    drawLine(
        palette.pole.copy(alpha = RAIL_ALPHA),
        Offset(0f, y1),
        Offset(size.width, y1),
        stroke.width,
    )

    for (channel in 0..1) {
        val color = markerColor(palette, channel).copy(alpha = RAIL_ALPHA)
        for (pole in filter.poles(channel)) {
            val start = toPx(PoleZeroGeometry.point(pole.start, width, height, zoom, scroll))
            val end = toPx(PoleZeroGeometry.point(pole.end, width, height, zoom, scroll))
            drawLine(color, start, end, stroke.width)
        }
    }

    val half = MARKER_HALF_SIZE * density
    for (ref in PoleZeroGeometry.refs(filter, 1 downTo 0)) {
        val point = PoleZeroGeometry.point(filter.coefficient(ref), width, height, zoom, scroll)
        val centre = toPx(point)
        if (selected == ref) {
            drawCircle(palette.selection, SELECTION_RADIUS * density, centre)
            drawCircle(palette.marker, SELECTION_RADIUS * density, centre, style = stroke)
        }
        drawMarker(palette, ref.channel, centre, half, stroke)
    }
}

/** Zero color for channel 0 (feedforward), pole color for channel 1 (feedback). */
private fun markerColor(
    palette: SynthPalette,
    channel: Int,
) = if (channel == 0) palette.zero else palette.pole

/** Circle for zero (channel 0), cross for pole (channel 1). */
private fun DrawScope.drawMarker(
    palette: SynthPalette,
    channel: Int,
    centre: Offset,
    half: Float,
    stroke: Stroke,
) {
    val color = markerColor(palette, channel)
    if (channel == 0) {
        drawCircle(color, half, centre, style = stroke)
    } else {
        val a = Offset(half, half)
        val b = Offset(half, -half)
        drawLine(color, centre - a, centre + a, stroke.width)
        drawLine(color, centre - b, centre + b, stroke.width)
    }
}
