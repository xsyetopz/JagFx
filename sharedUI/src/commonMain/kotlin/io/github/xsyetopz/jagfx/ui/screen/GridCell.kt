package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import io.github.xsyetopz.jagfx.core.model.Waveform
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.EnvelopeIntent
import io.github.xsyetopz.jagfx.ui.editor.FilterIntent
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.editor.SlotIntent
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.theme.SynthPalette
import io.github.xsyetopz.jagfx.ui.widgets.Icons
import io.github.xsyetopz.jagfx.ui.widgets.JagIconButton

/** The cells that draw the filter, and show an add button while the voice has none. */
private val FilterCells =
    setOf(SignalChainSlot.Filter, SignalChainSlot.PoleZero, SignalChainSlot.Bode)

/**
 * One cell of the rack: the [CellHeader] of [slot] over its scope.
 * Any press in the cell makes [slot] the selected cell, which the inspector shows; the selected
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
    Column(
        modifier
            .background(if (selected) colors.cellSelected else colors.cell)
            .border(dimens.border, if (selected) colors.accent else colors.border)
            .pointerInput(slot) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            actions.dispatch(SlotIntent.SelectTarget(slot))
                        }
                    }
                }
            }.padding(dimens.border),
    ) {
        CellHeader(slot, selected, view, actions)
        CellScope(slot, state, view, actions, Modifier.weight(1f).fillMaxWidth())
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
    val strings = LocalStrings.current
    val voice = state.voice
    val pairFirst = pairFirstOf(slot)
    when {
        voice != null && slot in FilterCells && voice.filter == null -> {
            AddButton(strings[UiString.AddFilter], modifier) {
                actions.dispatch(FilterIntent.SetEnabled(true))
            }
        }

        voice != null && pairFirst != null && envelopeOf(voice, slot) == null -> {
            AddButton(strings[UiString.AddEnvelope], modifier) {
                actions.dispatch(EnvelopeIntent.SetWaveform(pairFirst, Waveform.Square))
            }
        }

        else -> {
            SlotCanvas(slot, state, view, actions, modifier)
        }
    }
}

/** An empty scope with a centred add button that runs [onClick]. */
@Composable
private fun AddButton(
    tooltip: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(modifier.background(SynthPalette.Scope), contentAlignment = Alignment.Center) {
        JagIconButton(Icons.Add, tooltip, onClick)
    }
}
