package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.xsyetopz.jagfx.ui.canvas.EnvelopeCallbacks
import io.github.xsyetopz.jagfx.ui.canvas.EnvelopeCanvas
import io.github.xsyetopz.jagfx.ui.canvas.EnvelopeMenuLabels
import io.github.xsyetopz.jagfx.ui.canvas.EnvelopeStyle
import io.github.xsyetopz.jagfx.ui.canvas.EnvelopeView
import io.github.xsyetopz.jagfx.ui.canvas.FrequencyResponseCanvas
import io.github.xsyetopz.jagfx.ui.canvas.OutputWaveCanvas
import io.github.xsyetopz.jagfx.ui.canvas.PoleCallbacks
import io.github.xsyetopz.jagfx.ui.canvas.PoleView
import io.github.xsyetopz.jagfx.ui.canvas.PoleZeroCanvas
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.EnvelopeIntent
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.editor.SlotIntent
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString

/** The canvas of [slot] for the selected voice: an envelope, the pole-zero plot, the response, or the output wave. */
@Composable
internal fun SlotCanvas(
    slot: SignalChainSlot,
    state: EditorState,
    view: ScreenViewState,
    actions: EditorActions,
    modifier: Modifier,
) {
    when (slot) {
        SignalChainSlot.PoleZero -> {
            PoleZeroSlotCanvas(state, view, actions, modifier)
        }

        SignalChainSlot.Bode -> {
            FrequencyResponseCanvas(
                filter = state.voice?.filter,
                modifier = modifier,
                panZoom = actions.panZoom(slot, view.canvas(slot)),
            )
        }

        SignalChainSlot.Output -> {
            OutputWaveCanvas(
                samples = state.preview.samples,
                playbackPosition = state.playbackPosition,
                modifier = modifier,
                panZoom = actions.panZoom(slot, view.canvas(slot)),
            )
        }

        else -> {
            EnvelopeSlotCanvas(slot, state, view, actions, modifier)
        }
    }
}

@Composable
private fun PoleZeroSlotCanvas(
    state: EditorState,
    view: ScreenViewState,
    actions: EditorActions,
    modifier: Modifier,
) {
    val slot = SignalChainSlot.PoleZero
    PoleZeroCanvas(
        filter = state.voice?.filter,
        modifier = modifier,
        view = PoleView(snapEnabled = view.canvas(slot).snap, selected = view.selectedPole),
        callbacks =
            PoleCallbacks(
                onSelect = { ref -> actions.updateView { it.copy(selectedPole = ref) } },
                onCoefficientChange = {
                    ref,
                    phase,
                    magnitude,
                    ->
                    actions.setCoefficient(ref, phase, magnitude)
                },
                gestures = actions.gestureCallbacks(),
            ),
        panZoom = actions.panZoom(slot, view.canvas(slot)),
    )
}

@Composable
private fun EnvelopeSlotCanvas(
    slot: SignalChainSlot,
    state: EditorState,
    view: ScreenViewState,
    actions: EditorActions,
    modifier: Modifier,
) {
    val strings = LocalStrings.current
    val envelope = envelopeOf(state.voice, slot)
    val canvasView = view.canvas(slot)
    val panZoom = actions.panZoom(slot, canvasView)
    EnvelopeCanvas(
        view =
            EnvelopeView(
                envelope,
                panZoom.viewport.zoom,
                panZoom.viewport.scrollOffset,
                canvasView.selectedIndex,
                snapEnabled = canvasView.snap,
            ),
        callbacks =
            EnvelopeCallbacks(
                onZoomChange = panZoom.onZoomChange,
                onScrollOffsetChange = panZoom.onScrollOffsetChange,
                onSelect = { index ->
                    actions.updateCanvas(slot) { it.copy(selectedIndex = index) }
                },
                onSegmentsChange = { actions.dispatch(EnvelopeIntent.SetSegments(slot, it)) },
                onGestureStart = {
                    actions.dispatch(SlotIntent.SelectTarget(slot))
                    actions.beginGesture()
                },
                onGestureEnd = actions::endGesture,
            ),
        menuLabels =
            EnvelopeMenuLabels(
                deletePoint = strings[UiString.MenuDeletePoint],
                addPointHere = strings[UiString.MenuAddPointHere],
                addPointAtEnd = strings[UiString.MenuAddPointEnd],
            ),
        modifier = modifier,
        style =
            EnvelopeStyle(
                lineColor = slotColor(slot),
                selected = slot == state.selectedTarget,
                durationMs = state.voice?.duration?.value ?: 0,
            ),
    )
}
