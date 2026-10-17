package io.github.xsyetopz.jagfx.ui.widgets

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import io.github.xsyetopz.jagfx.ui.canvas.GestureCallbacks

/**
 * The [range] that one scrub covers, the [step] of one wheel notch, and the [defaultValue] that a
 * double click restores; without a default the double click restores the minimum.
 */
@Immutable
internal data class ScrubScale(
    val range: ClosedFloatingPointRange<Float>,
    val step: Float = 1f,
    val defaultValue: Float? = null,
)

/** The [value] and [scale] that a [scrub] reads, [onValueChange], and the [gestures] around each drag. */
internal class ScrubState(
    val value: Int,
    val scale: ScrubScale,
    val onValueChange: (Int) -> Unit,
    val gestures: GestureCallbacks = GestureCallbacks(),
)

/**
 * Sets the value of [state] by drag (right or up increases, one hundred pixels per range), wheel,
 * and double click (the default), each change one undo step; the cursor is a hand.
 */
internal fun Modifier.scrub(state: State<ScrubState>): Modifier {
    fun emit(value: Float) {
        val next = value.toInt()
        if (next != state.value.value) state.value.onValueChange(next)
    }
    return pointerHoverIcon(PointerIcon.Hand)
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
            var anchor = 0f
            var travelled = 0f
            detectDragGestures(
                onDragStart = {
                    anchor = state.value.value.toFloat()
                    travelled = 0f
                    state.value.gestures.onStart()
                },
                onDragEnd = { state.value.gestures.onEnd() },
                onDragCancel = { state.value.gestures.onEnd() },
            ) { change, drag ->
                change.consume()
                travelled += drag.x - drag.y
                emit(ScrubMath.dragValue(anchor, travelled, state.value.scale.range))
            }
        }.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val change = event.changes.first()
                        val current = state.value
                        // Wheel up reports a negative y delta.
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
