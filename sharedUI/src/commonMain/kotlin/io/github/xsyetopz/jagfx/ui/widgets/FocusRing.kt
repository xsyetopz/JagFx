package io.github.xsyetopz.jagfx.ui.widgets

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/** Draws a [width] ring in [color] just inside the bounds, over the border, while [focused]. */
internal fun Modifier.focusRing(
    focused: Boolean,
    color: Color,
    width: Dp,
    radius: Dp,
): Modifier =
    if (!focused) {
        this
    } else {
        drawWithContent {
            drawContent()
            val stroke = width.toPx()
            drawRoundRect(
                color = color,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                cornerRadius = CornerRadius(radius.toPx()),
                style = Stroke(stroke),
            )
        }
    }

/** Draws a [thickness] bar in [color] along the bottom edge while [active], a mark besides color. */
internal fun Modifier.activeMark(
    active: Boolean,
    color: Color,
    thickness: Dp,
): Modifier =
    if (!active) {
        this
    } else {
        drawWithContent {
            drawContent()
            val bar = thickness.toPx()
            drawRect(color, Offset(0f, size.height - bar), Size(size.width, bar))
        }
    }
