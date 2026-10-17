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
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme

/** The envelope columns take this share of the rack width; the filter column takes one share. */
private const val ENVELOPE_COLUMNS = 3f

/**
 * All cells of the selected voice, as in the Scala JagFX rack: the [RackRows] over the output,
 * with the [FilterColumn] on the right.
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
        Column(Modifier.weight(ENVELOPE_COLUMNS).fillMaxHeight(), verticalArrangement = gap) {
            for (row in RackRows) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = gap) {
                    for (slot in row) {
                        GridCell(slot, state, view, actions, Modifier.weight(1f).fillMaxSize())
                    }
                }
            }
            GridCell(
                SignalChainSlot.Output,
                state,
                view,
                actions,
                Modifier.weight(1f).fillMaxWidth(),
            )
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = gap) {
            for (slot in FilterColumn) {
                GridCell(slot, state, view, actions, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}
