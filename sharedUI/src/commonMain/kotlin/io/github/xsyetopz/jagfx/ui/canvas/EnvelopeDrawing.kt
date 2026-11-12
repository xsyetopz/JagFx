package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import io.github.xsyetopz.jagfx.ui.screen.pointLabels
import io.github.xsyetopz.jagfx.ui.theme.SynthPalette

private const val HORIZONTAL_DIVISIONS = 4
private const val MARKER_HALF_SIZE = 3f
private const val SELECTION_RADIUS = 5f
private const val BORDER_INSET = 0.5f
private const val LABEL_GAP = 6f

private val Hairline = Stroke(width = 1f)

/**
 * Draws envelope plot of [view] in [style]: background, grid, time and value axes measured by
 * [measurer], line through breakpoints, breakpoint markers, and border in line
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
        val pointLabelLayouts =
            if (style.pointLabels && envelope != null && style.slot != null) {
                pointLabels(style.slot, envelope, style.durationMs).map {
                    it.index to measurer.measure(it.text, labelStyle)
                }
            } else {
                emptyList()
            }
        val path = geometry?.let(::envelopePath)
        onDrawBehind {
            drawRect(palette.scope)
            drawEnvelopeAxes(view, style, measurer, palette, labelStyle)
            if (geometry != null && path != null) {
                drawPath(path, lineColor, style = Hairline)
                drawBreakpoints(geometry, view.selectedIndex, palette)
                for ((index, layout) in pointLabelLayouts) {
                    drawPointLabel(layout, geometry.pointX(index), geometry.pointY(index))
                }
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

private fun envelopePath(geometry: EnvelopeGeometry): Path {
    val path = Path()
    if (geometry.pointCount > 0) {
        path.moveTo(geometry.pointX(0), geometry.pointY(0))
        for (i in 1 until geometry.pointCount) {
            path.lineTo(geometry.pointX(i), geometry.pointY(i))
        }
    }
    return path
}

/** Grid and value and time labels that [style] switches on. */
private fun DrawScope.drawEnvelopeAxes(
    view: EnvelopeView,
    style: EnvelopeStyle,
    measurer: TextMeasurer,
    palette: SynthPalette,
    labelStyle: TextStyle,
) {
    val slot = style.slot
    val gridColor = if (style.faintGrid) palette.gridFaint else palette.grid
    val valueAxis = slot?.range != null && view.scaleMode != EnvelopeScaleMode.AutoScale
    if (style.gridLines) {
        if (valueAxis) {
            drawValueGrid(gridColor, palette.midLine)
        } else {
            drawEnvelopeGrid(gridColor, palette.midLine)
        }
    }
    if (style.axisLabels && valueAxis) {
        drawValueLabels(measurer, slot, view.envelope, labelStyle)
    }
    drawTimeAxis(
        measurer,
        Viewport(view.zoom, view.scrollOffset),
        style.durationMs,
        labelStyle,
        gridColor,
        lines = style.gridLines,
        labels = style.axisLabels,
    )
}

private fun DrawScope.drawEnvelopeGrid(
    gridColor: Color,
    midColor: Color,
) {
    for (i in 1 until HORIZONTAL_DIVISIONS) {
        val y = size.height * i / HORIZONTAL_DIVISIONS
        val color = if (i * 2 == HORIZONTAL_DIVISIONS) midColor else gridColor
        drawLine(color, Offset(0f, y), Offset(size.width, y))
    }
}

/** Draws [layout] above and right of point at ([x], [y]), kept inside plot. */
private fun DrawScope.drawPointLabel(
    layout: TextLayoutResult,
    x: Float,
    y: Float,
) {
    val left = (x + LABEL_GAP).coerceAtMost(size.width - layout.size.width).coerceAtLeast(0f)
    val top = (y - LABEL_GAP - layout.size.height).coerceAtLeast(0f)
    drawText(layout, topLeft = Offset(left, top))
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
