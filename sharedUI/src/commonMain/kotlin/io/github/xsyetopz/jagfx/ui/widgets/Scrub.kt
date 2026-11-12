package io.github.xsyetopz.jagfx.ui.widgets

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.xsyetopz.jagfx.ui.canvas.GestureCallbacks

/** Least hit area of scrubbed control, whatever it draws. */
private val MinScrubTarget = 24.dp

/**
 * [range] that one scrub covers, [step] of one wheel notch, and [defaultValue] that
 * double click restores; without default double click restores minimum.
 */
@Immutable
internal data class ScrubScale(
    val range: ClosedFloatingPointRange<Float>,
    val step: Float = 1f,
    val defaultValue: Float? = null,
)

/** [value] and [scale] that [scrub] reads, [onValueChange], and [gestures] around each drag. */
internal class ScrubState(
    val value: Int,
    val scale: ScrubScale,
    val onValueChange: (Int) -> Unit,
    val gestures: GestureCallbacks = GestureCallbacks(),
)

/**
 * Sets value of [state] by wheel and double click (default), each change one undo step;
 * cursor is hand. Dragging belongs to stock slider that this sits on.
 */
internal fun Modifier.scrub(state: State<ScrubState>): Modifier {
    fun emit(value: Float) {
        val next = value.toInt()
        if (next != state.value.value) state.value.onValueChange(next)
    }
    return sizeIn(minWidth = MinScrubTarget, minHeight = MinScrubTarget)
        .pointerHoverIcon(PointerIcon.Hand)
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = {
                    val current = state.value
                    current.gestures.onStart()
                    emit(ScrubMath.resetValue(current.scale.defaultValue, current.scale.range))
                    current.gestures.onEnd()
                },
            )
        }.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val change = event.changes.first()
                        val current = state.value
                        // Wheel up reports negative y delta.
                        emit(
                            ScrubMath.wheelValue(
                                current.value.toFloat(),
                                -change.scrollDelta.y,
                                current.scale.step,
                                current.scale.range,
                            ),
                        )
                        change.consume()
                    }
                }
            }
        }
}
