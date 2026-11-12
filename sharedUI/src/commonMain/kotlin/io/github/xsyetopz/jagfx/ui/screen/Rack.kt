package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.RackMode
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme

/** Envelope rows take this share of rack width in [RackMode.Both]; filter column takes one share. */
private const val ENVELOPE_COLUMNS = 3f

/**
 * Cells of selected voice as in original rack, by [EditorState.mode]: three
 * [RackRows], each with its module strip after base cell; [FilterColumn] with filter
 * strip; or both side by side.
 * When [ScreenViewState.openCell] is set, that cell fills rack on its own.
 * While selected tone is off, rack is dimmed under [ToneOffOverlay] prompt.
 */
@Composable
internal fun Rack(
    state: EditorState,
    view: ScreenViewState,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    ToneOffOverlay(state, actions, modifier) {
        RackBody(state, view, actions, Modifier.fillMaxSize())
    }
}

@Composable
private fun RackBody(
    state: EditorState,
    view: ScreenViewState,
    actions: EditorActions,
    modifier: Modifier,
) {
    val openCell = view.openCell
    if (openCell != null) {
        GridCell(openCell, state, view, actions, modifier)
        return
    }
    val gap = Arrangement.spacedBy(JagFxTheme.dimens.spaceSm)
    Row(modifier, horizontalArrangement = gap) {
        if (state.mode != RackMode.Filter) {
            Column(Modifier.weight(ENVELOPE_COLUMNS).fillMaxHeight(), verticalArrangement = gap) {
                for (row in RackRows) ModuleRow(row, state, view, actions, Modifier.weight(1f))
            }
        }
        if (state.mode != RackMode.Main) {
            FilterStrip(state, view, actions, Modifier.fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = gap) {
                for (slot in FilterColumn) {
                    GridCell(slot, state, view, actions, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

/** One rack row: base cell, its module strip, and two cells of module. */
@Composable
private fun ModuleRow(
    row: List<SignalChainSlot>,
    state: EditorState,
    view: ScreenViewState,
    actions: EditorActions,
    modifier: Modifier,
) {
    val (base, first, second) = row
    val strip = Modifier.fillMaxHeight()
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JagFxTheme.dimens.spaceSm),
    ) {
        GridCell(base, state, view, actions, Modifier.weight(1f).fillMaxSize())
        when (base) {
            SignalChainSlot.Output -> GateStrip(state, actions, strip)
            else -> LfoStrip(first, state, actions, strip)
        }
        GridCell(first, state, view, actions, Modifier.weight(1f).fillMaxSize())
        GridCell(second, state, view, actions, Modifier.weight(1f).fillMaxSize())
    }
}
