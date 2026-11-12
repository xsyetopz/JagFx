package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.HistoryIntent
import io.github.xsyetopz.jagfx.ui.editor.PanelSide
import io.github.xsyetopz.jagfx.ui.editor.RequestIntent
import io.github.xsyetopz.jagfx.ui.editor.ViewIntent
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.Icons
import io.github.xsyetopz.jagfx.ui.widgets.JagButton
import io.github.xsyetopz.jagfx.ui.widgets.JagIconButton
import io.github.xsyetopz.jagfx.ui.widgets.JagMenu
import io.github.xsyetopz.jagfx.ui.widgets.JagMenuItem
import io.github.xsyetopz.jagfx.ui.widgets.JagText

/** The menu label that puts the voice strip on this side. */
internal val PanelSide.label: UiString
    get() = if (this == PanelSide.Left) UiString.PanelOnLeft else UiString.PanelOnRight

/** The title bar: the file name, the file menu, Undo, and Redo. */
@Composable
internal fun TitleBar(
    state: EditorState,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val dimens = JagFxTheme.dimens
    Row(
        modifier
            .height(dimens.headerHeight)
            .background(JagFxTheme.colors.header)
            .padding(horizontal = dimens.spaceXl),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JagText(state.fileName, Modifier.weight(1f), color = JagFxTheme.colors.textDim)
        FileMenu(state, actions)
        JagIconButton(
            Icons.Undo,
            strings[UiString.ToolbarUndo],
            { actions.dispatch(HistoryIntent.Undo) },
            enabled = state.canUndo,
        )
        JagIconButton(
            Icons.Redo,
            strings[UiString.ToolbarRedo],
            { actions.dispatch(HistoryIntent.Redo) },
            enabled = state.canRedo,
        )
    }
}

@Composable
private fun FileMenu(
    state: EditorState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val otherSide = if (state.panelSide == PanelSide.Left) PanelSide.Right else PanelSide.Left
    val items =
        listOf(
            Triple(UiString.MenuOpen, RequestIntent.Open, true),
            Triple(UiString.MenuSave, RequestIntent.Save, true),
            Triple(UiString.ToolbarOpenSample, RequestIntent.OpenSample, true),
            Triple(UiString.ToolbarSaveAs, RequestIntent.SaveAs, true),
            Triple(UiString.ImportTone, RequestIntent.ImportTone, true),
            Triple(UiString.ExportTone, RequestIntent.ExportTone, state.voice != null),
            Triple(otherSide.label, ViewIntent.SetPanelSide(otherSide), true),
        )
    val menuOffset =
        with(LocalDensity.current) { IntOffset(0, JagFxTheme.dimens.controlHeight.roundToPx()) }
    Box {
        JagButton(strings[UiString.MenuFile], { expanded = true }, active = expanded)
        if (expanded) {
            JagMenu({ expanded = false }, menuOffset) {
                for ((label, intent, enabled) in items) {
                    JagMenuItem(
                        strings[label],
                        {
                            expanded = false
                            actions.dispatch(intent)
                        },
                        enabled = enabled,
                    )
                }
            }
        }
    }
}
