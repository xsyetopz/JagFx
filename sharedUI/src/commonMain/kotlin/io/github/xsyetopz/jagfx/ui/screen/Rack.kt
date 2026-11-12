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

/** The envelope rows take this share of the rack width in [RackMode.Both]; the filter column takes one share. */
private const val ENVELOPE_COLUMNS = 3f

/**
 * The cells of the selected voice as in the original rack, by [EditorState.mode]: the three
 * [RackRows], each with its module strip after the base cell; the [FilterColumn] with the filter
 * strip; or both side by side.
 * When [ScreenViewState.openCell] is set, that cell fills the rack on its own.
 */
@Composable
internal fun Rack(
    state: EditorState,
    view: ScreenViewState,
    actions: EditorActions,
    modifier: Modifier = Modifier,
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

/** One rack row: the base cell, its module strip, and the two cells of the module. */
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
