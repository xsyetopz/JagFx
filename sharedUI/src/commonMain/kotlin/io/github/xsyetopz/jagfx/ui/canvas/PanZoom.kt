package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.runtime.Immutable

private const val MAX_ZOOM = 64

/** Zoom level and horizontal scroll offset, in pixels, of a canvas. */
@Immutable
internal data class Viewport(
    val zoom: Int = 1,
    val scrollOffset: Float = 0f,
)

/**
 * The part of the voice a time-domain plot shows, independent of its width: [zoom] and the start
 * of the visible window as a [scroll] fraction of the voice, from 0 to `1 - 1 / zoom`.
 */
@Immutable
internal data class TimeView(
    val zoom: Int = 1,
    val scroll: Float = 0f,
) {
    /** The fraction of the voice that the plot shows. */
    val span: Float get() = 1f / zoom

    /** The point of the voice, as a fraction, at [relative] (0 to 1) across the plot. */
    fun timeAt(relative: Float): Float = scroll + relative * span

    /**
     * This view at [zoom], limited to 1..[MAX_ZOOM], with the voice fraction [anchor] kept at
     * [relative] across the plot. By default the start of the window stays.
     */
    fun zoomedTo(
        zoom: Int,
        anchor: Float = scroll,
        relative: Float = 0f,
    ): TimeView {
        val level = zoom.coerceIn(1, MAX_ZOOM)
        return TimeView(level, (anchor - relative / level).coerceIn(0f, 1f - 1f / level))
    }

    /** The [Viewport] of this view on a plot [plotWidth] px wide. */
    fun toViewport(plotWidth: Float): Viewport = Viewport(zoom, scroll * plotWidth * zoom)

    companion object {
        /** The whole voice: zoom 1, scroll 0. */
        val Fit: TimeView = TimeView()
    }
}

/** The [TimeView] of this viewport on a plot [plotWidth] px wide. */
internal fun Viewport.toTimeView(plotWidth: Float): TimeView =
    if (plotWidth <= 0f) {
        TimeView(zoom)
    } else {
        TimeView(zoom, (scrollOffset / (plotWidth * zoom)).coerceIn(0f, 1f - 1f / zoom))
    }

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
