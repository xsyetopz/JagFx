package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.Icons
import io.github.xsyetopz.jagfx.ui.widgets.JagIconButton
import io.github.xsyetopz.jagfx.ui.widgets.JagLabel

/**
 * The title row of the cell of [slot]: its name, accent when [selected], then fit when zoomed,
 * snap on the cells that edit points, and a way back when the cell is open on its own.
 * A double click on the row opens the cell over the whole rack, or closes it again.
 */
@Composable
internal fun CellHeader(
    slot: SignalChainSlot,
    selected: Boolean,
    view: ScreenViewState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val dimens = JagFxTheme.dimens
    val colors = JagFxTheme.colors
    val canvas = view.canvas(slot)
    val open = view.openCell == slot
    Row(
        Modifier
            .fillMaxWidth()
            .height(dimens.cellHeaderHeight)
            .pointerInput(slot, open) {
                detectTapGestures(onDoubleTap = {
                    actions.updateView { it.copy(openCell = if (open) null else slot) }
                })
            }.padding(horizontal = dimens.spaceMd),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JagLabel(
            strings[titleOf(slot)],
            Modifier.weight(1f),
            if (selected) colors.text else colors.textDim,
        )
        if (canvas.zoom > 1) {
            JagIconButton(Icons.Fit, strings[UiString.ZoomFit], { actions.setZoom(slot, 1) })
        }
        if (slot.isEnvelope || slot == SignalChainSlot.PoleZero) {
            JagIconButton(
                Icons.Snap,
                strings[UiString.TooltipSnapGrid],
                { actions.updateCanvas(slot) { it.copy(snap = !it.snap) } },
                active = canvas.snap,
            )
        }
        if (open) {
            JagIconButton(Icons.Close, strings[UiString.Back], {
                actions.updateView { it.copy(openCell = null) }
            })
        }
    }
}
