package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

/** A pinch that changes the finger distance by this factor steps the zoom once. */
private const val PINCH_STEP = 1.25f

/**
 * Steps the zoom on a wheel event or a two-finger pinch, and keeps the scroll offset in range.
 * A pinch steps once each time the finger distance grows or shrinks by [PINCH_STEP].
 */
internal fun Modifier.zoomOnWheel(
    zoom: State<Int>,
    scroll: State<Float>,
    onZoomChange: State<(Int) -> Unit>,
    onScrollOffsetChange: State<(Float) -> Unit>,
): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            var pinch = 1f
            while (true) {
                val event = awaitPointerEvent()
                val delta =
                    when {
                        event.type == PointerEventType.Scroll -> {
                            -event.changes
                                .first()
                                .scrollDelta.y
                        }

                        event.changes.count { it.pressed } >= 2 -> {
                            pinch *= event.calculateZoom()
                            when {
                                pinch >= PINCH_STEP -> 1f
                                pinch <= 1f / PINCH_STEP -> -1f
                                else -> 0f
                            }
                        }

                        else -> {
                            pinch = 1f
                            continue
                        }
                    }
                if (delta != 0f) pinch = 1f
                val next = stepZoom(zoom.value, delta)
                if (next != zoom.value) {
                    onZoomChange.value(next)
                    onScrollOffsetChange.value(
                        scrollAfterZoom(scroll.value, size.width.toFloat(), next),
                    )
                }
                event.changes.forEach { it.consume() }
            }
        }
    }

/** Horizontal drag pans the view at zoom above 1: `offset = start offset - drag distance`. */
internal fun Modifier.panWhenZoomed(
    zoom: State<Int>,
    scroll: State<Float>,
    onScrollOffsetChange: State<(Float) -> Unit>,
): Modifier =
    pointerInput(Unit) {
        var startOffset = 0f
        var dragged = 0f
        detectHorizontalDragGestures(
            onDragStart = {
                startOffset = scroll.value
                dragged = 0f
            },
        ) { change, dragAmount ->
            if (zoom.value > 1) {
                dragged += dragAmount
                val max = maxScrollOffset(size.width.toFloat(), zoom.value)
                onScrollOffsetChange.value((startOffset - dragged).coerceIn(0f, max))
                change.consume()
            }
        }
    }
