package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import io.github.xsyetopz.jagfx.ui.theme.SynthPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.floor

private const val WAVE_SCALE = 0.45f
private const val PIXEL_CENTER = 0.5f

/**
 * Rendered output of a patch: one vertical line from the
 * minimum to the maximum sample of each pixel column, a mid line, and a playback marker when
 * [playbackPosition] is in (0, 1]. Each of [loopMarkers], fractions of the waveform width, adds a
 * loop line when it is in `0.0..1.0`.
 *
 * The column reduction runs on [Dispatchers.Default]. The scroll offset of [panZoom] is in pixels.
 * Dragging pans at zoom above 1; the mouse wheel or a pinch steps the zoom through [ZoomLevels],
 * which double up to 64.
 */
@Composable
internal fun OutputWaveCanvas(
    samples: FloatArray?,
    playbackPosition: Float,
    modifier: Modifier = Modifier,
    panZoom: PanZoom = PanZoom(),
    loopMarkers: List<Float> = emptyList(),
) {
    val zoom = panZoom.viewport.zoom
    val scrollOffset = panZoom.viewport.scrollOffset
    var width by remember { mutableIntStateOf(0) }
    val columns by produceState<FloatArray?>(null, samples, width, zoom, scrollOffset) {
        value =
            if (samples == null || samples.isEmpty() || width <= 0) {
                null
            } else {
                withContext(Dispatchers.Default) {
                    Downsampler.minMax(samples, width, zoom, scrollOffset)
                }
            }
    }
    val currentZoom = rememberUpdatedState(zoom)
    val currentScroll = rememberUpdatedState(scrollOffset)
    val currentOnZoom = rememberUpdatedState(panZoom.onZoomChange)
    val currentOnScroll = rememberUpdatedState(panZoom.onScrollOffsetChange)
    Spacer(
        modifier
            .onSizeChanged { width = it.width }
            .zoomOnWheel(currentZoom, currentScroll, currentOnZoom, currentOnScroll)
            .panWhenZoomed(currentZoom, currentScroll, currentOnScroll)
            .drawWithCache {
                val mid = floor(size.height / 2f) + PIXEL_CENTER
                val minMax = columns
                val path = minMax?.let { columnPath(it, mid, size.height * WAVE_SCALE) }
                val hasSamples = samples != null && samples.isNotEmpty()
                onDrawBehind {
                    drawRect(SynthPalette.Scope)
                    drawLine(SynthPalette.MidLine, Offset(0f, mid), Offset(size.width, mid), 1f)
                    if (path !=
                        null
                    ) {
                        drawPath(path, SynthPalette.Output, style = Stroke(width = 1f))
                    }
                    if (hasSamples) {
                        for (position in loopMarkers) {
                            drawMarker(position, zoom, scrollOffset, SynthPalette.Loop)
                        }
                        drawPlaybackMarker(
                            playbackPosition,
                            zoom,
                            scrollOffset,
                            SynthPalette.Playhead,
                        )
                    }
                }
            },
    )
}

/** One vertical line per column of [minMax] from its minimum to its maximum; `NaN` is skipped. */
private fun columnPath(
    minMax: FloatArray,
    mid: Float,
    scale: Float,
): Path {
    val path = Path()
    for (px in 0 until minMax.size / 2) {
        val min = minMax[px * 2]
        if (!min.isNaN()) {
            val x = px + PIXEL_CENTER
            path.moveTo(x, floor(mid - min * scale) + PIXEL_CENTER)
            path.lineTo(x, floor(mid - minMax[px * 2 + 1] * scale) + PIXEL_CENTER)
        }
    }
    return path
}

/** Playback marker at [position] in (0, 1] of the zoomed width, when it is in view. */
private fun DrawScope.drawPlaybackMarker(
    position: Float,
    zoom: Int,
    scrollOffset: Float,
    color: Color,
) {
    if (position <= 0f) return
    drawMarker(position, zoom, scrollOffset, color)
}

/** Vertical line at [position] in `0.0..1.0` of the zoomed width, when it is in view. */
private fun DrawScope.drawMarker(
    position: Float,
    zoom: Int,
    scrollOffset: Float,
    color: Color,
) {
    if (position < 0f || position > 1f) return
    val x = position * size.width * zoom - scrollOffset
    if (x < 0f || x > size.width) return
    val snapped = floor(x) + PIXEL_CENTER
    drawLine(color, Offset(snapped, 0f), Offset(snapped, size.height), 1f)
}
