package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.JagMenu
import io.github.xsyetopz.jagfx.ui.widgets.JagMenuItem
import io.github.xsyetopz.jagfx.ui.widgets.JagText
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
    var readout by remember { mutableStateOf<EnvelopeReadout?>(null) }
    val focusRequester = remember { FocusRequester() }
    val input =
        if (style.interactive) {
            Modifier
                .onKeyEvent { handleEnvelopeKey(it, latestView.value, latestCallbacks.value) }
                .focusRequester(focusRequester)
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
                            onReadout = { readout = it },
                        ),
                )
        } else {
            Modifier
        }
    Box(
        modifier
            .clipToBounds()
            .then(input)
            .drawEnvelope(view, style, measurer),
    ) {
        readout?.let { (position, level) ->
            JagText(
                "$position · $level",
                Modifier.align(Alignment.TopEnd).padding(JagFxTheme.dimens.spaceMd),
                JagFxTheme.colors.textDim,
            )
        }
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
