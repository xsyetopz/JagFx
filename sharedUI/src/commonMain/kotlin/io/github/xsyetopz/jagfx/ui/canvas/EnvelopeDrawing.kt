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
private const val MARKER_HALF_SIZE = 3f
private const val SELECTION_RADIUS = 5f
private const val BORDER_INSET = 0.5f

private val Hairline = Stroke(width = 1f)

/**
 * Draws the envelope plot of [view] in [style]: background, grid, time and value axes measured by
 * [measurer], the line through the breakpoints, breakpoint markers, and a border in the line
 * color when [EnvelopeStyle.selected].
 */
internal fun Modifier.drawEnvelope(
    view: EnvelopeView,
    style: EnvelopeStyle,
    measurer: TextMeasurer,
    palette: SynthPalette,
): Modifier =
    drawWithCache {
        val lineColor = if (style.lineColor.isSpecified) style.lineColor else palette.marker
        val labelStyle = plotLabelStyle(palette.scopeText)
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
            drawRect(palette.scope)
            val slot = style.slot
            if (slot?.range != null && view.scaleMode != EnvelopeScaleMode.AutoScale) {
                drawValueAxis(measurer, slot, envelope, labelStyle, palette.grid, palette.midLine)
            } else {
                drawEnvelopeGrid(palette)
            }
            drawTimeAxis(
                measurer,
                Viewport(view.zoom, view.scrollOffset),
                style.durationMs,
                labelStyle,
                palette.grid,
            )
            if (geometry != null) {
                drawPath(path, lineColor, style = Hairline)
                drawBreakpoints(geometry, view.selectedIndex, palette)
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

private fun DrawScope.drawEnvelopeGrid(palette: SynthPalette) {
    for (i in 1 until HORIZONTAL_DIVISIONS) {
        val y = size.height * i / HORIZONTAL_DIVISIONS
        val color = if (i * 2 == HORIZONTAL_DIVISIONS) palette.midLine else palette.grid
        drawLine(color, Offset(0f, y), Offset(size.width, y))
    }
}

private fun DrawScope.drawBreakpoints(
    geometry: EnvelopeGeometry,
    selectedIndex: Int,
    palette: SynthPalette,
) {
    for (i in 0 until geometry.pointCount) {
        val x = geometry.pointX(i)
        val y = geometry.pointY(i)
        if (i == selectedIndex) {
            drawCircle(palette.selection, SELECTION_RADIUS, Offset(x, y))
            drawCircle(palette.marker, SELECTION_RADIUS, Offset(x, y), style = Hairline)
        }
        val h = MARKER_HALF_SIZE
        drawLine(palette.marker, Offset(x - h, y - h), Offset(x + h, y + h))
        drawLine(palette.marker, Offset(x - h, y + h), Offset(x + h, y - h))
    }
}
