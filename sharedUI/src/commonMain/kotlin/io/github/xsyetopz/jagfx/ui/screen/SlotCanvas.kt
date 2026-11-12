package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import io.github.xsyetopz.jagfx.ui.canvas.ENVELOPE_PADDING
import io.github.xsyetopz.jagfx.ui.canvas.EnvelopeCallbacks
import io.github.xsyetopz.jagfx.ui.canvas.EnvelopeCanvas
import io.github.xsyetopz.jagfx.ui.canvas.EnvelopeMenuLabels
import io.github.xsyetopz.jagfx.ui.canvas.EnvelopeView
import io.github.xsyetopz.jagfx.ui.canvas.FrequencyResponseCanvas
import io.github.xsyetopz.jagfx.ui.canvas.OutputWaveCanvas
import io.github.xsyetopz.jagfx.ui.canvas.PoleCallbacks
import io.github.xsyetopz.jagfx.ui.canvas.PoleView
import io.github.xsyetopz.jagfx.ui.canvas.PoleZeroCanvas
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.EnvelopeIntent
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme

/** Canvas of [slot] for selected voice: envelope, pole-zero plot, response, or output wave. */
@Composable
internal fun SlotCanvas(
    slot: SignalChainSlot,
    state: EditorState,
    view: ScreenViewState,
    actions: EditorActions,
    modifier: Modifier,
) {
    val strings = LocalStrings.current
    when (slot) {
        SignalChainSlot.PoleZero -> {
            val summary = poleZeroSummary(strings, state.voice?.filter)
            val selection = poleState(strings, state.voice?.filter, view.selectedPole)
            PoleZeroSlotCanvas(
                state,
                view,
                actions,
                modifier.semantics {
                    contentDescription = summary
                    selection?.let { stateDescription = it }
                },
            )
        }

        SignalChainSlot.Bode -> {
            val summary = responseSummary(strings, state.voice?.filter)
            FrequencyResponseCanvas(
                filter = state.voice?.filter,
                modifier = modifier.semantics { contentDescription = summary },
                panZoom = actions.panZoom(slot, view.canvas(slot)),
            )
        }

        SignalChainSlot.Output -> {
            var width by remember { mutableIntStateOf(0) }
            val summary =
                remember(state.preview.samples) { outputSummary(strings, state.preview.samples) }
            OutputWaveCanvas(
                samples = state.preview.samples,
                playbackPosition = state.playbackPosition,
                modifier =
                    modifier
                        .onSizeChanged { width = it.width }
                        .semantics { contentDescription = summary },
                panZoom = actions.timePanZoom(slot, view, width.toFloat()),
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
    var width by remember { mutableIntStateOf(0) }
    val durationMs = state.voice?.duration?.value ?: 0
    val summary = envelopeSummary(strings, slot, envelope, durationMs)
    val selection = envelopeState(strings, slot, envelope, canvasView.selectedIndex, durationMs)
    val plotWidth = (width - 2 * ENVELOPE_PADDING).coerceAtLeast(0f)
    val panZoom = actions.timePanZoom(slot, view, plotWidth)
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
                onTimeViewChange = { next -> actions.updateTimeView(slot) { next } },
                onSelect = { index ->
                    actions.updateCanvas(slot) { it.copy(selectedIndex = index) }
                },
                onSegmentsChange = { actions.dispatch(EnvelopeIntent.SetSegments(slot, it)) },
                onGestureStart = actions::beginGesture,
                onGestureEnd = actions::endGesture,
            ),
        menuLabels =
            EnvelopeMenuLabels(
                deletePoint = strings[UiString.MenuDeletePoint],
                addPointHere = strings[UiString.MenuAddPointHere],
                addPointAtEnd = strings[UiString.MenuAddPointEnd],
            ),
        modifier =
            modifier
                .onSizeChanged { width = it.width }
                .semantics {
                    contentDescription = summary
                    selection?.let { stateDescription = it }
                },
        style =
            envelopeStyleOf(
                slot,
                cellChromeOf(slot, view.openCell),
                canvasView,
                slotColor(slot, JagFxTheme.palette),
                selected = slot == state.selectedTarget,
                durationMs = durationMs,
            ),
    )
}
