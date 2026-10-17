package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.TextMeasurer
import io.github.xsyetopz.jagfx.ui.theme.SynthPalette

private const val HORIZONTAL_DIVISIONS = 4
private const val VERTICAL_DIVISIONS = 8
private const val MARKER_HALF_SIZE = 3f
private const val SELECTION_RADIUS = 5f
private const val BORDER_INSET = 0.5f

private val Hairline = Stroke(width = 1f)

/**
 * Draws the envelope plot of [view] in [style]: background, grid, time labels measured by
 * [measurer], the line through the breakpoints, breakpoint markers, and a border in the line
 * color when [EnvelopeStyle.selected].
 */
internal fun Modifier.drawEnvelope(
    view: EnvelopeView,
    style: EnvelopeStyle,
    measurer: TextMeasurer,
): Modifier =
    drawWithCache {
        val lineColor = if (style.lineColor.isSpecified) style.lineColor else SynthPalette.Marker
        val envelope = view.envelope
        val geometry =
            envelope?.let {
                EnvelopeGeometry(
                    segments = it.segments,
                    width = size.width,
                    height = size.height,
                    viewport = Viewport(view.zoom, view.scrollOffset),
                    scaleMode = view.scaleMode,
                )
            }
        val path = Path()
        if (geometry != null && geometry.pointCount > 0) {
            path.moveTo(geometry.pointX(0), geometry.pointY(0))
            for (i in 1 until geometry.pointCount) {
                path.lineTo(
                    geometry.pointX(i),
                    geometry.pointY(i),
                )
            }
        }
        onDrawBehind {
            drawRect(SynthPalette.Scope)
            drawEnvelopeGrid(view.zoom, view.scrollOffset)
            drawTimeLabels(measurer, view.zoom, view.scrollOffset, style.durationMs)
            if (geometry != null) {
                drawPath(path, lineColor, style = Hairline)
                drawBreakpoints(geometry, view.selectedIndex)
            }
            if (style.selected) {
                drawRect(
                    color = lineColor,
                    topLeft = Offset(BORDER_INSET, BORDER_INSET),
                    size = Size(size.width - 2 * BORDER_INSET, size.height - 2 * BORDER_INSET),
                    style = Hairline,
                )
            }
        }
    }

private fun DrawScope.drawEnvelopeGrid(
    zoom: Int,
    scrollOffset: Float,
) {
    val width = size.width
    val height = size.height
    val rows = HORIZONTAL_DIVISIONS * zoom
    for (i in 1 until rows) {
        val y = height * i / rows
        val color = if (i * 2 == rows) SynthPalette.MidLine else SynthPalette.Grid
        drawLine(color, Offset(0f, y), Offset(width, y))
    }
    val columns = VERTICAL_DIVISIONS * zoom
    val step = (width - 2 * ENVELOPE_PADDING) / columns
    if (step <= 0f) return
    val shift = scrollOffset % step
    for (i in 0..columns) {
        val x = ENVELOPE_PADDING + i * step - shift
        if (x >= ENVELOPE_PADDING && x <= width - ENVELOPE_PADDING) {
            drawLine(SynthPalette.Grid, Offset(x, 0f), Offset(x, height))
        }
    }
}

private fun DrawScope.drawBreakpoints(
    geometry: EnvelopeGeometry,
    selectedIndex: Int,
) {
    for (i in 0 until geometry.pointCount) {
        val x = geometry.pointX(i)
        val y = geometry.pointY(i)
        if (i == selectedIndex) {
            drawCircle(SynthPalette.Selection, SELECTION_RADIUS, Offset(x, y))
            drawCircle(SynthPalette.Marker, SELECTION_RADIUS, Offset(x, y), style = Hairline)
        }
        val h = MARKER_HALF_SIZE
        drawLine(SynthPalette.Marker, Offset(x - h, y - h), Offset(x + h, y + h))
        drawLine(SynthPalette.Marker, Offset(x - h, y + h), Offset(x + h, y - h))
    }
}
