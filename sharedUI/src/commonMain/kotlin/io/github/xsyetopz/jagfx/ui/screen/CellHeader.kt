package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.Icons
import io.github.xsyetopz.jagfx.ui.widgets.JagButton
import io.github.xsyetopz.jagfx.ui.widgets.JagIconButton
import io.github.xsyetopz.jagfx.ui.widgets.JagLabel
import io.github.xsyetopz.jagfx.ui.widgets.JagMenu
import io.github.xsyetopz.jagfx.ui.widgets.JagMenuItem
import io.github.xsyetopz.jagfx.ui.widgets.Tip

private val ZoomPresets = listOf(1, 2, 4)

/**
 * The title row of the cell of [slot]: its classic name with a description tooltip. The enlarged
 * cell also has the [HeaderControls] and a way back.
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
    val open = view.openCell == slot
    val zoom = if (slot.isTimeDomain) view.timeViewOf(slot).zoom else view.canvas(slot).zoom
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
        Tip(strings[descriptionOf(slot)], Modifier.weight(1f)) {
            JagLabel(classicTitleOf(slot), color = if (selected) colors.text else colors.textDim)
        }
        if (cellChromeOf(slot, view.openCell).headerControls) {
            HeaderControls(slot, zoom, view, actions)
        }
        if (open) {
            JagIconButton(Icons.Close, strings[UiString.Back], {
                actions.updateView { it.copy(openCell = null) }
            })
        }
    }
}

/** The view toggles, `1x 2x 4x S`, `M…` and `▼` of an enlarged cell. */
@Composable
private fun HeaderControls(
    slot: SignalChainSlot,
    zoom: Int,
    view: ScreenViewState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    if (slot.isEnvelope) ViewToggles(slot, view.canvas(slot), actions)
    for (level in ZoomPresets) {
        val label = "${level}x"
        val tooltip = strings.format(UiString.TooltipZoomPreset, level)
        HeaderButton(label, tooltip, zoom == level) { actions.zoomTo(slot, level) }
    }
    HeaderButton("S", strings[UiString.TooltipFitView], false) { actions.fit(slot) }
    if (slot.isTimeDomain) {
        TimeViewButton(slot, view, actions)
        val panelOpen = view.canvas(slot).panelOpen
        HeaderButton("▼", strings[UiString.TooltipControlsPanel], panelOpen) {
            actions.updateCanvas(slot) { it.copy(panelOpen = !panelOpen) }
        }
    }
}

/** `123 # Pt`: the axis values, grid lines, and point readouts of the envelope in [slot]. */
@Composable
private fun ViewToggles(
    slot: SignalChainSlot,
    canvas: CanvasView,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    HeaderButton("123", strings[UiString.TooltipAxisValues], canvas.axisValues) {
        actions.updateCanvas(slot) { it.copy(axisValues = !it.axisValues) }
    }
    HeaderButton("#", strings[UiString.TooltipGridLines], canvas.gridLines) {
        actions.updateCanvas(slot) { it.copy(gridLines = !it.gridLines) }
    }
    HeaderButton("Pt", strings[UiString.TooltipPointReadouts], canvas.pointReadouts) {
        actions.updateCanvas(slot) { it.copy(pointReadouts = !it.pointReadouts) }
    }
}

/** A header button labelled [label] whose tooltip and accessible name is [description]. */
@Composable
private fun HeaderButton(
    label: String,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    JagButton(
        label,
        onClick,
        Modifier.semantics(mergeDescendants = true) { contentDescription = description },
        active = active,
        tooltip = description,
    )
}

/** `M…`: a menu that sets the time view of [slot] to Linked or This cell. */
@Composable
private fun TimeViewButton(
    slot: SignalChainSlot,
    view: ScreenViewState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    var menuOpen by remember { mutableStateOf(false) }
    val below = with(LocalDensity.current) { JagFxTheme.dimens.controlHeight.roundToPx() }
    val linked = view.isLinked(slot)

    fun choose(link: Boolean) {
        actions.updateView {
            val own = it.timeViewOf(slot)
            it.withCanvas(slot, it.canvas(slot).copy(linked = link, timeView = own))
        }
        menuOpen = false
    }
    Box {
        HeaderButton("M…", strings[UiString.TooltipTimeView], menuOpen) { menuOpen = true }
        if (menuOpen) {
            JagMenu({ menuOpen = false }, IntOffset(0, below)) {
                JagMenuItem(strings[UiString.TimeViewLinked], { choose(true) }, active = linked)
                JagMenuItem(strings[UiString.TimeViewThisCell], { choose(false) }, active = !linked)
            }
        }
    }
}
