package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import io.github.xsyetopz.jagfx.ui.editor.DocumentIntent
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.HistoryIntent
import io.github.xsyetopz.jagfx.ui.editor.PanelSide
import io.github.xsyetopz.jagfx.ui.editor.RackMode
import io.github.xsyetopz.jagfx.ui.editor.RequestIntent
import io.github.xsyetopz.jagfx.ui.editor.ThemePreference
import io.github.xsyetopz.jagfx.ui.editor.ViewIntent
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.Icons
import io.github.xsyetopz.jagfx.ui.widgets.JagButton
import io.github.xsyetopz.jagfx.ui.widgets.JagIconButton
import io.github.xsyetopz.jagfx.ui.widgets.JagLabel
import io.github.xsyetopz.jagfx.ui.widgets.JagMenu
import io.github.xsyetopz.jagfx.ui.widgets.JagMenuItem
import io.github.xsyetopz.jagfx.ui.widgets.JagText
import io.github.xsyetopz.jagfx.ui.widgets.JagToggleGroup

/** Menu label that puts voice strip on this side. */
internal val PanelSide.label: UiString
    get() = if (this == PanelSide.Left) UiString.PanelOnLeft else UiString.PanelOnRight

/** Menu label of this theme choice. */
internal val ThemePreference.label: UiString
    get() =
        when (this) {
            ThemePreference.System -> UiString.ThemeSystem
            ThemePreference.Classic -> UiString.ThemeClassic
            ThemePreference.Light -> UiString.ThemeLight
            ThemePreference.HighContrast -> UiString.ThemeHighContrast
        }

/** What title bar shows besides file name. */
internal data class TitleBarItems(
    val fileMenu: Boolean,
    val history: Boolean,
)

/** File menu and Undo and Redo, which [hasNativeMenu] platform already has in its menu bar. */
internal fun titleBarItems(hasNativeMenu: Boolean): TitleBarItems =
    TitleBarItems(fileMenu = !hasNativeMenu, history = !hasNativeMenu)

/**
 * Title bar: file name, View switch, and unless platform has menu bar, file menu, Undo,
 * and Redo.
 */
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
        ViewSwitch(state, actions)
        val items = titleBarItems(LocalNativeMenu.current)
        if (items.fileMenu) FileMenu(state, actions)
        if (items.history) {
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
            Triple(UiString.NewLabel, DocumentIntent.NewPatch, true),
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
                JagLabel(
                    strings[UiString.MenuTheme],
                    Modifier.padding(horizontal = JagFxTheme.dimens.spaceLg),
                )
                for (theme in ThemePreference.entries) {
                    JagMenuItem(
                        strings[theme.label],
                        { actions.dispatch(ViewIntent.SetTheme(theme)) },
                        active = state.theme == theme,
                    )
                }
                JagLabel(
                    strings[UiString.MenuTextSize],
                    Modifier.padding(horizontal = JagFxTheme.dimens.spaceLg),
                )
                for (percent in EditorState.TEXT_SCALE_STEPS) {
                    JagMenuItem(
                        "$percent%",
                        { actions.dispatch(ViewIntent.SetTextScale(percent)) },
                        active = state.textScalePercent == percent,
                    )
                }
            }
        }
    }
}

/** The rack view switch: Envelopes, Filter, or All. */
@Composable
private fun ViewSwitch(
    state: EditorState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(JagFxTheme.dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JagLabel(strings[UiString.ModeLabel])
        JagToggleGroup(
            RackMode.entries,
            state.mode,
            { mode -> actions.dispatch(ViewIntent.SetMode(mode)) },
            Modifier.width(IntrinsicSize.Max),
            tooltip = { strings[it.label] },
        ) { mode ->
            JagText(strings[mode.label], style = MaterialTheme.typography.labelSmall)
        }
    }
}
