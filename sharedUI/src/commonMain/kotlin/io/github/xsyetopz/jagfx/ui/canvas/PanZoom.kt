package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.runtime.Immutable

private const val MAX_ZOOM = 64

/** Zoom level and horizontal scroll offset, in pixels, of a canvas. */
@Immutable
internal data class Viewport(
    val zoom: Int = 1,
    val scrollOffset: Float = 0f,
)

/** A [viewport] with the callbacks through which the canvas reports a zoom or scroll change. */
internal class PanZoom(
    val viewport: Viewport = Viewport(),
    val onZoomChange: (Int) -> Unit = {},
    val onScrollOffsetChange: (Float) -> Unit = {},
)

/** Callbacks that bracket one drag so that the editor records it as a single undo step. */
internal class GestureCallbacks(
    val onStart: () -> Unit = {},
    val onEnd: () -> Unit = {},
)

/** Zoom levels of the canvases: each doubles the last, from 1 to [MAX_ZOOM]. */
internal val ZoomLevels: List<Int> =
    generateSequence(1) {
        it * 2
    }.takeWhile { it <= MAX_ZOOM }.toList()

/**
 * The zoom level next to [zoom] in [ZoomLevels]: higher when [delta] is positive, lower when it
 * is negative. The result stays at the ends of the list.
 */
internal fun stepZoom(
    zoom: Int,
    delta: Float,
): Int {
    val index = ZoomLevels.indexOf(zoom).coerceAtLeast(0)
    val next =
        when {
            delta > 0f -> index + 1
            delta < 0f -> index - 1
            else -> index
        }
    return ZoomLevels[next.coerceIn(ZoomLevels.indices)]
}

/** The largest horizontal scroll offset of a plot [width] pixels wide at [zoom]. */
internal fun maxScrollOffset(
    width: Float,
    zoom: Int,
): Float = (width * zoom - width).coerceAtLeast(0f)

/** [offset] after a zoom change: 0 at zoom 1, otherwise clamped to the new range. */
internal fun scrollAfterZoom(
    offset: Float,
    width: Float,
    zoom: Int,
): Float = if (zoom <= 1) 0f else offset.coerceIn(0f, maxScrollOffset(width, zoom))
