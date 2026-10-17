package io.github.xsyetopz.jagfx.ui.widgets

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.xsyetopz.jagfx.core.model.Waveform
import kotlin.math.PI
import kotlin.math.sin

/** Draws an icon filling the current draw scope in the given color. */
internal typealias IconPainter = DrawScope.(Color) -> Unit

/** A polyline or polygon in unit coordinates (0..1 across the icon box). */
private class Shape(
    val points: List<Pair<Float, Float>>,
    val closed: Boolean = false,
    val filled: Boolean = false,
) {
    /** The shapes of each icon. */
    companion object {
        val Play =
            Shape(listOf(0.3f to 0.2f, 0.8f to 0.5f, 0.3f to 0.8f), closed = true, filled = true)
        val Stop =
            Shape(
                listOf(0.25f to 0.25f, 0.75f to 0.25f, 0.75f to 0.75f, 0.25f to 0.75f),
                closed = true,
                filled = true,
            )
        val Loop =
            listOf(
                Shape(listOf(0.8f to 0.55f, 0.8f to 0.3f, 0.2f to 0.3f, 0.2f to 0.45f)),
                Shape(
                    listOf(0.1f to 0.4f, 0.2f to 0.55f, 0.3f to 0.4f),
                    closed = true,
                    filled = true,
                ),
                Shape(listOf(0.2f to 0.45f, 0.2f to 0.7f, 0.8f to 0.7f, 0.8f to 0.55f)),
                Shape(
                    listOf(0.7f to 0.6f, 0.8f to 0.45f, 0.9f to 0.6f),
                    closed = true,
                    filled = true,
                ),
            )
        val Open =
            listOf(
                Shape(
                    listOf(
                        0.1f to 0.25f,
                        0.4f to 0.25f,
                        0.5f to 0.35f,
                        0.9f to 0.35f,
                        0.9f to 0.75f,
                        0.1f to 0.75f,
                    ),
                    closed = true,
                ),
            )
        val Save =
            listOf(
                Shape(
                    listOf(
                        0.15f to 0.15f,
                        0.75f to 0.15f,
                        0.85f to 0.25f,
                        0.85f to 0.85f,
                        0.15f to 0.85f,
                    ),
                    closed = true,
                ),
                Shape(listOf(0.3f to 0.15f, 0.3f to 0.38f, 0.65f to 0.38f, 0.65f to 0.15f)),
                Shape(listOf(0.3f to 0.85f, 0.3f to 0.6f, 0.7f to 0.6f, 0.7f to 0.85f)),
            )
        val Copy =
            listOf(
                Shape(
                    listOf(0.15f to 0.3f, 0.6f to 0.3f, 0.6f to 0.85f, 0.15f to 0.85f),
                    closed = true,
                ),
                Shape(
                    listOf(
                        0.4f to 0.3f,
                        0.4f to 0.15f,
                        0.85f to 0.15f,
                        0.85f to 0.7f,
                        0.6f to 0.7f,
                    ),
                ),
            )
        val ResetArrow =
            Shape(listOf(0.5f to 0.0f, 0.7f to 0.2f, 0.5f to 0.4f), closed = true, filled = true)
        val Off = listOf(Shape(listOf(0.15f to 0.5f, 0.85f to 0.5f)))
        val Undefined =
            listOf(
                Shape(listOf(0.1f to 0.5f, 0.25f to 0.5f)),
                Shape(listOf(0.42f to 0.5f, 0.58f to 0.5f)),
                Shape(listOf(0.75f to 0.5f, 0.9f to 0.5f)),
            )
        val Undo =
            listOf(
                Shape(listOf(0.85f to 0.8f, 0.85f to 0.45f, 0.3f to 0.45f)),
                Shape(
                    listOf(0.1f to 0.45f, 0.32f to 0.25f, 0.32f to 0.65f),
                    closed = true,
                    filled = true,
                ),
            )
        val Redo = Undo.map(::mirrored)
        val Add =
            listOf(
                Shape(listOf(0.5f to 0.15f, 0.5f to 0.85f)),
                Shape(listOf(0.15f to 0.5f, 0.85f to 0.5f)),
            )
        val Close =
            listOf(
                Shape(listOf(0.2f to 0.2f, 0.8f to 0.8f)),
                Shape(listOf(0.8f to 0.2f, 0.2f to 0.8f)),
            )
        val Panel =
            listOf(
                Shape(
                    listOf(0.1f to 0.15f, 0.9f to 0.15f, 0.9f to 0.85f, 0.1f to 0.85f),
                    closed = true,
                ),
                Shape(listOf(0.62f to 0.15f, 0.62f to 0.85f)),
            )
        val Fit =
            listOf(
                Shape(listOf(0.1f to 0.35f, 0.1f to 0.1f, 0.35f to 0.1f)),
                Shape(listOf(0.65f to 0.1f, 0.9f to 0.1f, 0.9f to 0.35f)),
                Shape(listOf(0.9f to 0.65f, 0.9f to 0.9f, 0.65f to 0.9f)),
                Shape(listOf(0.35f to 0.9f, 0.1f to 0.9f, 0.1f to 0.65f)),
            )
        val Snap =
            listOf(
                Shape(
                    listOf(
                        0.25f to 0.15f,
                        0.25f to 0.55f,
                        0.35f to 0.78f,
                        0.5f to 0.85f,
                        0.65f to 0.78f,
                        0.75f to 0.55f,
                        0.75f to 0.15f,
                    ),
                ),
                Shape(listOf(0.15f to 0.32f, 0.35f to 0.32f)),
                Shape(listOf(0.65f to 0.32f, 0.85f to 0.32f)),
            )
        val Import =
            listOf(
                Shape(listOf(0.15f to 0.6f, 0.15f to 0.85f, 0.85f to 0.85f, 0.85f to 0.6f)),
                Shape(listOf(0.5f to 0.1f, 0.5f to 0.5f)),
                Shape(
                    listOf(0.3f to 0.42f, 0.5f to 0.68f, 0.7f to 0.42f),
                    closed = true,
                    filled = true,
                ),
            )
        val Export =
            listOf(
                Shape(listOf(0.15f to 0.6f, 0.15f to 0.85f, 0.85f to 0.85f, 0.85f to 0.6f)),
                Shape(listOf(0.5f to 0.35f, 0.5f to 0.7f)),
                Shape(
                    listOf(0.3f to 0.36f, 0.5f to 0.1f, 0.7f to 0.36f),
                    closed = true,
                    filled = true,
                ),
            )
        val Headphones =
            listOf(
                Shape(
                    listOf(0.12f to 0.55f, 0.28f to 0.55f, 0.28f to 0.85f, 0.12f to 0.85f),
                    closed = true,
                    filled = true,
                ),
                Shape(
                    listOf(0.72f to 0.55f, 0.88f to 0.55f, 0.88f to 0.85f, 0.72f to 0.85f),
                    closed = true,
                    filled = true,
                ),
            )

        /** [shape] mirrored left to right. */
        private fun mirrored(shape: Shape): Shape =
            Shape(shape.points.map { (x, y) -> (1f - x) to y }, shape.closed, shape.filled)

        val Square =
            listOf(
                Shape(
                    listOf(
                        0.1f to 0.7f,
                        0.1f to 0.3f,
                        0.4f to 0.3f,
                        0.4f to 0.7f,
                        0.6f to 0.7f,
                        0.6f to 0.3f,
                        0.9f to 0.3f,
                        0.9f to 0.7f,
                    ),
                ),
            )
        val Saw =
            listOf(
                Shape(
                    listOf(
                        0.1f to 0.5f,
                        0.3f to 0.25f,
                        0.3f to 0.75f,
                        0.7f to 0.25f,
                        0.7f to 0.75f,
                        0.9f to 0.5f,
                    ),
                ),
            )
        val Noise =
            listOf(
                Shape(
                    listOf(
                        0.1f to 0.5f,
                        0.2f to 0.25f,
                        0.3f to 0.75f,
                        0.4f to 0.35f,
                        0.5f to 0.65f,
                        0.6f to 0.2f,
                        0.7f to 0.8f,
                        0.8f to 0.4f,
                        0.9f to 0.5f,
                    ),
                ),
            )
        val Sine =
            listOf(
                Shape(
                    List(SINE_SAMPLES + 1) {
                        val x = it.toFloat() / SINE_SAMPLES
                        (RESET_INSET / 2f + x * (1f - RESET_INSET)) to
                            (0.5f - SINE_AMPLITUDE * sin(2f * PI.toFloat() * x))
                    },
                ),
            )
    }
}

private const val STROKE_RATIO = 0.1f
private const val SINE_SAMPLES = 24
private const val SINE_AMPLITUDE = 0.3f
private const val RESET_START = 40f
private const val RESET_SWEEP = 290f
private const val RESET_INSET = 0.2f
private const val RESET_SPAN = 0.6f
private const val DOT_RADIUS = 0.09f
private const val DOT_LEFT = 0.2f
private const val DOT_MIDDLE = 0.5f
private const val DOT_RIGHT = 0.8f
private val DotCenters = listOf(DOT_LEFT, DOT_MIDDLE, DOT_RIGHT)
private const val POWER_START = 300f
private const val POWER_SWEEP = 300f
private const val POWER_INSET = 0.15f
private const val POWER_SPAN = 0.7f
private const val POWER_STEM_TOP = 0.08f
private const val POWER_STEM_BOTTOM = 0.45f
private const val BAND_INSET = 0.18f
private const val BAND_TOP = 0.15f
private const val BAND_SPAN = 0.64f
private const val HALF = 0.5f
private const val HALF_TURN = 180f

private fun DrawScope.drawShapes(
    shapes: List<Shape>,
    color: Color,
) {
    val strokeWidth = size.minDimension * STROKE_RATIO
    for (shape in shapes) {
        val path = Path()
        shape.points.forEachIndexed { index, (x, y) ->
            val px = x * size.width
            val py = y * size.height
            if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        if (shape.closed) path.close()
        if (shape.filled) {
            drawPath(path, color)
        } else {
            drawPath(
                path,
                color,
                style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

private fun shapesIcon(shapes: List<Shape>): IconPainter = { color -> drawShapes(shapes, color) }

private fun DrawScope.stroke(): Stroke =
    Stroke(size.minDimension * STROKE_RATIO, cap = StrokeCap.Round)

/** Icon painters of the editor toolbar and waveform buttons. */
internal object Icons {
    val Play: IconPainter = shapesIcon(listOf(Shape.Play))
    val Stop: IconPainter = shapesIcon(listOf(Shape.Stop))
    val Loop: IconPainter = shapesIcon(Shape.Loop)
    val Open: IconPainter = shapesIcon(Shape.Open)
    val Save: IconPainter = shapesIcon(Shape.Save)
    val Copy: IconPainter = shapesIcon(Shape.Copy)
    val Undo: IconPainter = shapesIcon(Shape.Undo)
    val Redo: IconPainter = shapesIcon(Shape.Redo)
    val Add: IconPainter = shapesIcon(Shape.Add)
    val Close: IconPainter = shapesIcon(Shape.Close)
    val Fit: IconPainter = shapesIcon(Shape.Fit)
    val Panel: IconPainter = shapesIcon(Shape.Panel)
    val Snap: IconPainter = shapesIcon(Shape.Snap)
    val Import: IconPainter = shapesIcon(Shape.Import)
    val Export: IconPainter = shapesIcon(Shape.Export)

    /** A power symbol: an open ring with a stem through the gap. */
    val Power: IconPainter = { color ->
        drawArc(
            color = color,
            startAngle = POWER_START,
            sweepAngle = POWER_SWEEP,
            useCenter = false,
            topLeft = Offset(size.width * POWER_INSET, size.height * POWER_INSET),
            size = Size(size.width * POWER_SPAN, size.height * POWER_SPAN),
            style = stroke(),
        )
        drawLine(
            color,
            Offset(size.width * HALF, size.height * POWER_STEM_TOP),
            Offset(size.width * HALF, size.height * POWER_STEM_BOTTOM),
            size.minDimension * STROKE_RATIO,
            StrokeCap.Round,
        )
    }

    /** Headphones, for playing the selected voice alone. */
    val Solo: IconPainter = { color ->
        drawArc(
            color = color,
            startAngle = HALF_TURN,
            sweepAngle = HALF_TURN,
            useCenter = false,
            topLeft = Offset(size.width * BAND_INSET, size.height * BAND_TOP),
            size = Size(size.width * BAND_SPAN, size.height * BAND_SPAN),
            style = stroke(),
        )
        drawShapes(Shape.Headphones, color)
    }

    /** Open circular arrow. */
    val Reset: IconPainter = { color ->
        val strokeWidth = size.minDimension * STROKE_RATIO
        drawArc(
            color = color,
            startAngle = RESET_START,
            sweepAngle = RESET_SWEEP,
            useCenter = false,
            topLeft = Offset(size.width * RESET_INSET, size.height * RESET_INSET),
            size = Size(size.width * RESET_SPAN, size.height * RESET_SPAN),
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
        )
        drawShapes(listOf(Shape.ResetArrow), color)
    }

    /** Three dots in a row, for an overflow menu. */
    val More: IconPainter = { color ->
        for (x in DotCenters) {
            drawCircle(
                color,
                size.minDimension * DOT_RADIUS,
                Offset(
                    size.width * x,
                    size.height / 2f,
                ),
            )
        }
    }

    private val Waveforms: Map<Waveform, IconPainter> =
        mapOf(
            Waveform.Off to shapesIcon(Shape.Off),
            Waveform.Square to shapesIcon(Shape.Square),
            Waveform.Sine to shapesIcon(Shape.Sine),
            Waveform.Saw to shapesIcon(Shape.Saw),
            Waveform.Noise to shapesIcon(Shape.Noise),
        )

    private val Undefined: IconPainter = shapesIcon(Shape.Undefined)

    /** Shape icon of [waveform]; a dashed line for an undefined id ([Waveform.isUndefined]). */
    fun waveform(waveform: Waveform): IconPainter = Waveforms[waveform] ?: Undefined
}
