package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.screen.formatDecimal
import io.github.xsyetopz.jagfx.ui.screen.suffix
import io.github.xsyetopz.jagfx.ui.screen.timeTicks
import io.github.xsyetopz.jagfx.ui.screen.valueTicks
import io.github.xsyetopz.jagfx.ui.screen.visibleWindow

/** Axis labels drawn on a plot: label size, in the scope text [color]. */
internal fun plotLabelStyle(color: Color): TextStyle = TextStyle(color = color, fontSize = 11.sp)

private const val LABEL_INSET = 2f
private const val VALUE_DECIMALS = 1

/** Least distance in pixels between two time ticks; room for a label such as `1200 ms`. */
internal const val MIN_TICK_SPACING = 120f

/** Rows of the value grid: [VALUE_ROWS] + 1 lines, the middle one in the mid line color. */
internal const val VALUE_ROWS = 2

private const val MID_FRACTION = 0.5f

/** Heights of the value grid lines as fractions of the plot height from the bottom. */
internal fun valueGridFractions(rows: Int = VALUE_ROWS): List<Float> =
    List(rows + 1) { it.toFloat() / rows }

/**
 * The time axis of a voice [durationMs] long at [viewport]: a grid line when [lines] and a label
 * in ms when [labels] at each tick of [timeTicks], along the bottom edge.
 */
internal fun DrawScope.drawTimeAxis(
    measurer: TextMeasurer,
    viewport: Viewport,
    durationMs: Int,
    labelStyle: TextStyle,
    gridColor: Color,
    lines: Boolean = true,
    labels: Boolean = true,
) {
    val plotWidth = size.width - 2 * ENVELOPE_PADDING
    if (plotWidth <= 0f || durationMs <= 0) return
    val window = visibleWindow(viewport, plotWidth)
    val total = plotWidth * viewport.zoom
    for (ms in timeTicks(durationMs, window, plotWidth, MIN_TICK_SPACING)) {
        val x = ENVELOPE_PADDING + ms.toFloat() / durationMs * total - viewport.scrollOffset
        if (lines) drawLine(gridColor, Offset(x, 0f), Offset(x, size.height))
        if (!labels) continue
        val layout = measurer.measure("$ms ms", labelStyle)
        if (x + LABEL_INSET + layout.size.width <= size.width) {
            drawText(
                layout,
                topLeft = Offset(x + LABEL_INSET, size.height - layout.size.height - LABEL_INSET),
            )
        }
    }
}

/** The value grid: a line at each of [valueGridFractions], the middle one in [midColor]. */
internal fun DrawScope.drawValueGrid(
    gridColor: Color,
    midColor: Color,
) {
    val plotHeight = size.height - 2 * ENVELOPE_PADDING
    for (fraction in valueGridFractions()) {
        val y = ENVELOPE_PADDING + plotHeight * (1f - fraction)
        drawLine(
            if (fraction == MID_FRACTION) midColor else gridColor,
            Offset(0f, y),
            Offset(size.width, y),
        )
    }
}

/**
 * The value labels of [envelope] in [slot]: one at each of [VALUE_ROWS] + 1 ticks of [valueTicks],
 * in the unit of the slot on the left. The bottom label sits above the time labels.
 */
internal fun DrawScope.drawValueLabels(
    measurer: TextMeasurer,
    slot: SignalChainSlot,
    envelope: Envelope?,
    labelStyle: TextStyle,
) {
    val plotHeight = size.height - 2 * ENVELOPE_PADDING
    val ticks = envelope?.let { valueTicks(slot, it, VALUE_ROWS + 1) }.orEmpty()
    val unit = slot.range?.unit
    for (tick in ticks) {
        val y = ENVELOPE_PADDING + plotHeight * (1f - tick.fraction)
        val layout =
            measurer.measure(formatDecimal(tick.value, VALUE_DECIMALS) + unit.suffix(), labelStyle)
        val top = valueLabelTop(y, layout.size.height.toFloat(), size.height)
        drawText(layout, topLeft = Offset(LABEL_INSET, top))
    }
}

/**
 * The top of a value label of [labelHeight] centred on [y], kept above the time labels of a plot
 * [height] tall and below its top edge; a plot too short for both keeps the label at the top.
 */
internal fun valueLabelTop(
    y: Float,
    labelHeight: Float,
    height: Float,
): Float =
    (y - labelHeight / 2f).coerceAtMost(height - 2 * labelHeight - LABEL_INSET).coerceAtLeast(0f)
