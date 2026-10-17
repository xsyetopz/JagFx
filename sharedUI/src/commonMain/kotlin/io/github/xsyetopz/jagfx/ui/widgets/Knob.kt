package io.github.xsyetopz.jagfx.ui.widgets

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.xsyetopz.jagfx.ui.canvas.GestureCallbacks
import io.github.xsyetopz.jagfx.ui.theme.JagFxColors
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val ARC_START_DEGREES = 135f
private const val ARC_SWEEP_DEGREES = 270f
private const val DEGREES_PER_HALF_TURN = 180f
private const val MIN_RADIUS = 6f
private const val RADIUS_INSET_X = 3f
private const val RADIUS_INSET_Y = 2f
private const val TRACK_GAP = 2f
private const val ARC_STROKE = 3f
private const val BODY_STROKE = 1f
private const val INDICATOR_STROKE = 2f
private const val INDICATOR_INNER = 0.35f
private const val INDICATOR_OUTER = 0.85f

/** Alpha of a disabled knob's arc, as Material dims disabled content. */
private const val DISABLED_ALPHA = 0.38f
private val LabelBandHeight = 13.dp
private val KnobWidth = 56.dp
private val KnobHeight = 44.dp

/**
 * The [range] of a [Knob], the [step] of one wheel notch, and the [defaultValue] that a double
 * click restores; without a default the double click restores the minimum.
 */
@Immutable
internal data class KnobScale(
    val range: ClosedFloatingPointRange<Float>,
    val step: Float = 1f,
    val defaultValue: Float? = null,
)

/** Callbacks of a [Knob]: [onValueChange] and the [gestures] that bracket each drag. */
internal class KnobCallbacks(
    val onValueChange: (Float) -> Unit,
    val gestures: GestureCallbacks = GestureCallbacks(),
)

/**
 * Texts and state of a [Knob]: the [label], the [valueText] under it when not empty, and
 * whether the knob responds to input.
 */
@Immutable
internal data class KnobDisplay(
    val label: String = "",
    val valueText: String = "",
    val enabled: Boolean = true,
)

/**
 * Rotary knob that is set by vertical drag, mouse wheel and double click (reset to the default
 * of [scale]). [display] sets the texts and whether the knob is enabled.
 */
@Composable
internal fun Knob(
    value: Float,
    scale: KnobScale,
    callbacks: KnobCallbacks,
    modifier: Modifier = Modifier,
    display: KnobDisplay = KnobDisplay(),
) {
    val (valueRange, step, defaultValue) = scale
    val (label, valueText, enabled) = display
    val colors = JagFxTheme.colors
    val currentValue by rememberUpdatedState(value)
    val currentOnChange by rememberUpdatedState(callbacks.onValueChange)
    val currentRange by rememberUpdatedState(valueRange)
    val currentStart by rememberUpdatedState(callbacks.gestures.onStart)
    val currentEnd by rememberUpdatedState(callbacks.gestures.onEnd)
    val emit: (Float) -> Unit = { if (it != currentValue) currentOnChange(it) }

    val gestures =
        if (enabled) {
            Modifier.knobWheel(step, { currentValue }, { currentRange }, emit).knobGestures(
                value = { currentValue },
                onReset = { emit(ScrubMath.resetValue(defaultValue, currentRange)) },
                onDrag = { anchor, up -> emit(ScrubMath.dragValue(anchor, up, currentRange)) },
                onStart = { currentStart() },
                onEnd = { currentEnd() },
            )
        } else {
            Modifier
        }

    val height = if (valueText.isEmpty()) KnobHeight else KnobHeight + LabelBandHeight
    Column(
        modifier = modifier.size(KnobWidth, height),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(gestures)
                    .knobFace(enabled, colors) { ScrubMath.fraction(currentValue, currentRange) },
        )
        if (label.isNotEmpty()) {
            KnobLabel(
                label,
                JagFxTheme.typography.label,
                colors.textDim,
            )
        }
        if (valueText.isNotEmpty()) {
            KnobLabel(valueText, JagFxTheme.typography.body, colors.text)
        }
    }
}

/** Draws the knob at the [fraction] it reads on each frame, dimmed unless [enabled]. */
private fun Modifier.knobFace(
    enabled: Boolean,
    colors: JagFxColors,
    fraction: () -> Float,
): Modifier =
    drawWithCache {
        val strokes =
            KnobStrokes(
                Stroke(ARC_STROKE.dp.toPx()),
                Stroke(BODY_STROKE.dp.toPx()),
                INDICATOR_STROKE.dp.toPx(),
            )
        val fill = if (enabled) colors.accent else colors.text.copy(alpha = DISABLED_ALPHA)
        onDrawBehind { drawKnob(fraction(), fill, colors, strokes) }
    }

private class KnobStrokes(
    val track: Stroke,
    val body: Stroke,
    val indicatorWidth: Float,
)

private fun DrawScope.drawKnob(
    fraction: Float,
    fillColor: Color,
    colors: JagFxColors,
    strokes: KnobStrokes,
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius =
        max(
            MIN_RADIUS.dp.toPx(),
            min(center.x - RADIUS_INSET_X.dp.toPx(), center.y - RADIUS_INSET_Y.dp.toPx()),
        )
    val arcRadius = radius + TRACK_GAP.dp.toPx()
    val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
    val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
    drawArc(
        color = colors.border,
        startAngle = ARC_START_DEGREES,
        sweepAngle = ARC_SWEEP_DEGREES,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = strokes.track,
    )
    drawArc(
        color = fillColor,
        startAngle = ARC_START_DEGREES,
        sweepAngle = ARC_SWEEP_DEGREES * fraction,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = strokes.track,
    )
    drawCircle(colors.input, radius, center)
    drawCircle(colors.textSubtle, radius, center, style = strokes.body)
    val angle =
        (ARC_START_DEGREES + ARC_SWEEP_DEGREES * fraction) * (PI.toFloat() / DEGREES_PER_HALF_TURN)
    val direction = Offset(cos(angle), sin(angle))
    drawLine(
        color = colors.text,
        start = center + direction * (radius * INDICATOR_INNER),
        end = center + direction * (radius * INDICATOR_OUTER),
        strokeWidth = strokes.indicatorWidth,
    )
}

private fun Modifier.knobGestures(
    value: () -> Float,
    onReset: () -> Unit,
    onDrag: (anchor: Float, upwardPixels: Float) -> Unit,
    onStart: () -> Unit,
    onEnd: () -> Unit,
): Modifier =
    pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = {
                onStart()
                onReset()
                onEnd()
            },
        )
    }.pointerInput(Unit) {
        var anchor = 0f
        var travelled = 0f
        detectDragGestures(
            onDragStart = {
                anchor = value()
                travelled = 0f
                onStart()
            },
            onDragEnd = onEnd,
            onDragCancel = onEnd,
        ) { change, drag ->
            change.consume()
            travelled -= drag.y
            onDrag(anchor, travelled)
        }
    }

private fun Modifier.knobWheel(
    step: Float,
    value: () -> Float,
    range: () -> ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
): Modifier =
    pointerInput(step) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll) {
                    val change = event.changes.first()
                    // Wheel up reports a negative y delta.
                    onChange(ScrubMath.wheelValue(value(), -change.scrollDelta.y, step, range()))
                    change.consume()
                }
            }
        }
    }

@Composable
private fun KnobLabel(
    text: String,
    style: TextStyle,
    color: Color,
) {
    Box(
        modifier = Modifier.height(LabelBandHeight).fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text = text, style = style.copy(color = color), maxLines = 1)
    }
}
