package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.xsyetopz.jagfx.ui.screen.PointReadout
import io.github.xsyetopz.jagfx.ui.screen.pointReadout
import io.github.xsyetopz.jagfx.ui.screen.readoutText
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.JagMenu
import io.github.xsyetopz.jagfx.ui.widgets.JagMenuItem
import io.github.xsyetopz.jagfx.ui.widgets.JagText
import io.github.xsyetopz.jagfx.ui.widgets.focusRing
import kotlin.math.roundToInt

/**
 * An envelope editor plot.
 * Zoom, scroll, and selection are hoisted: the canvas draws [view] and reports changes through
 * [callbacks].
 *
 * When [EnvelopeStyle.interactive] of [style] is false (thumbnails), the canvas only draws. The
 * selected index of [view] is a segment index or -1.
 */
@Composable
internal fun EnvelopeCanvas(
    view: EnvelopeView,
    callbacks: EnvelopeCallbacks,
    menuLabels: EnvelopeMenuLabels,
    modifier: Modifier = Modifier,
    style: EnvelopeStyle = EnvelopeStyle(),
) {
    val measurer = rememberTextMeasurer()
    val latestView = rememberUpdatedState(view)
    val latestCallbacks = rememberUpdatedState(callbacks)
    var menu by remember { mutableStateOf<EnvelopeMenu?>(null) }
    var hoverIcon by remember { mutableStateOf(PointerIcon.Default) }
    var hovered by remember { mutableStateOf<Int?>(null) }
    var focused by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val focusRequester = remember { FocusRequester() }
    val input =
        if (style.interactive) {
            Modifier
                .onKeyEvent { handleEnvelopeKey(it, latestView.value, latestCallbacks.value) }
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .focusable()
                .pointerHoverIcon(hoverIcon)
                .envelopePointerInput(
                    view = latestView,
                    callbacks = latestCallbacks,
                    hooks =
                        EnvelopeCanvasHooks(
                            requestFocus = { focusRequester.requestFocus() },
                            onMenu = { menu = it },
                            onHoverIcon = { hoverIcon = it },
                            onReadout = { hovered = it },
                        ),
                )
        } else {
            Modifier
        }
    Box(
        modifier
            .clipToBounds()
            .onSizeChanged { size = it }
            .then(input)
            .focusRing(focused, JagFxTheme.colors.accent, JagFxTheme.dimens.focusRing, 0.dp)
            .drawEnvelope(view, style, measurer, JagFxTheme.palette),
    ) {
        val point = hovered ?: view.selectedIndex.takeIf { it >= 0 }
        val slot = style.slot
        val readout =
            if (view.envelope != null && slot != null && point != null) {
                pointReadout(slot, view.envelope, point, style.durationMs)
            } else {
                null
            }
        readout?.let { PointReadoutLabel(it, view, size, point ?: 0) }
        menu?.let { open ->
            EnvelopeContextMenu(
                menu = open,
                labels = menuLabels,
                onChoose = { latestCallbacks.value.applyMenu(open) },
                onDismiss = { menu = null },
            )
        }
    }
}

@Composable
private fun EnvelopeContextMenu(
    menu: EnvelopeMenu,
    labels: EnvelopeMenuLabels,
    onChoose: () -> Unit,
    onDismiss: () -> Unit,
) {
    val label =
        when (menu.kind) {
            EnvelopeMenuKind.DeletePoint -> labels.deletePoint
            EnvelopeMenuKind.AddPointHere -> labels.addPointHere
            EnvelopeMenuKind.AddPointAtEnd -> labels.addPointAtEnd
        }
    JagMenu(onDismiss, IntOffset(menu.position.x.roundToInt(), menu.position.y.roundToInt())) {
        JagMenuItem(label, {
            onChoose()
            onDismiss()
        })
    }
}

/** The readout of breakpoint [index], placed beside the point and kept inside the canvas. */
@Composable
private fun PointReadoutLabel(
    readout: PointReadout,
    view: EnvelopeView,
    size: IntSize,
    index: Int,
) {
    val geometry =
        EnvelopeGeometry(
            segments = view.envelope?.segments.orEmpty(),
            width = size.width.toFloat(),
            height = size.height.toFloat(),
            viewport = Viewport(view.zoom, view.scrollOffset),
            scaleMode = view.scaleMode,
        )
    if (index !in 0 until geometry.pointCount) return
    val x = geometry.pointX(index)
    val y = geometry.pointY(index)
    val gap = READOUT_GAP_PX
    JagText(
        readoutText(readout),
        Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val left =
                    (x + gap)
                        .coerceAtMost(
                            size.width - placeable.width.toFloat(),
                        ).coerceAtLeast(0f)
                val top = (y - gap - placeable.height).coerceAtLeast(0f)
                layout(
                    placeable.width,
                    placeable.height,
                ) { placeable.place(left.roundToInt(), top.roundToInt()) }
            }.background(JagFxTheme.colors.header)
            .padding(horizontal = JagFxTheme.dimens.spaceMd),
        JagFxTheme.colors.text,
    )
}

private const val READOUT_GAP_PX = 8f
