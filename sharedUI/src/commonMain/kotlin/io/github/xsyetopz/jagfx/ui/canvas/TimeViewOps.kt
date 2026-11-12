package io.github.xsyetopz.jagfx.ui.canvas

/** This view with its scroll set from [offset] px on plot [plotWidth] px wide at this zoom. */
internal fun TimeView.withScrollOffset(
    offset: Float,
    plotWidth: Float,
): TimeView =
    if (plotWidth <= 0f) {
        this
    } else {
        copy(scroll = (offset / (plotWidth * zoom)).coerceIn(0f, 1f - span))
    }

private const val MIDDLE = 0.5f

/** This view at [zoom] with middle of window kept; zoom 1 shows whole voice. */
internal fun TimeView.zoomedAroundCenter(zoom: Int): TimeView =
    zoomedTo(zoom, timeAt(MIDDLE), MIDDLE)

/**
 * This view zoomed one step ([stepZoom] by [delta]) with voice fraction under [relative]
 * (0 to 1 across plot) kept there; same view at ends of zoom range.
 */
internal fun TimeView.zoomedStepAt(
    delta: Float,
    relative: Float,
): TimeView = zoomedTo(stepZoom(zoom, delta), timeAt(relative), relative)
