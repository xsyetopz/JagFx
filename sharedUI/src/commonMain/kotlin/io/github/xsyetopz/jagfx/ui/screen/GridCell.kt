package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.editor.SlotIntent
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme

/** The cells that draw the filter, and show an add button while the voice has none. */
private val FilterCells =
    setOf(SignalChainSlot.Filter, SignalChainSlot.PoleZero, SignalChainSlot.Bode)

/**
 * One cell of the rack: the [CellHeader] of [slot] over its scope.
 * A press that comes up inside the cell makes [slot] the selected cell (WCAG 2.5.2); leaving the
 * cell before the release cancels it, which the inspector shows; the selected
 * cell has an accent border.
 */
@Composable
internal fun GridCell(
    slot: SignalChainSlot,
    state: EditorState,
    view: ScreenViewState,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    val colors = JagFxTheme.colors
    val dimens = JagFxTheme.dimens
    val selected = slot == state.selectedTarget
    val focus = remember { FocusRequester() }
    LaunchedEffect(selected) {
        if (selected && actions.focusSelectedCell) {
            actions.focusSelectedCell = false
            focus.requestFocus()
        }
    }
    Column(
        modifier
            .onPreviewKeyEvent { event ->
                viewKeyCommandOf(event)?.also { actions.applyViewKey(it, slot, view) } != null
            }.onKeyEvent(actions::handleCellNavigationKey)
            .focusRequester(focus)
            .focusGroup()
            .background(if (selected) colors.cellSelected else colors.cell)
            .border(dimens.border, if (selected) colors.accent else colors.border)
            .selectOnRelease(slot, actions)
            .padding(dimens.border),
    ) {
        CellHeader(slot, selected, view, actions)
        val summary = plotSummary(slot, envelopeOf(state.voice, slot), LocalStrings.current)
        CellScope(
            slot,
            state,
            view,
            actions,
            Modifier.weight(1f).fillMaxWidth().semantics { contentDescription = summary },
        )
        if (cellChromeOf(slot, view.openCell).controlsPanel &&
            slot.isTimeDomain &&
            view.canvas(slot).panelOpen
        ) {
            ControlsPanel(slot, state, view, actions, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

/**
 * The canvas of [slot], or an add button on the scope when the voice lacks what the cell draws:
 * the filter for the filter cells, and the LFO or gate pair for those envelopes.
 */
@Composable
private fun CellScope(
    slot: SignalChainSlot,
    state: EditorState,
    view: ScreenViewState,
    actions: EditorActions,
    modifier: Modifier,
) {
    val voice = state.voice
    val pairFirst = pairFirstOf(slot)
    val promptKey = pairFirst?.let(::emptyPromptKey)
    when {
        voice != null && slot in FilterCells && voice.filter == null -> {
            FilterOffPrompt(actions, modifier)
        }

        voice != null && pairFirst != null && promptKey != null && envelopeOf(
            voice,
            slot,
        ) == null -> {
            EmptyModulePrompt(pairFirst, promptKey, actions, modifier)
        }

        else -> {
            SlotCanvas(slot, state, view, actions, modifier)
        }
    }
}

private fun Offset.isInside(size: IntSize) = x >= 0f && y >= 0f && x < size.width && y < size.height

/** Selects [slot] when a press in the cell comes up inside it; see [PressTracker]. */
private fun Modifier.selectOnRelease(
    slot: SignalChainSlot,
    actions: EditorActions,
): Modifier =
    pointerInput(slot) {
        val press = PressTracker()
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val inside = event.changes.all { it.position.isInside(size) }
                when (event.type) {
                    PointerEventType.Press -> {
                        press.down(inside)
                    }

                    PointerEventType.Move -> {
                        press.move(inside)
                    }

                    PointerEventType.Exit -> {
                        press.cancel()
                    }

                    PointerEventType.Release -> {
                        if (press.up(inside)) {
                            actions.focusSelectedCell = false
                            actions.dispatch(SlotIntent.SelectTarget(slot))
                        }
                    }
                }
            }
        }
    }
