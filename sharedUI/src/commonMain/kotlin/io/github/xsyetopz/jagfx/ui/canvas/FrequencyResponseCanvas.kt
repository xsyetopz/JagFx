package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import kotlin.math.max

private const val MIN_RESPONSE_POINTS = 50
private const val MIN_DB = -80.0
private const val MAX_DB = 80.0
private const val PLOT_PADDING = 4.0
private const val TRACE_WIDTH = 1f
private const val SECTIONS = 4
private val SectionFactors = DoubleArray(SECTIONS - 1) { (it + 1).toDouble() / SECTIONS }
private const val END_FACTOR = 1.0
private const val START_FACTOR = 0.0
internal const val SECTION_TRACE_ALPHA = 0.6f

/**
 * Magnitude response of [filter] from -80 dB to 80 dB over C1 to C9 frequency axis.
 * Scroll offset of [panZoom] is in pixels; wheel zooms, and dragging pans when zoomed.
 *
 * Filter that changes over voice also shows response at factors 0.25, 0.5 and 0.75
 * in fainter tints and at 1 in filter's hue. Response at factor 0 is always drawn last,
 * in brightest tint.
 */
@Composable
internal fun FrequencyResponseCanvas(
    filter: Filter?,
    modifier: Modifier = Modifier,
    panZoom: PanZoom = PanZoom(),
) {
    val palette = JagFxTheme.palette
    val zoom = rememberUpdatedState(panZoom.viewport.zoom)
    val scroll = rememberUpdatedState(panZoom.viewport.scrollOffset)
    val onZoom = rememberUpdatedState(panZoom.onZoomChange)
    val onScroll = rememberUpdatedState(panZoom.onScrollOffsetChange)
    val viewport = panZoom.viewport
    Spacer(
        modifier
            .focusablePlot()
            .zoomOnWheel(zoom, scroll, onZoom, onScroll)
            .panWhenZoomed(zoom, scroll, onScroll)
            .clipToBounds()
            .drawWithCache {
                val widthDp = size.width / density * viewport.zoom
                val points = max(widthDp.toInt(), MIN_RESPONSE_POINTS)
                val traces = ArrayList<Pair<Path, Color>>()

                fun trace(
                    factor: Double,
                    color: Color,
                ) {
                    val response = FilterResponse.magnitudeDb(filter, factor, points)
                    traces += responsePath(response, widthDp, size.height / density) to color
                }
                if (filter != null && filter.needsModulationEnvelope) {
                    for (factor in SectionFactors) {
                        trace(factor, palette.filter.copy(alpha = SECTION_TRACE_ALPHA))
                    }
                    trace(END_FACTOR, palette.filter)
                }
                trace(START_FACTOR, palette.marker)
                val stroke = Stroke(width = TRACE_WIDTH * density)
                onDrawBehind {
                    drawRect(palette.scope)
                    drawAnalysisGrid(palette, viewport.zoom, viewport.scrollOffset)
                    translate(left = -viewport.scrollOffset) {
                        for ((path, color) in traces) drawPath(path, color, style = stroke)
                    }
                }
            },
    )
}

/** Polyline of [response] in pixels; geometry is in density-independent units. */
private fun CacheDrawScope.responsePath(
    response: DoubleArray,
    width: Float,
    height: Float,
): Path {
    val path = Path()
    val count = response.size
    for (i in 0 until count) {
        val x = PLOT_PADDING + (width - 2 * PLOT_PADDING) * i / (count - 1)
        val normalized = (response[i] - MIN_DB) / (MAX_DB - MIN_DB)
        val y =
            (height - PLOT_PADDING - normalized * (height - 2 * PLOT_PADDING))
                .coerceIn(PLOT_PADDING, max(PLOT_PADDING, height - PLOT_PADDING))
        val px = (x * density).toFloat()
        val py = (y * density).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    return path
}
