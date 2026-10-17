package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import io.github.xsyetopz.jagfx.ui.theme.SynthPalette
import kotlin.math.ceil
import kotlin.math.floor

/** Axis labels drawn on a plot: small, in the scope text color. */
internal val PlotLabelStyle: TextStyle = TextStyle(color = SynthPalette.ScopeText, fontSize = 9.sp)

private const val LABEL_INSET = 2f
private const val ENVELOPE_COLUMNS = 8

/** Least distance in pixels between the starts of two time labels. */
private const val MIN_LABEL_SPACING = 36f

/**
 * Times in ms of the vertical lines of an envelope plot of a voice [durationMs] long, along its
 * bottom edge.
 * The lines match the envelope grid: `8 * zoom` columns across the visible width, shifted left by
 * [scroll] pixels.
 * When columns are narrow, only every second or fourth is labelled.
 */
internal fun DrawScope.drawTimeLabels(
    measurer: TextMeasurer,
    zoom: Int,
    scroll: Float,
    durationMs: Int,
) {
    val columns = ENVELOPE_COLUMNS * zoom.coerceAtLeast(1)
    val step = (size.width - 2 * ENVELOPE_PADDING) / columns
    if (step <= 0f || durationMs <= 0) return
    var every = 1
    while (step * every < MIN_LABEL_SPACING && every < columns) every *= 2
    val total = columns * zoom.coerceAtLeast(1)
    val first = ceil(scroll / step / every).toInt() * every
    val last = minOf(total, floor((size.width - 2 * ENVELOPE_PADDING + scroll) / step).toInt())
    for (k in first..last step every) {
        val x = ENVELOPE_PADDING + k * step - scroll
        val ms = (k.toLong() * durationMs / total).toInt()
        val layout = measurer.measure(ms.toString(), PlotLabelStyle)
        if (x + LABEL_INSET + layout.size.width <= size.width) {
            val y = size.height - layout.size.height - LABEL_INSET
            drawText(layout, topLeft = Offset(x + LABEL_INSET, y))
        }
    }
}
