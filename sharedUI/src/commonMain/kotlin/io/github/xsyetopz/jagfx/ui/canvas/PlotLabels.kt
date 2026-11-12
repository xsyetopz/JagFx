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
private const val MIN_TICK_SPACING = 64f

/** Rows of the value grid: [VALUE_ROWS] + 1 lines, the middle one in the mid line color. */
internal const val VALUE_ROWS = 4

/**
 * The time axis of a voice [durationMs] long at [viewport]: a grid line and a label in ms at each
 * tick of [timeTicks], along the bottom edge.
 */
internal fun DrawScope.drawTimeAxis(
    measurer: TextMeasurer,
    viewport: Viewport,
    durationMs: Int,
    labelStyle: TextStyle,
    gridColor: Color,
) {
    val plotWidth = size.width - 2 * ENVELOPE_PADDING
    if (plotWidth <= 0f || durationMs <= 0) return
    val window = visibleWindow(viewport, plotWidth)
    val total = plotWidth * viewport.zoom
    for (ms in timeTicks(durationMs, window, plotWidth, MIN_TICK_SPACING)) {
        val x = ENVELOPE_PADDING + ms.toFloat() / durationMs * total - viewport.scrollOffset
        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height))
        val layout = measurer.measure("$ms ms", labelStyle)
        if (x + LABEL_INSET + layout.size.width <= size.width) {
            drawText(
                layout,
                topLeft = Offset(x + LABEL_INSET, size.height - layout.size.height - LABEL_INSET),
            )
        }
    }
}

/**
 * The value axis of [envelope] in [slot]: a grid line at each of [VALUE_ROWS] + 1 ticks of
 * [valueTicks], the middle one in [midColor], each labelled in the unit of the slot on the left.
 * The bottom label sits above the time labels.
 */
internal fun DrawScope.drawValueAxis(
    measurer: TextMeasurer,
    slot: SignalChainSlot,
    envelope: Envelope?,
    labelStyle: TextStyle,
    gridColor: Color,
    midColor: Color,
) {
    val plotHeight = size.height - 2 * ENVELOPE_PADDING
    for (i in 0..VALUE_ROWS) {
        val y = ENVELOPE_PADDING + plotHeight * (1f - i.toFloat() / VALUE_ROWS)
        drawLine(
            if (i * 2 ==
                VALUE_ROWS
            ) {
                midColor
            } else {
                gridColor
            },
            Offset(0f, y),
            Offset(size.width, y),
        )
    }
    val ticks = envelope?.let { valueTicks(slot, it, VALUE_ROWS + 1) }.orEmpty()
    val unit = slot.range?.unit
    for (tick in ticks) {
        val y = ENVELOPE_PADDING + plotHeight * (1f - tick.fraction)
        val layout =
            measurer.measure(formatDecimal(tick.value, VALUE_DECIMALS) + unit.suffix(), labelStyle)
        val top =
            (y - layout.size.height / 2f).coerceIn(
                0f,
                size.height - 2 * layout.size.height - LABEL_INSET,
            )
        drawText(layout, topLeft = Offset(LABEL_INSET, top))
    }
}
